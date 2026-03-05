/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.operation;

import com.mulesoft.connectors.b360.api.GenericRequestResponseAttributes;
import com.mulesoft.connectors.b360.api.HttpMethod;
import com.mulesoft.connectors.b360.internal.config.B360Configuration;
import com.mulesoft.connectors.b360.internal.connection.B360Connection;
import com.mulesoft.connectors.b360.internal.error.B360ErrorProvider;
import com.mulesoft.connectors.b360.internal.error.B360ErrorType;
import com.mulesoft.connectors.b360.internal.operation.utils.B360Utils;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.http.api.HttpConstants;
import org.mule.runtime.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic HTTP request operation that reuses the connector's authenticated session.
 * Acts as an API client (like Postman) — the user chooses the HTTP method, path, headers,
 * query parameters, and body. The connector appends the IDS-SESSION-ID automatically.
 * <p>
 * This ensures the connector remains useful even when Informatica adds new API resources
 * before the connector ships a dedicated operation for them.
 */
public class GenericRequestOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericRequestOperation.class);

    @DisplayName("INFA MDM - Generic Request")
    @Summary("Send a generic HTTP request to any Informatica B360 / MDM API endpoint, reusing the connector's authenticated session."
            + "<ul>"
            + "<li> Choose the HTTP method (GET, POST, PUT, PATCH, DELETE)"
            + "<li> Specify the relative path (appended to the B360 MDM base URL)"
            + "<li> Optionally add query parameters, custom headers, and a JSON request body"
            + "<li> The IDS-SESSION-ID header is added automatically"
            + "<li> Returns the raw response body (InputStream) with HTTP status, headers, and request ID as attributes"
            + "</ul>")
    @MediaType(value = "application/json", strict = false)
    @Throws(B360ErrorProvider.class)
    public void genericRequest(
            @Config B360Configuration config,
            @Connection B360Connection connection,
            @DisplayName("HTTP Method")
            @Summary("HTTP method for the request.")
            HttpMethod method,
            @DisplayName("Path")
            @Summary("Relative path appended to the B360 MDM base URL (e.g. /business-entity/public/api/v1/entity/Person)."
                    + " Must start with /.")
            String path,
            @Optional
            @NullSafe
            @DisplayName("Query Parameters")
            @Summary("Query parameters to append to the URL (e.g. sourceSystem=CRM, _showContentMeta=true).")
            Map<String, String> queryParameters,
            @Optional
            @NullSafe
            @DisplayName("Custom Headers")
            @Summary("Additional HTTP headers to include in the request. The IDS-SESSION-ID and Accept headers are added automatically.")
            Map<String, String> customHeaders,
            @Optional
            @Content
            @DisplayName("Request Body")
            @Summary("Optional JSON request body. Accepts an InputStream (raw JSON), a String, or a Java object (Map/List) which is serialized to JSON.")
            Object body,
            CompletionCallback<InputStream, GenericRequestResponseAttributes> callback) {

        if (path == null || path.trim().isEmpty()) {
            throw new ModuleException("Path is required and must not be empty.", B360ErrorType.CLIENT_ERROR);
        }
        String normalizedPath = path.trim();
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        String fullUri = connection.getBaseApiUrl() + normalizedPath;

        HttpRequestBuilder builder = HttpRequest.builder()
                .uri(fullUri)
                .method(toMuleMethod(method))
                .addHeader("Accept", "application/json");

        for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isEmpty()) {
                builder.addQueryParam(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
            }
        }

        for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isEmpty()) {
                builder.addHeader(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
            }
        }

        if (body != null) {
            byte[] bodyBytes = toJsonBytes(body);
            builder.addHeader("Content-Type", "application/json");
            builder.entity(new InputStreamHttpEntity(new ByteArrayInputStream(bodyBytes)));
        }

        LOGGER.info("INFA MDM Generic Request: {} {}", method, fullUri);

        connection.executeAsyncRaw(builder)
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        B360ErrorType errorType = isTimeout(throwable) ? B360ErrorType.TIMEOUT : B360ErrorType.CONNECTIVITY;
                        callback.error(new ModuleException(
                                "Generic request failed: " + (throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getSimpleName()),
                                errorType, throwable));
                        return;
                    }
                    try {
                        InputStream responseBody = response.getEntity() != null ? response.getEntity().getContent() : null;
                        GenericRequestResponseAttributes attrs = buildAttributes(response);
                        callback.success(Result.<InputStream, GenericRequestResponseAttributes>builder()
                                .output(responseBody)
                                .attributes(attrs)
                                .build());
                    } catch (Exception e) {
                        callback.error(e);
                    }
                });
    }

    private static GenericRequestResponseAttributes buildAttributes(HttpResponse response) {
        int statusCode = response.getStatusCode();
        String reasonPhrase = response.getReasonPhrase();
        String requestId = B360Connection.getRequestIdFromResponse(response);
        Map<String, String> headers = new LinkedHashMap<>();
        try {
            if (response.getHeaderNames() != null) {
                for (String name : response.getHeaderNames()) {
                    headers.put(name, response.getHeaderValue(name));
                }
            }
        } catch (Exception ignored) {
        }
        return new GenericRequestResponseAttributes(statusCode, reasonPhrase, requestId, headers);
    }

    private static HttpConstants.Method toMuleMethod(HttpMethod method) {
        switch (method) {
            case GET:    return HttpConstants.Method.GET;
            case POST:   return HttpConstants.Method.POST;
            case PUT:    return HttpConstants.Method.PUT;
            case PATCH:  return HttpConstants.Method.PATCH;
            case DELETE: return HttpConstants.Method.DELETE;
            default:     return HttpConstants.Method.GET;
        }
    }

    private static byte[] toJsonBytes(Object body) {
        if (body instanceof InputStream) {
            byte[] raw = B360Utils.readFully((InputStream) body);
            return raw.length == 0 ? null : raw;
        }
        if (body instanceof String) {
            String s = ((String) body).trim();
            return s.isEmpty() ? null : s.getBytes(StandardCharsets.UTF_8);
        }
        try {
            String json = B360Utils.OBJECT_MAPPER.writeValueAsString(body);
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ModuleException("Request body could not be serialized to JSON.", B360ErrorType.CLIENT_ERROR, e);
        }
    }

    private static boolean isTimeout(Throwable t) {
        if (t == null) return false;
        if (t instanceof SocketTimeoutException) return true;
        String msg = t.getMessage();
        if (msg != null) {
            String m = msg.toLowerCase();
            if (m.contains("timeout") || m.contains("timed out")) return true;
        }
        return isTimeout(t.getCause());
    }
}

/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.operation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.mulesoft.connectors.b360.api.SourceSubmitResponseAttributes;
import com.mulesoft.connectors.b360.internal.config.B360Configuration;
import com.mulesoft.connectors.b360.internal.connection.B360Connection;
import com.mulesoft.connectors.b360.internal.connection.B360HttpResponseContext;
import com.mulesoft.connectors.b360.internal.model.sourcesubmit.SourceSubmitParameterGroup;
import com.mulesoft.connectors.b360.internal.metadata.SourceSubmitInputTypeResolver;
import com.mulesoft.connectors.b360.internal.metadata.SourceSubmitOutputMetadataResolver;
import com.mulesoft.connectors.b360.internal.operation.utils.B360Endpoints;
import com.mulesoft.connectors.b360.internal.operation.utils.B360Utils;
import com.mulesoft.connectors.b360.internal.error.B360ErrorProvider;
import com.mulesoft.connectors.b360.internal.error.B360ErrorType;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.http.api.HttpConstants;
import org.mule.runtime.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;

/**
 * Source Submit: submit source system data to create or update a master record via the
 * Business 360 Create Master Record API.
 * POST /business-entity/public/api/v1/entity/{businessEntity}?sourceSystem={sourceSystem}[&amp;sourcePKey=...][&amp;_resolveCrosswalk=...]
 * <p>
 * The MDM engine decides whether to create a new master, merge with an existing one, or hold
 * for approval. The response indicates the business ID and whether approval is required.
 *
 * @see <a href="https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Business_entity_record_APIs.html">Business entity record APIs</a>
 */
public class SourceSubmitOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceSubmitOperation.class);

    @DisplayName("INFA MDM - Source Submit")
    @Summary("Submit source system data to create or update a master record. The MDM engine runs match/merge and may require approval." +
            "<ul>" +
            "<li> Uses the Business 360 Create Master Record API (POST " + B360Endpoints.BUSINESS_ENTITY + "/{businessEntity}?sourceSystem=...)" +
            "<li> The request body is the record payload (JSON with field data, optionally _contentMeta)" +
            "<li> Response contains businessId and approvalRequired" +
            "<li> <a href=\"https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Business_entity_record_APIs.html\">Business entity record APIs docs</a>" +
            "</ul>")
    @MediaType(value = "application/json", strict = false)
    @OutputResolver(output = SourceSubmitOutputMetadataResolver.class, attributes = SourceSubmitOutputMetadataResolver.class)
    @Throws(B360ErrorProvider.class)
    public void sourceSubmit(
            @Config B360Configuration config,
            @Connection B360Connection connection,
            @ParameterGroup(name = "Source Record")
            @DisplayName("Source Record")
            @Summary("Business Entity Id (dropdown), Source System (dropdown), optional Source Primary Key, and optional custom Business ID.")
            SourceSubmitParameterGroup sourceRecord,
            @Content
            @TypeResolver(SourceSubmitInputTypeResolver.class)
            @DisplayName("Record Payload")
            @Summary("Record data as JSON or a Java object (Map/List). If Java is provided, it is serialized to JSON before sending. Field data (e.g. firstName, PostalAddress[], _contentMeta) must match the selected Business Entity.")
            Object body,
            @Optional(defaultValue = "false")
            @DisplayName("Resolve Crosswalk")
            @Summary("Indicates whether to standardize picklist values based on the source system configuration. Set to true to standardize. Default is false.")
            @Placement(tab = ADVANCED_TAB, order = 1)
            boolean resolveCrosswalk,
            CompletionCallback<Map<String, Object>, SourceSubmitResponseAttributes> callback) {

        String businessEntity = sourceRecord != null ? sourceRecord.getBusinessEntity() : null;
        String sourceSystem = sourceRecord != null ? sourceRecord.getSourceSystem() : null;
        String sourcePKey = sourceRecord != null ? sourceRecord.getSourcePKey() : null;
        String businessId = sourceRecord != null ? sourceRecord.getBusinessId() : null;

        byte[] bodyBytes = toJsonBytes(body);
        String path = buildPath(connection.getBaseApiUrl(), businessEntity);
        LOGGER.info("INFA MDM Source Submit request: {}", path);

        HttpRequestBuilder builder = HttpRequest.builder()
                .uri(path)
                .method(HttpConstants.Method.POST)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .entity(new InputStreamHttpEntity(new ByteArrayInputStream(bodyBytes)));
        builder.addQueryParam("sourceSystem", sourceSystem != null ? sourceSystem : "");
        if (sourcePKey != null && !sourcePKey.isEmpty()) builder.addQueryParam("sourcePKey", sourcePKey);
        if (businessId != null && !businessId.isEmpty()) builder.addQueryParam("businessId", businessId);
        if (resolveCrosswalk) builder.addQueryParam("_resolveCrosswalk", "true");

        connection.executeAsync(builder, new CompletionCallback<InputStream, B360HttpResponseContext>() {
            @Override
            public void success(Result<InputStream, B360HttpResponseContext> asyncResult) {
                try {
                    InputStream responseStream = asyncResult.getOutput();
                    B360HttpResponseContext httpContext = asyncResult.getAttributes().orElse(new B360HttpResponseContext(200, null));
                    JsonNode root = B360Utils.OBJECT_MAPPER.readTree(responseStream);
                    SourceSubmitResponseAttributes attrs = parseResponseAttributes(root, httpContext.getStatusCode(), httpContext.getRequestId());
                    Map<String, Object> output = B360Utils.OBJECT_MAPPER.convertValue(root, new TypeReference<Map<String, Object>>() {});
                    callback.success(Result.<Map<String, Object>, SourceSubmitResponseAttributes>builder()
                            .output(output)
                            .attributes(attrs)
                            .mediaType(org.mule.runtime.api.metadata.MediaType.APPLICATION_JAVA)
                            .build());
                } catch (Exception e) {
                    callback.error(e);
                }
            }

            @Override
            public void error(Throwable t) {
                callback.error(t);
            }
        });
    }

    /**
     * Normalizes the Record Payload to JSON bytes for the API. Accepts InputStream (raw JSON) or
     * Java objects (Map, List, etc.); Java is serialized to JSON so the API always receives valid JSON.
     */
    private static byte[] toJsonBytes(Object body) {
        if (body == null) {
            return "{}".getBytes(StandardCharsets.UTF_8);
        }
        if (body instanceof InputStream) {
            byte[] raw = B360Utils.readFully((InputStream) body);
            if (raw.length == 0) {
                return "{}".getBytes(StandardCharsets.UTF_8);
            }
            try {
                B360Utils.OBJECT_MAPPER.readTree(raw);
                return raw;
            } catch (Exception e) {
                throw new ModuleException(
                        "Record Payload InputStream did not contain valid JSON. Use a JSON stream or a Java object (Map/List) so the connector can send JSON to the API.",
                        B360ErrorType.CLIENT_ERROR,
                        e);
            }
        }
        try {
            String json = B360Utils.OBJECT_MAPPER.writeValueAsString(body);
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ModuleException("Record Payload could not be serialized to JSON.", B360ErrorType.CLIENT_ERROR, e);
        }
    }

    private static String buildPath(String baseApiUrl, String businessEntity) {
        StringBuilder sb = new StringBuilder(baseApiUrl);
        sb.append(B360Endpoints.BUSINESS_ENTITY);
        sb.append('/').append(B360Utils.encodePathSegment(businessEntity));
        return sb.toString();
    }

    private static SourceSubmitResponseAttributes parseResponseAttributes(JsonNode root, int statusCode, String requestId) {
        String businessId = "";
        boolean approvalRequired = false;
        try {
            businessId = root.path("businessId").asText("");
            if (businessId.isEmpty()) {
                businessId = root.path("id").asText("");
            }
            approvalRequired = root.path("approvalRequired").asBoolean(false);
        } catch (Exception ignored) {
        }
        return new SourceSubmitResponseAttributes(businessId, approvalRequired, statusCode, requestId);
    }
}

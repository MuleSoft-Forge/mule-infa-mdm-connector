/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.connection;

import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.api.i18n.I18nMessageFactory;

import com.mulesoft.connectors.b360.api.B360ApiError;
import com.mulesoft.connectors.b360.internal.operation.utils.B360Utils;
import com.mulesoft.connectors.b360.internal.error.B360ErrorType;

import java.io.InputStream;
import java.net.SocketTimeoutException;

/**
 * Encapsulates the B360 session and HTTP client. Operations must not access the HttpClient directly.
 */
public final class B360Connection {

    private static final String IDS_SESSION_ID_HEADER = "IDS-SESSION-ID";

    private final String sessionId;
    private final String baseApiUrl;
    private final HttpClient httpClient;
    private final boolean bypassMetadataCache;

    public B360Connection(String sessionId, String baseApiUrl, HttpClient httpClient, boolean bypassMetadataCache) {
        this.sessionId = sessionId;
        this.baseApiUrl = baseApiUrl;
        this.httpClient = httpClient;
        this.bypassMetadataCache = bypassMetadataCache;
    }

    public String getBaseApiUrl() {
        return baseApiUrl;
    }

    public String getSessionId() {
        return sessionId;
    }

    public boolean isBypassMetadataCache() {
        return bypassMetadataCache;
    }

    /**
     * Returns true if the session token is a JWT that expires within the given buffer (seconds).
     * Used to refresh the connection at least 5 minutes before JWT expiry per Informatica recommendation.
     * For non-JWT session IDs this returns false (session expiry is handled by server/validation failure).
     */
    public boolean shouldRefreshSession(long bufferSeconds) {
        long expSeconds = B360JwtExpirationHelper.getExpirationTimeSeconds(sessionId);
        if (expSeconds <= 0) {
            return false;
        }
        long nowSeconds = System.currentTimeMillis() / 1000;
        return (expSeconds - nowSeconds) <= bufferSeconds;
    }

    /**
     * Executes an HTTP request asynchronously, adding the B360 session header. Completes the callback with the response body stream and HTTP context (status code, request ID) or error.
     */
    public void executeAsync(HttpRequestBuilder requestBuilder, CompletionCallback<InputStream, B360HttpResponseContext> callback) {
        requestBuilder.addHeader(IDS_SESSION_ID_HEADER, sessionId);
        HttpRequest request = requestBuilder.build();
        String requestUri = request.getUri() != null ? request.getUri().toString() : null;
        httpClient.sendAsync(request)
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        callback.error(enrichWithUri(throwable, requestUri));
                        return;
                    }
                    if (response.getStatusCode() >= 300) {
                        String responseBody = B360Utils.readStreamAsString(response.getEntity() != null ? response.getEntity().getContent() : null);
                        B360ApiError apiError = B360ErrorParser.parseFromResponseBody(responseBody);
                        String fullMessage = buildErrorMessage(response.getStatusCode(), response.getReasonPhrase(), apiError, requestUri);
                        B360ErrorType errorType = response.getStatusCode() >= 500 ? B360ErrorType.SERVER_ERROR
                                : response.getStatusCode() >= 400 ? B360ErrorType.CLIENT_ERROR : B360ErrorType.CONNECTIVITY;
                        callback.error(new B360ConnectionException(fullMessage, apiError, errorType));
                        return;
                    }
                    InputStream body = response.getEntity() != null ? response.getEntity().getContent() : null;
                    String requestId = getRequestIdFromResponse(response);
                    callback.success(Result.<InputStream, B360HttpResponseContext>builder()
                            .output(body)
                            .attributes(new B360HttpResponseContext(response.getStatusCode(), requestId))
                            .build());
                });
    }

    /** Extracts request/tracing ID from response headers (INFA-REQUEST-ID, X-Request-Id, Tracking-Id). */
    private static String getRequestIdFromResponse(HttpResponse response) {
        if (response == null) return null;
        String v = getHeaderValueIgnoreCase(response, "INFA-REQUEST-ID");
        if (v != null && !v.isEmpty()) return v;
        v = getHeaderValueIgnoreCase(response, "X-Request-Id");
        if (v != null && !v.isEmpty()) return v;
        v = getHeaderValueIgnoreCase(response, "Tracking-Id");
        if (v != null && !v.isEmpty()) return v;
        return null;
    }

    private static String getHeaderValueIgnoreCase(HttpResponse response, String name) {
        try {
            if (response.getHeaderNames() != null) {
                for (String n : response.getHeaderNames()) {
                    if (name.equalsIgnoreCase(n)) {
                        return response.getHeaderValue(n);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Executes an HTTP request synchronously for use by PagingProvider. Blocks until the response is received.
     * Do not use for non-paged operations; use executeAsync instead. Timeout is governed by the HTTP client configuration.
     *
     * @param requestBuilder the request to execute (session header is added by this method)
     * @return response body stream
     */
    public InputStream executeSync(HttpRequestBuilder requestBuilder) {
        requestBuilder.addHeader(IDS_SESSION_ID_HEADER, sessionId);
        HttpRequest request = requestBuilder.build();
        String requestUri = request.getUri() != null ? request.getUri().toString() : null;
        try {
            HttpResponse response = httpClient.send(request);
            if (response.getStatusCode() >= 300) {
                String responseBody = B360Utils.readStreamAsString(response.getEntity() != null ? response.getEntity().getContent() : null);
                B360ApiError apiError = B360ErrorParser.parseFromResponseBody(responseBody);
                String fullMessage = buildErrorMessage(response.getStatusCode(), response.getReasonPhrase(), apiError, requestUri);
                B360ErrorType errorType = response.getStatusCode() >= 500 ? B360ErrorType.SERVER_ERROR
                        : response.getStatusCode() >= 400 ? B360ErrorType.CLIENT_ERROR : B360ErrorType.CONNECTIVITY;
                throw new B360ConnectionException(fullMessage, apiError, errorType);
            }
            return response.getEntity() != null ? response.getEntity().getContent() : null;
        } catch (Exception e) {
            if (e instanceof B360ConnectionException) {
                throw (B360ConnectionException) e;
            }
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            B360ErrorType errorType = isTimeout(e) ? B360ErrorType.TIMEOUT : B360ErrorType.CONNECTIVITY;
            throw new B360ConnectionException(appendRequestUri(msg, requestUri), null, errorType, e);
        }
    }

    public void invalidate() {
        // Connection state only; do not stop the HttpClient (provider's stop() does that).
    }

    private static String appendRequestUri(String message, String requestUri) {
        if (requestUri == null || requestUri.isEmpty()) {
            return message;
        }
        return message + " Request URL: " + requestUri;
    }

    private static String buildErrorMessage(int statusCode, String reasonPhrase, B360ApiError apiError, String requestUri) {
        String fullMessage = "HTTP " + statusCode + " " + reasonPhrase;
        if (statusCode == 401) {
            fullMessage += ". Authentication failed. Verify your Login URL and credentials in the B360 Global Configuration.";
        } else if (statusCode == 403) {
            fullMessage += ". Access forbidden. Verify your B360 credentials and that your user has access to this resource.";
        }
        if (apiError != null) {
            fullMessage += " " + apiError.toFullMessage();
        }
        return appendRequestUri(fullMessage, requestUri);
    }

    private static Throwable enrichWithUri(Throwable t, String requestUri) {
        if (requestUri == null || requestUri.isEmpty()) {
            return t;
        }
        String enriched = appendRequestUri(t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName(), requestUri);
        return new B360ConnectionException(enriched, null, isTimeout(t) ? B360ErrorType.TIMEOUT : B360ErrorType.CONNECTIVITY, t);
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

    /**
     * Thrown when the B360 API returns an error response. The message always includes the full B360 error object when present.
     * For HTTP errors and connection failures, the message includes the request URL for easier debugging.
     */
    public static final class B360ConnectionException extends ModuleException {

        private final B360ApiError apiError;

        public B360ConnectionException(String message, B360ErrorType errorType) {
            super(I18nMessageFactory.createStaticMessage(message), errorType, null);
            this.apiError = null;
        }

        public B360ConnectionException(String message, B360ApiError apiError, B360ErrorType errorType) {
            super(I18nMessageFactory.createStaticMessage(message), errorType, null);
            this.apiError = apiError;
        }

        public B360ConnectionException(String message, B360ApiError apiError, B360ErrorType errorType, Throwable cause) {
            super(I18nMessageFactory.createStaticMessage(message), errorType, cause);
            this.apiError = apiError;
        }

        public B360ApiError getApiError() {
            return apiError;
        }
    }
}

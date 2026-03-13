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
import org.mule.runtime.api.connection.ConnectionException;

import com.mulesoft.connectors.b360.api.B360ApiError;
import com.mulesoft.connectors.b360.internal.operation.utils.B360Utils;
import com.mulesoft.connectors.b360.internal.error.B360ErrorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Encapsulates the B360 session and HTTP client. Operations must not access the HttpClient directly.
 * <p>
 * On HTTP 401, the connection attempts a single transparent re-authentication via the
 * {@link SessionRefresher} callback (supplied by the connection provider) and replays the
 * request. If no refresher is available (Passthrough) or the retry also returns 401, the
 * error propagates as a {@link ConnectionException}.
 */
public final class B360Connection {

    private static final Logger LOGGER = LoggerFactory.getLogger(B360Connection.class);
    private static final String IDS_SESSION_ID_HEADER = "IDS-SESSION-ID";

    private volatile String sessionId;
    private final String baseApiUrl;
    private final HttpClient httpClient;
    private final boolean bypassMetadataCache;
    private final SessionRefresher sessionRefresher;

    /**
     * Callback that performs re-authentication and returns a fresh session ID.
     * Supplied by the connection provider; null for Passthrough (no refresh possible).
     * Use {@link #refreshAsync()} from async paths to avoid blocking the HTTP worker thread.
     */
    @FunctionalInterface
    public interface SessionRefresher {
        String refresh() throws ConnectionException;

        /**
         * Async re-authentication. Default runs {@link #refresh()} on another thread.
         * Override to use sendAsync so no thread is blocked (recommended for connection providers).
         */
        default CompletableFuture<String> refreshAsync() {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return refresh();
                } catch (ConnectionException e) {
                    throw new CompletionException(e);
                }
            });
        }
    }

    public B360Connection(String sessionId, String baseApiUrl, HttpClient httpClient,
                          boolean bypassMetadataCache, SessionRefresher sessionRefresher) {
        this.sessionId = sessionId;
        this.baseApiUrl = baseApiUrl;
        this.httpClient = httpClient;
        this.bypassMetadataCache = bypassMetadataCache;
        this.sessionRefresher = sessionRefresher;
    }

    public B360Connection(String sessionId, String baseApiUrl, HttpClient httpClient, boolean bypassMetadataCache) {
        this(sessionId, baseApiUrl, httpClient, bypassMetadataCache, null);
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
     * Executes an HTTP request asynchronously, adding the B360 session header. On 401,
     * transparently re-authenticates once and replays the request. Completes the callback
     * with the response body stream and HTTP context or error.
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
                    if (response.getStatusCode() == 401 && sessionRefresher != null) {
                        LOGGER.info("HTTP 401 on {}; attempting re-authentication.", requestUri);
                        consumeBody(response);
                        sessionRefresher.refreshAsync()
                                .thenCompose(newSession -> {
                                    sessionId = newSession;
                                    return httpClient.sendAsync(rebuildRequestWithNewSession(request));
                                })
                                .whenComplete((retryResponse, retryEx) -> {
                                    if (retryEx != null) {
                                        Throwable cause = retryEx instanceof CompletionException ? retryEx.getCause() : retryEx;
                                        callback.error(cause instanceof ConnectionException ? cause : enrichWithUri(retryEx, requestUri));
                                        return;
                                    }
                                    completeAsyncCallback(retryResponse, requestUri, callback);
                                });
                        return;
                    }
                    completeAsyncCallback(response, requestUri, callback);
                });
    }

    private void completeAsyncCallback(HttpResponse response, String requestUri,
                                       CompletionCallback<InputStream, B360HttpResponseContext> callback) {
        if (response.getStatusCode() >= 300) {
            String responseBody = B360Utils.readStreamAsString(response.getEntity() != null ? response.getEntity().getContent() : null);
            B360ApiError apiError = B360ErrorParser.parseFromResponseBody(responseBody);
            String fullMessage = buildErrorMessage(response.getStatusCode(), response.getReasonPhrase(), apiError, requestUri);
            if (response.getStatusCode() == 401) {
                callback.error(new ConnectionException(fullMessage));
                return;
            }
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
    }

    /**
     * Executes an HTTP request asynchronously, adding the B360 session header, and returns the raw
     * {@link HttpResponse}. On 401, transparently re-authenticates once and replays the request.
     * Unlike {@link #executeAsync}, non-2xx responses (except 401 after failed retry) are returned
     * as-is — the caller is responsible for interpreting the status code.
     */
    public CompletableFuture<HttpResponse> executeAsyncRaw(HttpRequestBuilder requestBuilder) {
        requestBuilder.addHeader(IDS_SESSION_ID_HEADER, sessionId);
        HttpRequest request = requestBuilder.build();
        CompletableFuture<HttpResponse> future = httpClient.sendAsync(request);
        if (sessionRefresher == null) {
            return future;
        }
        return future.thenCompose(response -> {
            if (response.getStatusCode() != 401) {
                return CompletableFuture.completedFuture(response);
            }
            String requestUri = request.getUri() != null ? request.getUri().toString() : null;
            LOGGER.info("HTTP 401 on {}; attempting re-authentication.", requestUri);
            consumeBody(response);
            return sessionRefresher.refreshAsync()
                    .thenCompose(newSession -> {
                        sessionId = newSession;
                        return httpClient.sendAsync(rebuildRequestWithNewSession(request));
                    });
        });
    }

    /** Extracts request/tracing ID from response headers (INFA-REQUEST-ID, X-Request-Id, Tracking-Id). */
    public static String getRequestIdFromResponse(HttpResponse response) {
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
     * Executes an HTTP request synchronously. On 401, transparently re-authenticates once
     * and replays the request. Used by PagingProvider, metadata resolvers, and value providers.
     */
    public InputStream executeSync(HttpRequestBuilder requestBuilder) {
        requestBuilder.addHeader(IDS_SESSION_ID_HEADER, sessionId);
        HttpRequest request = requestBuilder.build();
        String requestUri = request.getUri() != null ? request.getUri().toString() : null;
        try {
            HttpResponse response = httpClient.send(request);
            if (response.getStatusCode() == 401 && sessionRefresher != null) {
                LOGGER.info("HTTP 401 on {}; attempting re-authentication.", requestUri);
                consumeBody(response);
                try {
                    String newSession = sessionRefresher.refresh();
                    sessionId = newSession;
                } catch (Exception refreshEx) {
                    LOGGER.warn("Re-authentication failed: {}", refreshEx.getMessage());
                    String fullMessage = buildErrorMessage(401, response.getReasonPhrase(), null, requestUri)
                            + " Re-authentication also failed: " + refreshEx.getMessage();
                    throw new B360ConnectionException(fullMessage, B360ErrorType.CONNECTIVITY);
                }
                HttpRequest retryRequest = rebuildRequestWithNewSession(request);
                HttpResponse retryResponse = httpClient.send(retryRequest);
                return handleSyncResponse(retryResponse, requestUri);
            }
            return handleSyncResponse(response, requestUri);
        } catch (B360ConnectionException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            B360ErrorType errorType = isTimeout(e) ? B360ErrorType.TIMEOUT : B360ErrorType.CONNECTIVITY;
            throw new B360ConnectionException(appendRequestUri(msg, requestUri), null, errorType, e);
        }
    }

    private InputStream handleSyncResponse(HttpResponse response, String requestUri) {
        if (response.getStatusCode() >= 300) {
            String responseBody = B360Utils.readStreamAsString(response.getEntity() != null ? response.getEntity().getContent() : null);
            B360ApiError apiError = B360ErrorParser.parseFromResponseBody(responseBody);
            String fullMessage = buildErrorMessage(response.getStatusCode(), response.getReasonPhrase(), apiError, requestUri);
            B360ErrorType errorType = response.getStatusCode() >= 500 ? B360ErrorType.SERVER_ERROR
                    : response.getStatusCode() >= 400 ? B360ErrorType.CLIENT_ERROR : B360ErrorType.CONNECTIVITY;
            throw new B360ConnectionException(fullMessage, apiError, errorType);
        }
        return response.getEntity() != null ? response.getEntity().getContent() : null;
    }

    public void invalidate() {
        // Connection state only; do not stop the HttpClient (provider's stop() does that).
    }

    /**
     * Rebuilds an already-built request with the current (refreshed) session ID.
     * Copies URI, method, headers (replacing IDS-SESSION-ID), and entity from the original.
     */
    private HttpRequest rebuildRequestWithNewSession(HttpRequest original) {
        HttpRequestBuilder builder = HttpRequest.builder()
                .uri(original.getUri().toString())
                .method(original.getMethod());
        for (String headerName : original.getHeaderNames()) {
            if (IDS_SESSION_ID_HEADER.equalsIgnoreCase(headerName)) {
                builder.addHeader(IDS_SESSION_ID_HEADER, sessionId);
            } else {
                builder.addHeader(headerName, original.getHeaderValue(headerName));
            }
        }
        if (original.getEntity() != null) {
            builder.entity(original.getEntity());
        }
        return builder.build();
    }

    private static void consumeBody(HttpResponse response) {
        try {
            if (response.getEntity() != null && response.getEntity().getContent() != null) {
                B360Utils.readStreamAsString(response.getEntity().getContent());
            }
        } catch (Exception ignored) {
        }
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

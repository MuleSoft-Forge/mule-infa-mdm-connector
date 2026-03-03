/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.connection.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.mulesoft.connectors.b360.api.B360ApiError;
import com.mulesoft.connectors.b360.internal.connection.B360ErrorParser;
import com.mulesoft.connectors.b360.internal.operation.utils.B360Utils;


/**
 * Builds design-time friendly error messages for connection test and login failures.
 * Used so Anypoint Studio shows actionable messages instead of a generic "Error".
 */
final class B360ConnectionErrorMessages {

    private B360ConnectionErrorMessages() {}

    /**
     * User-facing message for connection validation failure (Test connection).
     * When the API returns a B360 error body, the full {@link B360ApiError} is included in the message.
     * When {@code requestUri} is non-null, the message includes " Request URL: &lt;uri&gt;" for debugging.
     */
    static String forValidationFailure(int statusCode, String reasonPhrase, String responseBody) {
        return forValidationFailure(statusCode, reasonPhrase, responseBody, null);
    }

    static String forValidationFailure(int statusCode, String reasonPhrase, String responseBody, String requestUri) {
        String msg = forValidationFailureInternal(statusCode, reasonPhrase, responseBody);
        return appendRequestUri(msg, requestUri);
    }

    private static String forValidationFailureInternal(int statusCode, String reasonPhrase, String responseBody) {
        B360ApiError b360 = B360ErrorParser.parseFromResponseBody(responseBody);
        if (b360 != null) {
            String msg = "Test connection failed.";
            if (b360.getMessage() != null && !b360.getMessage().isBlank()) {
                msg += " " + b360.getMessage().trim();
            }
            msg += " " + b360.toFullMessage();
            return msg;
        }
        String serverHint = parseMessageFromBody(responseBody);
        String base = forHttpStatus(statusCode, reasonPhrase, true);
        return serverHint != null ? base + " " + serverHint : base;
    }

    private static String appendRequestUri(String message, String requestUri) {
        if (requestUri == null || requestUri.isEmpty()) {
            return message;
        }
        return message + " Request URL: " + requestUri;
    }

    /**
     * User-facing message for login failure (connect()).
     * When the API returns a B360 error body, the full {@link B360ApiError} is included in the message.
     */
    static String forLoginFailure(int statusCode, String reasonPhrase, String responseBody) {
        B360ApiError b360 = B360ErrorParser.parseFromResponseBody(responseBody);
        if (b360 != null) {
            String msg = "Login failed.";
            if (b360.getMessage() != null && !b360.getMessage().isBlank()) {
                msg += " " + b360.getMessage().trim();
            }
            msg += " " + b360.toFullMessage();
            return msg;
        }
        String serverHint = parseMessageFromBody(responseBody);
        String base = forHttpStatus(statusCode, reasonPhrase, false);
        return serverHint != null ? base + " " + serverHint : base;
    }

    /**
     * User-facing message for exception during connection test (e.g. timeout, DNS).
     */
    static String forException(Throwable e) {
        return forException(e, null);
    }

    /**
     * User-facing message for exception during connection test (e.g. timeout, DNS).
     * When {@code requestUri} is non-null, the message includes " Request URL: &lt;uri&gt;" for debugging.
     */
    static String forException(Throwable e, String requestUri) {
        String msg = forExceptionInternal(e);
        return appendRequestUri(msg, requestUri);
    }

    private static String forExceptionInternal(Throwable e) {
        String msg = e != null ? e.getMessage() : "Unknown error";
        if (msg == null) msg = e.getClass().getSimpleName();

        if (isTimeout(msg, e)) {
            return "Connection timed out. Check the Base URL and network connectivity, and try again.";
        }
        if (isUnknownHost(msg, e)) {
            return "Could not resolve host. Check the Base URL (e.g. https://dm-us.informaticacloud.com).";
        }
        if (isConnectionRefused(msg, e)) {
            return "Connection refused. Check the Base URL and that the service is reachable.";
        }
        if (isSslOrTls(msg, e)) {
            return "TLS/SSL error. If using a custom TLS context, verify certificates and truststore.";
        }
        // Generic but still better than "Error"
        return "Connection test failed: " + msg;
    }

    private static String forHttpStatus(int status, String reasonPhrase, boolean isValidation) {
        String ctx = isValidation ? "Test connection failed." : "Login failed.";
        switch (status) {
            case 401:
                return ctx + " Invalid username or password. Check your B360 credentials.";
            case 403:
                return ctx + " Access denied. Your B360 user may not have permission.";
            case 404:
                return ctx + " B360 API endpoint not found. Verify the Base URL (e.g. https://dm-us.informaticacloud.com).";
            case 408:
                return ctx + " Request timed out. Check network and Base URL.";
            case 429:
                return ctx + " Too many requests. Try again in a few minutes.";
            case 500:
            case 502:
            case 503:
                return ctx + " B360 server error (HTTP " + status + "). Try again later or contact support.";
            default:
                if (status >= 400 && status < 500) {
                    return ctx + " Client error (HTTP " + status + " " + (reasonPhrase != null ? reasonPhrase : "") + "). Check your configuration.";
                }
                if (status >= 500) {
                    return ctx + " Server error (HTTP " + status + "). Try again later.";
                }
                return ctx + " Unexpected response: HTTP " + status + " " + (reasonPhrase != null ? reasonPhrase : "");
        }
    }

    private static String parseMessageFromBody(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            JsonNode root = B360Utils.OBJECT_MAPPER.readTree(body);
            if (root == null || !root.isObject()) return null;
            for (String key : new String[] { "message", "error", "errorDescription", "error_message", "msg" }) {
                if (root.has(key) && root.get(key).isTextual()) {
                    String s = root.get(key).asText().trim();
                    if (!s.isEmpty() && s.length() <= 200) return "(" + s + ")";
                }
            }
        } catch (Exception ignored) { }
        return null;
    }

    private static boolean isTimeout(String msg, Throwable e) {
        if (msg == null) return false;
        String m = msg.toLowerCase();
        return m.contains("timeout") || m.contains("timed out") || e instanceof java.net.SocketTimeoutException;
    }

    private static boolean isUnknownHost(String msg, Throwable e) {
        if (msg == null) return false;
        return msg.toLowerCase().contains("unknown host") || e instanceof java.net.UnknownHostException;
    }

    private static boolean isConnectionRefused(String msg, Throwable e) {
        if (msg == null) return false;
        return msg.toLowerCase().contains("connection refused") || msg.contains("Connection refused")
                || e instanceof java.net.ConnectException;
    }

    private static boolean isSslOrTls(String msg, Throwable e) {
        if (msg == null) return false;
        String m = msg.toLowerCase();
        return m.contains("ssl") || m.contains("certificate") || m.contains("handshake")
                || e instanceof javax.net.ssl.SSLException;
    }
}

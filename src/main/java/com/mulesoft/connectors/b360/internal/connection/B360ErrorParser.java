/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.connection;

import com.fasterxml.jackson.databind.JsonNode;
import com.mulesoft.connectors.b360.api.B360ApiError;
import com.mulesoft.connectors.b360.internal.operation.utils.B360Utils;

/**
 * Parses B360 error response bodies into {@link B360ApiError}.
 * Supports:
 * - Platform v3: { "error": { "code", "message", "requestId", "details" } }
 * - Search API:   { "errorCode", "errorSummary", "errorDetail": { "details": [ { "code", "message" } ] } }
 */
public final class B360ErrorParser {

    private B360ErrorParser() {}

    /**
     * Parses error object from the response body. Tries platform v3 shape first, then Search API shape. Returns null if body is null/empty or not recognized.
     */
    public static B360ApiError parseFromResponseBody(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return null;
        try {
            JsonNode root = B360Utils.OBJECT_MAPPER.readTree(responseBody);
            if (root == null || !root.isObject()) return null;

            B360ApiError v3 = parsePlatformV3(root);
            if (v3 != null) return v3;

            return parseSearchApiError(root);
        } catch (Exception e) {
            return null;
        }
    }

    /** Platform v3: { "error": { "code", "message", "requestId", "details" } } */
    private static B360ApiError parsePlatformV3(JsonNode root) {
        if (!root.has("error")) return null;
        JsonNode err = root.get("error");
        if (err == null || !err.isObject()) return null;
        try {
            B360ApiError out = B360Utils.OBJECT_MAPPER.treeToValue(err, B360ApiError.class);
            return out != null && (out.getCode() != null || out.getMessage() != null) ? out : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Search API: { "errorCode", "errorSummary", "errorDetail": { "details": [ { "code", "message" } ] } } */
    private static B360ApiError parseSearchApiError(JsonNode root) {
        if (!root.has("errorCode") && !root.has("errorSummary")) return null;
        String code = root.has("errorCode") && root.get("errorCode").isTextual() ? root.get("errorCode").asText() : null;
        String summary = root.has("errorSummary") && root.get("errorSummary").isTextual() ? root.get("errorSummary").asText() : null;
        StringBuilder message = new StringBuilder(summary != null ? summary : "");
        if (root.has("errorDetail") && root.get("errorDetail").isObject()) {
            JsonNode details = root.get("errorDetail").get("details");
            if (details != null && details.isArray()) {
                for (JsonNode d : details) {
                    if (d.has("message") && d.get("message").isTextual()) {
                        if (message.length() > 0) message.append(" ");
                        message.append(d.get("message").asText());
                    }
                }
            }
        }
        B360ApiError out = new B360ApiError(code, message.toString(), root.has("errorId") && root.get("errorId").isTextual() ? root.get("errorId").asText() : null, null);
        return out.getCode() != null || out.getMessage() != null ? out : null;
    }
}

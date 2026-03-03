/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.connection;

import com.fasterxml.jackson.databind.JsonNode;
import com.mulesoft.connectors.b360.internal.operation.utils.B360Utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Reads the expiration time from an Informatica session token when it is a JWT.
 * Used to refresh the connection before the token expires (e.g. 5 minutes before expiry).
 * No signature verification is performed; only the payload is decoded to read the exp claim.
 */
final class B360JwtExpirationHelper {

    private B360JwtExpirationHelper() {}

    /**
     * Returns the JWT "exp" claim in seconds since epoch, or 0 if the value is not a JWT or has no exp.
     */
    static long getExpirationTimeSeconds(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return 0;
        }
        if (!sessionId.startsWith("eyJ")) {
            return 0;
        }
        String[] parts = sessionId.split("\\.");
        if (parts.length != 3) {
            return 0;
        }
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            if (payloadBytes == null || payloadBytes.length == 0) {
                return 0;
            }
            JsonNode payload = B360Utils.OBJECT_MAPPER.readTree(new String(payloadBytes, StandardCharsets.UTF_8));
            if (payload == null || !payload.has("exp")) {
                return 0;
            }
            return payload.get("exp").asLong(0);
        } catch (Exception e) {
            return 0;
        }
    }
}

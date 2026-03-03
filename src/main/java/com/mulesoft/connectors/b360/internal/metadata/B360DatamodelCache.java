/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.mulesoft.connectors.b360.internal.connection.B360Connection;
import com.mulesoft.connectors.b360.internal.operation.utils.B360Endpoints;
import com.mulesoft.connectors.b360.internal.operation.utils.B360Utils;
import org.mule.runtime.http.api.HttpConstants;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Design-time cache for the B360 tenant datamodel (GET /metadata/api/v2/objects/tenantModel/datamodel).
 * Caches the <em>parsed</em> JsonNode so consumers avoid re-parsing 10MB+ JSON on every UI interaction.
 * <p>
 * Pure-Java LRU cache: LinkedHashMap with removeEldestEntry (max 10 entries), wrapped in
 * Collections.synchronizedMap for thread safety. TTL 30 minutes: on get, if entry is expired it is
 * removed and a fresh copy is fetched. Cache key is baseUrl + SHA-256(sessionId) for multi-tenant
 * isolation. No third-party dependencies.
 * <p>
 * When the cache is cold or an entry has expired, only one thread per cache key fetches the
 * datamodel; other threads for the same key wait on a per-key lock and then use the refreshed entry.
 */
public final class B360DatamodelCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(B360DatamodelCache.class);

    private static final int MAX_SIZE = 10;
    private static final long TTL_MS = 30 * 60 * 1000L;

    @SuppressWarnings("serial")
    private static final Map<String, CacheEntry> CACHE = Collections.synchronizedMap(
            new LinkedHashMap<String, CacheEntry>(MAX_SIZE + 1, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                    return size() > MAX_SIZE;
                }
            });

    /** Per-key locks so only one thread fetches the datamodel per tenant when expired or missing. */
    private static final Map<String, Object> KEY_LOCKS = new ConcurrentHashMap<>();

    private B360DatamodelCache() {}

    /**
     * Returns the tenant datamodel as a parsed JsonNode. Fetches via GET if not cached or expired.
     * Parsing is done once per tenant per TTL window inside this cache.
     *
     * @param connection B360 connection (session header is added by connection)
     * @return parsed datamodel root, or null if the request fails or body is empty
     */
    public static JsonNode getDatamodel(B360Connection connection) {
        if (connection == null) return null;
        String baseUrl = connection.getBaseApiUrl();
        if (baseUrl == null || baseUrl.isEmpty()) return null;
        String key = buildCacheKey(baseUrl, connection.getSessionId());
        long now = System.currentTimeMillis();
        Object lock = KEY_LOCKS.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            synchronized (CACHE) {
                CacheEntry entry = CACHE.get(key);
                if (!connection.isBypassMetadataCache() && entry != null && (now - entry.creationTime) <= TTL_MS) {
                    return entry.root;
                }
                if (entry != null) {
                    CACHE.remove(key);
                }
            }
            if (connection.isBypassMetadataCache()) {
                LOGGER.debug("Bypassing metadata cache for fresh fetch from B360.");
            }
            JsonNode root = fetchAndParseDatamodel(connection);
            if (root != null) {
                synchronized (CACHE) {
                    CACHE.put(key, new CacheEntry(root, now));
                }
                LOGGER.debug("Cached datamodel for {}", baseUrl);
            }
            return root;
        }
    }

    /**
     * Clears the cache. Package-private for use by tests or when tenant changes; not called in production.
     */
    static void clear() {
        CACHE.clear();
    }

    private static String buildCacheKey(String baseUrl, String sessionId) {
        String sessionPart = sessionId != null && !sessionId.isEmpty()
                ? sha256Prefix(sessionId) : "no-session";
        return baseUrl + "|" + sessionPart;
    }

    private static String sha256Prefix(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8 && i < hash.length; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private static JsonNode fetchAndParseDatamodel(B360Connection connection) {
        String uri = connection.getBaseApiUrl() + B360Endpoints.DATAMODEL;
        HttpRequestBuilder builder = HttpRequest.builder()
                .uri(uri)
                .method(HttpConstants.Method.GET)
                .addHeader("Accept", "application/json");
        InputStream response = connection.executeSync(builder);
        String body = B360Utils.readStreamAsString(response);
        if (body == null || body.isEmpty()) return null;
        try {
            return B360Utils.OBJECT_MAPPER.readTree(body);
        } catch (Exception e) {
            LOGGER.debug("Failed to parse datamodel JSON: {}", e.getMessage());
            return null;
        }
    }

    private static final class CacheEntry {
        final JsonNode root;
        final long creationTime;

        CacheEntry(JsonNode root, long creationTime) {
            this.root = root;
            this.creationTime = creationTime;
        }
    }
}

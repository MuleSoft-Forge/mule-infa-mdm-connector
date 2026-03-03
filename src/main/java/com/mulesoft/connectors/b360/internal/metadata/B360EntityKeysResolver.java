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
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyBuilder;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.http.api.HttpConstants;
import org.mule.runtime.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Dynamically resolves Business Entity IDs from the tenant's B360 datamodel (metadata v2).
 * Uses {@link B360DatamodelCache} to fetch/cache GET /metadata/api/v2/objects/tenantModel/datamodel,
 * then filters payload.businessEntity[].guid to exclude rdm.*, *enum*, *template* (same logic as DataWeave).
 * Populates the Business Entity Id dropdown in Anypoint Studio. Falls back to META/META_ALT or OOTB list if datamodel is unavailable.
 * <p>
 * Implemented in Java (not DataWeave) for efficiency and because {@code TypeKeysResolver} runs in the metadata path without
 * access to {@code ExpressionManager}; the equivalent DataWeave is documented below for use in flows.
 */
public class B360EntityKeysResolver implements TypeKeysResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(B360EntityKeysResolver.class);

    /** Filter logic matches DataWeave: exclude ids where lower(id) startsWith "rdm." or contains "enum" or "template".
     * Equivalent script: payload.businessEntity map $.guid then filter (id) -> !(lower(id) startsWith "rdm.") and !(lower(id) contains "enum") and !(lower(id) contains "template"). */
    private static boolean includeEntityId(String id) {
        if (id == null || id.isEmpty()) return false;
        String lower = id.toLowerCase();
        return !lower.startsWith("rdm.")
                && !lower.contains("enum")
                && !lower.contains("template");
    }

    /** Fallback when metadata API is not available (e.g. tenant-specific path or no meta resource). */
    private static final Map<String, String> OOTB_FALLBACK = new LinkedHashMap<>();
    static {
        OOTB_FALLBACK.put("c360.person", "Person");
        OOTB_FALLBACK.put("c360.organization", "Organization");
        OOTB_FALLBACK.put("p360.item", "Item");
        OOTB_FALLBACK.put("p360.product", "Product");
        OOTB_FALLBACK.put("p360.productVariant", "Product Variant");
        OOTB_FALLBACK.put("s360.supplier", "Supplier");
        OOTB_FALLBACK.put("r360.referenceData", "Reference Data");
    }

    @Override
    public String getCategoryName() {
        return "BusinessEntity";
    }

    @Override
    public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
        Set<MetadataKey> keys = new LinkedHashSet<>();
        Optional<B360Connection> connectionOpt = context.getConnection();
        if (!connectionOpt.isPresent()) {
            LOGGER.debug("No B360 connection available for metadata; using fallback entity list.");
            keys.addAll(buildKeysFromFallback());
            return keys;
        }
        B360Connection connection = connectionOpt.get();
        Set<MetadataKey> entityKeys = fetchFromDatamodel(connection);
        if (!entityKeys.isEmpty()) {
            keys.addAll(entityKeys);
            return keys;
        }
        entityKeys = fetchFromMetaApi(connection, B360Endpoints.META);
        if (!entityKeys.isEmpty()) {
            keys.addAll(entityKeys);
            return keys;
        }
        entityKeys = fetchFromMetaApi(connection, B360Endpoints.META_ALT);
        if (!entityKeys.isEmpty()) {
            keys.addAll(entityKeys);
            return keys;
        }
        keys.addAll(buildKeysFromFallback());
        return keys;
    }

    /**
     * Same as {@link #getKeys(MetadataContext)} but with connection passed in.
     * Used by {@link com.mulesoft.connectors.b360.internal.valueprovider.B360BusinessEntityValueProvider}.
     */
    public Set<MetadataKey> getKeysForConnection(B360Connection connection) throws MetadataResolvingException {
        if (connection == null) return buildKeysFromFallback();
        Set<MetadataKey> entityKeys = fetchFromDatamodel(connection);
        if (!entityKeys.isEmpty()) return entityKeys;
        entityKeys = fetchFromMetaApi(connection, B360Endpoints.META);
        if (!entityKeys.isEmpty()) return entityKeys;
        entityKeys = fetchFromMetaApi(connection, B360Endpoints.META_ALT);
        if (!entityKeys.isEmpty()) return entityKeys;
        return buildKeysFromFallback();
    }

    /** Resolves entity keys from cached datamodel (payload.businessEntity[].guid with rdm/enum/template filter). */
    private Set<MetadataKey> fetchFromDatamodel(B360Connection connection) throws MetadataResolvingException {
        JsonNode root;
        try {
            root = B360DatamodelCache.getDatamodel(connection);
        } catch (Exception e) {
            throw new MetadataResolvingException(
                    "Failed to connect to B360 API. Verify your Login URL and credentials in the Global Configuration.",
                    FailureCode.INVALID_METADATA_KEY, e);
        }
        if (root == null || root.isMissingNode()) return new LinkedHashSet<>();
        JsonNode businessEntity = root.path("businessEntity");
        if (!businessEntity.isArray()) return new LinkedHashSet<>();
        Set<MetadataKey> keys = new LinkedHashSet<>();
        for (JsonNode entity : businessEntity) {
            JsonNode guidNode = entity.path("guid");
            if (guidNode.isMissingNode()) guidNode = entity.path("id");
            String id = guidNode.asText("");
            if (id.isEmpty() || !includeEntityId(id)) continue;
            keys.add(MetadataKeyBuilder.newKey(id).withDisplayName(id).build());
        }
        return keys;
    }

    private Set<MetadataKey> fetchFromMetaApi(B360Connection connection, String path) throws MetadataResolvingException {
        String uri = connection.getBaseApiUrl() + path;
        HttpRequestBuilder builder = HttpRequest.builder()
                .uri(uri)
                .method(HttpConstants.Method.GET)
                .addHeader("Accept", "application/json");
        try {
            InputStream response = connection.executeSync(builder);
            byte[] body = B360Utils.readFully(response);
            if (body == null || body.length == 0) return new LinkedHashSet<>();
            return parseEntitiesResponse(body);
        } catch (Exception e) {
            throw new MetadataResolvingException(
                    "Failed to connect to B360 API. Verify your Login URL and credentials in the Global Configuration.",
                    FailureCode.INVALID_METADATA_KEY, e);
        }
    }

    private Set<MetadataKey> parseEntitiesResponse(byte[] body) throws MetadataResolvingException {
        Set<MetadataKey> keys = new LinkedHashSet<>();
        try {
            JsonNode root = B360Utils.OBJECT_MAPPER.readTree(body);
            JsonNode entities = root.path("entities");
            if (!entities.isArray()) return keys;
            for (JsonNode entity : entities) {
                JsonNode idNode = entity.path("identifier");
                if (idNode.isMissingNode()) idNode = entity.path("id");
                String id = idNode.asText("");
                if (id.isEmpty()) continue;
                JsonNode nameNode = entity.path("name");
                if (nameNode.isMissingNode()) nameNode = entity.path("displayName");
                String displayName = nameNode.asText(id);
                keys.add(MetadataKeyBuilder.newKey(id).withDisplayName(displayName).build());
            }
        } catch (Exception e) {
            throw new MetadataResolvingException("Failed to parse business entities from metadata response.", FailureCode.INVALID_METADATA_KEY, e);
        }
        return keys;
    }

    private Set<MetadataKey> buildKeysFromFallback() {
        Set<MetadataKey> keys = new LinkedHashSet<>();
        for (Map.Entry<String, String> e : OOTB_FALLBACK.entrySet()) {
            keys.add(MetadataKeyBuilder.newKey(e.getKey()).withDisplayName(e.getValue()).build());
        }
        return keys;
    }
}

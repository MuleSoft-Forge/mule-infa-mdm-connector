/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.mulesoft.connectors.b360.internal.connection.B360Connection;
import com.mulesoft.connectors.b360.internal.operation.utils.B360Utils;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyBuilder;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves Source System internal IDs (e.g. {@code c360.default.system}) for the Source Read operation.
 * Uses the tenant datamodel from {@link B360DatamodelCache} (GET /metadata/api/v2/objects/tenantModel/datamodel)
 * and looks for source systems either at top-level {@code sourceSystem[]} or <em>restricted by business entity</em>
 * (e.g. {@code businessEntity[].sourceSystem[]} or {@code sourceSystem[]} with {@code businessEntity} / {@code entityId}).
 * Falls back to an OOTB list when the datamodel does not contain source systems or when no connection is available.
 *
 * @see <a href="https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Source_record_API.html">Source Record API</a>
 */
public class B360SourceSystemKeysResolver implements TypeKeysResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(B360SourceSystemKeysResolver.class);

    /**
     * When set to "true", throw a validation error with debug details when the source system list is empty,
     * so the message appears in Anypoint Studio Problems panel (design-time logs often not visible).
     * Start Studio with: -Db360.sourceread.debug=true
     */
    private static final String SYS_PROP_DEBUG = "b360.sourceread.debug";

    /** Possible top-level keys in tenant datamodel for source systems (tried in order). */
    private static final String[] DATAMODEL_SOURCE_SYSTEM_KEYS = {"sourceSystem", "sourceSystems", "dataSource", "source"};

    /** Keys on a source system item that reference a business entity (for filtering flat list). */
    private static final String[] SOURCE_SYSTEM_ENTITY_REF_KEYS = {"businessEntity", "entityId", "businessEntityRef", "entityRef", "entity", "$ref"};

    /** Fallback when datamodel has no source systems or connection is unavailable. */
    private static final Map<String, String> OOTB_FALLBACK = new LinkedHashMap<>();
    static {
        OOTB_FALLBACK.put("c360.default.system", "Default System");
        OOTB_FALLBACK.put("sals_bid", "Sales BID");
        OOTB_FALLBACK.put("HRSource", "HR Source");
    }

    @Override
    public String getCategoryName() {
        return "SourceSystem";
    }

    @Override
    public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
        Set<MetadataKey> keys = new LinkedHashSet<>();
        Optional<B360Connection> connectionOpt = context.getConnection();
        if (!connectionOpt.isPresent()) {
            LOGGER.debug("No B360 connection for metadata; using fallback source system list.");
            keys.addAll(buildKeysFromFallback());
            return keys;
        }
        B360Connection connection = connectionOpt.get();
        Set<MetadataKey> fromDatamodel = fetchFromDatamodel(connection);
        if (!fromDatamodel.isEmpty()) {
            keys.addAll(fromDatamodel);
            return keys;
        }
        keys.addAll(buildKeysFromFallback());
        return keys;
    }

    /**
     * Same as {@link #getKeys(MetadataContext)} but with connection passed in (global list).
     * Used by {@link com.mulesoft.connectors.b360.internal.valueprovider.B360SourceSystemValueProvider} as fallback.
     */
    public Set<MetadataKey> getKeysForConnection(B360Connection connection) throws MetadataResolvingException {
        if (connection == null) return buildKeysFromFallback();
        Set<MetadataKey> fromDatamodel = fetchFromDatamodel(connection);
        if (!fromDatamodel.isEmpty()) return fromDatamodel;
        return buildKeysFromFallback();
    }

    /**
     * Parses source systems from the tenant datamodel JSON. Tries each known top-level array key;
     * for each element reads id from {@code guid}, {@code identifier}, or {@code id}, and
     * display name from {@code name} or {@code displayName}.
     */
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
        for (String arrayKey : DATAMODEL_SOURCE_SYSTEM_KEYS) {
            JsonNode array = root.path(arrayKey);
            if (!array.isArray() || array.size() == 0) continue;
            Set<MetadataKey> keys = new LinkedHashSet<>();
            for (JsonNode item : array) {
                String id = item.path("guid").asText("");
                if (id.isEmpty()) id = item.path("identifier").asText("");
                if (id.isEmpty()) id = item.path("id").asText("");
                if (id.isEmpty()) continue;
                String displayName = item.path("name").asText("");
                if (displayName.isEmpty()) displayName = item.path("displayName").asText("");
                if (displayName.isEmpty()) displayName = id;
                keys.add(MetadataKeyBuilder.newKey(id).withDisplayName(displayName).build());
            }
            if (!keys.isEmpty()) {
                LOGGER.debug("Resolved {} source system(s) from datamodel.{}", keys.size(), arrayKey);
                return keys;
            }
        }
        return new LinkedHashSet<>();
    }

    /**
     * Returns source system keys <em>restricted by business entity</em> from the cached datamodel.
     * Tries: (1) businessEntity[].sourceSystem[] for the matching entity,
     * (2) root sourceSystem[] filtered by businessEntity/entityId field.
     * Used by {@link SourceReadTypeKeysResolver#resolveChilds} so the second dropdown shows only
     * source systems associated with the selected entity.
     * Falls back to global list from {@link #getKeys(MetadataContext)} when no entity-scoped list is found.
     */
    public Set<MetadataKey> getKeysForEntity(B360Connection connection, String businessEntityId) throws MetadataResolvingException {
        if (connection == null || businessEntityId == null || businessEntityId.isEmpty()) {
            LOGGER.info("[B360 Source System] getKeysForEntity skipped: connection or businessEntityId null/empty.");
            if ("true".equalsIgnoreCase(System.getProperty(SYS_PROP_DEBUG, "").trim())) {
                throw new MetadataResolvingException(
                        "B360 Source Read DEBUG: getKeysForEntity skipped. connection=" + (connection != null) + " businessEntityId=" + businessEntityId,
                        FailureCode.INVALID_METADATA_KEY);
            }
            return new LinkedHashSet<>();
        }
        JsonNode root;
        try {
            root = B360DatamodelCache.getDatamodel(connection);
        } catch (Exception e) {
            throw new MetadataResolvingException(
                    "Failed to connect to B360 API. Verify your Login URL and credentials in the Global Configuration.",
                    FailureCode.INVALID_METADATA_KEY, e);
        }
        if (root == null || root.isMissingNode()) {
            LOGGER.info("[B360 Source System] getKeysForEntity: datamodel empty or null for entityId={}.", businessEntityId);
            if ("true".equalsIgnoreCase(System.getProperty(SYS_PROP_DEBUG, "").trim())) {
                throw new MetadataResolvingException(
                        "B360 Source Read DEBUG: datamodel empty or null. entityId=" + businessEntityId + " (check connection and GET /metadata/api/v2/objects/tenantModel/datamodel)",
                        FailureCode.INVALID_METADATA_KEY);
            }
            return new LinkedHashSet<>();
        }
        LOGGER.info("[B360 Source System] getKeysForEntity entityId={}.", businessEntityId);
        try {
            Set<MetadataKey> keys = fetchFromBusinessEntityScope(root, businessEntityId);
            if (!keys.isEmpty()) {
                LOGGER.info("[B360 Source System] entity-scoped (nested): {} keys for entity {}.", keys.size(), businessEntityId);
                return keys;
            }
            keys = fetchFromFlatListFilteredByEntity(root, businessEntityId);
            if (!keys.isEmpty()) {
                LOGGER.info("[B360 Source System] flat filtered: {} keys for entity {}.", keys.size(), businessEntityId);
                return keys;
            }
            if ("true".equalsIgnoreCase(System.getProperty(SYS_PROP_DEBUG, "").trim())) {
                String msg = buildDebugMessage(root, businessEntityId);
                throw new MetadataResolvingException(msg + " (disable: remove -Db360.sourceread.debug=true)", FailureCode.INVALID_METADATA_KEY);
            }
            LOGGER.info("[B360 Source System] no keys found for entity {} (tried nested and flat filtered).", businessEntityId);
        } catch (MetadataResolvingException e) {
            throw e;
        } catch (Exception e) {
            throw new MetadataResolvingException(
                    "Failed to load Source System list for entity " + businessEntityId + ". Verify your B360 connection and Global Configuration.",
                    FailureCode.INVALID_METADATA_KEY, e);
        }
        return new LinkedHashSet<>();
    }

    /** Find source systems nested under businessEntity[?guid==entityId]: deep traversal (..*sourceSystem) then first value per object to handle $ref. */
    private Set<MetadataKey> fetchFromBusinessEntityScope(JsonNode root, String entityId) {
        JsonNode businessEntityArray = root.path("businessEntity");
        if (!businessEntityArray.isArray()) return new LinkedHashSet<>();
        for (JsonNode be : businessEntityArray) {
            String guid = be.path("guid").asText("");
            if (guid.isEmpty()) guid = be.path("id").asText("");
            if (!entityId.equals(guid)) continue;
            Set<MetadataKey> keys = collectSourceSystemsDeep(be);
            if (!keys.isEmpty()) return keys;
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>();
    }

    /** Recursively collect all nodes named "sourceSystem" under entity (..*sourceSystem), then extract id from each (first value if object to bypass $ref). */
    private Set<MetadataKey> collectSourceSystemsDeep(JsonNode entity) {
        Set<MetadataKey> keys = new LinkedHashSet<>();
        collectSourceSystemNodes(entity, keys);
        return keys;
    }

    private void collectSourceSystemNodes(JsonNode node, Set<MetadataKey> out) {
        if (node == null || node.isMissingNode()) return;
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                if ("sourceSystem".equals(entry.getKey())) {
                    JsonNode val = entry.getValue();
                    if (val.isArray()) {
                        for (JsonNode child : val) addSourceSystemKey(child, out);
                    } else {
                        addSourceSystemKey(val, out);
                    }
                } else {
                    collectSourceSystemNodes(entry.getValue(), out);
                }
            });
        } else if (node.isArray()) {
            for (JsonNode child : node) collectSourceSystemNodes(child, out);
        }
    }

    private void addSourceSystemKey(JsonNode item, Set<MetadataKey> out) {
        String id = idFromSourceSystemNode(item);
        if (id == null || id.isEmpty()) return;
        String displayName = item.isObject() ? item.path("name").asText("") : "";
        if (displayName.isEmpty() && item.isObject()) displayName = item.path("displayName").asText("");
        if (displayName.isEmpty()) displayName = id;
        out.add(MetadataKeyBuilder.newKey(id).withDisplayName(displayName).build());
    }

    /** Extract id from a sourceSystem node: guid/id/identifier, or first string value if object (bypasses $ref). */
    private static String idFromSourceSystemNode(JsonNode item) {
        if (item.isTextual()) return item.asText("");
        if (!item.isObject()) return null;
        String id = item.path("guid").asText("");
        if (!id.isEmpty()) return id;
        id = item.path("identifier").asText("");
        if (!id.isEmpty()) return id;
        id = item.path("id").asText("");
        if (!id.isEmpty()) return id;
        // First value of object (bypass $ref key) - matches DataWeave valuesOf($)[0]
        Iterator<JsonNode> it = item.elements();
        while (it.hasNext()) {
            JsonNode v = it.next();
            if (v.isTextual()) {
                String s = v.asText("");
                if (!s.isEmpty() && !s.startsWith("//")) return s;
            }
        }
        return null;
    }

    /** Find source systems from root sourceSystem[] where item.businessEntity (or entityId, etc.) == entityId. */
    private Set<MetadataKey> fetchFromFlatListFilteredByEntity(JsonNode root, String entityId) {
        for (String arrayKey : DATAMODEL_SOURCE_SYSTEM_KEYS) {
            JsonNode array = root.path(arrayKey);
            if (!array.isArray() || array.size() == 0) continue;
            Set<MetadataKey> keys = new LinkedHashSet<>();
            boolean loggedFirst = false;
            for (JsonNode item : array) {
                if (!loggedFirst && array.size() > 0) {
                    logFirstSourceSystemItemForDebug(arrayKey, array, entityId);
                    loggedFirst = true;
                }
                if (!sourceSystemItemReferencesEntity(item, entityId)) continue;
                String id = item.path("guid").asText("");
                if (id.isEmpty()) id = item.path("identifier").asText("");
                if (id.isEmpty()) id = item.path("id").asText("");
                if (id.isEmpty()) continue;
                String displayName = item.path("name").asText("");
                if (displayName.isEmpty()) displayName = item.path("displayName").asText("");
                if (displayName.isEmpty()) displayName = id;
                keys.add(MetadataKeyBuilder.newKey(id).withDisplayName(displayName).build());
            }
            if (!keys.isEmpty()) {
                LOGGER.info("[B360 Source System] fetchFromFlatListFilteredByEntity arrayKey={} size={} matched={}.", arrayKey, array.size(), keys.size());
                return keys;
            }
        }
        return new LinkedHashSet<>();
    }

    /** Log first source system item structure and extracted entity refs for debugging empty dropdown. */
    private void logFirstSourceSystemItemForDebug(String arrayKey, JsonNode array, String entityId) {
        String msg = buildFirstItemDebugString(array, entityId);
        if (msg != null) LOGGER.info("[B360 Source System] {}", msg);
    }

    /**
     * Builds a debug message for the Problems panel when source system list is empty.
     * Enable with -Db360.sourceread.debug=true so this is thrown as MetadataResolvingException.
     */
    private static String buildDebugMessage(JsonNode root, String requestedEntityId) {
        StringBuilder sb = new StringBuilder();
        sb.append("B360 Source Read DEBUG (empty source system list). requestedEntityId=").append(requestedEntityId);
        for (String arrayKey : DATAMODEL_SOURCE_SYSTEM_KEYS) {
            JsonNode array = root.path(arrayKey);
            if (!array.isArray() || array.size() == 0) continue;
            sb.append(" | root.").append(arrayKey).append(".length=").append(array.size());
            String firstItem = buildFirstItemDebugString(array, requestedEntityId);
            if (firstItem != null) sb.append(" | firstItem: ").append(firstItem);
            break;
        }
        JsonNode beArray = root.path("businessEntity");
        if (beArray.isArray()) sb.append(" | businessEntity.length=").append(beArray.size());
        return sb.toString();
    }

    /** Returns a string describing the first source system item and extracted entity refs, or null. */
    private static String buildFirstItemDebugString(JsonNode array, String entityId) {
        try {
            JsonNode first = array.get(0);
            if (first == null || !first.isObject()) return null;
            StringBuilder keyList = new StringBuilder();
            first.fieldNames().forEachRemaining(name -> keyList.append(name).append(","));
            int refCount = 0;
            StringBuilder refs = new StringBuilder();
            for (String refKey : SOURCE_SYSTEM_ENTITY_REF_KEYS) {
                JsonNode ref = first.path(refKey);
                if (ref.isMissingNode()) continue;
                refCount++;
                String extracted = ref.isTextual() ? ref.asText("") : (ref.isObject() ? extractEntityRefFromRefNode(ref) : null);
                if (extracted != null) {
                    refs.append(refKey).append("=").append(extracted).append(" ");
                } else if (ref.isObject() && ref.has("$ref")) {
                    String path = ref.path("$ref").asText("");
                    String fromPath = extractGuidFromRefPath(path);
                    refs.append(refKey).append(".$ref=").append(path).append("->").append(fromPath).append(" ");
                } else {
                    refs.append(refKey).append("=(object/array) ");
                }
            }
            return "keys=[" + (keyList.length() > 0 ? keyList.substring(0, keyList.length() - 1) : "") + "] entityRefKeysFound=" + refCount + " refs=[" + refs.toString().trim() + "]";
        } catch (Exception e) {
            return "buildFirstItemDebugString failed: " + e.getMessage();
        }
    }

    /**
     * True if this source system item references the given business entity id.
     * When the item has no entity ref at all, we include it (flat list without refs).
     * Handles single ref, object with $ref path, and array of refs.
     */
    private static boolean sourceSystemItemReferencesEntity(JsonNode item, String entityId) {
        boolean hasAnyRef = false;
        for (String refKey : SOURCE_SYSTEM_ENTITY_REF_KEYS) {
            JsonNode ref = item.path(refKey);
            if (ref.isMissingNode()) continue;
            if (ref.isArray()) {
                for (JsonNode element : ref) {
                    String extracted = extractEntityRefFromRefNode(element);
                    if (extracted != null) {
                        hasAnyRef = true;
                        if (entityId.equals(extracted)) return true;
                    }
                }
                continue;
            }
            String extracted = extractEntityRefFromRefNode(ref);
            if (extracted != null) {
                hasAnyRef = true;
                if (entityId.equals(extracted)) return true;
            }
        }
        return !hasAnyRef; // no ref on this item -> include for any entity
    }

    private static String extractEntityRefFromRefNode(JsonNode ref) {
        if (ref.isTextual()) {
            String s = ref.asText("");
            return s.isEmpty() ? null : s;
        }
        if (!ref.isObject()) return null;
        String id = ref.path("id").asText("");
        if (!id.isEmpty()) return id;
        String guid = ref.path("guid").asText("");
        if (!guid.isEmpty()) return guid;
        JsonNode refPath = ref.path("$ref");
        if (!refPath.isMissingNode() && refPath.isTextual())
            return extractGuidFromRefPath(refPath.asText(""));
        return null;
    }

    /**
     * Parses Informatica-style ref path to extract entity guid.
     * e.g. "//@businessEntity[guid='c360.person']" -> "c360.person"; ".c360.person" -> "c360.person".
     */
    private static String extractGuidFromRefPath(String refPath) {
        if (refPath == null || refPath.isEmpty()) return null;
        // Pattern: guid='...' or guid="..."
        int guidIdx = refPath.indexOf("guid='");
        if (guidIdx >= 0) {
            int start = guidIdx + 6;
            int end = refPath.indexOf("'", start);
            if (end > start) return refPath.substring(start, end);
        }
        guidIdx = refPath.indexOf("guid=\"");
        if (guidIdx >= 0) {
            int start = guidIdx + 6;
            int end = refPath.indexOf("\"", start);
            if (end > start) return refPath.substring(start, end);
        }
        // Relative ref: ".c360.person" or "//@businessEntity[guid='c360.person']" already handled above
        if (refPath.startsWith(".")) return refPath.substring(1);
        return null;
    }

    private Set<MetadataKey> buildKeysFromFallback() {
        Set<MetadataKey> keys = new LinkedHashSet<>();
        for (Map.Entry<String, String> e : OOTB_FALLBACK.entrySet()) {
            keys.add(MetadataKeyBuilder.newKey(e.getKey()).withDisplayName(e.getValue()).build());
        }
        return keys;
    }
}

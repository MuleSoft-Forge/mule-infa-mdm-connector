/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.mulesoft.connectors.b360.internal.connection.B360Connection;
import com.mulesoft.connectors.b360.internal.metadata.B360DatamodelCache;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds nested output {@link MetadataType} for a business entity from the tenant datamodel.
 * Traverses {@code businessEntity.field[]} recursively: leaf fields (TextField, LookupField, etc.) become
 * primitives; FieldGroups with nested {@code field[]} become objects or arrays (when {@code allowMany} is true).
 * Root-level entity fields use a dotted prefix derived from the entity key (e.g. p360.category → p360category.).
 * Also adds a standard {@code _meta} object type to match the Search/Get Record response shape.
 */
public final class B360EntityOutputTypeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(B360EntityOutputTypeBuilder.class);

    /** eClass suffixes in datamodel. */
    private static final String BOOLEAN_FIELD = "BooleanField";
    private static final String NUMBER_FIELD = "NumberField";
    private static final String LOOKUP_FIELD = "LookupField";

    private B360EntityOutputTypeBuilder() {}

    /**
     * Returns the <em>element</em> type for Search output (a single record).
     * The operation returns {@code List<Object>}; the SDK expects the resolver to describe the
     * element type and wraps it in Array once. Do not return array type here or the runtime
     * will produce Array&lt;Array&lt;Object&gt;&gt;.
     */
    public static MetadataType buildSearchOutputType(MetadataContext context, String entityKey, B360Connection connection)
            throws MetadataResolvingException, ConnectionException {
        MetadataType elementType = buildSearchOutputElementType(context, entityKey, connection);
        return context.getTypeBuilder().arrayType().of(elementType).build();
    }

    /**
     * Returns the single-record (element) type for Search output. Use this from the operation's
     * OutputTypeResolver when the operation returns a List — the SDK wraps the element type in Array.
     */
    public static MetadataType buildSearchOutputElementType(MetadataContext context, String entityKey, B360Connection connection)
            throws MetadataResolvingException, ConnectionException {
        if (context == null) {
            throw new MetadataResolvingException("MetadataContext is required", FailureCode.INVALID_METADATA_KEY);
        }
        if (entityKey == null || entityKey.isEmpty()) {
            return SearchOutputStaticResolver.getGenericRecordType(context);
        }
        JsonNode root = connection != null ? B360DatamodelCache.getDatamodel(connection) : null;
        if (root == null || root.isMissingNode()) {
            LOGGER.debug("No datamodel for entity {}; using generic record type.", entityKey);
            return SearchOutputStaticResolver.getGenericRecordType(context);
        }
        try {
            JsonNode businessEntityArray = root.path("businessEntity");
            if (!businessEntityArray.isArray()) return SearchOutputStaticResolver.getGenericRecordType(context);
            for (JsonNode be : businessEntityArray) {
                String guid = be.path("guid").asText("");
                if (guid.isEmpty()) guid = be.path("id").asText("");
                if (!entityKey.equals(guid)) continue;
                String entityPrefix = toEntityPrefix(entityKey);
                org.mule.metadata.api.builder.ObjectTypeBuilder objectBuilder =
                        context.getTypeBuilder().objectType().id("Record_" + sanitizeId(entityKey));
                addMetaObject(objectBuilder, context);
                JsonNode fieldArray = be.path("field");
                if (fieldArray.isArray()) {
                    for (JsonNode fieldNode : fieldArray) {
                        addFieldToObject(objectBuilder, context, fieldNode, entityPrefix);
                    }
                }
                return objectBuilder.build();
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to build nested type for entity {}: {}", entityKey, e.getMessage());
        }
        return SearchOutputStaticResolver.getGenericRecordType(context);
    }

    /**
     * Returns a single record object type (not wrapped in an array) for the given entity key.
     * Used by Source Read and other single-record GET operations.
     * Falls back to generic open object if datamodel is unavailable or entity not found.
     */
    public static MetadataType buildSingleRecordOutputType(MetadataContext context, String entityKey, B360Connection connection)
            throws MetadataResolvingException, ConnectionException {
        return buildSingleRecordOutputType(context, entityKey, connection, true);
    }

    /**
     * Builds a single record type for Source Read: root-level keys without entity prefix (firstName, Phone, etc.),
     * plus _meta and _contentMeta to match the Source Record API response.
     * Uses the same datamodel as Search; only the key shape and _contentMeta differ.
     */
    public static MetadataType buildSourceReadOutputType(MetadataContext context, String entityKey, B360Connection connection)
            throws MetadataResolvingException, ConnectionException {
        return buildSingleRecordOutputType(context, entityKey, connection, false, true);
    }

    /**
     * Builds the input type for Source Submit: root-level entity fields only, no _meta or _contentMeta.
     * The Create Master Record API accepts record field data; _meta/_contentMeta are response-only.
     */
    public static MetadataType buildSourceSubmitInputType(MetadataContext context, String entityKey, B360Connection connection)
            throws MetadataResolvingException, ConnectionException {
        return buildSingleRecordOutputType(context, entityKey, connection, false, false);
    }

    private static MetadataType buildSingleRecordOutputType(MetadataContext context, String entityKey, B360Connection connection,
                                                           boolean useEntityPrefixForRoot)
            throws MetadataResolvingException, ConnectionException {
        return buildSingleRecordOutputType(context, entityKey, connection, useEntityPrefixForRoot, true);
    }

    private static MetadataType buildSingleRecordOutputType(MetadataContext context, String entityKey, B360Connection connection,
                                                           boolean useEntityPrefixForRoot, boolean includeMetaAndContentMeta)
            throws MetadataResolvingException, ConnectionException {
        if (context == null) {
            throw new MetadataResolvingException("MetadataContext is required", FailureCode.INVALID_METADATA_KEY);
        }
        if (entityKey == null || entityKey.isEmpty()) {
            return SearchOutputStaticResolver.getGenericRecordType(context);
        }
        JsonNode root = connection != null ? B360DatamodelCache.getDatamodel(connection) : null;
        if (root == null || root.isMissingNode()) {
            LOGGER.debug("No datamodel for entity {}; using generic record type.", entityKey);
            return SearchOutputStaticResolver.getGenericRecordType(context);
        }
        try {
            JsonNode businessEntityArray = root.path("businessEntity");
            if (!businessEntityArray.isArray()) return SearchOutputStaticResolver.getGenericRecordType(context);
            for (JsonNode be : businessEntityArray) {
                String guid = be.path("guid").asText("");
                if (guid.isEmpty()) guid = be.path("id").asText("");
                if (!entityKey.equals(guid)) continue;
                String entityPrefix = useEntityPrefixForRoot ? toEntityPrefix(entityKey) : "";
                org.mule.metadata.api.builder.ObjectTypeBuilder objectBuilder =
                        context.getTypeBuilder().objectType().id("Record_" + sanitizeId(entityKey));
                if (includeMetaAndContentMeta) {
                    addMetaObject(objectBuilder, context);
                    if (!useEntityPrefixForRoot) {
                        addContentMetaObject(objectBuilder, context);
                    }
                }
                JsonNode fieldArray = be.path("field");
                if (fieldArray.isArray()) {
                    for (JsonNode fieldNode : fieldArray) {
                        addFieldToObject(objectBuilder, context, fieldNode, entityPrefix);
                    }
                }
                return objectBuilder.build();
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to build nested type for entity {}: {}", entityKey, e.getMessage());
        }
        return SearchOutputStaticResolver.getGenericRecordType(context);
    }

    /** Adds _meta object type to match API response (updatedBy, businessId, states, etc.). */
    private static void addMetaObject(org.mule.metadata.api.builder.ObjectTypeBuilder parent, MetadataContext context) {
        org.mule.metadata.api.builder.ObjectTypeBuilder meta = context.getTypeBuilder().objectType().id("_meta");
        meta.addField().key("updatedBy").value(context.getTypeBuilder().stringType());
        meta.addField().key("createdBy").value(context.getTypeBuilder().stringType());
        meta.addField().key("lastUpdateDate").value(context.getTypeBuilder().stringType());
        meta.addField().key("businessId").value(context.getTypeBuilder().stringType());
        meta.addField().key("id").value(context.getTypeBuilder().stringType());
        meta.addField().key("businessEntity").value(context.getTypeBuilder().stringType());
        meta.addField().key("type").value(context.getTypeBuilder().stringType());
        meta.addField().key("creationDate").value(context.getTypeBuilder().stringType());
        org.mule.metadata.api.builder.ObjectTypeBuilder states = context.getTypeBuilder().objectType();
        states.addField().key("validation").value(context.getTypeBuilder().stringType());
        states.addField().key("recordStateIndicator").value(context.getTypeBuilder().stringType());
        meta.addField().key("states").value(states.build());
        meta.addField().key("status").value(context.getTypeBuilder().stringType());
        meta.addField().key("score").value(context.getTypeBuilder().numberType());
        parent.addField().key("_meta").value(meta.build());
    }

    /** Adds _contentMeta object type for Source Read (trust, dataEnhancementRule per Source Record API). */
    private static void addContentMetaObject(org.mule.metadata.api.builder.ObjectTypeBuilder parent, MetadataContext context) {
        org.mule.metadata.api.builder.ObjectTypeBuilder contentMeta = context.getTypeBuilder().objectType().id("_contentMeta");
        org.mule.metadata.api.builder.ObjectTypeBuilder trust = context.getTypeBuilder().objectType();
        trust.addField().key("updateCount").value(context.getTypeBuilder().numberType());
        trust.addField().key("fieldData").value(context.getTypeBuilder().arrayType().of(context.getTypeBuilder().objectType().build()).build());
        contentMeta.addField().key("trust").value(trust.build());
        contentMeta.addField().key("dataEnhancementRule").value(context.getTypeBuilder().objectType().build());
        parent.addField().key("_contentMeta").value(contentMeta.build());
    }

    private static void addFieldToObject(org.mule.metadata.api.builder.ObjectTypeBuilder parent, MetadataContext context,
                                        JsonNode fieldNode, String keyPrefix) {
        String name = fieldNode.path("name").asText("");
        if (name.isEmpty()) name = fieldNode.path("identifier").asText("");
        if (name.isEmpty()) return;
        String key = keyPrefix + name;
        JsonNode nested = fieldNode.path("field");
        boolean hasNested = nested.isArray() && nested.size() > 0;
        boolean allowMany = fieldNode.path("allowMany").asBoolean(false);
        String eClassShort = toShortEClass(fieldNode.path("eClass").asText(""));

        if (hasNested) {
            MetadataType nestedType = allowMany ? buildNestedTypeWithArrayItemId(context, nested) : buildNestedType(context, nested);
            if (allowMany) {
                parent.addField().key(key).value(context.getTypeBuilder().arrayType().of(nestedType));
            } else {
                parent.addField().key(key).value(nestedType);
            }
        } else {
            parent.addField().key(key).value(fieldTypeFromNode(context, fieldNode, eClassShort));
        }
    }

    /** Builds object type from an array of field nodes (e.g. children of a FieldGroup). */
    private static MetadataType buildNestedType(MetadataContext context, JsonNode fieldArray) {
        return buildNestedTypeInternal(context, fieldArray, false);
    }

    /** Like buildNestedType but adds _id to match API array items (e.g. descriptions[]._id). */
    private static MetadataType buildNestedTypeWithArrayItemId(MetadataContext context, JsonNode fieldArray) {
        return buildNestedTypeInternal(context, fieldArray, true);
    }

    private static MetadataType buildNestedTypeInternal(MetadataContext context, JsonNode fieldArray, boolean addArrayItemId) {
        org.mule.metadata.api.builder.ObjectTypeBuilder object = context.getTypeBuilder().objectType();
        if (addArrayItemId) {
            object.addField().key("_id").value(context.getTypeBuilder().stringType());
        }
        for (JsonNode fieldNode : fieldArray) {
            String name = fieldNode.path("name").asText("");
            if (name.isEmpty()) name = fieldNode.path("identifier").asText("");
            if (name.isEmpty()) continue;
            JsonNode nested = fieldNode.path("field");
            boolean hasNested = nested.isArray() && nested.size() > 0;
            boolean allowMany = fieldNode.path("allowMany").asBoolean(false);
            String eClassShort = toShortEClass(fieldNode.path("eClass").asText(""));

            if (hasNested) {
                MetadataType childType = allowMany ? buildNestedTypeWithArrayItemId(context, nested) : buildNestedType(context, nested);
                if (allowMany) {
                    object.addField().key(name).value(context.getTypeBuilder().arrayType().of(childType));
                } else {
                    object.addField().key(name).value(childType);
                }
            } else {
                object.addField().key(name).value(fieldTypeFromNode(context, fieldNode, eClassShort));
            }
        }
        return object.build();
    }

    /** Primitive or Lookup object derived from datamodel (codeField + extraField refs). */
    private static MetadataType fieldTypeFromNode(MetadataContext context, JsonNode fieldNode, String eClassShort) {
        if (LOOKUP_FIELD.equals(eClassShort)) {
            return lookupObjectTypeFromDatamodel(context, fieldNode);
        }
        return primitiveType(context, eClassShort);
    }

    /**
     * Builds object type for LookupField from datamodel: codeField implies "Code",
     * extraField[].field.$ref implies keys like "Name" (parsed from @field[name='Name']).
     * Field values use built MetadataType so the SDK resolves the object shape correctly.
     * When datamodel omits codeField/extraField (e.g. API variant), returns default { Code, Name } object.
     */
    private static MetadataType lookupObjectTypeFromDatamodel(MetadataContext context, JsonNode fieldNode) {
        org.mule.metadata.api.builder.ObjectTypeBuilder lookup = context.getTypeBuilder().objectType();
        MetadataType stringType = context.getTypeBuilder().stringType().build();
        if (!fieldNode.path("codeField").isMissingNode()) {
            lookup.addField().key("Code").value(stringType);
        }
        JsonNode extraField = fieldNode.path("extraField");
        if (extraField.isArray()) {
            for (JsonNode item : extraField) {
                String ref = item.path("field").path("$ref").asText("");
                if (ref.isEmpty()) ref = item.path("field").path("ref").asText("");
                String name = parseRefFieldName(ref);
                if (!name.isEmpty()) {
                    lookup.addField().key(name).value(stringType);
                }
            }
        }
        if (!fieldNode.path("codeField").isMissingNode() || (extraField.isArray() && extraField.size() > 0)) {
            return lookup.build();
        }
        // LookupField but no codeField/extraField in datamodel (e.g. API returns slimmer shape): default object
        lookup.addField().key("Code").value(stringType);
        lookup.addField().key("Name").value(stringType);
        return lookup.build();
    }

    /** Extracts field name from $ref like "//@businessEntity[...]/@field[name='Name']". */
    private static String parseRefFieldName(String ref) {
        if (ref == null || ref.isEmpty()) return "";
        int idx = ref.indexOf("@field[name='");
        if (idx < 0) return "";
        idx += "@field[name='".length();
        int end = ref.indexOf("']", idx);
        return end > idx ? ref.substring(idx, end) : "";
    }

    private static MetadataType primitiveType(MetadataContext context, String eClassShort) {
        if (BOOLEAN_FIELD.equals(eClassShort)) return context.getTypeBuilder().booleanType().build();
        if (NUMBER_FIELD.equals(eClassShort)) return context.getTypeBuilder().numberType().build();
        return context.getTypeBuilder().stringType().build();
    }

    /** p360.category → p360category. (entity key to API key prefix) */
    private static String toEntityPrefix(String entityKey) {
        if (entityKey == null) return "";
        return entityKey.replace(".", "") + ".";
    }

    /** Datamodel eClass is like "http://...Core#//LookupField"; extract "LookupField" for type checks. */
    private static String toShortEClass(String eClass) {
        if (eClass == null || eClass.isEmpty()) return "";
        String suffix = eClass.contains("#") ? eClass.substring(eClass.lastIndexOf('#') + 1) : eClass;
        return suffix.startsWith("//") ? suffix.substring(2) : suffix;
    }

    private static String sanitizeId(String key) {
        if (key == null) return "Record";
        return key.replace(".", "_").replaceAll("[^a-zA-Z0-9_]", "");
    }
}

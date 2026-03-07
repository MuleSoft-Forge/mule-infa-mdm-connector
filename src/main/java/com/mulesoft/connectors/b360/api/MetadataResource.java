/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.api;

/**
 * Metadata v2 resource type. Renders as a dropdown in Anypoint Studio.
 * Determines which Informatica MDM Metadata API resource is called.
 * <p>
 * URL format: {@code /metadata/api/v2/objects/<resourceSegment>/<objectInternalId>} for
 * resource types that require an object; otherwise a fixed path (e.g. tenantModel/datamodel, relationship).
 *
 * @see <a href="https://knowledge.informatica.com/s/article/metadata-from-org?language=en_US">Informatica KB: Metadata from Org</a>
 */
public enum MetadataResource {

    /**
     * Org Discovery: returns the entire tenant data model (business entities, relationships,
     * field groups, lookup types). No object internal id.
     * Path: {@code /metadata/api/v2/objects/tenantModel/datamodel}.
     */
    DATAMODEL,

    /**
     * Business Entity blueprint: field types, lengths, constraints, and relationships for a
     * specific business entity. Requires Object Internal Id (e.g. {@code c360.person}).
     * Path: {@code /metadata/api/v2/objects/businessEntity/{objectInternalId}}.
     */
    BUSINESS_ENTITY,

    /**
     * Reference Entity metadata for a specific reference entity. Requires Object Internal Id.
     * Path: {@code /metadata/api/v2/objects/referenceEntity/{objectInternalId}}.
     */
    REFERENCE_ENTITY,

    /**
     * Hierarchy &amp; Graph: metadata about how objects link to each other. No object internal id.
     * Contains relationship_Internal_ID and attributes for Create/Update Relationship APIs.
     * Path: {@code /metadata/api/v2/objects/relationship}.
     */
    RELATIONSHIP,

    /**
     * View metadata for a specific view. Requires Object Internal Id.
     * Path: {@code /metadata/api/v2/objects/view/{objectInternalId}}.
     */
    VIEW,

    /**
     * External resource metadata for a specific external resource. Requires Object Internal Id.
     * Path: {@code /metadata/api/v2/objects/externalResource/{objectInternalId}}.
     */
    EXTERNAL_RESOURCE;

    /** Path segment used in the URL after {@code /metadata/api/v2/objects/}. Null for DATAMODEL (uses tenantModel/datamodel). */
    public String getPathSegment() {
        switch (this) {
            case DATAMODEL:
                return null; // full path is tenantModel/datamodel
            case BUSINESS_ENTITY:
                return "businessEntity";
            case REFERENCE_ENTITY:
                return "referenceEntity";
            case RELATIONSHIP:
                return "relationship";
            case VIEW:
                return "view";
            case EXTERNAL_RESOURCE:
                return "externalResource";
            default:
                return null;
        }
    }

    /** True when the resource requires an Object Internal Id path parameter. */
    public boolean requiresObjectInternalId() {
        return getPathSegment() != null && this != RELATIONSHIP;
    }
}

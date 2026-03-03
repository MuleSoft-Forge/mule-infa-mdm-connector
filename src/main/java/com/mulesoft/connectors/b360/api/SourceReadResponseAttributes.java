/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.api;

import java.io.Serializable;
import java.util.Objects;

/**
 * Attributes for the Source Read operation output. Exposes the {@code _meta} section
 * from the Business 360 Source Record (entity-xref) API response so flows can use them
 * in expressions (e.g. {@code attributes.businessId}, {@code attributes.states}).
 *
 * @see <a href="https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Source_record_API.html">Source Record API</a>
 */
public final class SourceReadResponseAttributes implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String businessId;
    private final String businessEntity;
    private final String createdBy;
    private final String creationDate;
    private final String updatedBy;
    private final String lastUpdateDate;
    private final String sourceLastUpdatedDate;
    private final String sourceSystem;
    private final String sourcePrimaryKey;
    private final String xrefType;
    private final int statusCode;
    private final String requestId;

    public SourceReadResponseAttributes(String businessId, String businessEntity,
                                        String createdBy, String creationDate,
                                        String updatedBy, String lastUpdateDate, String sourceLastUpdatedDate,
                                        String sourceSystem, String sourcePrimaryKey,
                                        String xrefType, int statusCode, String requestId) {
        this.businessId = businessId;
        this.businessEntity = businessEntity;
        this.createdBy = createdBy;
        this.creationDate = creationDate;
        this.updatedBy = updatedBy;
        this.lastUpdateDate = lastUpdateDate;
        this.sourceLastUpdatedDate = sourceLastUpdatedDate;
        this.sourceSystem = sourceSystem;
        this.sourcePrimaryKey = sourcePrimaryKey;
        this.xrefType = xrefType;
        this.statusCode = statusCode;
        this.requestId = requestId != null ? requestId : "";
    }

    /** Unique identifier of the source record (from _meta.businessId). */
    public String getBusinessId() {
        return businessId;
    }

    /** Internal ID of the business entity (from _meta.businessEntity). */
    public String getBusinessEntity() {
        return businessEntity;
    }

    /** Name of the user who created the record (from _meta.createdBy). */
    public String getCreatedBy() {
        return createdBy;
    }

    /** Date when the record was created (from _meta.creationDate). */
    public String getCreationDate() {
        return creationDate;
    }

    /** Name of the user who last updated the record (from _meta.updatedBy). */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /** Date when the master record was last updated (from _meta.lastUpdateDate). */
    public String getLastUpdateDate() {
        return lastUpdateDate;
    }

    /** Date when the source record was last updated (from _meta.sourceLastUpdatedDate). */
    public String getSourceLastUpdatedDate() {
        return sourceLastUpdatedDate;
    }

    /** Internal ID of the source system (from _meta.sourceSystem). */
    public String getSourceSystem() {
        return sourceSystem;
    }

    /** Primary key of the source record (from _meta.sourcePrimaryKey). */
    public String getSourcePrimaryKey() {
        return sourcePrimaryKey;
    }

    /** Cross-reference type (from _meta.xrefType, e.g. DATA). */
    public String getXrefType() {
        return xrefType;
    }

    /** HTTP status code of the response. */
    public int getStatusCode() {
        return statusCode;
    }

    /** Request/tracing ID from response headers for flow correlation. */
    public String getRequestId() {
        return requestId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceReadResponseAttributes that = (SourceReadResponseAttributes) o;
        return statusCode == that.statusCode
                && Objects.equals(businessId, that.businessId)
                && Objects.equals(businessEntity, that.businessEntity)
                && Objects.equals(createdBy, that.createdBy)
                && Objects.equals(creationDate, that.creationDate)
                && Objects.equals(updatedBy, that.updatedBy)
                && Objects.equals(lastUpdateDate, that.lastUpdateDate)
                && Objects.equals(sourceLastUpdatedDate, that.sourceLastUpdatedDate)
                && Objects.equals(sourceSystem, that.sourceSystem)
                && Objects.equals(sourcePrimaryKey, that.sourcePrimaryKey)
                && Objects.equals(xrefType, that.xrefType)
                && Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(businessId, businessEntity, createdBy, creationDate, updatedBy,
                lastUpdateDate, sourceLastUpdatedDate, sourceSystem, sourcePrimaryKey, xrefType, statusCode, requestId);
    }
}

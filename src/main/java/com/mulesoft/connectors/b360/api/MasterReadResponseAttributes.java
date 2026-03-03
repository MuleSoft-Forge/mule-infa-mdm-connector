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
 * Attributes for the Master Read operation output. Exposes the {@code _meta} section
 * from the Business 360 Read Master Record API response.
 *
 * @see <a href="https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Business_entity_record_APIs.html">Business entity record APIs</a>
 */
public final class MasterReadResponseAttributes implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String businessId;
    private final String businessEntity;
    private final String state;
    private final String validation;
    private final String consolidation;
    private final String createdBy;
    private final String creationDate;
    private final String updatedBy;
    private final String lastUpdateDate;
    private final int statusCode;
    private final String requestId;

    public MasterReadResponseAttributes(String businessId, String businessEntity,
                                        String state, String validation, String consolidation,
                                        String createdBy, String creationDate,
                                        String updatedBy, String lastUpdateDate,
                                        int statusCode, String requestId) {
        this.businessId = businessId;
        this.businessEntity = businessEntity;
        this.state = state;
        this.validation = validation;
        this.consolidation = consolidation;
        this.createdBy = createdBy;
        this.creationDate = creationDate;
        this.updatedBy = updatedBy;
        this.lastUpdateDate = lastUpdateDate;
        this.statusCode = statusCode;
        this.requestId = requestId != null ? requestId : "";
    }

    /** Unique identifier of the business entity record (from _meta.businessId). */
    public String getBusinessId() {
        return businessId;
    }

    /** Internal ID of the business entity (from _meta.businessEntity). */
    public String getBusinessEntity() {
        return businessEntity;
    }

    /** Record state: active, inactive, or pending (from _meta.states.base or _meta.state). */
    public String getState() {
        return state;
    }

    /** Validation status: PENDING, PASSED, FAILED, IN_PROGRESS (from _meta.states.validation). */
    public String getValidation() {
        return validation;
    }

    /** Match and merge status (from _meta.states.consolidation). */
    public String getConsolidation() {
        return consolidation;
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
        MasterReadResponseAttributes that = (MasterReadResponseAttributes) o;
        return statusCode == that.statusCode
                && Objects.equals(businessId, that.businessId)
                && Objects.equals(businessEntity, that.businessEntity)
                && Objects.equals(state, that.state)
                && Objects.equals(validation, that.validation)
                && Objects.equals(consolidation, that.consolidation)
                && Objects.equals(createdBy, that.createdBy)
                && Objects.equals(creationDate, that.creationDate)
                && Objects.equals(updatedBy, that.updatedBy)
                && Objects.equals(lastUpdateDate, that.lastUpdateDate)
                && Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(businessId, businessEntity, state, validation, consolidation,
                createdBy, creationDate, updatedBy, lastUpdateDate, statusCode, requestId);
    }
}

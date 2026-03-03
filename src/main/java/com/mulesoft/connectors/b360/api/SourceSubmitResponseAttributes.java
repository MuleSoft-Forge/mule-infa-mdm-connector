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
 * Attributes for the Source Submit operation output. Exposes the response fields
 * from the Business 360 Create Master Record API (POST /entity/{businessEntity}).
 *
 * @see <a href="https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Business_entity_record_APIs.html">Business entity record APIs</a>
 */
public final class SourceSubmitResponseAttributes implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String businessId;
    private final boolean approvalRequired;
    private final int statusCode;
    private final String requestId;

    public SourceSubmitResponseAttributes(String businessId, boolean approvalRequired, int statusCode, String requestId) {
        this.businessId = businessId;
        this.approvalRequired = approvalRequired;
        this.statusCode = statusCode;
        this.requestId = requestId != null ? requestId : "";
    }

    /** Business ID of the created/updated record (from response.businessId). */
    public String getBusinessId() {
        return businessId;
    }

    /** Whether a data steward must approve the record (from response.approvalRequired). */
    public boolean isApprovalRequired() {
        return approvalRequired;
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
        SourceSubmitResponseAttributes that = (SourceSubmitResponseAttributes) o;
        return approvalRequired == that.approvalRequired
                && statusCode == that.statusCode
                && Objects.equals(businessId, that.businessId)
                && Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(businessId, approvalRequired, statusCode, requestId);
    }
}

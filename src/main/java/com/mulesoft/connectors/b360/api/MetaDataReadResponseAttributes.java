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
 * Attributes for the MetaData Read operation output. Exposes the resource type
 * that was queried, the resolved API path, and HTTP context so flows can
 * branch or log based on the metadata call that was made.
 *
 * @see <a href="https://knowledge.informatica.com/s/article/metadata-from-org?language=en_US">Informatica KB: Metadata from Org</a>
 */
public final class MetaDataReadResponseAttributes implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String resource;
    private final String objectInternalId;
    private final int statusCode;
    private final String requestId;

    public MetaDataReadResponseAttributes(String resource, String objectInternalId,
                                          int statusCode, String requestId) {
        this.resource = resource != null ? resource : "";
        this.objectInternalId = objectInternalId != null ? objectInternalId : "";
        this.statusCode = statusCode;
        this.requestId = requestId != null ? requestId : "";
    }

    /** Metadata resource type that was queried (e.g. DATAMODEL, BUSINESS_ENTITY, RELATIONSHIP). */
    public String getResource() {
        return resource;
    }

    /** Object internal id when resource requires it (e.g. c360.person for BUSINESS_ENTITY). Empty for DATAMODEL and RELATIONSHIP. */
    public String getObjectInternalId() {
        return objectInternalId;
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
        MetaDataReadResponseAttributes that = (MetaDataReadResponseAttributes) o;
        return statusCode == that.statusCode
                && Objects.equals(resource, that.resource)
                && Objects.equals(objectInternalId, that.objectInternalId)
                && Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, objectInternalId, statusCode, requestId);
    }
}

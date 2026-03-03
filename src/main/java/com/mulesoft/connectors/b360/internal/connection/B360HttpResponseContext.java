/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.connection;

import java.io.Serializable;
import java.util.Objects;

/**
 * Carries HTTP response metadata from {@link B360Connection#executeAsync} to operations
 * so they can set actual status code and request/tracing ID on response attributes.
 */
public final class B360HttpResponseContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final String requestId;

    public B360HttpResponseContext(int statusCode, String requestId) {
        this.statusCode = statusCode;
        this.requestId = requestId != null ? requestId : "";
    }

    public int getStatusCode() {
        return statusCode;
    }

    /** Request/tracing ID from response headers (e.g. INFA-REQUEST-ID, X-Request-Id). */
    public String getRequestId() {
        return requestId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B360HttpResponseContext that = (B360HttpResponseContext) o;
        return statusCode == that.statusCode && Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusCode, requestId);
    }
}

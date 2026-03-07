/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.api;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP response attributes for the Http Request operation.
 * Exposes status code, reason phrase, response headers, and the Informatica request/tracing ID.
 */
public final class HttpRequestResponseAttributes implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final String reasonPhrase;
    private final String requestId;
    private final Map<String, String> headers;

    public HttpRequestResponseAttributes(int statusCode, String reasonPhrase, String requestId,
                                         Map<String, String> headers) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase != null ? reasonPhrase : "";
        this.requestId = requestId != null ? requestId : "";
        this.headers = headers != null ? Collections.unmodifiableMap(headers) : Collections.emptyMap();
    }

    /** HTTP status code of the response (e.g. 200, 201, 404). */
    public int getStatusCode() {
        return statusCode;
    }

    /** HTTP reason phrase (e.g. "OK", "Not Found"). */
    public String getReasonPhrase() {
        return reasonPhrase;
    }

    /** Request/tracing ID from Informatica response headers (INFA-REQUEST-ID, X-Request-Id, Tracking-Id). */
    public String getRequestId() {
        return requestId;
    }

    /** All response headers as a flat map (last value wins for duplicate header names). */
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpRequestResponseAttributes that = (HttpRequestResponseAttributes) o;
        return statusCode == that.statusCode
                && Objects.equals(reasonPhrase, that.reasonPhrase)
                && Objects.equals(requestId, that.requestId)
                && Objects.equals(headers, that.headers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusCode, reasonPhrase, requestId, headers);
    }
}

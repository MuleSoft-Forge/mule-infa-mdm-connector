/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

/**
 * Informatica Intelligent Cloud Services REST API error object (version 3).
 * Returned when the API returns an error response.
 *
 * @see <a href="https://docs.informatica.com/cloud-common-services/administrator/current-version/rest-api-reference/informatica-intelligent-cloud-services-rest-api/error-object.html">Error object</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class B360ApiError implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("code")
    private String code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("details")
    private Object details;

    public B360ApiError() {}

    public B360ApiError(String code, String message, String requestId, Object details) {
        this.code = code;
        this.message = message;
        this.requestId = requestId;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Object getDetails() {
        return details;
    }

    public void setDetails(Object details) {
        this.details = details;
    }

    /**
     * Returns this error in its entirety for inclusion in runtime error messages.
     * The Mule runtime will show this when a B360 REST API call fails.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("B360 API error:");
        sb.append(" code=").append(code != null ? code : "");
        sb.append(", message=").append(message != null ? message : "");
        sb.append(", requestId=").append(requestId != null ? requestId : "");
        if (details != null) {
            sb.append(", details=").append(details);
        }
        return sb.toString();
    }

    /**
     * Same as {@link #toString()} but explicitly named for use in error messages.
     */
    public String toFullMessage() {
        return toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B360ApiError that = (B360ApiError) o;
        return Objects.equals(code, that.code)
                && Objects.equals(message, that.message)
                && Objects.equals(requestId, that.requestId)
                && Objects.equals(details, that.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, message, requestId, details);
    }
}

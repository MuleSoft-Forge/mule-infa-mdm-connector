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
 * Attributes for the Search operation output. Exposes pagination and result metadata
 * from the Business 360 Search API response so flows can use them (e.g. in expressions).
 *
 * @see <a href="https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Search_API.html">Search API</a>
 */
public final class SearchResponseAttributes implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int hits;
    private final int pageSize;
    private final int pageOffset;
    private final int recordsToReturn;
    private final int recordOffset;
    private final int maxRecords;
    private final int statusCode;
    private final String requestId;

    public SearchResponseAttributes(int hits, int pageSize, int pageOffset,
                                    int recordsToReturn, int recordOffset, int maxRecords,
                                    int statusCode, String requestId) {
        this.hits = hits;
        this.pageSize = pageSize;
        this.pageOffset = pageOffset;
        this.recordsToReturn = recordsToReturn;
        this.recordOffset = recordOffset;
        this.maxRecords = maxRecords;
        this.statusCode = statusCode;
        this.requestId = requestId != null ? requestId : "";
    }

    /** Total number of records matching the search (hits). */
    public int getHits() {
        return hits;
    }

    /** Page size from the response. */
    public int getPageSize() {
        return pageSize;
    }

    /** Page offset (pages skipped). */
    public int getPageOffset() {
        return pageOffset;
    }

    /** Number of records requested to return for this page. */
    public int getRecordsToReturn() {
        return recordsToReturn;
    }

    /** Record offset (records skipped) for this page. */
    public int getRecordOffset() {
        return recordOffset;
    }

    /** Maximum records allowed per request (e.g. 10000). */
    public int getMaxRecords() {
        return maxRecords;
    }

    /** HTTP status code of the response. */
    public int getStatusCode() {
        return statusCode;
    }

    /** Request/tracing ID from response headers (e.g. INFA-REQUEST-ID, X-Request-Id) for flow correlation. */
    public String getRequestId() {
        return requestId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchResponseAttributes that = (SearchResponseAttributes) o;
        return hits == that.hits
                && pageSize == that.pageSize
                && pageOffset == that.pageOffset
                && recordsToReturn == that.recordsToReturn
                && recordOffset == that.recordOffset
                && maxRecords == that.maxRecords
                && statusCode == that.statusCode
                && Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hits, pageSize, pageOffset, recordsToReturn, recordOffset, maxRecords, statusCode, requestId);
    }
}

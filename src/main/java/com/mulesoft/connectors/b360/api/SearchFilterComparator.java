/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.api;

/**
 * Search filter comparator. Renders as a dropdown in Anypoint Studio.
 * Matches B360 Search API filter condition operators.
 *
 * @see <a href="https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Search_API.html">Search API - filters</a>
 */
public enum SearchFilterComparator {

    EQUALS,
    NOT_EQUALS,
    IN,
    CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    IS_MISSING,
    IS_NOT_MISSING,
    BETWEEN,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL_TO,
    LESS_THAN,
    LESS_THAN_OR_EQUAL_TO;

    /** Value sent in the API request body. */
    public String getApiValue() {
        return name();
    }
}

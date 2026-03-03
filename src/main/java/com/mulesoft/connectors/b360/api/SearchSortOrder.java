/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.api;

/**
 * Sort direction for Search API sort spec. Renders as a dropdown in Anypoint Studio.
 */
public enum SearchSortOrder {

    ASCENDING,
    DESCENDING;

    /** Value sent in the API request body. */
    public String getApiValue() {
        return name();
    }
}

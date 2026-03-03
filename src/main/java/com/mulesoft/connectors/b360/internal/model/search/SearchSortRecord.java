/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.model.search;

import com.mulesoft.connectors.b360.api.SearchSortOrder;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

/**
 * One sort spec for the Search API sort array. Rendered as a row in the Sort section table.
 */
public class SearchSortRecord {

    @Parameter
    @DisplayName("Field Name")
    @Summary("Name of the field to sort by (e.g. c360person.firstName).")
    private String fieldName;

    @Parameter
    @DisplayName("Order")
    @Summary("Sort direction: ASCENDING or DESCENDING.")
    private SearchSortOrder order;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public SearchSortOrder getOrder() {
        return order;
    }

    public void setOrder(SearchSortOrder order) {
        this.order = order;
    }
}

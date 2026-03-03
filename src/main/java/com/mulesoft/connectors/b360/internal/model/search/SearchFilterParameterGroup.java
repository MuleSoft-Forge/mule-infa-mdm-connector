/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.model.search;

import com.mulesoft.connectors.b360.api.SearchFilterOperator;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.List;

/**
 * Optional parameter group for Search API filters. Add filter records in the table; each row is one filter.
 * The UI shows an edit-inline table to add/remove comparator, fieldName, fieldValue, and fieldValueGroups.
 *
 * @see <a href="https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Search_API.html">Search API - filters</a>
 */
public class SearchFilterParameterGroup {

    public static final String TAB_NAME = "General";

    @Parameter
    @Optional(defaultValue = "AND")
    @DisplayName("Operator")
    @Summary("Boolean operator that joins multiple filter records: AND (all must match) or OR (any can match).")
    @Placement(tab = TAB_NAME, order = 1)
    private SearchFilterOperator operator = SearchFilterOperator.AND;

    @Parameter
    @Optional
    @NullSafe
    @DisplayName("Filter")
    @Summary("Add filter records. Each record has comparator, field name, and values. Use + to add a row.")
    @Placement(tab = TAB_NAME, order = 2)
    private List<SearchFilterRecord> filters;

    public SearchFilterOperator getOperator() {
        return operator;
    }

    public void setOperator(SearchFilterOperator operator) {
        this.operator = operator;
    }

    public List<SearchFilterRecord> getFilters() {
        return filters;
    }

    public void setFilters(List<SearchFilterRecord> filters) {
        this.filters = filters;
    }
}

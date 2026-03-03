/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.model.search;

import com.mulesoft.connectors.b360.api.SearchFilterComparator;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.List;

/**
 * One filter record for the Search API filters array. Rendered as a row in the Filter section table.
 *
 * @see <a href="https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Search_API.html">Search API - filters</a>
 */
public class SearchFilterRecord {

    @Parameter
    @DisplayName("Comparator")
    @Summary("Condition for filtering. Use IN or BETWEEN for multiple values in Field Value; omit Field Value for IS_MISSING/IS_NOT_MISSING.")
    private SearchFilterComparator comparator;

    @Parameter
    @DisplayName("Field Name")
    @Summary("Name of the field to filter on (e.g. c360person.firstName, c360person.gender.Name). For error-type filtering use validationType or validationState.")
    private String fieldName;

    @Parameter
    @Optional
    @NullSafe
    @DisplayName("Field Value")
    @Summary("Value(s) to filter by. Single value for EQUALS, NOT_EQUALS, CONTAINS, STARTS_WITH, GREATER_THAN, etc. Use multiple entries for IN; two values for BETWEEN. Omit for IS_MISSING/IS_NOT_MISSING.")
    private List<String> fieldValue;

    @Parameter
    @Optional
    @NullSafe
    @DisplayName("Field Value Groups")
    @Summary("Optional. Filter by group (e.g. INFO, WARNING, ERROR for validationType).")
    private List<String> fieldValueGroups;

    public SearchFilterComparator getComparator() {
        return comparator;
    }

    public void setComparator(SearchFilterComparator comparator) {
        this.comparator = comparator;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public List<String> getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(List<String> fieldValue) {
        this.fieldValue = fieldValue;
    }

    public List<String> getFieldValueGroups() {
        return fieldValueGroups;
    }

    public void setFieldValueGroups(List<String> fieldValueGroups) {
        this.fieldValueGroups = fieldValueGroups;
    }
}

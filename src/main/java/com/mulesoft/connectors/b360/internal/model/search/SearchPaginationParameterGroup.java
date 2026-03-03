/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.model.search;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.param.display.Example;

/**
 * Parameter group for Search pagination: record offset and page size.
 */
public class SearchPaginationParameterGroup {

    public static final String TAB_NAME = "General";

    @Parameter
    @Optional(defaultValue = "0")
    @DisplayName("Record Offset")
    @Summary("Number of records to skip (for pagination).")
    @Example("0")
    @Placement(tab = TAB_NAME, order = 1)
    private Integer recordOffset = 0;

    @Parameter
    @Optional(defaultValue = "200")
    @DisplayName("Records To Return")
    @Summary("Maximum number of records to return for this page.")
    @Example("200")
    @Placement(tab = TAB_NAME, order = 2)
    private Integer recordsToReturn = 200;

    public Integer getRecordOffset() {
        return recordOffset;
    }

    public Integer getRecordsToReturn() {
        return recordsToReturn;
    }
}

/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.model.search;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.List;

/**
 * Optional parameter group for Search API sort. Add sort records; each row specifies fieldName and order.
 */
public class SearchSortParameterGroup {

    public static final String TAB_NAME = "General";

    @Parameter
    @Optional
    @NullSafe
    @DisplayName("Sort")
    @Summary("Add sort records. Each record has field name and order (ASCENDING/DESCENDING). Use + to add a row.")
    @Placement(tab = TAB_NAME, order = 1)
    private List<SearchSortRecord> sorts;

    public List<SearchSortRecord> getSorts() {
        return sorts;
    }

    public void setSorts(List<SearchSortRecord> sorts) {
        this.sorts = sorts;
    }
}

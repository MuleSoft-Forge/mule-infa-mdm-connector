/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.model.sourceread;

import com.mulesoft.connectors.b360.internal.metadata.B360EntityKeysResolver;
import com.mulesoft.connectors.b360.internal.valueprovider.B360BusinessEntityValueProvider;
import com.mulesoft.connectors.b360.internal.valueprovider.B360SourceSystemValueProvider;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.values.OfValues;

/**
 * Source Read parameters: Business Entity and Source System (both dropdowns from API, second dependent on first),
 * plus Source Primary Key. Uses ValueProvider (same pattern as IDP connector) so the second dropdown refreshes when the first changes.
 *
 * @see <a href="https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Source_record_API.html">Source Record API</a>
 */
public class SourceReadParameterGroup {

    public static final String TAB_NAME = "General";

    @Parameter
    @OfValues(B360BusinessEntityValueProvider.class)
    @MetadataKeyId(B360EntityKeysResolver.class)
    @DisplayName("Business Entity Id")
    @Summary("Internal ID of the business entity (e.g. c360.person, c360.organization).")
    @Placement(tab = TAB_NAME, order = 0)
    private String businessEntity;

    @Parameter
    @OfValues(B360SourceSystemValueProvider.class)
    @DisplayName("Source System")
    @Summary("Internal ID of the source system (e.g. c360.default.system). Options depend on selected Business Entity.")
    @Example("c360.default.system")
    @Placement(tab = TAB_NAME, order = 1)
    private String sourceSystem;

    @Parameter
    @DisplayName("Source Primary Key")
    @Summary("The primary key of the source record in the source system.")
    @Example("611d13e9e6dbd16ffa69a143")
    @Placement(tab = TAB_NAME, order = 2)
    private String sourcePKey;

    public String getBusinessEntity() {
        return businessEntity;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getSourcePKey() {
        return sourcePKey;
    }
}

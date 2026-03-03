/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.model.sourcesubmit;

import com.mulesoft.connectors.b360.internal.metadata.B360EntityKeysResolver;
import com.mulesoft.connectors.b360.internal.valueprovider.B360BusinessEntityValueProvider;
import com.mulesoft.connectors.b360.internal.valueprovider.B360SourceSystemValueProvider;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.values.OfValues;

/**
 * Source Submit parameters: Business Entity (dropdown), Source System (dependent dropdown),
 * optional Source Primary Key, and optional custom Business ID.
 *
 * @see <a href="https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Business_entity_record_APIs.html">Business entity record APIs</a>
 */
public class SourceSubmitParameterGroup {

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
    @Summary("Internal ID of the source system to which the record belongs (e.g. c360.default.system).")
    @Example("c360.default.system")
    @Placement(tab = TAB_NAME, order = 1)
    private String sourceSystem;

    @Parameter
    @Optional
    @DisplayName("Source Primary Key")
    @Summary("The primary key of the source record. If omitted, the MDM engine assigns one.")
    @Example("pkey910")
    @Placement(tab = TAB_NAME, order = 2)
    private String sourcePKey;

    @Parameter
    @Optional
    @DisplayName("Business ID")
    @Summary("Custom business ID for the record. When specified, its length must differ from the business ID format length configured for the entity.")
    @Example("999456789222123")
    @Placement(tab = TAB_NAME, order = 3)
    private String businessId;

    public String getBusinessEntity() {
        return businessEntity;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getSourcePKey() {
        return sourcePKey;
    }

    public String getBusinessId() {
        return businessId;
    }
}

/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.model.masterread;

import com.mulesoft.connectors.b360.internal.metadata.B360EntityKeysResolver;
import com.mulesoft.connectors.b360.internal.valueprovider.B360BusinessEntityValueProvider;
import com.mulesoft.connectors.b360.internal.valueprovider.B360SourceSystemValueProvider;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.values.OfValues;

/**
 * Master Read parameters: Business Entity (required), then either Business ID or
 * Source System + Source Primary Key. Validated at runtime — provide one lookup path, not both.
 * <p>
 * Combines two Informatica APIs into one operation:
 * <ul>
 *   <li>Read Master Record by Business ID: GET /entity/{businessEntity}/{businessId}</li>
 *   <li>Read Master Record by SourcePKey: GET /entity/{businessEntity}/{sourceSystem}/{sourcePKey}</li>
 * </ul>
 *
 * @see <a href="https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Business_entity_record_APIs.html">Business entity record APIs</a>
 */
public class MasterReadParameterGroup {

    public static final String TAB_NAME = "General";

    @Parameter
    @OfValues(B360BusinessEntityValueProvider.class)
    @MetadataKeyId(B360EntityKeysResolver.class)
    @DisplayName("Business Entity Id")
    @Summary("Internal ID of the business entity (e.g. c360.person, c360.organization).")
    @Placement(tab = TAB_NAME, order = 0)
    private String businessEntity;

    @Parameter
    @Optional
    @DisplayName("Business ID")
    @Summary("Unique identifier of the master record. Use this OR Source System + Source Primary Key, not both.")
    @Example("MDM0000000Q092")
    @Placement(tab = TAB_NAME, order = 1)
    private String businessId;

    @Parameter
    @Optional
    @OfValues(B360SourceSystemValueProvider.class)
    @DisplayName("Source System")
    @Summary("Internal ID of the source system. Required when looking up by source primary key instead of business ID.")
    @Example("c360.default.system")
    @Placement(tab = TAB_NAME, order = 2)
    private String sourceSystem;

    @Parameter
    @Optional
    @DisplayName("Source Primary Key")
    @Summary("Primary key of the source record. Required when looking up by source system instead of business ID.")
    @Example("1003885252")
    @Placement(tab = TAB_NAME, order = 3)
    private String sourcePKey;

    public String getBusinessEntity() {
        return businessEntity;
    }

    public String getBusinessId() {
        return businessId;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getSourcePKey() {
        return sourcePKey;
    }
}

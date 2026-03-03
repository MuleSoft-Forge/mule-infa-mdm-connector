/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.model.sourceread;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyPart;

/**
 * Multi-level metadata key for Source Read: level 1 = Business Entity Id, level 2 = Source System Id.
 * Used so the operation can show two dropdowns (Business Entity, then Source System) via a single
 * {@link org.mule.runtime.api.metadata.resolving.PartialTypeKeysResolver}.
 */
public class SourceReadKey {

    @Parameter
    @MetadataKeyPart(order = 1)
    @DisplayName("Business Entity Id")
    @Summary("Internal ID of the business entity (e.g. c360.person, c360.organization).")
    private String businessEntity;

    @Parameter
    @MetadataKeyPart(order = 2)
    @DisplayName("Source System")
    @Summary("Internal ID of the source system to which the record belongs (e.g. c360.default.system).")
    private String sourceSystem;

    public String getBusinessEntity() {
        return businessEntity;
    }

    public void setBusinessEntity(String businessEntity) {
        this.businessEntity = businessEntity;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }
}

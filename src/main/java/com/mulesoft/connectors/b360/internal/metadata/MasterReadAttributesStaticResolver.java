/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.metadata;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.resolving.AttributesStaticTypeResolver;

/**
 * Static resolver for Master Read operation output attributes. Exposes _meta fields
 * (businessId, businessEntity, state, validation, consolidation, createdBy, etc.).
 *
 * @see com.mulesoft.connectors.b360.api.MasterReadResponseAttributes
 */
public final class MasterReadAttributesStaticResolver extends AttributesStaticTypeResolver {

    @Override
    public String getCategoryName() {
        return "BusinessEntity";
    }

    @Override
    public MetadataType getStaticMetadata() {
        return buildAttributesType();
    }

    /** Shared for use as attributes resolver in Master Read and Source Read operations. */
    static MetadataType buildAttributesType() {
        ObjectTypeBuilder object =
                BaseTypeBuilder.create(MetadataFormat.JSON).objectType().id("MasterReadResponseAttributes");
        object.addField().key("businessId").value().stringType();
        object.addField().key("businessEntity").value().stringType();
        object.addField().key("state").value().stringType();
        object.addField().key("validation").value().stringType();
        object.addField().key("consolidation").value().stringType();
        object.addField().key("createdBy").value().stringType();
        object.addField().key("creationDate").value().stringType();
        object.addField().key("updatedBy").value().stringType();
        object.addField().key("lastUpdateDate").value().stringType();
        object.addField().key("statusCode").value().numberType();
        object.addField().key("requestId").value().stringType();
        return object.build();
    }
}

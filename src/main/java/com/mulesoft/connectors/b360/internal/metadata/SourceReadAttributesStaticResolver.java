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
 * Static resolver for Source Read operation output attributes. Exposes key _meta fields
 * (businessId, businessEntity, sourceSystem, sourcePrimaryKey, etc.) so Anypoint Studio
 * shows attributes even when Business Entity Id is not yet set.
 *
 * @see com.mulesoft.connectors.b360.api.SourceReadResponseAttributes
 */
public final class SourceReadAttributesStaticResolver extends AttributesStaticTypeResolver {

    @Override
    public String getCategoryName() {
        return "BusinessEntity";
    }

    @Override
    public MetadataType getStaticMetadata() {
        return buildAttributesType();
    }

    /** Shared so {@link SourceReadOutputMetadataResolver} can return the same attributes type. */
    static MetadataType buildAttributesType() {
        ObjectTypeBuilder object =
                BaseTypeBuilder.create(MetadataFormat.JSON).objectType().id("SourceReadResponseAttributes");
        object.addField().key("businessId").value().stringType();
        object.addField().key("businessEntity").value().stringType();
        object.addField().key("createdBy").value().stringType();
        object.addField().key("creationDate").value().stringType();
        object.addField().key("updatedBy").value().stringType();
        object.addField().key("lastUpdateDate").value().stringType();
        object.addField().key("sourceLastUpdatedDate").value().stringType();
        object.addField().key("sourceSystem").value().stringType();
        object.addField().key("sourcePrimaryKey").value().stringType();
        object.addField().key("xrefType").value().stringType();
        object.addField().key("statusCode").value().numberType();
        object.addField().key("requestId").value().stringType();
        return object.build();
    }
}

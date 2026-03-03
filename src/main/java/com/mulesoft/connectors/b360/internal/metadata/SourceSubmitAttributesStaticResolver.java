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
 * Static resolver for Source Submit operation output attributes. Exposes response fields
 * (businessId, approvalRequired, statusCode, requestId).
 *
 * @see com.mulesoft.connectors.b360.api.SourceSubmitResponseAttributes
 */
public final class SourceSubmitAttributesStaticResolver extends AttributesStaticTypeResolver {

    @Override
    public String getCategoryName() {
        return "BusinessEntity";
    }

    @Override
    public MetadataType getStaticMetadata() {
        return buildAttributesType();
    }

    /** Shared so {@link SourceSubmitOutputMetadataResolver} can return the same attributes type. */
    static MetadataType buildAttributesType() {
        ObjectTypeBuilder object =
                BaseTypeBuilder.create(MetadataFormat.JSON).objectType().id("SourceSubmitResponseAttributes");
        object.addField().key("businessId").value().stringType();
        object.addField().key("approvalRequired").value().booleanType();
        object.addField().key("statusCode").value().numberType();
        object.addField().key("requestId").value().stringType();
        return object.build();
    }
}

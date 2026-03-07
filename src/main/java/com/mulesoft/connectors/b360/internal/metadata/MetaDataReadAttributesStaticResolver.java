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
 * Static resolver for MetaData Read operation output attributes.
 * Exposes resource, objectInternalId, statusCode, and requestId.
 *
 * @see com.mulesoft.connectors.b360.api.MetaDataReadResponseAttributes
 */
public final class MetaDataReadAttributesStaticResolver extends AttributesStaticTypeResolver {

    @Override
    public String getCategoryName() {
        return "Metadata";
    }

    @Override
    public MetadataType getStaticMetadata() {
        ObjectTypeBuilder object =
                BaseTypeBuilder.create(MetadataFormat.JSON).objectType().id("MetaDataReadResponseAttributes");
        object.addField().key("resource").value().stringType();
        object.addField().key("objectInternalId").value().stringType();
        object.addField().key("statusCode").value().numberType();
        object.addField().key("requestId").value().stringType();
        return object.build();
    }
}

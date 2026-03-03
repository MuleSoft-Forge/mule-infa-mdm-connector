/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.metadata;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.resolving.AttributesStaticTypeResolver;

/**
 * Static resolver for Search operation output attributes. Does not depend on a metadata key,
 * so Anypoint Studio shows attributes (hits, pageSize, pageOffset, etc.) even when
 * Business Entity Id is not yet set.
 *
 * @see com.mulesoft.connectors.b360.api.SearchResponseAttributes
 */
public final class SearchAttributesStaticResolver extends AttributesStaticTypeResolver {

    @Override
    public String getCategoryName() {
        return "BusinessEntity";
    }

    @Override
    public MetadataType getStaticMetadata() {
        return buildAttributesType();
    }

    /** Shared so {@link SearchOutputMetadataResolver} can return the same attributes type. */
    static MetadataType buildAttributesType() {
        org.mule.metadata.api.builder.ObjectTypeBuilder object =
                BaseTypeBuilder.create(MetadataFormat.JSON).objectType().id("SearchResponseAttributes");
        object.addField().key("hits").value().numberType();
        object.addField().key("pageSize").value().numberType();
        object.addField().key("pageOffset").value().numberType();
        object.addField().key("recordsToReturn").value().numberType();
        object.addField().key("recordOffset").value().numberType();
        object.addField().key("maxRecords").value().numberType();
        object.addField().key("statusCode").value().numberType();
        object.addField().key("requestId").value().stringType();
        return object.build();
    }
}

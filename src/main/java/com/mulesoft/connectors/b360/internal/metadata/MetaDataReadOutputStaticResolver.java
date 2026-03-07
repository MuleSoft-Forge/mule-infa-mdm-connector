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
import org.mule.runtime.api.metadata.resolving.OutputStaticTypeResolver;

/**
 * Static resolver for MetaData Read operation output payload.
 * All three metadata resources (DATAMODEL, OBJECT, RELATIONSHIP) return complex JSON
 * structures, so we describe the output as an open JSON object.
 */
public final class MetaDataReadOutputStaticResolver extends OutputStaticTypeResolver {

    @Override
    public MetadataType getStaticMetadata() {
        return BaseTypeBuilder.create(MetadataFormat.JSON)
                .objectType()
                .id("MetadataResponse")
                .build();
    }
}

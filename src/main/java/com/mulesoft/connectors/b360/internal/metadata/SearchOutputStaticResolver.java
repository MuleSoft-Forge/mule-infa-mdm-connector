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
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.OutputStaticTypeResolver;

/**
 * Static resolver for Search operation output payload: describes the <em>element</em> type
 * (a single record as open object). The runtime treats the operation output as array of this type.
 * <p>
 * Uses only {@link BaseTypeBuilder} for static metadata; {@link #getGenericRecordType(MetadataContext)}
 * for dynamic resolution (generic record, no flat field list).
 */
public final class SearchOutputStaticResolver extends OutputStaticTypeResolver {

    @Override
    public MetadataType getStaticMetadata() {
        return BaseTypeBuilder.create(MetadataFormat.JSON)
                .objectType()
                .id("SearchRecord")
                .build();
    }

    /** Generic record type for Search output (open object). Use when response is nested, not flat. */
    public static MetadataType getGenericRecordType(MetadataContext context) throws MetadataResolvingException {
        if (context == null) {
            throw new MetadataResolvingException("MetadataContext is required", FailureCode.INVALID_METADATA_KEY);
        }
        return context.getTypeBuilder().objectType().id("SearchRecord").build();
    }
}

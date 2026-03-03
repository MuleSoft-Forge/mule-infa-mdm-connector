/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.metadata;

import com.mulesoft.connectors.b360.api.SearchResponseAttributes;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;

/**
 * Resolves only the <em>attributes</em> metadata for the Search operation so that
 * Anypoint Studio (design time) can show {@link SearchResponseAttributes} structure
 * (hits, pageSize, pageOffset, recordsToReturn, recordOffset, maxRecords).
 * <p>
 * Kept separate from output payload resolution for clearer control and consistency
 * with connectors that use a dedicated attributes resolver (e.g. PDFBox module).
 * Uses the same category as {@link B360EntityKeysResolver} so the entity type key
 * from the operation is used for resolution.
 */
public final class SearchAttributesMetadataResolver implements AttributesTypeResolver<String> {

    @Override
    public String getCategoryName() {
        return "BusinessEntity";
    }

    @Override
    public String getResolverName() {
        return "SearchAttributesMetadataResolver";
    }

    @Override
    public MetadataType getAttributesType(MetadataContext context, String key)
            throws MetadataResolvingException, ConnectionException {
        return context.getTypeLoader().load(SearchResponseAttributes.class);
    }
}

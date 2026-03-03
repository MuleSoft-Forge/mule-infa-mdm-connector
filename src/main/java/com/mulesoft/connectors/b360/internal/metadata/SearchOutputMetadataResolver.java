/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.metadata;

import com.mulesoft.connectors.b360.internal.connection.B360Connection;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

import java.util.Optional;

/**
 * Resolves the <em>output payload</em> and <em>attributes</em> metadata for the Search operation.
 * Output is a nested record type derived from the datamodel for the selected Business Entity Id
 * (e.g. p360.category → p360category.identifier, p360category.descriptions[], _meta). Falls back to generic
 * open object when datamodel or connection is unavailable. Attributes: static pagination (hits, pageSize, etc.).
 */
public final class SearchOutputMetadataResolver implements OutputTypeResolver<String>, AttributesTypeResolver<String> {

    @Override
    public String getCategoryName() {
        return "BusinessEntity";
    }

    @Override
    public String getResolverName() {
        return "SearchOutputMetadataResolver";
    }

    @Override
    public MetadataType getOutputType(MetadataContext context, String key)
            throws MetadataResolvingException, ConnectionException {
        Optional<B360Connection> connectionOpt = context.getConnection();
        if (connectionOpt.isPresent()) {
            return B360EntityOutputTypeBuilder.buildSearchOutputElementType(context, key, connectionOpt.get());
        }
        return B360EntityOutputTypeBuilder.buildSearchOutputElementType(context, key, null);
    }

    @Override
    public MetadataType getAttributesType(MetadataContext context, String key)
            throws MetadataResolvingException, ConnectionException {
        return SearchAttributesStaticResolver.buildAttributesType();
    }
}
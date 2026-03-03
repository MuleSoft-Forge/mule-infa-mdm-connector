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
 * Resolves the <em>output payload</em> and <em>attributes</em> metadata for the Source Read operation.
 * Output is a single record object derived from the datamodel for the selected Business Entity Id
 * (root-level keys without prefix, plus _meta and _contentMeta to match the Source Record API).
 * Key is the entity id string from {@link org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId} on the businessEntity parameter.
 * Falls back to generic open object when datamodel or connection is unavailable.
 * Attributes: static _meta fields (businessId, sourceSystem, etc.).
 */
public final class SourceReadOutputMetadataResolver implements OutputTypeResolver<String>, AttributesTypeResolver<String> {

    @Override
    public String getCategoryName() {
        return "BusinessEntity";
    }

    @Override
    public String getResolverName() {
        return "SourceReadOutputMetadataResolver";
    }

    @Override
    public MetadataType getOutputType(MetadataContext context, String entityKey)
            throws MetadataResolvingException, ConnectionException {
        Optional<B360Connection> connectionOpt = context.getConnection();
        if (connectionOpt.isPresent()) {
            return B360EntityOutputTypeBuilder.buildSourceReadOutputType(context, entityKey, connectionOpt.get());
        }
        return B360EntityOutputTypeBuilder.buildSourceReadOutputType(context, entityKey, null);
    }

    @Override
    public MetadataType getAttributesType(MetadataContext context, String entityKey)
            throws MetadataResolvingException, ConnectionException {
        return SourceReadAttributesStaticResolver.buildAttributesType();
    }
}

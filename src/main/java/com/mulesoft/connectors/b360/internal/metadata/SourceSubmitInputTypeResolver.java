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
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;

import java.util.Optional;

/**
 * Resolves the <em>input</em> metadata for the Source Submit Record Payload parameter.
 * The payload is the POST body (record field data); its structure depends on the selected Business Entity Id.
 * Omits _meta and _contentMeta (response-only); the input type includes only root-level entity fields.
 * Key is the entity id from {@link org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId}
 * on the Business Entity Id parameter.
 */
public final class SourceSubmitInputTypeResolver implements InputTypeResolver<String> {

    @Override
    public String getCategoryName() {
        return "BusinessEntity";
    }

    @Override
    public String getResolverName() {
        return "SourceSubmitInputTypeResolver";
    }

    @Override
    public MetadataType getInputMetadata(MetadataContext context, String entityKey)
            throws MetadataResolvingException, ConnectionException {
        Optional<B360Connection> connectionOpt = context.getConnection();
        B360Connection connection = connectionOpt.orElse(null);
        return B360EntityOutputTypeBuilder.buildSourceSubmitInputType(context, entityKey, connection);
    }
}

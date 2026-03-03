/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.metadata;

import com.mulesoft.connectors.b360.internal.connection.B360Connection;
import com.mulesoft.connectors.b360.internal.model.sourceread.SourceReadKey;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyBuilder;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.PartialTypeKeysResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Multi-level metadata keys for Source Read: level 1 = Business Entity Id, level 2 = Source System Id.
 * First dropdown shows business entities; after selection, second dropdown shows source systems.
 * Uses {@link B360EntityKeysResolver} for level 1 and {@link B360SourceSystemKeysResolver} for level 2.
 */
public class SourceReadTypeKeysResolver implements PartialTypeKeysResolver<SourceReadKey> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceReadTypeKeysResolver.class);

    private final B360EntityKeysResolver entityResolver = new B360EntityKeysResolver();
    private final B360SourceSystemKeysResolver sourceSystemResolver = new B360SourceSystemKeysResolver();

    @Override
    public String getCategoryName() {
        return "SourceRead";
    }

    @Override
    public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
        return entityResolver.getKeys(context);
    }

    @Override
    public MetadataKey resolveChilds(MetadataContext context, SourceReadKey key)
            throws MetadataResolvingException, ConnectionException {
        String businessEntity = key != null ? key.getBusinessEntity() : null;
        if (businessEntity == null || businessEntity.isEmpty()) {
            throw new MetadataResolvingException("Business Entity Id is required to resolve Source System keys.",
                    org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_METADATA_KEY);
        }
        Set<MetadataKey> sourceSystemKeys = new LinkedHashSet<>();
        Optional<B360Connection> connectionOpt = context.getConnection();
        if (connectionOpt.isPresent()) {
            sourceSystemKeys = sourceSystemResolver.getKeysForEntity(connectionOpt.get(), businessEntity);
            LOGGER.info("[B360 Source Read] resolveChilds entity={} from getKeysForEntity: {} keys.", businessEntity, sourceSystemKeys.size());
        } else {
            LOGGER.info("[B360 Source Read] resolveChilds entity={} no connection available.", businessEntity);
        }
        if (sourceSystemKeys.isEmpty()) {
            sourceSystemKeys = sourceSystemResolver.getKeys(context);
            LOGGER.info("[B360 Source Read] resolveChilds entity={} fallback to getKeys: {} keys.", businessEntity, sourceSystemKeys.size());
        }
        MetadataKeyBuilder parent = MetadataKeyBuilder.newKey(businessEntity).withDisplayName(businessEntity);
        for (MetadataKey child : sourceSystemKeys) {
            parent.withChild(MetadataKeyBuilder.newKey(child.getId()).withDisplayName(child.getDisplayName()).build());
        }
        return parent.build();
    }
}

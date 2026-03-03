/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.valueprovider;

import com.mulesoft.connectors.b360.internal.connection.B360Connection;
import com.mulesoft.connectors.b360.internal.metadata.B360SourceSystemKeysResolver;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Value provider for Source System dropdown, dependent on Business Entity Id.
 * Parameter name {@code businessEntity} must match the operation's first dropdown parameter so the SDK injects it.
 * When an entity is selected, returns only source systems for that entity (no fallback to full list).
 * When no entity is selected, returns the global list from the datamodel.
 */
public class B360SourceSystemValueProvider implements ValueProvider {

    @Parameter
    private String businessEntity;

    @Connection
    private B360Connection connection;

    @Override
    public Set<Value> resolve() throws ValueResolvingException {
        B360SourceSystemKeysResolver resolver = new B360SourceSystemKeysResolver();
        Set<MetadataKey> keys;
        try {
            if (businessEntity != null && !businessEntity.isEmpty()) {
                keys = resolver.getKeysForEntity(connection, businessEntity);
                // When entity is selected, return only entity-scoped list (no fallback to full list).
            } else {
                keys = resolver.getKeysForConnection(connection);
            }
        } catch (MetadataResolvingException e) {
            throw new ValueResolvingException(e.getMessage(), "INVALID_VALUE", e);
        }
        Map<String, String> idToDisplayName = new LinkedHashMap<>();
        for (MetadataKey key : keys) {
            String display = key.getDisplayName();
            idToDisplayName.put(key.getId(), display != null && !display.isEmpty() ? display : key.getId());
        }
        return ValueBuilder.getValuesFor(idToDisplayName);
    }
}

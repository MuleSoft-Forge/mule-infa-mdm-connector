/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.valueprovider;

import com.mulesoft.connectors.b360.internal.connection.B360Connection;
import com.mulesoft.connectors.b360.internal.metadata.B360EntityKeysResolver;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Value provider for Business Entity Id dropdown. Loads entities from B360 datamodel (same as Search).
 * Used with @OfValues for dependent combo: first dropdown (Business Entity), then Source System.
 */
public class B360BusinessEntityValueProvider implements ValueProvider {

    @Connection
    private B360Connection connection;

    @Override
    public Set<Value> resolve() throws ValueResolvingException {
        Map<String, String> idToDisplayName = new LinkedHashMap<>();
        try {
            for (MetadataKey key : new B360EntityKeysResolver().getKeysForConnection(connection)) {
                String display = key.getDisplayName();
                idToDisplayName.put(key.getId(), display != null && !display.isEmpty() ? display : key.getId());
            }
        } catch (MetadataResolvingException e) {
            throw new ValueResolvingException(e.getMessage(), "INVALID_VALUE", e);
        }
        return ValueBuilder.getValuesFor(idToDisplayName);
    }
}

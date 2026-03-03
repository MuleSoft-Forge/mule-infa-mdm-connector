/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.error;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides the set of B360 error types for operation error handling and Anypoint Studio.
 */
public final class B360ErrorProvider implements ErrorTypeProvider {

    @Override
    public Set<ErrorTypeDefinition> getErrorTypes() {
        return new HashSet<>(Arrays.asList(B360ErrorType.values()));
    }
}

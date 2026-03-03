/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.error;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.MuleErrors;

import java.util.Optional;

public enum B360ErrorType implements ErrorTypeDefinition<B360ErrorType> {
    CONNECTIVITY(MuleErrors.CONNECTIVITY),
    CLIENT_ERROR(CONNECTIVITY),
    SERVER_ERROR(CONNECTIVITY),
    TIMEOUT(CONNECTIVITY);

    private final ErrorTypeDefinition<?> parent;

    B360ErrorType(ErrorTypeDefinition<?> parent) {
        this.parent = parent;
    }

    B360ErrorType() {
        this.parent = null;
    }

    @Override
    public Optional<ErrorTypeDefinition<?>> getParent() {
        return Optional.ofNullable(parent);
    }
}

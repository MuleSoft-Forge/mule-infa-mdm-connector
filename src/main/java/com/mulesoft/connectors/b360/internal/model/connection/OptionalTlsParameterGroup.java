/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.model.connection;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.http.api.HttpConstants.Protocol;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.http.api.HttpConstants.Protocol.HTTPS;

/**
 * Optional TLS parameter group for connection providers. B360 uses HTTPS by default.
 */
public class OptionalTlsParameterGroup implements TlsParameterGroup {

    @Parameter
    @Optional(defaultValue = "HTTPS")
    @Expression(NOT_SUPPORTED)
    @Summary("Protocol to use for communication. Valid values are HTTP and HTTPS.")
    @Placement(tab = TAB_NAME, order = 1)
    private Protocol protocol = HTTPS;

    @Parameter
    @Optional
    @Expression(NOT_SUPPORTED)
    @DisplayName("TLS Configuration")
    @Summary("Optional TLS context for custom certificates/truststore.")
    @Placement(tab = TAB_NAME, order = 2)
    private TlsContextFactory tlsContext;

    @Override
    public Protocol getProtocol() {
        return protocol;
    }

    @Override
    public TlsContextFactory getTlsContext() {
        return tlsContext;
    }
}

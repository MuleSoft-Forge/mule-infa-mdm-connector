/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.model.connection;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.http.api.HttpConstants.Protocol;

/**
 * Defines the contract of a parameter group for TLS configuration.
 */
public interface TlsParameterGroup {

    String TAB_NAME = "Security";

    Protocol getProtocol();

    TlsContextFactory getTlsContext();
}

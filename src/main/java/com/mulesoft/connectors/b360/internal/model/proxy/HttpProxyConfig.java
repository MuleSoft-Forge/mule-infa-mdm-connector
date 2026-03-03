/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.model.proxy;

import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.http.api.client.proxy.ProxyConfig;
import org.mule.sdk.api.annotation.semantics.connectivity.ConfiguresProxy;
import org.mule.sdk.api.annotation.semantics.connectivity.Host;
import org.mule.sdk.api.annotation.semantics.connectivity.Port;
import org.mule.sdk.api.annotation.semantics.security.Username;

import java.util.Objects;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;

/**
 * HTTP proxy configuration for outbound connections (host, port, optional proxy auth).
 */
@ConfiguresProxy
public class HttpProxyConfig implements ProxyConfig {

    @Parameter
    @Expression(NOT_SUPPORTED)
    @Summary("Proxy host")
    @Host
    private String host;

    @Parameter
    @Expression(NOT_SUPPORTED)
    @Summary("Proxy port")
    @Port
    private int port = Integer.MAX_VALUE;

    @Parameter
    @Optional
    @Expression(NOT_SUPPORTED)
    @Summary("Username for proxy authentication")
    @Username
    private String username;

    @Parameter
    @Optional
    @Expression(NOT_SUPPORTED)
    @Summary("Password for proxy authentication")
    @Password
    private String password;

    @Parameter
    @Optional
    @Expression(NOT_SUPPORTED)
    @Summary("Comma-separated hosts that bypass the proxy")
    private String nonProxyHosts;

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getNonProxyHosts() {
        return nonProxyHosts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpProxyConfig that = (HttpProxyConfig) o;
        return port == that.port
                && Objects.equals(host, that.host)
                && Objects.equals(username, that.username)
                && Objects.equals(password, that.password)
                && Objects.equals(nonProxyHosts, that.nonProxyHosts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, username, password, nonProxyHosts);
    }
}

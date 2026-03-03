/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.connection.provider;

import com.mulesoft.connectors.b360.internal.connection.B360Connection;
import com.mulesoft.connectors.b360.internal.operation.utils.B360Endpoints;
import com.mulesoft.connectors.b360.internal.operation.utils.B360Utils;
import com.mulesoft.connectors.b360.internal.model.connection.TlsParameterGroup;
import com.mulesoft.connectors.b360.internal.model.proxy.HttpProxyConfig;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.http.api.HttpConstants;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.tcp.TcpClientSocketProperties;
import org.mule.runtime.http.api.tcp.TcpClientSocketPropertiesBuilder;
import org.mule.runtime.api.lifecycle.Startable;

import javax.inject.Inject;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import static org.mule.runtime.http.api.HttpConstants.Protocol.HTTP;
import static org.mule.runtime.http.api.HttpConstants.Protocol.HTTPS;

/**
 * Base connection provider for B360. Manages HTTP client lifecycle and optional TLS.
 * Subclasses implement authentication (e.g. Basic via login API, future OAuth).
 */
public abstract class BaseB360ConnectionProvider implements CachedConnectionProvider<B360Connection>, Initialisable, Startable, Stoppable {

    @RefName
    private String configName;

    @Inject
    private HttpService httpService;

    private HttpClient httpClient;
    private TlsContextFactory effectiveTlsContextFactory;

    // --- Advanced tab ---
    @Parameter
    @Optional(defaultValue = "30")
    @Expression(NOT_SUPPORTED)
    @Summary("Timeout for establishing connections to the remote service.")
    @Placement(tab = ADVANCED_TAB, order = 1)
    private Integer connectionTimeout;

    @Parameter
    @Optional(defaultValue = "SECONDS")
    @Expression(NOT_SUPPORTED)
    @Summary("Time unit for connection timeout.")
    @Placement(tab = ADVANCED_TAB, order = 2)
    private java.util.concurrent.TimeUnit connectionTimeoutUnit = SECONDS;

    @Parameter
    @Optional(defaultValue = "true")
    @Expression(NOT_SUPPORTED)
    @Summary("If false, each connection is closed after the first request.")
    @Placement(tab = ADVANCED_TAB, order = 3)
    private boolean usePersistentConnections = true;

    @Parameter
    @Optional(defaultValue = "-1")
    @Expression(NOT_SUPPORTED)
    @Summary("Maximum number of outbound connections (-1 = unlimited).")
    @Placement(tab = ADVANCED_TAB, order = 4)
    private Integer maxConnections;

    @Parameter
    @Optional(defaultValue = "30")
    @Expression(NOT_SUPPORTED)
    @Summary("How long a connection can remain idle before it is closed.")
    @Placement(tab = ADVANCED_TAB, order = 5)
    private Integer connectionIdleTimeout;

    @Parameter
    @Optional(defaultValue = "SECONDS")
    @Expression(NOT_SUPPORTED)
    @Summary("Time unit for connection idle timeout.")
    @Placement(tab = ADVANCED_TAB, order = 6)
    private java.util.concurrent.TimeUnit connectionIdleTimeoutUnit = SECONDS;

    @Parameter
    @Optional(defaultValue = "false")
    @Expression(NOT_SUPPORTED)
    @Summary("Whether to stream response bodies.")
    @Placement(tab = ADVANCED_TAB, order = 7)
    private boolean streamResponse;

    @Parameter
    @Optional(defaultValue = "-1")
    @Expression(NOT_SUPPORTED)
    @Summary("Response buffer size in bytes (-1 = default).")
    @Placement(tab = ADVANCED_TAB, order = 8)
    private int responseBufferSize = -1;

    @Parameter
    @Optional(defaultValue = "false")
    @Expression(NOT_SUPPORTED)
    @DisplayName("Bypass Metadata Cache")
    @Summary("When true, fetches fresh metadata from Informatica B360, ignoring the local design-time cache. Use this if you recently updated your B360 datamodel.")
    @Placement(tab = ADVANCED_TAB, order = 10)
    private boolean bypassMetadataCache;

    // --- Proxy tab ---
    @Parameter
    @Optional
    @Summary("Proxy configuration for outbound connections.")
    @Placement(tab = "Proxy")
    private HttpProxyConfig proxyConfig;

    /**
     * Subclasses that use TLS must override and return their TLS config.
     */
    protected java.util.Optional<TlsParameterGroup> getTlsConfig() {
        return java.util.Optional.empty();
    }

    @Override
    public void initialise() throws InitialisationException {
        verifyConnectionParameters();
        TlsParameterGroup tls = getTlsConfig().orElse(null);
        if (tls == null) {
            return;
        }
        HttpConstants.Protocol protocol = tls.getProtocol();
        TlsContextFactory tlsContextFactory = tls.getTlsContext();

        if (protocol.equals(HTTP) && tlsContextFactory != null) {
            throw new InitialisationException(createStaticMessage(
                    "TLS context cannot be configured with protocol HTTP. Use protocol HTTPS when configuring TLS."),
                    this);
        }

        if (protocol.equals(HTTPS) && tlsContextFactory == null) {
            tlsContextFactory = TlsContextFactory.builder().buildDefault();
        }
        if (tlsContextFactory != null) {
            if (tlsContextFactory instanceof Initialisable) {
                ((Initialisable) tlsContextFactory).initialise();
            }
            effectiveTlsContextFactory = tlsContextFactory;
        }
    }

    @Override
    public void start() throws MuleException {
        HttpClientConfiguration.Builder builder = new HttpClientConfiguration.Builder()
                .setName(format("b360.connect.%s", configName != null ? configName : "default"));
        if (effectiveTlsContextFactory != null) {
            builder.setTlsContextFactory(effectiveTlsContextFactory);
        }
        if (proxyConfig != null) {
            builder.setProxyConfig(proxyConfig);
        }
        builder.setMaxConnections(maxConnections != null ? maxConnections : -1)
                .setUsePersistentConnections(usePersistentConnections)
                .setConnectionIdleTimeout(asMillis(connectionIdleTimeout, connectionIdleTimeoutUnit))
                .setStreaming(streamResponse)
                .setResponseBufferSize(responseBufferSize >= 0 ? responseBufferSize : -1);

        TcpClientSocketPropertiesBuilder socketProps = TcpClientSocketProperties.builder();
        if (connectionTimeout != null && connectionTimeout > 0) {
            socketProps.connectionTimeout(asMillis(connectionTimeout, connectionTimeoutUnit));
        }
        builder.setClientSocketProperties(socketProps.build());

        httpClient = httpService.getClientFactory().create(builder.build());
        httpClient.start();
    }

    private void verifyConnectionParameters() throws InitialisationException {
        int max = maxConnections != null ? maxConnections : -1;
        if (max == 0 || max < -1) {
            throw new InitialisationException(createStaticMessage(
                    "maxConnections must be positive or -1 for unlimited."), this);
        }
        if (!usePersistentConnections) {
            connectionIdleTimeout = 0;
        }
    }

    private static int asMillis(Integer value, java.util.concurrent.TimeUnit unit) {
        if (value == null || value < 0) {
            return -1;
        }
        return (int) unit.toMillis(value);
    }

    @Override
    public void stop() throws MuleException {
        if (httpClient != null) {
            httpClient.stop();
            httpClient = null;
        }
    }

    protected final HttpClient getHttpClient() {
        return httpClient;
    }

    protected boolean isBypassMetadataCache() {
        return bypassMetadataCache;
    }

    @Override
    public void disconnect(B360Connection connection) {
        connection.invalidate();
    }

    @Override
    public ConnectionValidationResult validate(B360Connection connection) {
        // When using JWT, refresh at least 5 minutes before expiry to avoid service interruption (per Informatica JWT Support).
        if (connection.shouldRefreshSession(300)) {
            return failure("JWT token expiring within 5 minutes; connection will be re-established.", null);
        }
        String validateUri = connection.getBaseApiUrl() + B360Endpoints.BUSINESS_ENTITY;
        HttpRequest request = HttpRequest.builder()
                .uri(validateUri)
                .method(HttpConstants.Method.GET)
                .addHeader("Accept", "application/json")
                .addHeader("IDS-SESSION-ID", connection.getSessionId())
                .build();
        try {
            org.mule.runtime.http.api.domain.message.response.HttpResponse response =
                    getHttpClient().send(request);
            int status = response.getStatusCode();
            if (status >= 200 && status < 300) {
                return success();
            }
            if (status == 400 || status == 404) {
                return success();
            }
            String body = response.getEntity() != null
                    ? B360Utils.readStreamAsString(response.getEntity().getContent())
                    : null;
            String message = B360ConnectionErrorMessages.forValidationFailure(
                    status, response.getReasonPhrase(), body, validateUri);
            return failure(message, new Exception("HTTP " + status));
        } catch (Exception e) {
            String message = B360ConnectionErrorMessages.forException(e, validateUri);
            return failure(message, e);
        }
    }
}

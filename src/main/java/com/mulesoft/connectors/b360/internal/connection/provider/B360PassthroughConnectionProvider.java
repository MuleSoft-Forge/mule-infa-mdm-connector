/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.connection.provider;

import com.mulesoft.connectors.b360.internal.connection.B360Connection;
import com.mulesoft.connectors.b360.internal.model.connection.OptionalTlsParameterGroup;
import com.mulesoft.connectors.b360.internal.model.connection.TlsParameterGroup;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;

/**
 * Passthrough connection provider that delegates authentication to the caller.
 * <p>
 * The connector does <b>not</b> manage login, refresh, or logout. The caller supplies
 * a pre-obtained session ID (IDS-SESSION-ID) and the B360 MDM base API URL at runtime,
 * typically via DataWeave expressions (e.g. {@code #[attributes.headers.authorization]}).
 * </p>
 * <h3>Memory / Connection-Pool Considerations</h3>
 * <p>
 * Because both {@code sessionId} and {@code baseApiUrl} accept expressions, Mule treats
 * each unique combination of resolved values as a distinct connection key. This means a
 * new {@link B360Connection} instance is created whenever the expression evaluates to a
 * value not already in the pool. To prevent unbounded growth, Mule's built-in
 * <b>Expiration Policy</b> (configurable in the XML/Studio on the {@code <b360:config>}
 * element) automatically evicts idle connections. The default policy (dynamic TTL with
 * 5-minute idle timeout) is suitable for most workloads; users processing high-cardinality
 * tokens should tune the policy or switch to a shorter idle timeout.
 * </p>
 */
@Alias("passthrough")
@DisplayName("Passthrough Auth Connection Provider")
@Summary("Uses a pre-obtained session ID. The connector does not manage login or refresh."
        + "<ul>"
        + "<li> Supply the IDS-SESSION-ID via a DataWeave expression (e.g. from a prior Login call)"
        + "<li> Supply the B360 MDM base API URL (e.g. https://use4-mdm.dm-us.informaticacloud.com)"
        + "<li> Configure an Expiration Policy on the config element to control idle-connection cleanup"
        + "</ul>")
public class B360PassthroughConnectionProvider extends BaseB360ConnectionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(B360PassthroughConnectionProvider.class);

    @Parameter
    @DisplayName("Session ID")
    @Summary("IDS-SESSION-ID value to send with every request. Accepts DataWeave expressions"
            + " (e.g. #[vars.sessionId] or #[attributes.headers.'ids-session-id']).")
    @Password
    @Expression(SUPPORTED)
    private String sessionId;

    @Parameter
    @DisplayName("B360 MDM Base API URL")
    @Summary("Base URL of the B360 MDM REST API, already in MDM form"
            + " (e.g. https://use4-mdm.dm-us.informaticacloud.com)."
            + " Accepts DataWeave expressions.")
    @Expression(SUPPORTED)
    private String baseApiUrl;

    @ParameterGroup(name = "tls")
    private OptionalTlsParameterGroup tlsConfig;

    @Override
    protected java.util.Optional<TlsParameterGroup> getTlsConfig() {
        return java.util.Optional.ofNullable(tlsConfig);
    }

    @Override
    public B360Connection connect() throws ConnectionException {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new ConnectionException(
                    "Session ID is required for the Passthrough connection provider. "
                            + "Provide a valid IDS-SESSION-ID value or DataWeave expression.");
        }
        if (baseApiUrl == null || baseApiUrl.trim().isEmpty()) {
            throw new ConnectionException(
                    "B360 MDM Base API URL is required for the Passthrough connection provider. "
                            + "Provide a valid URL (e.g. https://use4-mdm.dm-us.informaticacloud.com).");
        }
        String normalizedUrl = baseApiUrl.trim();
        if (normalizedUrl.endsWith("/")) {
            normalizedUrl = normalizedUrl.substring(0, normalizedUrl.length() - 1);
        }
        LOGGER.debug("Creating passthrough B360 connection to {}", normalizedUrl);
        return new B360Connection(sessionId.trim(), normalizedUrl, getHttpClient(), isBypassMetadataCache());
    }
}

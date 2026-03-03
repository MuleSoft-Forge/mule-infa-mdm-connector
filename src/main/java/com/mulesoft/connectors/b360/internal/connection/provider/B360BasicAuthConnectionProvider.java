/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.connection.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.mulesoft.connectors.b360.internal.connection.B360Connection;
import com.mulesoft.connectors.b360.internal.connection.B360Connection.B360ConnectionException;
import com.mulesoft.connectors.b360.internal.model.connection.OptionalTlsParameterGroup;
import com.mulesoft.connectors.b360.internal.model.connection.TlsParameterGroup;
import com.mulesoft.connectors.b360.internal.operation.utils.B360Utils;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.http.api.HttpConstants;
import org.mule.runtime.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.sdk.api.annotation.semantics.security.Username;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;

/**
 * B360 connection using username/password login (Informatica Cloud Login API V3).
 * Username and password support expressions (e.g. secure properties).
 */
@Alias("basic")
@DisplayName("Basic Auth Connection Provider")
public class B360BasicAuthConnectionProvider extends BaseB360ConnectionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(B360BasicAuthConnectionProvider.class);
    private static final String LOGIN_PATH = "/saas/public/core/v3/login";

    @Parameter
    @Optional(defaultValue = "https://<Regional Hostname: Like dmp-us.informaticacloud.com>/saas/public/core/v3/login")
    @DisplayName("Login URL")
    @Summary("Informatica Cloud login URL. Use the full path above or just the host (e.g. https://dmp-us.informaticacloud.com).")
    @Expression(SUPPORTED)
    private String baseUrl;

    @Parameter
    @DisplayName("Username")
    @Summary("Informatica Intelligent Cloud Services user name for the organization that you want to log in to. Maximum length is 255 characters.")
    @Username
    @Expression(SUPPORTED)
    private String username;

    @Parameter
    @DisplayName("Password")
    @Summary("Informatica Intelligent Cloud Services password. Maximum length is 255 characters.")
    @Password
    @Expression(SUPPORTED)
    private String password;

    @ParameterGroup(name = "tls")
    private OptionalTlsParameterGroup tlsConfig;

    @Override
    protected java.util.Optional<TlsParameterGroup> getTlsConfig() {
        return java.util.Optional.ofNullable(tlsConfig);
    }

    @Override
    public B360Connection connect() throws ConnectionException {
        String loginUrl = buildLoginUrl(baseUrl);
        Map<String, String> login = new LinkedHashMap<>();
        login.put("username", username != null ? username : "");
        login.put("password", password != null ? password : "");
        String body;
        try {
            body = B360Utils.OBJECT_MAPPER.writeValueAsString(login);
        } catch (JsonProcessingException e) {
            throw new ConnectionException("Failed to build login request body", e);
        }
        // TODO: comment out to disable request payload logging (body contains credentials)
        // LOGGER.info("INFA MDM Login request payload: {}", body);

        HttpRequest request = HttpRequest.builder()
                .uri(loginUrl)
                .method(HttpConstants.Method.POST)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .entity(new InputStreamHttpEntity(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))))
                .build();

        try {
            HttpResponse response = getHttpClient().send(request);
            int status = response.getStatusCode();
            if (status != 200) {
                String responseBody = response.getEntity() != null
                        ? B360Utils.readStreamAsString(response.getEntity().getContent())
                        : null;
                String message = B360ConnectionErrorMessages.forLoginFailure(
                        status, response.getReasonPhrase(), responseBody);
                throw new ConnectionException(message);
            }
            String responseBody = B360Utils.readStreamAsString(response.getEntity().getContent());
            JsonNode root = B360Utils.OBJECT_MAPPER.readTree(responseBody);
            // API returns sessionId under userInfo (or root); value may be a session ID or a JWT (per Informatica JWT Support).
            String sessionId = root.has("userInfo") && root.get("userInfo").has("sessionId")
                    ? root.get("userInfo").get("sessionId").asText()
                    : (root.has("sessionId") ? root.get("sessionId").asText() : "");
            String baseApiUrl = null;
            if (root.has("products") && root.get("products").isArray() && root.get("products").size() > 0) {
                baseApiUrl = root.get("products").get(0).has("baseApiUrl")
                        ? root.get("products").get(0).get("baseApiUrl").asText()
                        : null;
            }
            if (baseApiUrl == null && root.has("baseApiUrl")) {
                baseApiUrl = root.get("baseApiUrl").asText();
            }
            if (sessionId == null || sessionId.isEmpty()) {
                throw new ConnectionException("Login response missing sessionId");
            }
            if (baseApiUrl == null || baseApiUrl.isEmpty()) {
                throw new ConnectionException("Login response missing baseApiUrl");
            }
            // Per Informatica B360 REST API: remove /saas and replace first host segment with {segment}-mdm
            // e.g. https://use4.dm-us.informaticacloud.com/saas → https://use4-mdm.dm-us.informaticacloud.com
            // e.g. https://usw1.dmp-us.informaticacloud.com/saas → https://usw1-mdm.dmp-us.informaticacloud.com
            String publicUrl = toB360MdmBaseUrl(baseApiUrl);
            return new B360Connection(sessionId, publicUrl, getHttpClient(), isBypassMetadataCache());
        } catch (ConnectionException e) {
            throw e;
        } catch (B360ConnectionException e) {
            throw new ConnectionException(e.getMessage(), e);
        } catch (Exception e) {
            String message = B360ConnectionErrorMessages.forException(e);
            throw new ConnectionException(message, e);
        }
    }

    /** Resolves login URL: if baseUrl is already the full login path, use it; otherwise append LOGIN_PATH. */
    private static String buildLoginUrl(String baseUrl) {
        if (baseUrl == null) baseUrl = "";
        if (baseUrl.contains("/saas/public/core/v3/login")) return baseUrl;
        String base = baseUrl.trim().endsWith("/") ? baseUrl.trim().substring(0, baseUrl.trim().length() - 1) : baseUrl.trim();
        return base.isEmpty() ? "https://<host>" + LOGIN_PATH : base + LOGIN_PATH;
    }

    /**
     * Converts login baseApiUrl to B360 MDM REST API base URL per Informatica docs:
     * Remove /saas and replace the first host segment with {segment}-mdm.
     */
    private static String toB360MdmBaseUrl(String baseApiUrl) {
        if (baseApiUrl == null) return "";
        String withoutSaas = baseApiUrl.replace("/saas", "").trim();
        if (withoutSaas.endsWith("/")) {
            withoutSaas = withoutSaas.substring(0, withoutSaas.length() - 1);
        }
        // Replace first subdomain (e.g. use4, usw1) with {subdomain}-mdm
        return withoutSaas.replaceFirst("(://)([^.]+)(\\.)", "$1$2-mdm$3");
    }
}

/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.operation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.mulesoft.connectors.b360.api.SourceReadResponseAttributes;
import com.mulesoft.connectors.b360.internal.config.B360Configuration;
import com.mulesoft.connectors.b360.internal.connection.B360Connection;
import com.mulesoft.connectors.b360.internal.connection.B360HttpResponseContext;
import com.mulesoft.connectors.b360.internal.metadata.SourceReadOutputMetadataResolver;
import com.mulesoft.connectors.b360.internal.model.sourceread.SourceReadParameterGroup;
import com.mulesoft.connectors.b360.internal.operation.utils.B360Endpoints;
import com.mulesoft.connectors.b360.internal.operation.utils.B360Utils;
import com.mulesoft.connectors.b360.internal.error.B360ErrorProvider;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.http.api.HttpConstants;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;

/**
 * Source Read API: retrieve a single business entity source record by source system and primary key.
 * GET /business-entity/public/api/v1/entity-xref/{businessEntity}/{sourceSystem}/{sourcePKey}
 * <p>
 * Returns a {@link Result}: payload is the full source record (JSON object including field data,
 * {@code _meta}, and optionally {@code _contentMeta}); attributes are {@link SourceReadResponseAttributes}
 * (businessId, businessEntity, sourceSystem, sourcePrimaryKey, xrefType, statusCode).
 *
 * @see <a href="https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Source_record_API.html">Source Record API</a>
 */
public class SourceReadOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceReadOperation.class);

    @DisplayName("INFA MDM - Source Read")
    @Summary("Read a source record (one system's contribution / cross-reference) by business entity, source system, and source primary key." +
            "<ul>" +
            "<li> Uses the Business 360 Source Record API (GET " + B360Endpoints.ENTITY_XREF + "/{businessEntity}/{sourceSystem}/{sourcePKey})" +
            "<li> Optional query: _resolveCrosswalk=true and _showContentMeta=true (same " + B360Endpoints.QUERY_SHOW_CONTENT_META + " as INFA MDM - Master Search)" +
            "<li> Returns the full source record including _meta and optionally _contentMeta and dataEnhancementRule" +
            "<li> <a href=\"https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Source_record_API.html\">Source Record API docs</a>" +
            "</ul>")
    @MediaType(value = "application/json", strict = false)
    @OutputResolver(output = SourceReadOutputMetadataResolver.class, attributes = SourceReadOutputMetadataResolver.class)
    @Throws(B360ErrorProvider.class)
    public void sourceRead(
            @Config B360Configuration config,
            @Connection B360Connection connection,
            @ParameterGroup(name = "Source Record")
            @DisplayName("Source Record")
            @Summary("Business Entity Id (dropdown), Source System (text, e.g. c360.default.system), and Source Primary Key.")
            SourceReadParameterGroup sourceRecord,
            @Optional(defaultValue = "false")
            @DisplayName("Resolve Crosswalk")
            @Summary("Indicates whether to standardize picklist values based on the source system configuration. Set to true to standardize. When you specify this parameter, you must set the Source System parameter. Default is false.")
            @Placement(tab = ADVANCED_TAB, order = 1)
            boolean resolveCrosswalk,
            @Optional(defaultValue = "false")
            @DisplayName("Show Content Meta")
            @Summary("When true, adds query parameter _showContentMeta=true (same as INFA MDM - Master Search). The API returns a _contentMeta section (trust score, survivorship, etc.).")
            @Placement(tab = ADVANCED_TAB, order = 2)
            boolean showContentMeta,
            CompletionCallback<Map<String, Object>, SourceReadResponseAttributes> callback) {

        SourceReadParameterGroup params = sourceRecord;
        String businessEntity = params != null ? params.getBusinessEntity() : null;
        String sourceSystem = params != null ? params.getSourceSystem() : null;
        String sourcePKey = params != null ? params.getSourcePKey() : null;

        String path = buildPath(connection.getBaseApiUrl(), businessEntity, sourceSystem, sourcePKey);
        LOGGER.info("INFA MDM Source Read request: {}", path);

        HttpRequestBuilder builder = HttpRequest.builder()
                .uri(path)
                .method(HttpConstants.Method.GET)
                .addHeader("Accept", "application/json");
        if (resolveCrosswalk) builder.addQueryParam("_resolveCrosswalk", "true");
        if (showContentMeta) builder.addQueryParam(B360Endpoints.QUERY_SHOW_CONTENT_META, "true");

        connection.executeAsync(builder, new CompletionCallback<InputStream, B360HttpResponseContext>() {
            @Override
            public void success(Result<InputStream, B360HttpResponseContext> asyncResult) {
                try {
                    InputStream responseStream = asyncResult.getOutput();
                    B360HttpResponseContext httpContext = asyncResult.getAttributes().orElse(new B360HttpResponseContext(200, null));
                    JsonNode root = B360Utils.OBJECT_MAPPER.readTree(responseStream);
                    SourceReadResponseAttributes attrs = parseResponseAttributes(root, httpContext.getStatusCode(), httpContext.getRequestId());
                    Map<String, Object> output = B360Utils.OBJECT_MAPPER.convertValue(root, new TypeReference<Map<String, Object>>() {});
                    callback.success(Result.<Map<String, Object>, SourceReadResponseAttributes>builder()
                            .output(output)
                            .attributes(attrs)
                            .mediaType(org.mule.runtime.api.metadata.MediaType.APPLICATION_JAVA)
                            .build());
                } catch (Exception e) {
                    callback.error(e);
                }
            }

            @Override
            public void error(Throwable t) {
                callback.error(t);
            }
        });
    }

    private static String buildPath(String baseApiUrl, String businessEntity, String sourceSystem, String sourcePKey) {
        StringBuilder sb = new StringBuilder(baseApiUrl);
        sb.append(B360Endpoints.ENTITY_XREF);
        sb.append('/').append(B360Utils.encodePathSegment(businessEntity));
        sb.append('/').append(B360Utils.encodePathSegment(sourceSystem));
        sb.append('/').append(B360Utils.encodePathSegment(sourcePKey));
        return sb.toString();
    }

    private static SourceReadResponseAttributes parseResponseAttributes(JsonNode root, int statusCode, String requestId) {
        String businessId = "";
        String businessEntity = "";
        String createdBy = "";
        String creationDate = "";
        String updatedBy = "";
        String lastUpdateDate = "";
        String sourceLastUpdatedDate = "";
        String sourceSystem = "";
        String sourcePrimaryKey = "";
        String xrefType = "";
        try {
            JsonNode meta = root.path("_meta");
            if (!meta.isMissingNode()) {
                businessId = meta.path("businessId").asText("");
                businessEntity = meta.path("businessEntity").asText("");
                createdBy = meta.path("createdBy").asText("");
                creationDate = meta.path("creationDate").asText("");
                updatedBy = meta.path("updatedBy").asText("");
                lastUpdateDate = meta.path("lastUpdateDate").asText("");
                sourceLastUpdatedDate = meta.path("sourceLastUpdatedDate").asText("");
                sourceSystem = meta.path("sourceSystem").asText("");
                sourcePrimaryKey = meta.path("sourcePrimaryKey").asText("");
                xrefType = meta.path("xrefType").asText("");
            }
        } catch (Exception ignored) {
        }
        return new SourceReadResponseAttributes(businessId, businessEntity, createdBy, creationDate,
                updatedBy, lastUpdateDate, sourceLastUpdatedDate,
                sourceSystem, sourcePrimaryKey, xrefType, statusCode, requestId);
    }
}

/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.operation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.mulesoft.connectors.b360.api.MasterReadResponseAttributes;
import com.mulesoft.connectors.b360.internal.config.B360Configuration;
import com.mulesoft.connectors.b360.internal.connection.B360Connection;
import com.mulesoft.connectors.b360.internal.connection.B360HttpResponseContext;
import com.mulesoft.connectors.b360.internal.model.masterread.MasterReadParameterGroup;
import com.mulesoft.connectors.b360.internal.metadata.MasterReadAttributesStaticResolver;
import com.mulesoft.connectors.b360.internal.metadata.SourceReadOutputMetadataResolver;
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
import org.mule.runtime.extension.api.annotation.error.Throws;

/**
 * Master Read: retrieve a single master (golden) record by business ID or by source system + source primary key.
 * Combines two Informatica APIs into one operation:
 * <ul>
 *   <li>GET /business-entity/public/api/v1/entity/{businessEntity}/{businessId}</li>
 *   <li>GET /business-entity/public/api/v1/entity/{businessEntity}/{sourceSystem}/{sourcePKey}</li>
 * </ul>
 * Returns the fully blended, survived "best version of truth" record.
 *
 * @see <a href="https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Business_entity_record_APIs.html">Business entity record APIs</a>
 */
public class MasterReadOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(MasterReadOperation.class);

    @DisplayName("INFA MDM - Master Read")
    @Summary("Read a master (golden) record by Business ID or by Source System + Source Primary Key." +
            "<ul>" +
            "<li> Uses the Business 360 Read Master Record API (GET " + B360Endpoints.BUSINESS_ENTITY + "/{businessEntity}/...)" +
            "<li> Provide either Business ID or Source System + Source Primary Key, not both" +
            "<li> Returns the fully blended, survived record including _meta and optionally _contentMeta" +
            "<li> <a href=\"https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Business_entity_record_APIs.html\">Read by Business ID docs</a>" +
            " | <a href=\"https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Business_entity_record_APIs.html\">Read by SourcePKey docs</a>" +
            "</ul>")
    @MediaType(value = "application/json", strict = false)
    @OutputResolver(output = SourceReadOutputMetadataResolver.class, attributes = MasterReadAttributesStaticResolver.class)
    @Throws(B360ErrorProvider.class)
    public void masterRead(
            @Config B360Configuration config,
            @Connection B360Connection connection,
            @ParameterGroup(name = "Record Lookup")
            @DisplayName("Record Lookup")
            @Summary("Business Entity Id (required), then either Business ID or Source System + Source Primary Key.")
            MasterReadParameterGroup lookup,
            @Optional(defaultValue = "false")
            @DisplayName("Skip Lookup")
            @Summary("When true, retrieves picklist values from cache instead of data store (improves response time). Default is false.")
            @Placement(tab = ADVANCED_TAB, order = 1)
            boolean skipLookup,
            @Optional(defaultValue = "false")
            @DisplayName("Show Content Meta")
            @Summary("When true, the response includes _contentMeta (trust score, survivorship, data enhancement rule details). Default is false.")
            @Placement(tab = ADVANCED_TAB, order = 2)
            boolean showContentMeta,
            @Optional(defaultValue = "false")
            @DisplayName("Show Pending")
            @Summary("When true, retrieves only master records pending review for creation, update, or deletion. Default is false.")
            @Placement(tab = ADVANCED_TAB, order = 3)
            boolean showPending,
            CompletionCallback<Map<String, Object>, MasterReadResponseAttributes> callback) {

        String businessEntity = lookup != null ? lookup.getBusinessEntity() : null;
        String businessId = lookup != null ? lookup.getBusinessId() : null;
        String sourceSystem = lookup != null ? lookup.getSourceSystem() : null;
        String sourcePKey = lookup != null ? lookup.getSourcePKey() : null;

        boolean hasBizId = businessId != null && !businessId.isEmpty();
        boolean hasSource = (sourceSystem != null && !sourceSystem.isEmpty())
                || (sourcePKey != null && !sourcePKey.isEmpty());

        if (hasBizId && hasSource) {
            throw new IllegalArgumentException(
                    "Provide either Business ID or Source System + Source Primary Key, not both.");
        }
        if (!hasBizId && !hasSource) {
            throw new IllegalArgumentException(
                    "Provide either Business ID or Source System + Source Primary Key.");
        }
        if (!hasBizId) {
            if (sourceSystem == null || sourceSystem.isEmpty()) {
                throw new IllegalArgumentException(
                        "Source System is required when looking up by Source Primary Key.");
            }
            if (sourcePKey == null || sourcePKey.isEmpty()) {
                throw new IllegalArgumentException(
                        "Source Primary Key is required when looking up by Source System.");
            }
        }

        String path = buildPath(connection.getBaseApiUrl(), businessEntity, businessId, sourceSystem, sourcePKey);
        LOGGER.info("INFA MDM Master Read request: {}", path);

        HttpRequestBuilder builder = HttpRequest.builder()
                .uri(path)
                .method(HttpConstants.Method.GET)
                .addHeader("Accept", "application/json");
        if (skipLookup) builder.addQueryParam("_skipLookup", "true");
        if (showContentMeta) builder.addQueryParam(B360Endpoints.QUERY_SHOW_CONTENT_META, "true");
        if (showPending) builder.addQueryParam("_showPending", "true");

        connection.executeAsync(builder, new CompletionCallback<InputStream, B360HttpResponseContext>() {
            @Override
            public void success(Result<InputStream, B360HttpResponseContext> asyncResult) {
                try {
                    InputStream responseStream = asyncResult.getOutput();
                    B360HttpResponseContext httpContext = asyncResult.getAttributes().orElse(new B360HttpResponseContext(200, null));
                    JsonNode root = B360Utils.OBJECT_MAPPER.readTree(responseStream);
                    MasterReadResponseAttributes attrs = parseResponseAttributes(root, httpContext.getStatusCode(), httpContext.getRequestId());
                    Map<String, Object> output = B360Utils.OBJECT_MAPPER.convertValue(root, new TypeReference<Map<String, Object>>() {});
                    callback.success(Result.<Map<String, Object>, MasterReadResponseAttributes>builder()
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

    private static String buildPath(String baseApiUrl, String businessEntity, String businessId,
                                    String sourceSystem, String sourcePKey) {
        StringBuilder sb = new StringBuilder(baseApiUrl);
        sb.append(B360Endpoints.BUSINESS_ENTITY);
        sb.append('/').append(B360Utils.encodePathSegment(businessEntity));
        boolean byBusinessId = businessId != null && !businessId.isEmpty();
        if (byBusinessId) {
            sb.append('/').append(B360Utils.encodePathSegment(businessId));
        } else {
            sb.append('/').append(B360Utils.encodePathSegment(sourceSystem));
            sb.append('/').append(B360Utils.encodePathSegment(sourcePKey));
        }
        return sb.toString();
    }

    private static MasterReadResponseAttributes parseResponseAttributes(JsonNode root, int statusCode, String requestId) {
        String businessId = "";
        String businessEntity = "";
        String state = "";
        String validation = "";
        String consolidation = "";
        String createdBy = "";
        String creationDate = "";
        String updatedBy = "";
        String lastUpdateDate = "";
        try {
            JsonNode meta = root.path("_meta");
            if (!meta.isMissingNode()) {
                businessId = meta.path("businessId").asText("");
                businessEntity = meta.path("businessEntity").asText("");
                createdBy = meta.path("createdBy").asText("");
                creationDate = meta.path("creationDate").asText("");
                updatedBy = meta.path("updatedBy").asText("");
                lastUpdateDate = meta.path("lastUpdateDate").asText("");
                JsonNode states = meta.path("states");
                if (!states.isMissingNode()) {
                    state = states.path("base").asText("");
                    if (state.isEmpty()) state = states.path("state").asText("");
                    validation = states.path("validation").asText("");
                    consolidation = states.path("consolidation").asText("");
                }
                if (state.isEmpty()) state = meta.path("state").asText("");
            }
        } catch (Exception ignored) {
        }
        return new MasterReadResponseAttributes(businessId, businessEntity, state, validation,
                consolidation, createdBy, creationDate, updatedBy, lastUpdateDate, statusCode, requestId);
    }
}

/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.operation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.mulesoft.connectors.b360.api.MetaDataReadResponseAttributes;
import com.mulesoft.connectors.b360.api.MetadataResource;
import com.mulesoft.connectors.b360.internal.config.B360Configuration;
import com.mulesoft.connectors.b360.internal.connection.B360Connection;
import com.mulesoft.connectors.b360.internal.connection.B360HttpResponseContext;
import com.mulesoft.connectors.b360.internal.error.B360ErrorProvider;
import com.mulesoft.connectors.b360.internal.metadata.MetaDataReadAttributesStaticResolver;
import com.mulesoft.connectors.b360.internal.metadata.MetaDataReadOutputStaticResolver;
import com.mulesoft.connectors.b360.internal.operation.utils.B360Endpoints;
import com.mulesoft.connectors.b360.internal.operation.utils.B360Utils;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.http.api.HttpConstants;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mulesoft.connectors.b360.internal.error.B360ErrorType;

import java.io.InputStream;
import java.util.Map;

/**
 * MetaData Read: retrieve structural metadata from the Informatica MDM Metadata v2 API.
 * Resource dropdown determines the path:
 * <ul>
 *   <li><b>DATAMODEL</b> — GET /metadata/api/v2/objects/tenantModel/datamodel</li>
 *   <li><b>BUSINESS_ENTITY</b> — GET /metadata/api/v2/objects/businessEntity/{objectInternalId}</li>
 *   <li><b>REFERENCE_ENTITY</b> — GET /metadata/api/v2/objects/referenceEntity/{objectInternalId}</li>
 *   <li><b>RELATIONSHIP</b> — GET /metadata/api/v2/objects/relationship</li>
 *   <li><b>VIEW</b> — GET /metadata/api/v2/objects/view/{objectInternalId}</li>
 *   <li><b>EXTERNAL_RESOURCE</b> — GET /metadata/api/v2/objects/externalResource/{objectInternalId}</li>
 * </ul>
 *
 * @see <a href="https://knowledge.informatica.com/s/article/metadata-from-org?language=en_US">Informatica KB: Metadata from Org</a>
 * @see <a href="https://docs.informatica.com/master-data-management-cloud/business-360-console/current-version/business-360-rest-api-reference/business-360-rest-api/relationship-api/metadata-api.html">Relationship Metadata API</a>
 */
public class MetaDataReadOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetaDataReadOperation.class);

    @DisplayName("INFA MDM - MetaData Read")
    @Summary("Read structural metadata from the Informatica MDM Metadata v2 API."
            + "<ul>"
            + "<li> <b>DATAMODEL</b> — Full tenant schema (org discovery)"
            + "<li> <b>BUSINESS_ENTITY</b> — Entity blueprint (requires Object Internal Id, e.g. c360.person)"
            + "<li> <b>REFERENCE_ENTITY</b> — Reference entity metadata (requires Object Internal Id)"
            + "<li> <b>RELATIONSHIP</b> — Relationship metadata (hierarchy &amp; graph)"
            + "<li> <b>VIEW</b> — View metadata (requires Object Internal Id)"
            + "<li> <b>EXTERNAL_RESOURCE</b> — External resource metadata (requires Object Internal Id)"
            + "<li> Base path: /metadata/api/v2/objects"
            + "<li> <a href=\"https://knowledge.informatica.com/s/article/metadata-from-org?language=en_US\">Informatica KB: Metadata from Org</a>"
            + " | <a href=\"https://docs.informatica.com/master-data-management-cloud/business-360-console/current-version/business-360-rest-api-reference/business-360-rest-api/relationship-api/metadata-api.html\">Relationship Metadata API</a>"
            + "</ul>")
    @MediaType(value = "application/json", strict = false)
    @OutputResolver(output = MetaDataReadOutputStaticResolver.class, attributes = MetaDataReadAttributesStaticResolver.class)
    @Throws(B360ErrorProvider.class)
    public void metaDataRead(
            @Config B360Configuration config,
            @Connection B360Connection connection,
            @DisplayName("Resource")
            @Summary("Metadata resource to read."
                    + "<ul>"
                    + "<li> <b>DATAMODEL</b> — Full tenant data model (no Object Internal Id)"
                    + "<li> <b>BUSINESS_ENTITY</b> — Single business entity blueprint (e.g. c360.person)"
                    + "<li> <b>REFERENCE_ENTITY</b> — Reference entity (requires Object Internal Id)"
                    + "<li> <b>RELATIONSHIP</b> — Relationship metadata (no Object Internal Id)"
                    + "<li> <b>VIEW</b> — View (requires Object Internal Id)"
                    + "<li> <b>EXTERNAL_RESOURCE</b> — External resource (requires Object Internal Id)"
                    + "</ul>")
            MetadataResource resource,
            @Optional
            @DisplayName("Object Internal Id")
            @Summary("Internal id of the object (e.g. c360.person for BUSINESS_ENTITY). Required when Resource is BUSINESS_ENTITY, REFERENCE_ENTITY, VIEW, or EXTERNAL_RESOURCE; ignored for DATAMODEL and RELATIONSHIP.")
            String objectInternalId,
            CompletionCallback<Map<String, Object>, MetaDataReadResponseAttributes> callback) {

        if (resource.requiresObjectInternalId()) {
            if (objectInternalId == null || objectInternalId.trim().isEmpty()) {
                throw new ModuleException(
                        "Object Internal Id is required when Resource is " + resource + ".", B360ErrorType.CLIENT_ERROR);
            }
        }

        String resolvedId = resource.requiresObjectInternalId() ? objectInternalId.trim() : "";
        String path = buildPath(connection.getBaseApiUrl(), resource, resolvedId);
        LOGGER.info("INFA MDM MetaData Read request: {} (resource={})", path, resource);

        HttpRequestBuilder builder = HttpRequest.builder()
                .uri(path)
                .method(HttpConstants.Method.GET)
                .addHeader("Accept", "application/json");

        connection.executeAsync(builder, new CompletionCallback<InputStream, B360HttpResponseContext>() {
            @Override
            public void success(Result<InputStream, B360HttpResponseContext> asyncResult) {
                try {
                    InputStream responseStream = asyncResult.getOutput();
                    B360HttpResponseContext httpContext = asyncResult.getAttributes()
                            .orElse(new B360HttpResponseContext(200, null));

                    JsonNode root = B360Utils.OBJECT_MAPPER.readTree(responseStream);
                    MetaDataReadResponseAttributes attrs = new MetaDataReadResponseAttributes(
                            resource.name(), resolvedId,
                            httpContext.getStatusCode(), httpContext.getRequestId());

                    Map<String, Object> output = B360Utils.OBJECT_MAPPER.convertValue(
                            root, new TypeReference<Map<String, Object>>() {});

                    callback.success(Result.<Map<String, Object>, MetaDataReadResponseAttributes>builder()
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

    private static String buildPath(String baseApiUrl, MetadataResource resource, String objectInternalId) {
        if (resource == MetadataResource.DATAMODEL) {
            return baseApiUrl + B360Endpoints.DATAMODEL;
        }
        if (resource == MetadataResource.RELATIONSHIP) {
            return baseApiUrl + B360Endpoints.METADATA_RELATIONSHIP;
        }
        String segment = resource.getPathSegment();
        if (segment == null || objectInternalId.isEmpty()) {
            return baseApiUrl + B360Endpoints.DATAMODEL;
        }
        return baseApiUrl + B360Endpoints.METADATA_OBJECTS
                + "/" + segment
                + "/" + B360Utils.encodePathSegment(objectInternalId);
    }
}

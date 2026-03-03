/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.metadata;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

/**
 * Resolves the output payload and attributes metadata for the Source Submit operation.
 * Output is the response body (businessId, approvalRequired). Attributes: statusCode, requestId, etc.
 */
public final class SourceSubmitOutputMetadataResolver implements OutputTypeResolver<String>, AttributesTypeResolver<String> {

    @Override
    public String getCategoryName() {
        return "BusinessEntity";
    }

    @Override
    public String getResolverName() {
        return "SourceSubmitOutputMetadataResolver";
    }

    @Override
    public MetadataType getOutputType(MetadataContext context, String key)
            throws MetadataResolvingException {
        if (context == null) {
            return BaseTypeBuilder.create(MetadataFormat.JSON).objectType().id("SourceSubmitOutput").build();
        }
        org.mule.metadata.api.builder.ObjectTypeBuilder object =
                context.getTypeBuilder().objectType().id("SourceSubmitOutput");
        object.addField().key("businessId").value(context.getTypeBuilder().stringType());
        object.addField().key("approvalRequired").value(context.getTypeBuilder().booleanType());
        return object.build();
    }

    @Override
    public MetadataType getAttributesType(MetadataContext context, String key)
            throws MetadataResolvingException {
        return SourceSubmitAttributesStaticResolver.buildAttributesType();
    }
}

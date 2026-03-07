/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.config;

import com.mulesoft.connectors.b360.internal.connection.provider.B360BasicAuthConnectionProvider;
import com.mulesoft.connectors.b360.internal.connection.provider.B360PassthroughConnectionProvider;
import com.mulesoft.connectors.b360.internal.operation.HttpRequestOperation;
import com.mulesoft.connectors.b360.internal.operation.MasterReadOperation;
import com.mulesoft.connectors.b360.internal.operation.MetaDataReadOperation;
import com.mulesoft.connectors.b360.internal.operation.SearchOperation;
import com.mulesoft.connectors.b360.internal.operation.SourceReadOperation;
import com.mulesoft.connectors.b360.internal.operation.SourceSubmitOperation;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

/**
 * Informatica MDM - Business 360 connector configuration.
 * <p>
 * <b>Business 360 REST API</b><br>
 * Use the Business 360 REST APIs to interact with your Informatica Intelligent Cloud Services
 * organization through API calls. You can use the REST APIs to perform tasks and get data from
 * your organization.
 * </p>
 * <p>
 * Documentation: <a href="https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Business_360_REST_API.html">Business 360 REST API Reference</a>
 * </p>
 */
@Configuration(name = "config")
@DisplayName("Configuration")
@Summary("<ul><li>Informatica MDM - Business 360 Configuration</li><li>Documentation: <a href=\"https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Authentication_method.html\">Business 360 REST API Authentication</a></li></ul>")
@ConnectionProviders({B360BasicAuthConnectionProvider.class, B360PassthroughConnectionProvider.class})
@Operations({HttpRequestOperation.class, MasterReadOperation.class, MetaDataReadOperation.class, SearchOperation.class, SourceReadOperation.class, SourceSubmitOperation.class})
public class B360Configuration {
}

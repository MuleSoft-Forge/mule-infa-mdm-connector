/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.extension;

import com.mulesoft.connectors.b360.internal.config.B360Configuration;
import com.mulesoft.connectors.b360.internal.error.B360ErrorType;
import org.mule.runtime.api.meta.Category;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.sdk.api.annotation.JavaVersionSupport;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_11;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_8;

@Extension(name = "Informatica MDM - Business 360", category = Category.SELECT)
@Xml(prefix = "b360")
@Configurations(B360Configuration.class)
@ErrorTypes(B360ErrorType.class)
@JavaVersionSupport({JAVA_8, JAVA_11, JAVA_17})
public class B360Connector {
}

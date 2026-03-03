/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.model.search;

import com.mulesoft.connectors.b360.internal.metadata.B360EntityKeysResolver;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;

import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;

/**
 * Parameter group for Search criteria: use either <em>search</em> or <em>fields</em>, not both.
 * At least one is required per the B360 Search API.
 * <p>
 * Per {@link ExclusiveOptionals}, exclusivity applies only to <em>optional</em> parameters.
 * Business Entity Id is required (no {@code @Optional}) so it is not part of the exclusive set;
 * only <em>search</em> and <em>fields</em> are mutually exclusive, with one required.
 * <p>
 * Both optionals are declared as {@code String} so Studio shows expression vs edit-inline consistently
 * (see <a href="https://docs.mulesoft.com/mule-sdk/latest/exclusive-optionals">Exclusive Optionals</a>).
 *
 * @see <a href="https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Search_API.html">Search API</a>
 */
@ExclusiveOptionals(isOneRequired = true)
public class SearchCriteriaParameterGroup {

    public static final String TAB_NAME = "General";

    @Parameter
    @DisplayName("Business Entity Id")
    @Summary("Business entity to search (e.g. Person, Organization).")
    @MetadataKeyId(B360EntityKeysResolver.class)
    @Placement(tab = TAB_NAME, order = 0)
    private String entityType;

    @Parameter
    @Optional
    @DisplayName("Search")
    @Summary("<p>Search string.</p>"
            + "<p>To retrieve all the records, use an asterisk (*).</p>"
            + "<p>To perform a fuzzy search, enter a search word or phrase. When you search for records, you can use additional wildcard characters, such as *, ~, &&, and ||.</p>"
            + "<p>You can use search strings similar to the ones in the following examples:</p>"
            + "<ul><li><strong>John Smith.</strong> Searches for records that contain John Smith as a field value.</li>"
            + "<li><strong>Jo*n.</strong> Searches for records that start with Jo and end with n, such as Johansson or Jordan.</li>"
            + "<li><strong>Hans && Williams.</strong> Searches for records that contain Hans and Williams in the same record. For example, the first name is Hans, and the last name is Williams.</li></ul>"
            + "<p>To perform an exact search, enclose the search string within double quotes in the following format: <code>\\\"&lt;Field_Value&gt;\\\"</code></p>"
            + "<p>For example, to search for records that contain the field value London, specify the value in the following format: <code>\\\"London\\\"</code></p>"
            + "<p>Use this format to perform exact searches for string, date, and date and time field types. To perform an exact search on records based on other field types, do not enclose the value in the specified format.</p>"
            + "<p><strong>Note:</strong> When you use only white spaces as a search string without any other characters, the search does not return any results.</p>"
            + "<p>To search for records by specifying a date, ensure that you use the YYYY-MM-DD format.</p>"
            + "<p>When you search for records with keywords, such as AND, OR, and NOT, as field values, ensure that you specify the values in the following format: <code>\\\"OR\\\"</code>, <code>\\\"AND\\\"</code>, <code>\\\"NOT\\\"</code>. "
            + "If you do not enter the values in the specified format, the Search REST API does not return any response. "
            + "However, if these keywords are part of a field value, such as SAND AND MARVEL, you do not have to enclose the keyword part of the value in the specified format for the Search REST API to return the response.</p>"
            + "<p>You cannot search for records with similar values in system fields. However, you can search for records that exactly match the specified values in system fields and continue to use the supported wildcards in search strings for system fields.</p>"
            + "<p>For more information about these wildcard characters, see Using the search box to find records in the Customer 360 help.</p>"
            + "<p>Use Search or Fields, not both.</p>")
    @Example("*")
    @Placement(tab = TAB_NAME, order = 1)
    private String search;

    @Parameter
    @Optional
    @Expression(SUPPORTED)
    @DisplayName("Fields")
    @Summary("JSON object of field names to search values (e.g. {\"c360person.firstName\":\"David\"}). Use search or fields, not both.")
    @Example("{\"c360person.firstName\":\"David\"}")
    @Placement(tab = TAB_NAME, order = 2)
    private String fields;

    public String getEntityType() {
        return entityType;
    }

    public String getSearch() {
        return search;
    }

    public String getFields() {
        return fields;
    }
}

/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.operation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.mulesoft.connectors.b360.api.SearchResponseAttributes;
import com.mulesoft.connectors.b360.internal.connection.B360Connection;
import com.mulesoft.connectors.b360.internal.connection.B360HttpResponseContext;
import com.mulesoft.connectors.b360.internal.config.B360Configuration;
import com.mulesoft.connectors.b360.internal.metadata.SearchOutputMetadataResolver;
import com.mulesoft.connectors.b360.internal.model.search.SearchCriteriaParameterGroup;
import com.mulesoft.connectors.b360.internal.model.search.SearchFilterParameterGroup;
import com.mulesoft.connectors.b360.internal.model.search.SearchFilterRecord;
import com.mulesoft.connectors.b360.internal.model.search.SearchPaginationParameterGroup;
import com.mulesoft.connectors.b360.internal.model.search.SearchSortParameterGroup;
import com.mulesoft.connectors.b360.internal.model.search.SearchSortRecord;
import com.mulesoft.connectors.b360.api.SearchFilterComparator;
import com.mulesoft.connectors.b360.api.SearchFilterOperator;
import com.mulesoft.connectors.b360.internal.operation.utils.B360Endpoints;
import com.mulesoft.connectors.b360.internal.operation.utils.B360Utils;
import com.mulesoft.connectors.b360.internal.error.B360ErrorProvider;
import com.mulesoft.connectors.b360.internal.error.B360ErrorType;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.http.api.HttpConstants;
import org.mule.runtime.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Search API: search for records that match criteria across business entities.
 * POST /search/public/api/v1/search
 * <p>
 * Returns a single page as {@link Result}: payload is the <em>records</em> array from the
 * response (JSON array of business entity records), attributes are {@link SearchResponseAttributes}
 * (hits, pageSize, pageOffset, recordsToReturn, recordOffset, maxRecords).
 * Use recordOffset and recordsToReturn to paginate (e.g. in a loop).
 */
public class SearchOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchOperation.class);
    private static final int DEFAULT_RECORDS_PER_PAGE = 200;

    @DisplayName("INFA MDM - Master Search")
    @Summary("Search master (golden) records that match your search criteria within a business entity." +
            "<ul>" +
            "<li> Uses the Business 360 Search API (POST " + B360Endpoints.SEARCH + ")" +
            "<li> In Search Criteria, select Business Entity Id and use either Search or Fields" +
            "<li> Use either Search or Fields for criteria, not both (at least one required)" +
            "<li> Use record offset and records to return for pagination" +
            "<li> <a href=\"https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Search_API.html\">Search API docs</a>" +
            "</ul>")
    @MediaType(value = "application/json", strict = false)
    @OutputResolver(output = SearchOutputMetadataResolver.class, attributes = SearchOutputMetadataResolver.class)
    @Throws(B360ErrorProvider.class)
    public void search(
            @Config B360Configuration config,
            @Connection B360Connection connection,
            @ParameterGroup(name = "Pagination")
            SearchPaginationParameterGroup pagination,
            @ParameterGroup(name = "Search Criteria")
            SearchCriteriaParameterGroup criteria,
            @ParameterGroup(name = "Filters")
            SearchFilterParameterGroup filter,
            @ParameterGroup(name = "Sort")
            SearchSortParameterGroup sortGroup,
            @Optional(defaultValue = "false")
            @DisplayName("Show Content Meta")
            @Summary("When true, adds query parameter _showContentMeta=true so the API returns content meta (e.g. full trust score in _meta.score).")
            @Placement(tab = ADVANCED_TAB, order = 1)
            boolean showContentMeta,
            CompletionCallback<List<Object>, SearchResponseAttributes> callback) {

        int offset = pagination != null && pagination.getRecordOffset() != null
                ? pagination.getRecordOffset() : 0;
        int limit = pagination != null && pagination.getRecordsToReturn() != null && pagination.getRecordsToReturn() > 0
                ? pagination.getRecordsToReturn() : DEFAULT_RECORDS_PER_PAGE;

        String entityType = criteria != null ? criteria.getEntityType() : null;
        String fieldsStr = criteria != null ? criteria.getFields() : null;
        boolean hasFields = fieldsStr != null && !fieldsStr.trim().isEmpty();
        if (hasFields) {
            validateFieldsIsJsonObject(fieldsStr.trim());
        }
        String body = buildSearchBody(entityType, criteria, filter, sortGroup, offset, limit);
        // TODO: comment out to disable request payload logging
        // LOGGER.info("INFA MDM Search request payload: {}", body);

        String path = connection.getBaseApiUrl() + B360Endpoints.SEARCH;
        HttpRequestBuilder builder = HttpRequest.builder()
                .uri(path)
                .method(HttpConstants.Method.POST)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .entity(new InputStreamHttpEntity(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))));
        if (showContentMeta) builder.addQueryParam(B360Endpoints.QUERY_SHOW_CONTENT_META, "true");

        final int recordOffset = offset;
        final int recordsToReturn = limit;
        connection.executeAsync(builder, new CompletionCallback<InputStream, B360HttpResponseContext>() {
            @Override
            public void success(Result<InputStream, B360HttpResponseContext> asyncResult) {
                try {
                    InputStream responseStream = asyncResult.getOutput();
                    B360HttpResponseContext httpContext = asyncResult.getAttributes().orElse(new B360HttpResponseContext(200, null));
                    JsonNode root = B360Utils.OBJECT_MAPPER.readTree(responseStream);
                    SearchResponseAttributes attrs = parseResponseAttributes(root, recordOffset, recordsToReturn, httpContext.getStatusCode(), httpContext.getRequestId());
                    List<Object> recordsList = extractRecordsList(root);
                    callback.success(Result.<List<Object>, SearchResponseAttributes>builder()
                            .output(recordsList)
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

    private static String buildSearchBody(String entityType, SearchCriteriaParameterGroup criteria,
            SearchFilterParameterGroup filter, SearchSortParameterGroup sortGroup, int recordOffset, int recordsToReturn) {
        StringBuilder sb = new StringBuilder();
        // API expects entity type; send empty string only if null/empty.
        String apiEntityType = (entityType == null || entityType.isEmpty()) ? "" : entityType;
        sb.append("{\"entityType\":\"").append(escapeJson(apiEntityType)).append("\"");
        boolean hasSearch = criteria != null && criteria.getSearch() != null && !criteria.getSearch().isEmpty();
        String fieldsStr = criteria != null ? criteria.getFields() : null;
        boolean hasFields = fieldsStr != null && !fieldsStr.trim().isEmpty();
        if (hasSearch) {
            sb.append(",\"search\":\"").append(escapeJson(criteria.getSearch())).append("\"");
        } else if (hasFields) {
            sb.append(",\"fields\":");
            appendFieldsJson(sb, fieldsStr.trim());
        }
        sb.append(",\"recordOffset\":").append(recordOffset);
        sb.append(",\"recordsToReturn\":").append(recordsToReturn);
        if (filter != null) {
            List<SearchFilterRecord> filterList = filter.getFilters();
            if (filterList != null && !filterList.isEmpty()) {
                sb.append(",\"filters\":");
                appendFiltersFromRecords(sb, filter.getOperator(), filterList);
            }
        }
        if (sortGroup != null) {
            List<SearchSortRecord> sortList = sortGroup.getSorts();
            if (sortList != null && !sortList.isEmpty()) {
                sb.append(",\"sort\":");
                appendSortFromRecords(sb, sortList);
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Validates that the Search Criteria Fields parameter is valid JSON and a JSON object
     * (not an array or primitive). Call before building the request to fail fast with a clear
     * error instead of a generic 400 Bad Request from the API.
     *
     * @throws ModuleException with CLIENT_ERROR if fields is not valid JSON or not a JSON object
     */
    private static void validateFieldsIsJsonObject(String fieldsStr) {
        if (fieldsStr == null || fieldsStr.isEmpty()) {
            return;
        }
        try {
            JsonNode node = B360Utils.OBJECT_MAPPER.readTree(fieldsStr);
            if (!node.isObject()) {
                throw new ModuleException(
                        "Search Criteria Fields must be a JSON object (e.g. {\"firstName\":\"J*\"}), not an array or primitive.",
                        B360ErrorType.CLIENT_ERROR);
            }
        } catch (ModuleException e) {
            throw e;
        } catch (Exception e) {
            throw new ModuleException(
                    "Invalid JSON in Search Criteria Fields. Expect a JSON object (e.g. {\"firstName\":\"J*\"}). " + e.getMessage(),
                    B360ErrorType.CLIENT_ERROR,
                    e);
        }
    }

    /**
     * Appends the "fields" value as JSON. Accepts a JSON object string (e.g. {"firstName":"J*"})
     * or a DataWeave-evaluated string; parses and re-serializes to ensure valid JSON.
     * @throws ModuleException with CLIENT_ERROR if the fields string is not valid JSON
     */
    private static void appendFieldsJson(StringBuilder sb, String fieldsStr) {
        try {
            JsonNode node = B360Utils.OBJECT_MAPPER.readTree(fieldsStr);
            sb.append(B360Utils.OBJECT_MAPPER.writeValueAsString(node));
        } catch (Exception e) {
            throw new ModuleException(
                    "Invalid JSON in Search Criteria Fields. Expect a JSON object (e.g. {\"firstName\":\"J*\"}). " + e.getMessage(),
                    B360ErrorType.CLIENT_ERROR,
                    e);
        }
    }

    /**
     * Appends the "filters" value as JSON from the Filters table. fieldValue format depends on comparator:
     * IN/BETWEEN use array; IS_MISSING/IS_NOT_MISSING omit fieldValue; all others use single value (per B360 Search API).
     * Includes optional operator (AND/OR) at root of filters object.
     */
    private static void appendFiltersFromRecords(StringBuilder sb, SearchFilterOperator operator, List<SearchFilterRecord> records) {
        try {
            Map<String, Object> filtersObj = new LinkedHashMap<>();
            filtersObj.put("operator", operator != null ? operator.getApiValue() : "AND");
            List<Map<String, Object>> filterArray = new ArrayList<>();
            for (SearchFilterRecord rec : records) {
                if (rec == null) continue;
                Map<String, Object> entry = new LinkedHashMap<>();
                SearchFilterComparator comp = rec.getComparator();
                String comparatorStr = comp != null ? comp.getApiValue() : "";
                entry.put("comparator", comparatorStr);
                entry.put("fieldName", rec.getFieldName() != null ? rec.getFieldName() : "");
                List<String> fv = rec.getFieldValue();
                if ("IS_MISSING".equals(comparatorStr) || "IS_NOT_MISSING".equals(comparatorStr)) {
                    // Omit fieldValue for IS_MISSING / IS_NOT_MISSING per Search API
                } else if ("IN".equals(comparatorStr) || "BETWEEN".equals(comparatorStr)) {
                    entry.put("fieldValue", fv != null ? fv : List.of());
                } else {
                    // EQUALS, NOT_EQUALS, CONTAINS, STARTS_WITH, GREATER_THAN, etc. require single value
                    if (fv != null && !fv.isEmpty()) {
                        entry.put("fieldValue", fv.get(0));
                    } else {
                        entry.put("fieldValue", "");
                    }
                }
                if (rec.getFieldValueGroups() != null && !rec.getFieldValueGroups().isEmpty()) {
                    entry.put("fieldValueGroups", rec.getFieldValueGroups());
                }
                filterArray.add(entry);
            }
            filtersObj.put("filter", filterArray);
            sb.append(B360Utils.OBJECT_MAPPER.writeValueAsString(filtersObj));
        } catch (Exception e) {
            sb.append("{\"filter\":[]}");
        }
    }

    /**
     * Appends the "sort" value as JSON from the Sort table (array of { fieldName, order }).
     */
    private static void appendSortFromRecords(StringBuilder sb, List<SearchSortRecord> records) {
        try {
            List<Map<String, Object>> sortArray = new ArrayList<>();
            for (SearchSortRecord rec : records) {
                if (rec == null) continue;
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("fieldName", rec.getFieldName() != null ? rec.getFieldName() : "");
                entry.put("order", rec.getOrder() != null ? rec.getOrder().getApiValue() : "ASCENDING");
                sortArray.add(entry);
            }
            sb.append(B360Utils.OBJECT_MAPPER.writeValueAsString(sortArray));
        } catch (Exception e) {
            sb.append("[]");
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static final int DEFAULT_MAX_RECORDS = 10000;

    private static SearchResponseAttributes parseResponseAttributes(JsonNode root, int recordOffset, int recordsToReturn, int statusCode, String requestId) {
        int hits = 0;
        int pageSize = 0;
        int pageOffset = 0;
        int maxRecords = DEFAULT_MAX_RECORDS;
        try {
            JsonNode searchResult = root.path("searchResult");
            if (!searchResult.isMissingNode()) {
                hits = searchResult.path("hits").asInt(0);
                if (hits == 0 && searchResult.has("totalCount")) {
                    hits = searchResult.path("totalCount").asInt(0);
                }
                pageSize = searchResult.path("pageSize").asInt(pageSize);
                pageOffset = searchResult.path("pageOffset").asInt(pageOffset);
                if (searchResult.has("maxRecords")) {
                    maxRecords = searchResult.path("maxRecords").asInt(DEFAULT_MAX_RECORDS);
                }
            }
            if (hits == 0 && root.has("hits")) {
                hits = root.path("hits").asInt(0);
            }
            if (root.has("pageSize")) pageSize = root.path("pageSize").asInt(0);
            if (root.has("pageOffset")) pageOffset = root.path("pageOffset").asInt(0);
            if (root.has("maxRecords")) maxRecords = root.path("maxRecords").asInt(DEFAULT_MAX_RECORDS);
            if (pageSize == 0) pageSize = recordsToReturn;
        } catch (Exception ignored) {
        }
        return new SearchResponseAttributes(hits, pageSize, pageOffset, recordsToReturn, recordOffset, maxRecords, statusCode, requestId);
    }

    /**
     * Extracts the records array from the search response as a List.
     * Uses searchResult.records when present, otherwise an empty list.
     */
    private static List<Object> extractRecordsList(JsonNode root) {
        try {
            JsonNode recordsNode = root.path("searchResult").path("records");
            if (!recordsNode.isArray()) {
                return Collections.emptyList();
            }
            return B360Utils.OBJECT_MAPPER.convertValue(recordsNode, new TypeReference<List<Object>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}

/*
 * Copyright 2026 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.mulesoft.connectors.b360.internal.operation.utils;

/**
 * B360 REST API path constants. All operations should use these so paths are defined in one place
 * and errors that include the request URL reference the same paths.
 *
 * Paths are relative to the connection base URL (e.g. from login baseApiUrl with B360 MDM host adjustment).
 */
public final class B360Endpoints {

    private B360Endpoints() {}

    /** Search API: POST with JSON body (entityType, search, recordOffset, recordsToReturn). */
    public static final String SEARCH = "/search/public/api/v1/search";

    /** Business entity record API: used for connection validation (GET). */
    public static final String BUSINESS_ENTITY = "/business-entity/public/api/v1/entity";

    /** Metadata: list business entities (GET). First path to try for entity keys. */
    public static final String META = "/meta";

    /** Metadata (alt): list business entities when /meta is not available (GET). */
    public static final String META_ALT = "/business-entity/public/api/v1/meta";

    /**
     * Metadata v2 datamodel: single source for business entities and other design-time metadata.
     * GET returns tenant datamodel (e.g. businessEntity[].guid). Cache at design time for entity dropdown and other UI.
     */
    public static final String DATAMODEL = "/metadata/api/v2/objects/tenantModel/datamodel";

    /**
     * Source record (entity-xref) API: GET/PUT/PATCH/DELETE with path params
     * {@code /{businessEntity}/{sourceSystem}/{sourcePKey}}.
     */
    public static final String ENTITY_XREF = "/business-entity/public/api/v1/entity-xref";

    /**
     * Query parameter for read-record and Search APIs to include content meta in the response.
     * <ul>
     *   <li><b>Search API:</b> When {@code true}, record {@code _meta.score} and related content meta may differ (e.g. full trust score).</li>
     *   <li><b>Read Master / Read Source Record:</b> When {@code true}, response includes a {@code _contentMeta} section (trust score, survivorship, etc.).</li>
     * </ul>
     * Append to URI: {@code ?_showContentMeta=true}
     */
    public static final String QUERY_SHOW_CONTENT_META = "_showContentMeta";
}

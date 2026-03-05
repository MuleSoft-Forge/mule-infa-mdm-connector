# 6. Generic Request

**Connector operation:** **INFA MDM - Generic Request** — Send a generic HTTP request to any Informatica B360 / MDM API endpoint, reusing the connector's authenticated session. Acts as an API client (like Postman) so the connector remains useful even when Informatica adds new resources before a dedicated operation is shipped.

---

## Why a Generic Request operation?

Informatica's B360 / MDM platform exposes dozens of REST API resources across business entity records, metadata, search, jobs, tasks, and more. This connector ships dedicated operations for the most common workflows (Master Read, Search, Source Read, Source Submit), but the API surface is large and evolves over time.

The **Generic Request** operation ensures:

1. **No connector obsolescence.** When Informatica adds a new resource (e.g. a new batch job API, a task management endpoint, or a v2 of an existing resource), users can call it immediately without waiting for a connector update.
2. **Full API coverage.** Resources that don't justify a dedicated operation (e.g. one-off admin calls, metadata introspection, or custom B360 extensions) are still accessible.
3. **Rapid prototyping.** Developers can experiment with any endpoint in Anypoint Studio before requesting a dedicated operation.

The operation reuses the connector's authenticated session — the `IDS-SESSION-ID` header is injected automatically — so the user only needs to specify the HTTP method, path, and optionally query parameters, headers, and a request body.

---

## How it works

```
<HTTP Method>  <B360 MDM Base URL><Path>?<Query Parameters>
Headers:  IDS-SESSION-ID: <session>   (automatic)
          Accept: application/json    (automatic)
          <Custom Headers>            (optional)
Body:     <Request Body>              (optional)
```

The connector:

1. Prepends the **B360 MDM Base API URL** (from the connection) to the **Path** you provide.
2. Adds the `IDS-SESSION-ID` header automatically (from the connection's session).
3. Adds `Accept: application/json` by default.
4. Appends any **Query Parameters** you supply.
5. Adds any **Custom Headers** you supply.
6. If a **Request Body** is provided, sets `Content-Type: application/json` and sends it.
7. Returns the **raw response body** as an `InputStream` and the **HTTP response metadata** as attributes.

Unlike the dedicated operations, the Generic Request **does not throw on non-2xx responses**. The user receives the full response (status code, headers, body) and can handle errors in DataWeave or error handlers. This is intentional — a "Postman-like" experience means you see exactly what the API returns.

---

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| **HTTP Method** | Enum: `GET`, `POST`, `PUT`, `PATCH`, `DELETE` | Yes | The HTTP method for the request. |
| **Path** | String | Yes | Relative path appended to the B360 MDM base URL. Must start with `/`. Example: `/business-entity/public/api/v1/entity/Person`. |
| **Query Parameters** | Map&lt;String, String&gt; | No | Key-value pairs appended to the URL as query parameters. Example: `sourceSystem` = `CRM`, `_showContentMeta` = `true`. |
| **Custom Headers** | Map&lt;String, String&gt; | No | Additional HTTP headers. `IDS-SESSION-ID` and `Accept` are added automatically; do not duplicate them here. |
| **Request Body** | Object (`@Content`) | No | Optional JSON request body. Accepts an `InputStream` (raw JSON), a `String`, or a Java object (`Map`/`List`) which is serialized to JSON. When `null`, no body is sent (typical for GET/DELETE). |

---

## Output

- **Payload:** `InputStream` — the raw response body from the API. Parse with DataWeave (`#[payload]`) or pass through. May be `null` if the API returns no body (e.g. 204 No Content).
- **Attributes:** `GenericRequestResponseAttributes` — generic HTTP response metadata:

| Attribute | Type | Description |
|-----------|------|-------------|
| `statusCode` | `int` | HTTP status code (e.g. 200, 201, 400, 404, 500). |
| `reasonPhrase` | `String` | HTTP reason phrase (e.g. "OK", "Created", "Not Found"). |
| `requestId` | `String` | Informatica request/tracing ID from response headers (`INFA-REQUEST-ID`, `X-Request-Id`, or `Tracking-Id`). Empty string if not present. |
| `headers` | `Map<String, String>` | All response headers as a flat map (last value wins for duplicate header names). |

---

## Error handling

| Scenario | Behavior |
|----------|----------|
| **HTTP 2xx** | Response body and attributes returned normally. |
| **HTTP 3xx / 4xx / 5xx** | Response body and attributes returned normally — **no exception is thrown**. Inspect `#[attributes.statusCode]` in DataWeave to handle errors. |
| **Network / connectivity failure** | Throws `B360:CONNECTIVITY`. |
| **Timeout** | Throws `B360:TIMEOUT`. |

This differs from the dedicated operations (Master Read, Search, etc.) which throw `B360:CLIENT_ERROR` or `B360:SERVER_ERROR` on all non-2xx responses. The Generic Request is designed to give you the raw response so you can decide how to handle errors.

### Transparent session refresh on 401

All operations (Generic Request **and** dedicated operations) share the same transparent retry behaviour:

1. The connector sends the request with the current `IDS-SESSION-ID`.
2. If the API returns **HTTP 401** (session expired), the connector **automatically re-authenticates** (calls the Informatica login API with the same credentials) and obtains a fresh session ID.
3. The original request is **replayed once** with the new session.
4. If the retry also fails, the error propagates normally.

This happens inside the `B360Connection` — no user configuration is required. The Mule app developer does **not** need to configure a `<reconnection>` strategy for session expiry (though it remains useful for startup connectivity issues).

| Provider | Retry behaviour on 401 |
|----------|----------------------|
| **Basic Auth** | Re-login is performed automatically; the request is replayed with the new session. Fully transparent. |
| **Passthrough** | No re-login is possible (the connector doesn't own the credentials). The 401 response is returned as-is. The caller must supply a fresh token. |

---

## Mule XML examples

### GET — Read a master record

```xml
<b360:generic-request config-ref="B360_Config"
    method="GET"
    path="/business-entity/public/api/v1/entity/Person/123456789">
    <b360:query-parameters>
        <b360:query-parameter key="_showContentMeta" value="true" />
    </b360:query-parameters>
</b360:generic-request>
```

### POST — Create/submit a source record

```xml
<b360:generic-request config-ref="B360_Config"
    method="POST"
    path="/business-entity/public/api/v1/entity/Person">
    <b360:query-parameters>
        <b360:query-parameter key="sourceSystem" value="CRM" />
    </b360:query-parameters>
    <b360:request-body><![CDATA[#[payload]]]></b360:request-body>
</b360:generic-request>
```

### PUT — Update a source record (entity-xref)

```xml
<b360:generic-request config-ref="B360_Config"
    method="PUT"
    path="#['/business-entity/public/api/v1/entity-xref/Person/' ++ vars.sourceSystem ++ '/' ++ vars.sourcePKey]">
    <b360:request-body><![CDATA[#[payload]]]></b360:request-body>
</b360:generic-request>
```

### DELETE — Remove a source contribution

```xml
<b360:generic-request config-ref="B360_Config"
    method="DELETE"
    path="#['/business-entity/public/api/v1/entity-xref/Person/' ++ vars.sourceSystem ++ '/' ++ vars.sourcePKey]" />
```

### Calling a non-standard endpoint (e.g. jobs API)

```xml
<b360:generic-request config-ref="B360_Config"
    method="POST"
    path="/batch-job/public/api/v1/jobs/run">
    <b360:request-body><![CDATA[#[{
        "jobId": vars.jobId,
        "parameters": {}
    }]]]></b360:request-body>
</b360:generic-request>
```

---

## DataWeave: inspecting the response

After the Generic Request operation, the response is available as `payload` (body) and `attributes` (HTTP metadata).

### Check status and parse body

```dataweave
%dw 2.0
output application/json
---
if (attributes.statusCode >= 200 and attributes.statusCode < 300)
    payload
else
    {
        error: true,
        statusCode: attributes.statusCode,
        reasonPhrase: attributes.reasonPhrase,
        requestId: attributes.requestId,
        body: payload
    }
```

### Access response headers

```dataweave
%dw 2.0
output application/json
---
{
    contentType: attributes.headers.'Content-Type',
    requestId: attributes.requestId,
    allHeaders: attributes.headers
}
```

---

## Common B360 REST API paths

The following paths are relative to the B360 MDM base URL. Use them with the Generic Request operation when a dedicated operation is not available or when you need more control.

| Method | Path | Description |
|--------|------|-------------|
| POST | `/search/public/api/v1/search` | Search golden records (Search API). |
| GET | `/business-entity/public/api/v1/entity/{entity}/{businessId}` | Read a master record by business ID. |
| GET | `/business-entity/public/api/v1/entity/{entity}/{sourceSystem}/{sourcePKey}` | Read a master record by source system + primary key. |
| POST | `/business-entity/public/api/v1/entity/{entity}?sourceSystem=...` | Create/submit a source record (Create Master Record API). |
| GET | `/business-entity/public/api/v1/entity-xref/{entity}/{sourceSystem}/{sourcePKey}` | Read a source record (entity cross-reference). |
| PUT | `/business-entity/public/api/v1/entity-xref/{entity}/{sourceSystem}/{sourcePKey}` | Update a source record. |
| DELETE | `/business-entity/public/api/v1/entity-xref/{entity}/{sourceSystem}/{sourcePKey}` | Delete a source record. |
| GET | `/business-entity/public/api/v1/entity` | List business entities (connection validation endpoint). |
| GET | `/metadata/api/v2/objects/tenantModel/datamodel` | Retrieve tenant datamodel (business entities, source systems, fields). |
| GET | `/meta` | List business entities (metadata, alternative endpoint). |

---

## When to use Generic Request vs. dedicated operations

| Use case | Recommendation |
|----------|----------------|
| Search golden records | Use **INFA MDM - Master Search** (pagination, metadata resolution, typed attributes). |
| Read a master record | Use **INFA MDM - Master Read** (entity dropdown, typed `MasterReadResponseAttributes`). |
| Read a source record | Use **INFA MDM - Source Read** (entity/source system dropdowns, typed attributes). |
| Submit source data | Use **INFA MDM - Source Submit** (entity/source system dropdowns, typed attributes). |
| Any endpoint not covered above | Use **INFA MDM - Generic Request**. |
| Need raw HTTP control (custom headers, inspect response headers, handle non-2xx yourself) | Use **INFA MDM - Generic Request**. |
| Prototyping or debugging an API call | Use **INFA MDM - Generic Request**. |
| Batch job triggers, task management, admin APIs | Use **INFA MDM - Generic Request**. |

---

*Informatica MDM — Business 360 REST API — [Business 360 REST API Reference](https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Business_360_REST_API.html)*

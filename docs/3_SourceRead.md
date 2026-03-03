# 3. Get Source Contribution

**Source:** [Source Record API — Business 360 REST API Reference](https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Source_record_API.html)

**Connector operation:** **Get Source Contribution** — GET a single source record (one system’s contribution / cross-reference) by business entity, source system, and source primary key. (Informatica: Source Record API / entity-xref.)

---

## Source Record API (Read)

Use the Read Source Record API to retrieve a business entity source record based on the source system and the primary key of the record that you specify. The API supports the GET method.

**URI:**

```
GET <baseApiUrl>/business-entity/public/api/v1/entity-xref/<businessEntity>/<sourceSystem>/<sourcePKey>[?_resolveCrosswalk=true|false][&_showContentMeta=true]
```

Query parameters appear **after** the path (after `?` and `&`). Informatica doc examples sometimes show `&_resolveCrosswalk=true` in the path by mistake; the correct form is path first, then optional query string.

**Examples (correct format):**

- With `_resolveCrosswalk=true`:
  ```
  GET <baseApiUrl>/business-entity/public/api/v1/entity-xref/c360.person/c360.default.system/611d13e9e6dbd16ffa69a143?_resolveCrosswalk=true
  ```
- With both `_resolveCrosswalk=true` and `_showContentMeta=true` (same `_showContentMeta` as **Search Golden Records**):
  ```
  GET <baseApiUrl>/business-entity/public/api/v1/entity-xref/c360.person/c360.default.system/610959f149cba67ca253a872?_resolveCrosswalk=true&_showContentMeta=true
  ```

The response is in JSON format.

---

## Path parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| **businessEntity** | String | Internal ID of the business entity for which you want to retrieve the source record. |
| **sourceSystem** | String | Internal ID of the source system to which the record belongs. |
| **sourcePKey** | String | The primary key of the source record. |

## Query parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| **_resolveCrosswalk** | Boolean | `false` | Indicates whether to standardize picklist values based on the source system configuration. Set to `true` to standardize. When you specify this parameter, you must set the sourceSystem parameter. |
| **_showContentMeta** | Boolean | `false` | When `true`, the response includes a `_contentMeta` section (trust score, survivorship, etc.). Same query parameter as **Search Golden Records** (reuse). |

---

## GET response

The response body contains the source record data. The GET response includes `_meta` parameters and may include `dataEnhancementRule` (data enrichment and validation details).

| Parameter | Type | Description |
|-----------|------|-------------|
| **businessId** | String | Unique identifier of the source record. |
| **id** | String | Unique identifier of the business entity record. (Don't use.) |
| **businessEntity** | String | Internal ID of the business entity from which you retrieved the source record. |
| **createdBy** | String | Name of the user who created the record. |
| **creationDate** | String/Number | Date when the record was created. |
| **updatedBy** | String | Name of the user who last updated the record. |
| **lastUpdateDate** | String/Number | Date when the master record was last updated. |
| **sourceLastUpdatedDate** | String | Date when the source record was last updated. |
| **states** | Object | Statuses of the source record: `base` (e.g. ACTIVE), `consolidation` (e.g. MATCH_INDEXED, CONSOLIDATED), `searchIndex` (e.g. SEARCH_DIRTY), `validation` (PENDING, PASSED, FAILED). |
| **sourceSystem** | String | Internal ID of the source system. |
| **sourcePrimaryKey** | String | Primary key of the source record. |
| **xrefType** | String | Cross-reference type (e.g. `DATA`). |
| **dataEnhancementRule** | Object | Data enrichment and validation details; contains `fail` array of failed data enhancement rules. |

When `_showContentMeta=true`, the response also includes a `_contentMeta` object with trust score and survivorship details (e.g. `trust.fieldData[]`).

---

## Connector: Get Source Contribution

The **Get Source Contribution** operation retrieves a single source record (one system’s contribution). It sends a GET request to `/business-entity/public/api/v1/entity-xref/{businessEntity}/{sourceSystem}/{sourcePKey}` with optional query parameters.

| Parameter / group | Required | Description |
|-------------------|----------|-------------|
| **Source Record** | — | **Business Entity Id** (dropdown from tenant datamodel), **Source System** (dropdown; options depend on selected Business Entity, from same API/datamodel), **Source Primary Key** (text). |
| **Resolve Crosswalk** (Advanced) | No | Indicates whether to standardize picklist values. When true, sourceSystem must be set. Default false. |
| **Show Content Meta** (Advanced) | No | Same `_showContentMeta` as Search Golden Records. When true, response includes `_contentMeta` section. |

**Output:**
- **Payload:** The full source record JSON (field data + `_meta` + optionally `_contentMeta` + `dataEnhancementRule`).
- **Attributes:** `SourceReadResponseAttributes` — `businessId`, `businessEntity`, `createdBy`, `creationDate`, `updatedBy`, `lastUpdateDate`, `sourceLastUpdatedDate`, `sourceSystem`, `sourcePrimaryKey`, `xrefType`, `statusCode`.

**Business Entity & Source System (dependent dropdowns):** Same approach as the IDP connector: two parameters each with **@OfValues(ValueProvider)**. The first dropdown uses **B360BusinessEntityValueProvider** (entities from datamodel). The second uses **B360SourceSystemValueProvider**, which has a **@Parameter String businessEntity** so the SDK injects the first parameter’s value and re-resolves the second list when the first changes. Both lists are loaded from the B360 API/datamodel; Source System is filtered by the selected entity when available.

**“Failed to retrieve Exchange asset” / “no asset matching given parameters”:** Studio is resolving the connector from Anypoint Exchange; this connector is not published there. Use the connector from your **local Maven repo** instead. (1) In the connector project run **`mvn clean install`**. (2) In your **Mule app’s** `pom.xml`, the dependency should use the same `groupId`/`artifactId` and the version you built (e.g. `1.0.90-SNAPSHOT`). Studio will resolve it from the local repository. If the app was created from Exchange or “Add from Exchange”, replace that dependency with the local artifact coordinates, or publish the connector to your organization’s private Exchange and reference it from there.

---

## DataWeave: unique source systems per business entity

Use this with **payload** = your tenant datamodel (e.g. from GET `/metadata/api/v2/objects/tenantModel/datamodel` or your cached file). It returns one entry per business entity internal id with an array of **unique** source systems (id and displayName). Handles both: source systems **nested** under each `businessEntity`, and **flat** `sourceSystem[]` with an entity reference on each item (e.g. `businessEntity.$ref` or `entityId`).

```dataweave
%dw 2.0
output application/json
var dm = payload

fun idFrom(item) = item.guid default item.identifier default item.id default (item as String default "")
fun displayFrom(item) = item.name default item.displayName default idFrom(item)

fun entityRefFrom(item) = (
  ["businessEntity", "entityId", "businessEntityRef", "entityRef", "entity"] 
  map (k) -> item[k] 
  filter ($ != null)
)[0] 
  then (if ($ is Object) ($.id default $.guid default "") else $ as String)
  else null

fun guidFromRefPath(refPath) = do {
  if (refPath == null or sizeOf(refPath) == 0) null
  else do {
    var idx = refPath indexOf "guid='"
    var start = if (idx >= 0) idx + 6 else -1
    var sub = if (start >= 0) refPath[start to -1] else ""
    var end = if (sizeOf(sub) > 0) sub indexOf "'" else -1
    ---
    if (start >= 0 and end >= 0) refPath[start to start + end]
    else do {
      var idx2 = refPath indexOf "guid=\""
      var start2 = if (idx2 >= 0) idx2 + 6 else -1
      var sub2 = if (start2 >= 0) refPath[start2 to -1] else ""
      var end2 = if (sizeOf(sub2) > 0) sub2 indexOf "\"" else -1
      ---
      if (start2 >= 0 and end2 >= 0) refPath[start2 to start2 + end2]
      else if (refPath startsWith ".") refPath[1 to -1]
      else null
    }
  }
}

// 1) From nested: businessEntity[].sourceSystem (or sourceSystems, dataSource, source, sourceSystemRef)
var nestedKeys = ["sourceSystem", "sourceSystems", "dataSource", "source", "sourceSystemRef"]
var fromNested = (dm.businessEntity default []) 
  filter (be) -> (be.guid default be.id default "") != ""
  map (be) -> {
    entityId: be.guid default be.id,
    sourceSystems: (
      (nestedKeys flatMap (k) -> (be[k] default []))
      map (item) -> { id: if (item is Object) idFrom(item) else item as String, displayName: if (item is Object) displayFrom(item) else (item as String) }
      filter ($.id != null and $.id != "")
    ) distinctBy $.id
  }

// 2) From flat: root sourceSystem[] (or sourceSystems, etc.) grouped by entity ref
var rootKeys = ["sourceSystem", "sourceSystems", "dataSource", "source"]
var flatList = rootKeys flatMap (k) -> (dm[k] default [])
var fromFlatByEntity = flatList
  filter (item) -> (entityRefFrom(item) default guidFromRefPath(item.businessEntity.$ref default "")) != null
  groupBy (item) -> (
    (entityRefFrom(item) 
      default (if (item.businessEntity != null and item.businessEntity.$ref != null) guidFromRefPath(item.businessEntity.$ref) else null))
    default "unknown"
  )
  mapObject (entityId, items) -> {
    (entityId): (items 
      map (item) -> { id: idFrom(item), displayName: displayFrom(item) } 
      filter ($.id != null and $.id != "") 
      distinctBy $.id)
  }

// 3) Merge: for each entity, combine nested + flat and unique by id
var allEntityIds = (fromNested map $.entityId) ++ (keysOf(fromFlatByEntity) filter $ != "unknown") distinctBy $
---
allEntityIds map (entityId) -> {
  businessEntityId: entityId,
  sourceSystems: (
    ((fromNested filter $.entityId == entityId)[0].sourceSystems default [])
    ++ 
    (fromFlatByEntity[entityId] default [])
  ) distinctBy $.id
}
```

Example output shape:

```json
[
  { "businessEntityId": "c360.person", "sourceSystems": [{ "id": "c360.default.system", "displayName": "Informatica Customer 360" }] },
  { "businessEntityId": "c360.organization", "sourceSystems": [{ "id": "c360.default.system", "displayName": "Default" }] }
]
```

If your datamodel uses different keys for the entity ref (e.g. only `$ref` on the item), extend the `entityRefFrom` logic or parse `$ref` in the same way as `guidFromRefPath`.

---

*Business 360 REST API — [Source Record API](https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Source_record_API.html)*

# 5. Master Read

**Source:** [Business entity record APIs — Business 360 REST API Reference](https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Business_entity_record_APIs.html)

**Connector operation:** **INFA MDM - Master Read** — Read a master (golden) record by Business ID or by Source System + Source Primary Key. Returns the fully blended, survived "best version of truth" record. Combines two Informatica APIs into one operation.

---

## Read Master Record APIs

Informatica provides two separate Read Master Record APIs. This connector combines them into a single operation with exclusive parameters.

### Read by Business ID

Retrieve an active business entity master record based on the business ID.

```
GET <baseApiUrl>/business-entity/public/api/v1/entity/<businessEntity>/<businessId>
```

[Read Master Record by Business ID docs](https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Business_entity_record_APIs.html#ww3_6_40_11_1)

### Read by SourcePKey

Retrieve an active business entity master record associated with a particular source system and primary key.

```
GET <baseApiUrl>/business-entity/public/api/v1/entity/<businessEntity>/<sourceSystem>/<sourcePKey>
```

[Read Master Record by SourcePKey docs](https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Business_entity_record_APIs.html#ww3_6_40_14_1)

---

## URI parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| **businessEntity** | Yes | Internal ID of the business entity (e.g. `c360.person`). |
| **businessId** | Exclusive | Unique identifier of the master record. Use this **or** sourceSystem + sourcePKey. |
| **sourceSystem** | Exclusive | Internal ID of the source system. Required with sourcePKey. |
| **sourcePKey** | Exclusive | Primary key of the source record. Required with sourceSystem. |

## Query parameters (Advanced tab)

| Parameter | Default | Description |
|-----------|---------|-------------|
| **_skipLookup** | `false` | When `true`, retrieves picklist values from cache (faster). |
| **_showContentMeta** | `false` | When `true`, includes `_contentMeta` (trust score, survivorship, data enhancement rule details). |
| **_showPending** | `false` | When `true`, retrieves only records pending review for creation, update, or deletion. |

---

## Response

The response is the full master record in JSON format, including field data and `_meta`. When `_showContentMeta=true`, includes `_contentMeta`.

### _meta fields

| Field | Type | Description |
|-------|------|-------------|
| **businessId** | String | Unique identifier of the record. |
| **businessEntity** | String | Internal ID of the business entity. |
| **states.base** | String | Active, inactive, or pending. |
| **states.validation** | String | PENDING, PASSED, FAILED, or IN_PROGRESS. |
| **states.consolidation** | String | Match and merge status. |
| **createdBy** | String | User who created the record. |
| **creationDate** | String | Date the record was created. |
| **updatedBy** | String | User who last updated the record. |
| **lastUpdateDate** | String | Date the record was last updated. |

---

## Connector: INFA MDM - Master Read

The **INFA MDM - Master Read** operation retrieves a single master (golden) record. It sends a GET request to one of two paths depending on which lookup parameters are provided:

- **By Business ID:** `/business-entity/public/api/v1/entity/{businessEntity}/{businessId}`
- **By SourcePKey:** `/business-entity/public/api/v1/entity/{businessEntity}/{sourceSystem}/{sourcePKey}`

### Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| **Business Entity Id** | Yes | Dropdown: internal ID of the business entity. |
| **Business ID** | Exclusive | The master record's business ID. Use this **or** Source System + Source Primary Key. |
| **Source System** | Exclusive | Dropdown (dependent on entity): internal ID of the source system. |
| **Source Primary Key** | Exclusive | Primary key of the source record. |
| **Skip Lookup** (Advanced) | No | Cache picklist values for faster response. Default false. |
| **Show Content Meta** (Advanced) | No | Include `_contentMeta` section. Default false. |
| **Show Pending** (Advanced) | No | Retrieve only pending records. Default false. |

### Output

- **Payload:** The full master record JSON (field data, `_meta`, optionally `_contentMeta`).
- **Attributes:** `MasterReadResponseAttributes` with `businessId`, `businessEntity`, `state`, `validation`, `consolidation`, `createdBy`, `creationDate`, `updatedBy`, `lastUpdateDate`, `statusCode`.

### Exclusive optionals

The parameter group uses `@ExclusiveOptionals(isOneRequired = true)`. In Anypoint Studio, the user must provide **either** Business ID **or** Source System + Source Primary Key. Both paths return the same master record shape.

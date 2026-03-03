# 4. Source Submit

**Source:** [Business entity record APIs — Business 360 REST API Reference](https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Business_entity_record_APIs.html#ww3_6_40_8_1)

**Connector operation:** **INFA MDM - Source Submit** — Submit source system data to create or update a master record. The MDM engine runs match/merge/survivorship and may require approval. (Informatica: Create Master Record.)

---

## Create Master Record API

Use the Create Master Record API to submit source system data for a business entity. The MDM engine decides whether to create a new master record, merge with an existing one, or hold the record for approval.

**Endpoint:**

```
POST <baseApiUrl>/business-entity/public/api/v1/entity/<businessEntity>?sourceSystem=<sourceSystem>[&sourcePKey=<pkey>][&_resolveCrosswalk=true|false][&businessId=<customId>]
```

The response is in JSON format. It returns the business ID of the created/updated record and whether a data steward must approve it.

---

## URI parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| **businessEntity** | Yes | Internal ID of the business entity (e.g. `c360.person`, `c360.organization`). |
| **sourceSystem** | Yes | Internal ID of the source system to which the record belongs (e.g. `c360.default.system`). |
| **sourcePKey** | No | The primary key of the source record. If omitted, the MDM engine assigns one. |
| **businessId** | No | Custom business ID. When specified, its length must differ from the business ID format length configured for the entity. |
| **_resolveCrosswalk** | No | When `true`, standardizes picklist values based on the source system crosswalk configuration. Default `false`. |

---

## Request body

The request body is a JSON object with the record field data. Include field values at the root level (e.g. `firstName`, `lastName`, `PostalAddress[]`, `Phone[]`). Optionally include `_contentMeta` for trust/survivorship data.

If a field group has a data quality rule, specify the `_id` parameter for the field group entry.

**Example request:**

```json
{
    "firstName": "Lewis",
    "lastName": "Hamilton",
    "middleName": "Decay",
    "PostalAddress": [
        {
            "_id": "3d4a8f4d07cb4f0db9ea1be9bf8d27e8",
            "defaultIndicator": true,
            "addressType": { "Name": "Home", "Code": "Home" },
            "addressLine1": "789456, park road",
            "country": { "Name": "United States", "Code": "US" },
            "state": { "Name": "California", "Code": "CA" },
            "city": "California"
        }
    ],
    "Phone": [
        {
            "_id": "f142a11b916c4a1b8239466f15852",
            "phoneType": { "Name": "Mobile", "Code": "Mobile" },
            "phoneNumber": "9900202777"
        }
    ]
}
```

---

## Response

```json
{
    "approvalRequired": false,
    "id": "9991234567890123",
    "businessId": "999456789222123"
}
```

| Field | Description |
|-------|-------------|
| **approvalRequired** | `true` if a data steward must approve the record; `false` if the record was created/updated immediately. |
| **businessId** | Unique identifier of the created/updated business entity record. |
| **id** | Deprecated. Use `businessId` instead. |

---

## Connector: INFA MDM - Source Submit

The **INFA MDM - Source Submit** operation submits source system data to create or update a master record. It sends a POST request to `/business-entity/public/api/v1/entity/{businessEntity}` with the source system and optional parameters as query parameters, and the record data as the JSON request body.

### Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| **Business Entity Id** | Yes | Dropdown: internal ID of the business entity. |
| **Source System** | Yes | Dropdown (dependent on entity): internal ID of the source system. |
| **Source Primary Key** | No | Primary key of the source record. If omitted, MDM assigns one. |
| **Business ID** | No | Custom business ID for the record. |
| **Record Payload** | Yes | JSON body with the record field data (`#[payload]` or inline). |
| **Resolve Crosswalk** (Advanced) | No | When true, standardizes picklist values via crosswalk. Default false. |

### Output

- **Payload:** The full JSON response (businessId, approvalRequired).
- **Attributes:** `SourceSubmitResponseAttributes` with `businessId` (String), `approvalRequired` (boolean), `statusCode` (int).

### Why "Source Submit"?

The Informatica API calls this "Create Master Record," but the operation always requires a **source system**. You are submitting source data; the MDM engine decides whether to create, merge, or hold for approval. "Submit" sets the right expectation: the caller sends data, the system makes the decision. See [0_Concept.md](0_Concept.md) for the full naming rationale.

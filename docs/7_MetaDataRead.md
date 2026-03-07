# 7. MetaData Read

**Source:** [Informatica KB: Metadata from Org](https://knowledge.informatica.com/s/article/metadata-from-org?language=en_US) | [Relationship Metadata API](https://docs.informatica.com/master-data-management-cloud/business-360-console/current-version/business-360-rest-api-reference/business-360-rest-api/relationship-api/metadata-api.html)

**Connector operation:** **INFA MDM - MetaData Read** — Read structural metadata from the Informatica MDM Metadata v2 API. Returns schema definitions, entity blueprints, or relationship metadata depending on the selected resource.

---

## Metadata v2 API Overview

The Informatica MDM Metadata v2 API exposes the structural definitions (blueprints) of your tenant's data model. Unlike the data APIs that return actual records, the metadata API returns the **schema** — fields, data types, constraints, relationships, and lookup types.

Base path for all resources:

```
GET <baseApiUrl>/metadata/api/v2/objects/...
```

For resources that target a specific object, the URL format is:

```
GET <baseApiUrl>/metadata/api/v2/objects/<resourceSegment>/<objectInternalId>
```

For example: `/metadata/api/v2/objects/businessEntity/c360.person` returns the metadata for the business entity whose internal id is `c360.person`.

### Resource Summary

| Resource | Path | Object Internal Id | Description |
|----------|------|--------------------|-------------|
| **DATAMODEL** | `/tenantModel/datamodel` | No | Returns the **entire schema** for the organization: all business entities, relationships, field groups, and lookup types. |
| **BUSINESS_ENTITY** | `/businessEntity/{objectInternalId}` | Yes (e.g. `c360.person`) | Field types, lengths, constraints, and relationships for a specific business entity. |
| **REFERENCE_ENTITY** | `/referenceEntity/{objectInternalId}` | Yes | Metadata for a specific reference entity. |
| **RELATIONSHIP** | `/relationship` | No | Metadata about how objects link. Contains `relationship_Internal_ID` and attributes for Create/Update Relationship APIs. |
| **VIEW** | `/view/{objectInternalId}` | Yes | Metadata for a specific view. |
| **EXTERNAL_RESOURCE** | `/externalResource/{objectInternalId}` | Yes | Metadata for a specific external resource. |

---

## DATAMODEL — Org Discovery

Retrieve the full structural definition of the tenant's data model.

```
GET <baseApiUrl>/metadata/api/v2/objects/tenantModel/datamodel
```

### What is returned

The response is a large JSON payload containing the complete schema for the organization:

| Section | Description |
|---------|-------------|
| **businessEntity** | Definitions of core objects (e.g. Person, Supplier) with their fields, data types, and constraints. |
| **relationship** | How entities are linked (One-to-Many, Many-to-Many). |
| **fieldGroups** | Logical groupings of fields used in the UI (e.g. "Address Details"). |
| **lookupTypes** | Metadata for dropdown values and reference data. |

### Common use cases

- **Automation:** Generating client-side code or UI forms dynamically based on the current data model.
- **Audit & Compliance:** Verifying that the tenant's data structure meets specific regulatory requirements.
- **Integration:** Mapping external systems to the internal schema of the SaaS platform.

---

## BUSINESS_ENTITY — Entity Blueprint

Drill down into the structural definition of a single business entity using its **internal id** (e.g. `c360.person`, not the display name).

```
GET <baseApiUrl>/metadata/api/v2/objects/businessEntity/<objectInternalId>
```

Example: `GET .../metadata/api/v2/objects/businessEntity/c360.person`

### Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| **Object Internal Id** | Yes | The business entity internal id (e.g. `c360.person`). Use the datamodel or entity list to discover valid values. |

### What is returned

The response is a JSON payload containing the metadata definition for that object:

| Section | Description |
|---------|-------------|
| **Field Properties** | Every attribute associated with the object (e.g. `FirstName`, `TaxID`). |
| **Data Types** | Whether a field is a String, Integer, Date, or Lookup. |
| **Constraints** | Length limits, "Required" flags, and unique constraints. |
| **Relationships** | How this object relates to others (e.g. a "One-to-Many" relationship between Account and Address). |

### Common use cases

- **Dynamic form generation:** Build UI forms based on the entity's field definitions.
- **Data validation:** Enforce field constraints before submitting records via the Source Submit API.
- **Schema documentation:** Auto-generate data dictionaries for specific entities.

---

## REFERENCE_ENTITY, VIEW, EXTERNAL_RESOURCE

Each of these resources follows the same URL pattern with a resource segment and an object internal id:

- **REFERENCE_ENTITY:** `GET .../metadata/api/v2/objects/referenceEntity/{objectInternalId}`
- **VIEW:** `GET .../metadata/api/v2/objects/view/{objectInternalId}`
- **EXTERNAL_RESOURCE:** `GET .../metadata/api/v2/objects/externalResource/{objectInternalId}`

Provide the **Object Internal Id** parameter when using any of these resources in the connector.

---

## RELATIONSHIP — Hierarchy & Graph

Retrieve metadata about how objects link to each other. No object internal id is required.

```
GET <baseApiUrl>/metadata/api/v2/objects/relationship
```

### What is returned

The response contains relationship metadata including:

| Field | Description |
|-------|-------------|
| **relationship_Internal_ID** | Internal identifier required for the Relationship APIs (Create, Update, Delete). |
| **Relationship attributes** | Attributes that can be included in the request body of Create Relationship and Update Relationship APIs. |
| **Cardinality** | How entities relate (One-to-One, One-to-Many, Many-to-Many). |
| **Related entities** | Which business entities participate in each relationship. |

### Common use cases

- **Relationship CRUD:** Obtain the `relationship_Internal_ID` before calling Create/Update/Delete Relationship APIs.
- **Graph visualization:** Build relationship diagrams showing how entities connect.
- **Integration mapping:** Understand parent-child and peer relationships for data migration.

---

## Connector: INFA MDM - MetaData Read

The **INFA MDM - MetaData Read** operation sends a GET request to the appropriate Metadata v2 path based on the **Resource** dropdown and, when required, the **Object Internal Id**.

### Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| **Resource** | Yes | Dropdown: `DATAMODEL`, `BUSINESS_ENTITY`, `REFERENCE_ENTITY`, `RELATIONSHIP`, `VIEW`, or `EXTERNAL_RESOURCE`. |
| **Object Internal Id** | Conditional | Internal id of the object (e.g. `c360.person` for BUSINESS_ENTITY). **Required** when Resource is `BUSINESS_ENTITY`, `REFERENCE_ENTITY`, `VIEW`, or `EXTERNAL_RESOURCE`; ignored for `DATAMODEL` and `RELATIONSHIP`. |

### Output

- **Payload:** The full metadata JSON response as a `Map<String, Object>`.
- **Attributes:** `MetaDataReadResponseAttributes` with:

| Attribute | Type | Description |
|-----------|------|-------------|
| **resource** | String | The resource type that was queried (e.g. `DATAMODEL`, `BUSINESS_ENTITY`, `RELATIONSHIP`). |
| **objectInternalId** | String | The object internal id when the resource requires it (e.g. `c360.person`). Empty for `DATAMODEL` and `RELATIONSHIP`. |
| **statusCode** | int | HTTP status code of the response. |
| **requestId** | String | Request/tracing ID from response headers. |

### Resolved API paths

| Resource | Resolved Path |
|----------|---------------|
| `DATAMODEL` | `<baseApiUrl>/metadata/api/v2/objects/tenantModel/datamodel` |
| `BUSINESS_ENTITY` | `<baseApiUrl>/metadata/api/v2/objects/businessEntity/{objectInternalId}` |
| `REFERENCE_ENTITY` | `<baseApiUrl>/metadata/api/v2/objects/referenceEntity/{objectInternalId}` |
| `RELATIONSHIP` | `<baseApiUrl>/metadata/api/v2/objects/relationship` |
| `VIEW` | `<baseApiUrl>/metadata/api/v2/objects/view/{objectInternalId}` |
| `EXTERNAL_RESOURCE` | `<baseApiUrl>/metadata/api/v2/objects/externalResource/{objectInternalId}` |

### Error handling

- If **Resource** requires an object (BUSINESS_ENTITY, REFERENCE_ENTITY, VIEW, EXTERNAL_RESOURCE) and **Object Internal Id** is not provided, the operation throws a `B360:CLIENT_ERROR`.
- Standard B360 error handling applies: HTTP 4xx → `B360:CLIENT_ERROR`, HTTP 5xx → `B360:SERVER_ERROR`, timeouts → `B360:TIMEOUT`.

### Troubleshooting

| Issue | Resolution |
|-------|------------|
| **401 Unauthorized** | Verify your Bearer token / session has the Metadata or System Administrator scope. |
| **404 Not Found** | Check that the Object Internal Id is valid for the selected resource (e.g. `c360.person` for BUSINESS_ENTITY). Use DATAMODEL or entity lists to discover internal ids. Verify the API version (`v2`) is supported by your instance. |
| **Tenant context** | Some environments require a specific Tenant-ID header. Use the Http Request operation if you need to pass custom headers. |

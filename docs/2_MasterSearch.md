# 2. Search

**Source:** [Search API — Business 360 REST API Reference](https://onlinehelp.informatica.com/IICS/prod/b360/en/wz-b360-rest-api/Search_API.html)

**Connector operation:** **Search Golden Records** — POST to the Search API to search master (golden) records. (Informatica: Master Search.) **Search Criteria** includes **Business Entity Id** (required; select a specific entity, e.g. Person, Organization), **Search** text, and **Fields**. Use **Search** or **Fields**, not both. Optional **Pagination**, **Filters**, **Sort**, **Show Content Meta**.

---

## Search API

Use the search API to search for records that match your search criteria across all the business entities or within a specific business entity (English and Japanese). You can filter results by severity levels, validation statuses, and by fields that are blank or contain values.

**Endpoint:**

```
POST <baseApiUrl>/search/public/api/v1/search
```

Example (after base URL modification): `https://use4-mdm.dm-us.informaticacloud.com/search/public/api/v1/search`

The response is in JSON format.

**Note:** After editing record and field privileges or data access rules, users with custom user roles must wait at least 10 minutes before searching via the Search REST API. Searching immediately after such changes can return inaccurate results.

---

## Request body (main parameters)

Use either the **search** or **fields** parameter, not both.

A **JSON Schema** for the request body is available: [search-request-schema.json](search-request-schema.json).

| Parameter       | Type   | Description |
|----------------|--------|-------------|
| **entityType** | String | Internal ID of the business entity to search (e.g. `c360.person`, `c360.organization`). **Connector:** required; you must select a specific entity (no search-across-all). |
| **search**     | String | Search string. Use `*` to retrieve all records (often with filters). Supports fuzzy search, wildcards (`*`, `~`, `&&`, `||`). For exact search, enclose in double quotes, e.g. `"\"London\""`. |
| **fields**     | Object | Search specific basic, smart, system, or dynamic fields (e.g. first name `"J*"`). Use **search** or **fields**, not both. |
| **maxRecords** | Number | Maximum number of records to return. Search cannot return more than 10,000 records per page. |
| **pageSize**   | Number | Number of search results per page. |
| **pageOffset** | Number | Number of pages to skip (default 0). |
| **recordsToReturn** | Number | Number of search results to return. |
| **recordOffset**   | Number | Number of records to skip (default 0). |
| **filters**    | Object | Filter conditions. Root object has **operator** (AND or OR) and **filter** (array of conditions). Each condition has **comparator** (EQUALS, NOT_EQUALS, IN, CONTAINS, STARTS_WITH, IS_MISSING, IS_NOT_MISSING, BETWEEN, GREATER_THAN, LESS_THAN, etc.), **fieldName**, and **fieldValue** (single value or array depending on comparator). |
| **sort**       | Array  | Sort by field (fieldName, order: ASCENDING or DESCENDING). |
| **searchLocale** | String | Optional. Language code: `en` (default) or `ja`. |

---

## Search string help

The following guidance is from the [Search API — Search string](https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Search_API.html#ww3_6_37_8_1) documentation.

- **To retrieve all records**, use an asterisk (`*`).
- **Fuzzy search:** Enter a search word or phrase. You can use wildcard characters: `*`, `~`, `&&`, and `||`.

**Examples:**

| Search string | Behavior |
|---------------|----------|
| `John Smith` | Records that contain *John Smith* as a field value. |
| `Jo*n` | Records that start with *Jo* and end with *n* (e.g. Johansson, Jordan). |
| `Hans && Williams` | Records that contain both *Hans* and *Williams* in the same record (e.g. first name Hans, last name Williams). |

**Exact search:** Enclose the value in double quotes in this format: `"\"<Field_Value>\""`.  
Example: to match the value *London* exactly, use: `"\"London\""`.  
This format applies to string, date, and date-time fields. For other field types, do not use the quoted format for exact match.

**Dates:** Use the **YYYY-MM-DD** format.

**Keywords as values:** When searching for the literal words AND, OR, or NOT as field values, specify them as: `"\"OR\""`, `"\"AND\""`, `"\"NOT\""`. If you don’t, the Search REST API may not return results. If the keyword is only part of a value (e.g. *SAND AND MARVEL*), you don’t need to enclose the keyword in this format.

**System fields:** You can’t search for *similar* values in system fields; only exact match is supported. Wildcards in search strings are still supported for system fields.

**Note:** A search string that contains only white space (no other characters) returns no results.

---

## Fuzzy search patterns (SAAS Search Scenarios) — great for MCP / Agent Planning

Find the search behavior for different patterns in the **top-right Search** tab in the application.

**Source:** [SAAS Search Scenarios](https://knowledge.informatica.com/s/article/SAAS-Search-Scenario-s?language=en_US&type=external) (Informatica Knowledge)

| Example search string | Search behaviour |
|------------------------|------------------|
| `John Smith` | Searches for records that contain John, Smith, or any variations of John or Smith as a field value. |
| `"John Smith"` | Searches for records that contain John Smith as a field value. |
| `John*` | Searches for records that contain a value that starts with John, such as Johnson or Johnny. |
| `Jo*n` | Searches for records that start with Jo and end with n, such as Johansson or Jordan. |
| `*` | Returns all the records. |
| `Jo?n` | Searches for records with a single character between Jo and n, such as John and Joan. You can use the question mark wildcard character multiple times in a word. |
| `The Washington Post` | Ignores the stopword *The* and searches for records that contain Washington, Post, or any variations of Washington or Post. Example stopwords are *a*, *an*, *of*, and *with*. For a complete list of stopwords, contact Informatica Global Customer Support. |
| `-John*` | Searches for records that don't start with John or any variations of John, such as Johnson or Johnny. |
| `+Manager +Janet*` | Searches for records that contain Manager and a field value that starts with Janet. For example, a record with the name Janet Williams and designation Manager matches the search string. |
| `Joan~` | Searches for records with similar values where up to two characters in the search string can be replaced. For example, John or Donn can match the search string. |
| `Hans && Williams` | Searches for records that contain Hans and Williams in the same record. For example, first name is Hans and last name is Williams. |
| `(Janet* \|\| John*) && Manager` | Searches for records that contain Manager and any variations of Janet or John as a field value. For example, a record with first name Johnny and designation Manager. |
| `/Joan_([0-9]+)/` | Searches for records that contain Joan_ followed by a numeral, such as Joan_123 or Joan_1. The forward slashes act as escape characters so that the wildcard characters are not considered as part of the search string. |

---

## Search string examples (quick reference)

- `Hans && Williams` — records containing both in the same record.
- `Jo*n` — values starting with *Jo* and ending with *n* (e.g. Johansson, Jordan).
- `*` — all records (typically used with filters).
- `"\"London\""` — exact match for the value London.
- Date format for search: `YYYY-MM-DD`.

---

## Request response

The response returns the search results (e.g. `searchResult` with `records`, pagination, etc.). See the [Search API](https://onlinehelp.informatica.com/IICS/prod/b360/en/wz-b360-rest-api/Search_API.html) documentation for full response structure and sample responses.

---

## Connector: Search Golden Records

The **Search Golden Records** operation searches master (golden) records only. It sends a POST request to `/search/public/api/v1/search` with a JSON body. It returns a **paged stream** of results (one page per response body); pagination is controlled by the **Pagination** parameter group.

| Parameter / group | Required | Description |
|-------------------|----------|-------------|
| **Pagination** | No | **Page Size**, **Records To Return**, **Record Offset**. |
| **Search Criteria** | — | **Business Entity Id** (required; select a specific entity, e.g. Person, Organization), **Search** text, **Fields**. Use Search or Fields, not both. See [Search string help](#search-string-help) below. |
| **Filters** | No | **Operator** — AND or OR (how to combine multiple filter rows). **Filter** — Table of conditions: **Comparator** (EQUALS, NOT_EQUALS, IN, BETWEEN, CONTAINS, STARTS_WITH, IS_MISSING, IS_NOT_MISSING, etc.), **Field Name**, **Field Value** (single value or list depending on comparator). |
| **Sort** | No | Table of **Field Name** and **Order** (ASCENDING or DESCENDING). |
| **Show Content Meta** (Advanced) | No | Toggle to include content metadata in the response. |

- **Request schema:** The payload sent to Informatica follows [search-request-schema.json](search-request-schema.json). The connector sends `entityType` (required; the selected business entity ID), `search` or `fields`, pagination (`pageSize`, `recordsToReturn`, `recordOffset`), `filters` (with `operator` and `filter` array), `sort`, and optional flags as configured.
- **Paging:** The connector uses a PagingProvider and requests subsequent pages by incrementing `recordOffset` until the requested records are returned or the API reports no more results.

---

*Business 360 REST API — [Search API](https://onlinehelp.informatica.com/IICS/prod/b360/en/wz-b360-rest-api/Search_API.html)*

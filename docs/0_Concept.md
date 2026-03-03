# 0. Concept: Canonical Application Integration Naming

This document captures the architectural recommendation for the Informatica Business 360 (B360) MuleSoft connector: **abstract raw MDM terminology and expose canonical, Application Integration–friendly operation names**.

---

## The Dilemma

There is a fundamental disconnect between:

- **Data Integration (ETL, MDM, batch syncing)** — thinks in terms of probabilistic governance: *Contribute, Merge, Survive, Unmerge*.
- **Application Integration (APIs, events, real-time CRUD)** — thinks in terms of clear, deterministic state changes: *Create, Read, Update, Delete*.

If the connector exposes raw Informatica names (e.g. *Create Master Record*, *Entity XREF*) to an application developer building a lightweight microservice, they will misunderstand what the API actually does. That leads to bad architecture and support load.

---

## Recommendation: Abstract It

**The connector should act as an API Facade or Anti-Corruption Layer (ACL).** Its job is not only to pass data; it is to translate MDM concepts into standard, predictable application concepts.

Use **canonical Application Integration naming** in the connector (e.g. display names in Anypoint Studio). Keep the exact Informatica endpoint and terminology in tooltips, summaries, or docs for architects who need to debug the raw HTTP call.

---

## Translation Guide

| Informatica raw name / endpoint | What it actually does | Connector canonical name |
|---------------------------------|------------------------|---------------------------|
| **Create Master Record** — `POST /entity/{entityType}` | Accepts source data and runs it through the match/merge engine. | **Upsert Source Candidate** or **Submit System Data** |
| **Update Master Record** — `PUT /entity/{entityType}/{id}` | Bypasses matching to explicitly update a known golden record based on source input. | **Update Known Record** |
| **Read Master Record** — `GET /entity/{entityType}/{id}` | Returns the fully blended, survived, “Best Version of Truth” record. | **Get Golden Record** or **Get Consolidated Profile** |
| **Source Record API (General)** — `/entity-xref/...` | Interacts with the raw, isolated data from one specific source system. | **System Cross-Reference (XREF)** |
| **Delete Source Record** — `DELETE /entity-xref/{id}` | Removes a specific system’s contribution; master recalculates. | **Remove Source Contribution** |

---

## Why Canonical Naming Helps

1. **Prevents the “I created it, why is it gone?” problem**  
   If the operation is called *Create Master Record*, an AppInt developer will assume they own that record. When the MDM engine merges it with another record later, they will think the API is broken. A name like **Submit Candidate** or **Upsert Source Candidate** sets the expectation that the engine makes the decision.

2. **Hides complexity they don’t need**  
   Application developers want to push source data and move on. They don’t need to read Informatica documentation on Survivorship Rules. The connector should encapsulate that complexity.

3. **Future-proofing**  
   If the organization replaces Informatica with another MDM (e.g. Reltio, Semarchy), application developers don’t have to rewrite. The connector’s canonical interface stays the same; only the wiring under the hood changes.

---

## The Compromise: Good Documentation

- **Operations** (e.g. display names in Anypoint Studio) → use **canonical AppInt names**.
- **Tooltips / descriptions / metadata** → include the exact Informatica endpoint and, where useful, the raw name.

**Example description:**

> **Upsert Source Candidate** — Submits target system data to the MDM hub for matching and consolidation.  
> *(Maps to Informatica: POST /business-entity/public/api/v1/entity/{entityType})*

That gives application developers a clear concept and gives senior architects a breadcrumb to the raw HTTP call.

---

## Relation to This Connector

Today this connector uses **canonical operation names** (renamed from the legacy INFA MDM names):

| Current name | What it does | Issue | Canonical alternative |
|--------------|--------------|--------|------------------------|
| **INFA MDM - Master Search** | Search over golden/master records (Search API). | “Master” is MDM jargon; “INFA MDM” is vendor branding. An AppInt developer may not know “master” = blended best-version-of-truth. | **Search Golden Records** or **Search Consolidated Profiles** — matches the “Get Golden Record” vocabulary above. |
| **INFA MDM - Source Read** | Read a single source system’s contribution (Source Record API / entity-xref). | “Source Read” is vague (read what?). It doesn’t say “one system’s contribution, not the blended record.” | **Get Source Contribution** or **Get Cross-Reference Record** — makes clear this is system-specific/XREF data. |

**Recommendation:** For a true ACL, prefer the canonical alternatives in the UI and keep “INFA MDM” and the raw Informatica operation name in the operation description/tooltip (e.g. *“Search Golden Records — searches the consolidated (golden) record set. Maps to Informatica: POST …/search.”*). If renaming is not done yet, treat the table above as the target and document it in the connector’s design so future operations (e.g. submit candidate, remove source contribution) use canonical names from the start.

Future operations (e.g. submit candidate, update golden record, remove source contribution) should follow the same pattern: **canonical names in the UI**, **Informatica mapping in the description**.

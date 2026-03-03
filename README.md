# mule-infa-b360-connector

MuleSoft Informatica MDM - Business 360 Anypoint Connector

A Mule 4 connector for the Informatica MDM Business 360 REST API, enabling integration with Business 360 master data management capabilities from MuleSoft Anypoint Platform.

## Features

- **Authentication** -- Session-based login to Informatica Business 360
- **Search** -- Search business entity records with filters, sorting, and pagination
- **Master Read** -- Read master (golden) records by row ID
- **Source Read** -- Read source system records
- **Source Submit** -- Create and update records via source system submissions

## Installation

Add the following dependency to your Mule application `pom.xml`:

```xml
<dependency>
    <groupId>com.mulesoftforge</groupId>
    <artifactId>mule-infa-b360-connector</artifactId>
    <version>1.0.0</version>
    <classifier>mule-plugin</classifier>
</dependency>
```

## License

This project is licensed under the [Common Public Attribution License v1.0 (CPAL)](http://www.mulesoft.com/CPAL).

# 1. Authentication

**Source:** [Login - Platform REST API version 3 resources](https://docs.informatica.com/cloud-common-services/administrator/current-version/rest-api-reference/platform-rest-api-version-3-resources/login.html)

**This connector uses Login API version 3** (`/saas/public/core/v3/login`).

**Connector behavior:** This connector does not store the session ID in Object Store; the session is held in the connection instance and is refreshed when the connection is re-established. The connector does not call the logout resource; sessions end when the connection is disposed or when they expire on the server (e.g. after 30 minutes of inactivity).

---

## Login

Use the login resource to log in to **Informatica Intelligent Cloud Services** to use version 3 REST API resources.

The login response includes the session ID and base URL that you need to include in the REST API calls that you make during the session.

Use values from the following fields that are returned in the login response:

- **sessionId.** A REST API session ID that you include in the header for REST API calls.  
  For more information about session IDs, see [Session IDs](https://docs.informatica.com/cloud-common-services/administrator/current-version/rest-api-reference/informatica-intelligent-cloud-services-rest-api/session-ids.html#GUID-7E9820B0-68C7-48A7-9CAD-4D65D6A58E9D).
- **baseApiUrl.** The base URL that you use in all version 3 resource URIs except for login, for example:  
  `<baseApiUrl>/public/core/v3/<resource>`

Your team might use multiple organizations such as an organization for development and an organization for testing. The user credentials that you use to log in determine the organization that you access.

---

## POST request

To log in, use the following URL:

```
https://<cloud provider>-<region>.informaticacloud.com/saas/public/core/v3/login
```

The values for cloud provider and region correspond to the name of the POD (Point of Deployment) that your organization uses. The following table lists the POD names and the corresponding cloud provider and region to use in the login URL:

| POD name | Cloud provider-region |
| -------- | --------------------- |
| USW1     | dm-us                 |
| USE2     | dm-us                 |
| USW3     | dm-us                 |
| USE4     | dm-us                 |
| USW5     | dm-us                 |
| USE6     | dm-us                 |
| USW1-1   | dm1-us                |
| USW3-1   | dm1-us                |
| USW1-2   | dm2-us                |
| CAC1     | dm-na                 |
| APSE1    | dm-ap                 |
| APSE2    | dm1-apse              |
| APNE1    | dm1-ap                |
| APNE2    | dm-apne               |
| APAUC1   | dm1-apau              |
| EMW1     | dm-em                 |
| EMC1     | dm1-em                |
| UK1      | dm-uk                 |

For example, if your organization uses the APNE1 POD, use the following URL:

```
https://dm1-ap.informaticacloud.com/saas/public/core/v3/login
```

If you don't know the name of the POD that your organization uses, contact your organization administrator or Informatica Global Customer Support.

For more information about the POD names and corresponding cloud providers and regions, see the [Product Availability Matrix (PAM) for Informatica Intelligent Cloud Services](https://knowledge.informatica.com/s/article/DOC-17579?language=en_US) on the Knowledge Base.

Use the following fields in a login object:

| Field    | Type   | Required | Description                                                                                                                         |
| -------- | ------ | -------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| username | String | Yes      | Informatica Intelligent Cloud Services user name for the organization that you want to log in to. Maximum length is 255 characters. |
| password | String | Yes      | Informatica Intelligent Cloud Services password. Maximum length is 255 characters.                                                  |

---

## POST response

Returns user information if the request is successful. Returns the error object if errors occur.

Use the base URL and session ID returned in the response for subsequent requests during this session.

A successful request returns the following objects:

| Field       | Type       | Description                                                                                                         |
| ----------- | ---------- | ------------------------------------------------------------------------------------------------------------------- |
| products    | Collection | Subscribed Informatica products.                                                                                    |
| name        | String     | Product name.                                                                                                       |
| baseApiUrl  | String     | Returned in the product object. Base API URL for the product. Use in REST API requests.                             |
| userInfo    | Collection | User information.                                                                                                   |
| sessionId   | String     | Returned in the userInfo object. REST API session ID for the current session. Use in most REST API request headers. |
| id          | String     | Returned in the userInfo object. User ID.                                                                           |
| name        | String     | User name.                                                                                                          |
| parentOrgId | String     | Organization ID for the parent organization.                                                                        |
| orgId       | String     | Returned in the userInfo object. ID of the organization the user belongs to. 22 characters.                         |
| orgName     | String     | Returned in the userInfo object. Organization name.                                                                 |
| groups      | Collection | User group information for the user.                                                                                |
| status      | String     | Status of the user. Returns one of the following values: Active, Inactive                                            |

---

## POST example

To log in to your **Informatica Intelligent Cloud Services** organization, you might use the following request:

```
POST https://dm-us.informaticacloud.com/saas/public/core/v3/login
Content-Type: application/json
Accept: application/json

{
    "username": "user@informatica.com",
    "password": "mypassword"
}
```

If successful, the response includes the products and userInfo objects which contain the baseApiUrl and sessionId values to use in subsequent calls, as shown in the following example:

```json
{
 	"products": [
 		{
 			"name": "Integration Cloud",
 			"baseApiUrl": "https://usw3.dm-us.informaticacloud.com/saas"
 		}
 	],
 	"userInfo": {
 		"id": "9L1GFroXSDHe2IIg7QhBaT",
 		"name": "user",
 		"parentOrgId": "52ZSTB0IDK6dXxaEQLUaQu",
 		"orgId": "0cuQSDTq5sikvN7x8r1xm1",
 		"orgName": "MyOrg_INFA",
 		"groups": {},
 		"status": "Active"
 	}
}
```

Using the session ID and base URL values in the above response as an example, to send a GET request to obtain license information, you might use the following request:

```
GET https://usw3.dm-us.informaticacloud.com/saas/public/core/v3/license/org/0cuQSDTq5sikvN7x8r1xm1
Content-Type: application/json
Accept: application/json
```

---

## Business 360–specific notes

The **Business 360 REST API** documentation adds product-specific details that apply when calling B360 (MDM) APIs; it does **not** contradict the platform login reference above.

- **Session header:** Use the **IDS-SESSION-ID** header for authentication on subsequent requests, not the `Authorization` header.  
  See [Authentication method](https://onlinehelp.informatica.com/IICS/prod/b360/en/wz-b360-rest-api/Authentication_method.html) (Business 360 Online Help).
- **Session ID vs JWT:** Login can return either a session ID or a JSON Web Token (JWT). Both are sent in the same **IDS-SESSION-ID** header. Prior to November 3, 2025, only session ID was used. From that date, an administrator can configure the organization to use JWT (expires per admin setting) or session ID (expires after 30 minutes of inactivity, with up to 2 minutes grace). If the value expires, log in again.  
  See [Session IDs and JSON Web Tokens](https://onlinehelp.informatica.com/IICS/prod/b360/en/wz-b360-rest-api/Session_IDs_and_JSON_Web_Tokens.html).
- **JWT support:** This connector supports both session ID and JWT. The login response returns either in the same `sessionId` field; the connector sends it in the **IDS-SESSION-ID** header for all subsequent requests. When your organization uses JWT-based authentication, token expiry is set by the administrator (default **30 minutes**; configurable in the Expiry Time field, e.g. 15, 30, 60, 120, 180, or 240 minutes). The connector **refreshes the token proactively**: if the token is a JWT and expires within 5 minutes, validation fails so that the connection is re-established and a new login is performed. That way you avoid service interruption. If you use IICS REST APIs with JWT, Informatica recommends refreshing the token at least 5 minutes before expiration—this connector does that automatically. **Do not enable JWT** if your organization uses **B2B Gateway**, as that service does not support JWT authentication. For details on JWT in Informatica Intelligent Cloud Services, see [JWT Support](https://knowledge.informatica.com/s/article/JWT-Support?language=en_US) (Knowledge Base).
- **Login resource versions:** You can use **Login API V3** (this connector) or **Login API V2** (`/ma/api/v2/user/login`). After login, session and base URL usage is the same.  
  See [REST API versions of the login resource](https://onlinehelp.informatica.com/IICS/prod/b360/en/wz-b360-rest-api/REST_API_versions_of_the_login_resource.html).
- **Base URL for B360 calls:** The login response returns a `baseApiUrl` (e.g. `https://use4.dm-us.informaticacloud.com/saas` or `https://usw1.dmp-us.informaticacloud.com/saas`). For Business 360 REST API calls you must:
  - Remove `/saas` from the path.
  - Replace the **first host segment** with `{segment}-mdm` (e.g. `use4` → `use4-mdm`, `usw1` → `usw1-mdm`).  
  Examples:
  - `https://use4.dm-us.informaticacloud.com/saas` → `https://use4-mdm.dm-us.informaticacloud.com`
  - `https://usw1.dmp-us.informaticacloud.com/saas` → `https://usw1-mdm.dmp-us.informaticacloud.com`  
  This connector performs this modification automatically after login.  
  See [Authentication method – Modifying the baseApiUrl](https://onlinehelp.informatica.com/IICS/prod/b360/en/index.htm#page/wz-b360-rest-api/Authentication_method.html).
- **Session expiry:** The session ID expires after 30 minutes of inactivity; log in again to continue.

---

*Informatica Cloud Common Services, Platform REST API version 3 — [Login](https://docs.informatica.com/cloud-common-services/administrator/current-version/rest-api-reference/platform-rest-api-version-3-resources/login.html)*

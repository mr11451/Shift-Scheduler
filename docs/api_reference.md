# API Reference Documentation

## Position in docs

- Category: Non-screen design/reference document
- Related: `domain_model.md`, `api_db_model.md`

Shift Scheduler REST API Reference - All Endpoints

**Base URL**: `http://localhost:8080/api`

**Default Content-Type**: `application/json`

**Authentication**:
- Protected endpoints require `Authorization: Bearer <JWT>`
- Actor identity (`staffId`) and role (`roleLevel`) are derived from JWT claims on the server side
- Custom identity headers such as `X-Staff-Id`, `X-Editor-Staff-Id`, `X-Updater-Staff-Id` are not used

---

## Table of Contents

1. [Staff Management API](#staff-management-api)
2. [Group Management API](#group-management-api)
3. [Qualification Management API](#qualification-management-api)
4. [Shift Type Management API](#shift-type-management-api)
5. [Shift Assignment API](#shift-assignment-api)
6. [Shift Request API](#shift-request-api)
7. [Calendar View Permission API](#calendar-view-permission-api)
8. [Authentication and Password Reset API](#authentication-and-password-reset-api)
9. [System Setting API](#system-setting-api)
10. [Error Handling](#error-handling)

---

## Staff Management API

Manage staff profiles, registration numbers, and role assignments.

### List All Staffs

**Request**
```
GET /staffs
```

**Query Parameters**
- `groupId` (optional): Filter by group ID
- `roleLevel` (optional): Filter by role (MEMBER, CHIEF, MASTER)

**Response** (200 OK)
```json
[
  {
    "id": 1,
    "staffCode": "STF-00001",
    "staffName": "田中太郎",
    "email": "tanaka@example.com",
    "phone": "090-1234-5678",
    "responsibility": "営業",
    "roleLevel": "MEMBER",
    "groupId": 1,
    "groupName": "営業部",
    "isActive": true,
    "createdAt": "2026-07-26T10:00:00+09:00",
    "updatedAt": "2026-07-26T10:00:00+09:00"
  }
]
```

### Get Staff by ID

**Request**
```
GET /staffs/{id}
```

**Path Parameters**
- `id` (required): Staff ID

**Response** (200 OK)
```json
{
  "id": 1,
  "staffCode": "STF-00001",
  "staffName": "田中太郎",
  "email": "tanaka@example.com",
  "phone": "090-1234-5678",
  "responsibility": "営業",
  "roleLevel": "MEMBER",
  "groupId": 1,
  "groupName": "営業部",
  "isActive": true,
  "createdAt": "2026-07-26T10:00:00+09:00",
  "updatedAt": "2026-07-26T10:00:00+09:00"
}
```

**Error Responses**
- `404 NOT FOUND`: Staff not found

### Create Staff

**Request**
```
POST /staffs
Content-Type: application/json

{
  "staffName": "山田花子",
  "email": "yamada@example.com",
  "phone": "090-9876-5432",
  "responsibility": "企画",
  "roleLevel": "CHIEF",
  "groupId": 2
}
```

**Fields**
- `staffName` (required): Staff name (max 100 chars)
- `email` (optional): Email address (required for MEMBER/CHIEF roles)
- `phone` (optional): Phone number (format: numbers and hyphens only)
- `responsibility` (required): Job responsibility (max 100 chars)
- `roleLevel` (required): Role level (MEMBER, CHIEF, MASTER)
- `groupId` (optional): Group assignment ID (required for MEMBER/CHIEF roles)

**Response** (201 CREATED)
```json
{
  "staff": {
    "id": 2,
    "staffCode": "STF-00002",
    "staffName": "山田花子",
    "email": "yamada@example.com",
    "phone": "090-9876-5432",
    "responsibility": "企画",
    "roleLevel": "MEMBER",
    "groupId": 2,
    "groupName": "企画部",
    "isActive": true
  },
  "initialLoginInformation": {
    "emailSent": true,
    "message": "初回ログイン情報をメールで送信しました。"
  }
}
```

MEMBERを新規登録すると初回ログイン情報を発行します。SMTP未設定、メールアドレス未登録、または送信失敗時は、`initialLoginInformation` に `accessUrl`、`loginCode`、`initialPassword` が含まれ、管理画面はこれをダイアログに表示します。メール送信成功時、これらの機密値はレスポンスに含まれません。

**Error Responses**
- `400 BAD REQUEST`: Validation error (invalid email, phone format, missing required fields)
- `409 CONFLICT`: Email already exists

### Update Staff

**Request**
```
PUT /staffs/{id}
Content-Type: application/json

{
  "phone": "090-5555-5555",
  "responsibility": "営業企画",
  "groupId": 3
}
```

**Fields** (all optional)
- `email`: Email (if updated)
- `phone`: Phone number
- `responsibility`: Job responsibility
- `roleLevel`: Role level (cannot downgrade from CHIEF to MEMBER)
- `groupId`: Group assignment

**Response** (200 OK)
```json
{
  "id": 1,
  "staffCode": "STF-00001",
  "staffName": "田中太郎",
  "email": "tanaka@example.com",
  "phone": "090-5555-5555",
  "responsibility": "営業企画",
  "roleLevel": "MEMBER",
  "groupId": 3,
  "groupName": "営業企画部",
  "isActive": true,
  "createdAt": "2026-07-26T10:00:00+09:00",
  "updatedAt": "2026-07-26T12:00:00+09:00"
}
```

### Deactivate Staff

**Request**
```
DELETE /staffs/{id}
```

**Response** (204 NO CONTENT)

**Note**: Soft delete - staff record remains but `isActive` set to false

### Reactivate Staff

**Request**
```
POST /staffs/{id}/reactivate
```

**Response** (200 OK)
```json
{
  "id": 1,
  ...
  "isActive": true,
  "updatedAt": "2026-07-26T12:30:00+09:00"
}
```

---

## Group Management API

Manage organizational groups for permission scoping.

### List All Groups

**Request**
```
GET /groups
```

**Response** (200 OK)
```json
[
  {
    "id": 1,
    "groupCode": "GRP001",
    "groupName": "営業部",
    "isActive": true,
    "createdAt": "2026-07-26T09:00:00+09:00",
    "updatedAt": "2026-07-26T09:00:00+09:00"
  },
  {
    "id": 2,
    "groupCode": "GRP002",
    "groupName": "企画部",
    "isActive": true,
    "createdAt": "2026-07-26T09:00:00+09:00",
    "updatedAt": "2026-07-26T09:00:00+09:00"
  }
]
```

### Get Group by ID

**Request**
```
GET /groups/{id}
```

**Response** (200 OK)

### Create Group

**Request**
```
POST /groups
Content-Type: application/json

{
  "groupCode": "GRP003",
  "groupName": "管理部"
}
```

**Fields**
- `groupCode` (required): Unique group code (max 50 chars)
- `groupName` (required): Group display name (max 100 chars)

**Response** (201 CREATED)

### Update Group

**Request**
```
PUT /groups/{id}
Content-Type: application/json

{
  "groupName": "管理・総務部"
}
```

**Response** (200 OK)

### Deactivate Group

**Request**
```
DELETE /groups/{id}
```

**Response** (204 NO CONTENT)

### Reactivate Group

**Request**
```
POST /groups/{id}/reactivate
```

**Response** (200 OK)

---

## Qualification Management API

Manage staff qualifications and certifications.

### List All Qualifications

**Request**
```
GET /qualifications
```

**Response** (200 OK)
```json
[
  {
    "id": 1,
    "qualificationName": "ITパスポート",
    "isActive": true,
    "createdAt": "2026-07-26T09:00:00+09:00",
    "updatedAt": "2026-07-26T09:00:00+09:00"
  }
]
```

### Get Qualification by ID

**Request**
```
GET /qualifications/{id}
```

**Response** (200 OK)

### Create Qualification

**Request**
```
POST /qualifications
Content-Type: application/json

{
  "qualificationName": "応用情報技術者"
}
```

**Response** (201 CREATED)

### Update Qualification

**Request**
```
PUT /qualifications/{id}
Content-Type: application/json

{
  "qualificationName": "応用情報技術者（新名称）"
}
```

**Response** (200 OK)

### Deactivate Qualification

**Request**
```
DELETE /qualifications/{id}
```

**Response** (204 NO CONTENT)

### Reactivate Qualification

**Request**
```
POST /qualifications/{id}/reactivate
```

**Response** (200 OK)

---

## Shift Type Management API

Manage shift templates and shift type definitions.

### List All Shift Types

**Request**
```
GET /shift-types
```

**Response** (200 OK)
```json
[
  {
    "id": 1,
    "shiftCode": "SFT001",
    "shiftName": "朝勤",
    "startTime": "09:00",
    "endTime": "17:00",
    "isOffType": false,
    "sortOrder": 1,
    "isActive": true,
    "createdAt": "2026-07-26T09:00:00+09:00",
    "updatedAt": "2026-07-26T09:00:00+09:00"
  }
]
```

### List Active Shift Types

**Request**
```
GET /shift-types/active
```

**Response** (200 OK)

### Get Shift Type by ID

**Request**
```
GET /shift-types/{id}
```

**Response** (200 OK)

### Create Shift Type

**Request**
```
POST /shift-types
Content-Type: application/json

{
  "shiftCode": "SFT004",
  "shiftName": "夜勤",
  "startTime": "21:00",
  "endTime": "06:00",
  "isOffType": false,
  "sortOrder": 3
}
```

**Fields**
- `shiftCode` (required): Unique shift code
- `shiftName` (required): Shift name
- `startTime` (required): Start time (HH:mm format)
- `endTime` (required): End time (HH:mm format)
- `isOffType` (required): Whether this is an "off" type shift
- `sortOrder` (required): Display order

**Response** (201 CREATED)

### Update Shift Type

**Request**
```
PUT /shift-types/{id}
Content-Type: application/json

{
  "endTime": "17:30",
  "sortOrder": 2
}
```

**Response** (200 OK)

### Deactivate Shift Type

**Request**
```
DELETE /shift-types/{id}
```

**Response** (204 NO CONTENT)

### Reactivate Shift Type

**Request**
```
POST /shift-types/{id}/reactivate
```

**Response** (200 OK)

---

## Shift Assignment API

Manage confirmed shift assignments.

**Authorization**: POST/PUT/DELETE require `Authorization: Bearer <JWT>`

### Get Shift Assignment by ID

**Request**
```
GET /shift-assignments/{id}
```

**Response** (200 OK)
```json
{
  "id": 1,
  "staffId": 1,
  "staffCode": "STF-00001",
  "staffName": "田中太郎",
  "shiftTypeId": 2,
  "shiftCode": "SFT002",
  "shiftName": "夜勤",
  "workDate": "2026-08-01",
  "updatedBy": "STF-00099",
  "updatedAt": "2026-07-26T10:00:00+09:00"
}
```

### Get Assignments by Staff and Date Range

**Request**
```
GET /shift-assignments/staff/{staffId}
?startDate=2026-08-01&endDate=2026-08-31
```

**Query Parameters**
- `startDate` (required): Start date (yyyy-MM-dd)
- `endDate` (required): End date (yyyy-MM-dd)

**Response** (200 OK)
```json
[
  {
    "id": 1,
    "staffId": 1,
    "staffCode": "STF-00001",
    "staffName": "田中太郎",
    "shiftTypeId": 2,
    "shiftCode": "SFT002",
    "shiftName": "夜勤",
    "workDate": "2026-08-01",
    "updatedBy": "STF-00099",
    "updatedAt": "2026-07-26T10:00:00+09:00"
  }
]
```

### Get Assignments by Group and Date Range

**Request**
```
GET /shift-assignments/group/{groupId}
?startDate=2026-08-01&endDate=2026-08-31
```

**Response** (200 OK)

### Get Assignments by Date Range

**Request**
```
GET /shift-assignments
?startDate=2026-08-01&endDate=2026-08-31
```

**Response** (200 OK)

### Create Shift Assignment

**Request**
```
POST /shift-assignments
Content-Type: application/json
Authorization: Bearer <JWT>

{
  "staffId": 1,
  "shiftTypeId": 2,
  "workDate": "2026-08-15"
}
```

**Headers**
- `Authorization` (required): Bearer token

**Fields**
- `staffId` (required): Staff ID
- `shiftTypeId` (required): Shift type ID
- `workDate` (required): Work date (yyyy-MM-dd)

**Response** (201 CREATED)

**Error Responses**
- `400 BAD REQUEST`: Validation error, UNIQUE constraint violation (staff already has shift on this date)
- `403 FORBIDDEN`: Insufficient permissions

### Update Shift Assignment

**Request**
```
PUT /shift-assignments/{id}
Content-Type: application/json
Authorization: Bearer <JWT>

{
  "shiftTypeId": 3
}
```

**Response** (200 OK)

### Delete Shift Assignment

**Request**
```
DELETE /shift-assignments/{id}
Authorization: Bearer <JWT>
```

**Response** (204 NO CONTENT)

### Delete Assignments by Staff and Date Range

**Request**
```
DELETE /shift-assignments/staff/{staffId}
?startDate=2026-08-01&endDate=2026-08-31
Authorization: Bearer <JWT>
```

**Response** (204 NO CONTENT)

### Clear Monthly Shift Assignments (Keep Requests)

**Request**
```
DELETE /shift-assignments/month
?year=2026&month=8
Authorization: Bearer <JWT>
```

**Behavior**
- Clears shift assignments for the specified month only.
- Shift requests are not modified.
- If the month is confirmed, the request fails.

**Response** (200 OK)
```json
{
  "year": 2026,
  "month": 8,
  "deletedCount": 42,
  "message": "シフト状態をクリアしました。申請データは保持されています。"
}
```

---

## Shift Request API

Manage desired shift requests with approval workflow.

**Status Workflow**: DRAFT → SUBMITTED → APPLIED | REJECTED

### Get Shift Request by ID

**Request**
```
GET /shift-requests/{id}
```

**Response** (200 OK)
```json
{
  "id": 1,
  "staffId": 1,
  "staffCode": "STF-00001",
  "staffName": "田中太郎",
  "shiftTypeId": 2,
  "shiftCode": "SFT002",
  "shiftName": "夜勤",
  "desiredDate": "2026-08-20",
  "status": "SUBMITTED",
  "submittedAt": "2026-07-26T10:00:00+09:00",
  "decidedAt": null,
  "notes": "希望理由: 実家の都合"
}
```

### Get Requests by Staff and Date Range

**Request**
```
GET /shift-requests/staff/{staffId}
?startDate=2026-08-01&endDate=2026-08-31
```

**Response** (200 OK)

### Get Requests by Staff, Status, and Date Range

**Request**
```
GET /shift-requests/staff/{staffId}/status/{status}
?startDate=2026-08-01&endDate=2026-08-31
```

**Path Parameters**
- `status`: DRAFT, SUBMITTED, APPLIED, REJECTED

**Response** (200 OK)

### Get Requests by Group and Date Range

**Request**
```
GET /shift-requests/group/{groupId}
?startDate=2026-08-01&endDate=2026-08-31
```

**Response** (200 OK)

### Get Unreflected Shift Requests

**Request**
```
GET /shift-requests/unreflected
?startDate=2026-08-01&endDate=2026-08-31
```

**Response** (200 OK)
```json
[
  {
    "id": 5,
    "staffId": 2,
    "staffCode": "STF-00002",
    "staffName": "山田花子",
    "shiftTypeId": 1,
    "shiftCode": "SFT001",
    "shiftName": "朝勤",
    "desiredDate": "2026-08-10",
    "status": "APPLIED",
    "submittedAt": "2026-07-20T14:00:00+09:00",
    "decidedAt": "2026-07-21T15:00:00+09:00",
    "notes": ""
  }
]
```

### Create Shift Request (Draft)

**Request**
```
POST /shift-requests
Content-Type: application/json
Authorization: Bearer <JWT>

{
  "shiftTypeId": 2,
  "desiredDate": "2026-08-20",
  "notes": "希望理由: 実家の都合"
}
```

**Headers**
- `Authorization` (required): Bearer token (requester is resolved from JWT)

**Fields**
- `shiftTypeId` (required): Desired shift type
- `desiredDate` (required): Desired date (yyyy-MM-dd)
- `notes` (optional): Reason or comments

**Response** (201 CREATED)
```json
{
  "id": 1,
  "status": "DRAFT",
  "submittedAt": null,
  ...
}
```

### Update Shift Request (Draft Only)

**Request**
```
PUT /shift-requests/{id}
Content-Type: application/json
Authorization: Bearer <JWT>

{
  "shiftTypeId": 3,
  "notes": "理由を変更: 医者の予定"
}
```

**Response** (200 OK)

**Restrictions**
- Only DRAFT requests can be updated
- Only the requesting staff can update

### Submit Shift Request

**Request**
```
POST /shift-requests/{id}/submit
Authorization: Bearer <JWT>
```

**Response** (200 OK)
```json
{
  "id": 1,
  "status": "SUBMITTED",
  "submittedAt": "2026-07-26T11:00:00+09:00",
  ...
}
```

**Error Responses**
- `400 BAD REQUEST`: Request is not in DRAFT status
- `403 FORBIDDEN`: Not the requesting staff

### Approve Shift Request

**Request**
```
POST /shift-requests/{id}/approve
Authorization: Bearer <JWT>
```

**Headers**
- `Authorization` (required): Bearer token (role must be MASTER or CHIEF)

**Response** (200 OK)
```json
{
  "id": 1,
  "status": "APPLIED",
  "decidedAt": "2026-07-26T12:00:00+09:00",
  ...
}
```

**Error Responses**
- `400 BAD REQUEST`: Request is not in SUBMITTED status
- `403 FORBIDDEN`: Insufficient permissions (must be MASTER or CHIEF)

### Reject Shift Request

**Request**
```
POST /shift-requests/{id}/reject
Authorization: Bearer <JWT>
```

**Response** (200 OK)
```json
{
  "id": 1,
  "status": "REJECTED",
  "decidedAt": "2026-07-26T12:00:00+09:00",
  ...
}
```

---

## Calendar View Permission API

Manage inter-staff calendar sharing permissions.

**Status Workflow**: PENDING → APPROVED | REJECTED | CANCELED | EXPIRED

### Get Permission by ID

**Request**
```
GET /calendar-view-permissions/{id}
```

**Response** (200 OK)
```json
{
  "id": 1,
  "requesterStaffId": 1,
  "requesterStaffCode": "STF-00001",
  "requesterStaffName": "田中太郎",
  "targetStaffId": 2,
  "targetStaffCode": "STF-00002",
  "targetStaffName": "山田花子",
  "status": "APPROVED",
  "expirationDate": "2026-12-26",
  "requestedAt": "2026-07-26T10:00:00+09:00",
  "approvedAt": "2026-07-26T11:00:00+09:00"
}
```

### Get Permission by Requester and Target

**Request**
```
GET /calendar-view-permissions/requester/{requesterStaffId}/target/{targetStaffId}
```

**Response** (200 OK)

### Get Approved Target Staff IDs for Requester

**Request**
```
GET /calendar-view-permissions/requester/{requesterStaffId}/approved-targets
```

**Response** (200 OK)
```json
[2, 3, 5]
```

### Get Permissions by Requester and Status

**Request**
```
GET /calendar-view-permissions/requester/{requesterStaffId}/status/{status}
```

**Path Parameters**
- `status`: PENDING, APPROVED, REJECTED, CANCELED, EXPIRED

**Response** (200 OK)

### Create Permission Request

**Request**
```
POST /calendar-view-permissions
Content-Type: application/json
Authorization: Bearer <JWT>

{
  "targetStaffId": 2,
  "expirationDate": "2026-12-26"
}
```

**Headers**
- `Authorization` (required): Requester is resolved from JWT

**Response** (201 CREATED)
```json
{
  "id": 1,
  "status": "PENDING",
  "requestedAt": "2026-07-26T10:00:00+09:00",
  ...
}
```

### Approve Permission

**Request**
```
POST /calendar-view-permissions/{id}/approve
Authorization: Bearer <JWT>
```

**Headers**
- `Authorization` (required): Approver is resolved from JWT

**Response** (200 OK)
```json
{
  "id": 1,
  "status": "APPROVED",
  "approvedAt": "2026-07-26T11:00:00+09:00",
  ...
}
```

### Reject Permission

**Request**
```
POST /calendar-view-permissions/{id}/reject
Authorization: Bearer <JWT>
```

**Response** (200 OK)
```json
{
  "id": 1,
  "status": "REJECTED",
  ...
}
```

### Cancel Permission

**Request**
```
POST /calendar-view-permissions/{id}/cancel
Authorization: Bearer <JWT>
```

**Response** (200 OK)
```json
{
  "id": 1,
  "status": "CANCELED",
  ...
}
```

---

## Authentication and Password Reset API

### Login

```
POST /login
Content-Type: application/json

{
  "staffCode": "STF-00001",
  "password": "password"
}
```

The response contains `token`, `staffId`, `staffCode`, `staffName`, and `roleLevel`.

### Request Password Reset

```
POST /password-reset-requests
Authorization: Bearer <JWT>
```

The logged-in staff member receives a one-time URL containing their staff ID and access token, plus a six-digit verification code. Both are valid for one hour. A new request invalidates previously unused reset tokens for that member.

When SMTP delivery succeeds, the response is:

```json
{ "emailSent": true, "message": "パスワード変更用のURLと確認コードをメールで送信しました。" }
```

When SMTP is unavailable or delivery fails, the response contains `accessUrl` and `verificationCode` so the member screen can display them in a dialog.

### Complete Password Reset

```
POST /password-resets/{staffId}/{token}
Content-Type: application/json

{
  "verificationCode": "123456",
  "newPassword": "new-password"
}
```

`newPassword` must contain at least eight characters. A successful reset consumes the token and invalidates every JWT issued on or before the password change, requiring a new login.

---

## System Setting API

Manage system-wide configuration settings.

**Authorization**: MASTER role required for all write operations

### List All Settings

**Request**
```
GET /system-settings
```

**Response** (200 OK)
```json
[
  {
    "settingKey": "calendarViewPermissionEnabled",
    "settingValueBoolean": true,
    "settingValueText": null,
    "updatedBy": "STF-00099",
    "updatedAt": "2026-07-26T10:00:00+09:00"
  },
  {
    "settingKey": "memberLoginNotificationEnabled",
    "settingValueBoolean": true,
    "settingValueText": null,
    "updatedBy": "STF-00099",
    "updatedAt": "2026-07-26T10:00:00+09:00"
  }
]
```

### Get Setting by Key

**Request**
```
GET /system-settings/{settingKey}
```

**Response** (200 OK)
```json
{
  "settingKey": "calendarViewPermissionEnabled",
  "settingValueBoolean": true,
  "settingValueText": null,
  "updatedBy": "STF-00099",
  "updatedAt": "2026-07-26T10:00:00+09:00"
}
```

### Update Boolean Setting

**Request**
```
PUT /system-settings/{settingKey}/boolean
?value=false
Authorization: Bearer <JWT>
```

**Query Parameters**
- `value` (required): Boolean value (true or false)

**Headers**
- `Authorization` (required): Bearer token (role must be MASTER)

**Response** (200 OK)
```json
{
  "settingKey": "calendarViewPermissionEnabled",
  "settingValueBoolean": false,
  "updatedBy": "STF-00099",
  "updatedAt": "2026-07-26T12:00:00+09:00"
}
```

### Update Text Setting

**Request**
```
PUT /system-settings/{settingKey}/text
?value=Company Name Updated
Authorization: Bearer <JWT>
```

**Query Parameters**
- `value` (required): Text value

**Response** (200 OK)
```json
{
  "settingKey": "company_name",
  "settingValueBoolean": null,
  "settingValueText": "Company Name Updated",
  "updatedBy": "STF-00099",
  "updatedAt": "2026-07-26T12:00:00+09:00"
}
```

---

## Error Handling

### Common HTTP Status Codes

| Status Code | Description | Scenario |
|------------|-------------|----------|
| 200 OK | Request succeeded | GET/PUT successful |
| 201 CREATED | Resource created | POST successful |
| 204 NO CONTENT | Request succeeded, no content | DELETE successful |
| 401 UNAUTHORIZED | Authentication required/invalid | Missing or invalid JWT |
| 400 BAD REQUEST | Validation error | Invalid input, missing required fields |
| 403 FORBIDDEN | Insufficient permissions | MASTER-only operation attempted by MEMBER |
| 404 NOT FOUND | Resource not found | Requested ID doesn't exist |
| 409 CONFLICT | Resource conflict | Duplicate email, UNIQUE constraint violation |
| 500 INTERNAL SERVER ERROR | Server error | Unhandled exception |

### Error Response Format

**400 / 401 / 403 / 404 / 409 / 500**
```json
{
  "timestamp": "2026-07-26T10:00:00+09:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid email format",
  "path": "/api/staffs"
}
```

### Validation Errors

Common validation scenarios:

- **Email**: Must be valid RFC 5322 format if provided
- **Phone**: Numbers and hyphens only (e.g., 090-1234-5678)
- **Date**: Must be yyyy-MM-dd format
- **Time**: Must be HH:mm format (24-hour)
- **Required Fields**: Cannot be null or empty

---

**Last Updated**: 2026-08-01
**API Version**: 1.0.0

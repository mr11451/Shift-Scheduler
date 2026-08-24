# API/DBモデル設計

## 位置づけ

- 分類: 設計資料（画面以外）
- 関連: `domain_model.md`、`api_reference.md`

## 目的

[domain_model.md](domain_model.md) を、Spring Boot + PostgreSQL 実装向けに具体化する。

## DBスキーマ（論理）

### 1. groups

- id: BIGSERIAL, PK
- group_code: VARCHAR(50), UNIQUE, NOT NULL
- group_name: VARCHAR(100), NOT NULL
- is_active: BOOLEAN, NOT NULL, DEFAULT TRUE
- created_at: TIMESTAMPTZ, NOT NULL
- updated_at: TIMESTAMPTZ, NOT NULL

制約:
- `group_code` 一意

### 2. qualifications

- id: BIGSERIAL, PK
- qualification_name: VARCHAR(100), UNIQUE, NOT NULL
- description: TEXT, NULL
- is_active: BOOLEAN, NOT NULL, DEFAULT TRUE
- created_at: TIMESTAMPTZ, NOT NULL
- updated_at: TIMESTAMPTZ, NOT NULL

### 3. staffs

- id: BIGSERIAL, PK（自動生成）
- staff_code: VARCHAR(50), UNIQUE, NOT NULL（自動生成、編集不可）
- staff_name: VARCHAR(100), NOT NULL
- email: VARCHAR(255), NULL
- phone: VARCHAR(20), NULL
- responsibility: VARCHAR(100), NOT NULL
- role_level: VARCHAR(20), NOT NULL
- group_id: BIGINT, FK -> groups.id, NULL
- is_active: BOOLEAN, NOT NULL, DEFAULT TRUE
- created_at: TIMESTAMPTZ, NOT NULL
- updated_at: TIMESTAMPTZ, NOT NULL

制約:
- `staff_code` は自動割り付け（例: STF-00001）、編集不可
- `role_level` は `MEMBER | CHIEF | MASTER`
- `role_level = MEMBER` の場合 `email` 必須
- `role_level in (MEMBER, CHIEF)` の場合 `group_id` 必須
- `role_level = MASTER` の場合 `group_id` は NULL 可
- `email` は形式チェック（RFC準拠の簡易バリデーション）
- `phone` は数字とハイフンのみ、未入力可

### 4. staff_qualifications

- staff_id: BIGINT, FK -> staffs.id, NOT NULL
- qualification_id: BIGINT, FK -> qualifications.id, NOT NULL
- created_at: TIMESTAMPTZ, NOT NULL

制約:
- PK(`staff_id`, `qualification_id`)

### 5. shift_types

- id: BIGSERIAL, PK
- shift_code: VARCHAR(20), UNIQUE, NOT NULL
- shift_name: VARCHAR(100), NOT NULL
- start_time: TIME, NULL
- end_time: TIME, NULL
- is_off_type: BOOLEAN, NOT NULL, DEFAULT FALSE
- is_active: BOOLEAN, NOT NULL, DEFAULT TRUE
- sort_order: INT, NOT NULL, DEFAULT 0
- created_at: TIMESTAMPTZ, NOT NULL
- updated_at: TIMESTAMPTZ, NOT NULL

制約:
- `is_off_type = FALSE` の場合 `start_time`/`end_time` 必須
- `start_time < end_time`（`is_off_type = FALSE` のとき）

### 6. shift_assignments

- id: BIGSERIAL, PK
- staff_id: BIGINT, FK -> staffs.id, NOT NULL
- work_date: DATE, NOT NULL
- shift_type_id: BIGINT, FK -> shift_types.id, NOT NULL
- note: VARCHAR(255), NULL
- updated_by_staff_id: BIGINT, FK -> staffs.id, NOT NULL
- updated_at: TIMESTAMPTZ, NOT NULL

制約:
- UNIQUE(`staff_id`, `work_date`)

### 7. shift_assignment_audits（推奨）

- id: BIGSERIAL, PK
- shift_assignment_id: BIGINT, FK -> shift_assignments.id
- action_type: VARCHAR(20), NOT NULL (`CREATE|UPDATE|DELETE`)
- before_value: JSONB, NULL
- after_value: JSONB, NULL
- action_by_staff_id: BIGINT, FK -> staffs.id, NOT NULL
- action_at: TIMESTAMPTZ, NOT NULL

### 8. shift_requests

- id: BIGSERIAL, PK
- staff_id: BIGINT, FK -> staffs.id, NOT NULL
- work_date: DATE, NOT NULL
- desired_shift_type_id: BIGINT, FK -> shift_types.id, NOT NULL
- status: VARCHAR(20), NOT NULL
- submitted_at: TIMESTAMPTZ, NULL
- decided_at: TIMESTAMPTZ, NULL
- created_at: TIMESTAMPTZ, NOT NULL
- updated_at: TIMESTAMPTZ, NOT NULL

制約:
- UNIQUE(`staff_id`, `work_date`)
- `status` は `DRAFT | SUBMITTED | APPLIED | REJECTED`

### 9. calendar_view_permissions

- id: BIGSERIAL, PK
- requester_staff_id: BIGINT, FK -> staffs.id, NOT NULL
- target_staff_id: BIGINT, FK -> staffs.id, NOT NULL
- status: VARCHAR(20), NOT NULL
- requested_at: TIMESTAMPTZ, NOT NULL
- responded_at: TIMESTAMPTZ, NULL
- expired_at: TIMESTAMPTZ, NULL
- created_at: TIMESTAMPTZ, NOT NULL
- updated_at: TIMESTAMPTZ, NOT NULL

制約:
- requester と target は同一不可
- `status` は `PENDING | APPROVED | REJECTED | CANCELED | EXPIRED`
- requester-target で有効な APPROVED は1件まで

### 10. system_settings

- setting_key: VARCHAR(100), PK
- setting_value_boolean: BOOLEAN, NULL
- setting_value_text: TEXT, NULL
- updated_by_staff_id: BIGINT, FK -> staffs.id, NOT NULL
- updated_at: TIMESTAMPTZ, NOT NULL

レコード例:
- `calendarViewPermissionEnabled` = true/false
- `memberLoginNotificationEnabled` = true/false
- `memberLoginNotificationBaseUrl` = https://example.com

### 11. member_login_provisionings

- id: BIGSERIAL, PK
- staff_id: BIGINT, FK -> staffs.id, NOT NULL
- login_code: VARCHAR(64), NOT NULL
- initial_password_hash: VARCHAR(255), NOT NULL
- access_url: TEXT, NOT NULL
- status: VARCHAR(20), NOT NULL
- issued_at: TIMESTAMPTZ, NOT NULL
- expires_at: TIMESTAMPTZ, NOT NULL
- sent_at: TIMESTAMPTZ, NULL
- last_error_message: TEXT, NULL
- created_at: TIMESTAMPTZ, NOT NULL
- updated_at: TIMESTAMPTZ, NOT NULL

制約:
- `status` は `ISSUED | SENT | FAILED | EXPIRED`

### 12. password_reset_tokens

- id: BIGSERIAL, PK
- staff_id: BIGINT, FK -> staffs.id, NOT NULL
- token_hash: VARCHAR(255), NOT NULL
- verification_code_hash: VARCHAR(255), NOT NULL
- expires_at: TIMESTAMP, NOT NULL
- used_at: TIMESTAMP, NULL

制約:
- トークンと確認コードはハッシュのみを保存する
- 有効期限は発行から1時間
- `used_at` が設定済みのトークンは再利用不可

### staffs.password_changed_at

- password_changed_at: TIMESTAMP, NULL
- パスワード変更時に設定し、この日時以前に発行されたJWTを無効化する

## 既存 `shifts` テーブルからの移行方針

- 段階1: 新テーブル作成（既存 `shifts` は残置）
- 段階2: `employee_name` から `staffs` を生成
- 段階3: `role` を `role_level` にマップ
- 段階4: `shifts` を `shift_assignments` に移送
- 段階5: アプリ切替後に `shifts` 廃止

注意:
- 既存方針「移行用スクリプト排除」に合わせ、Flyway SQL 内で完結させるか、手動移行手順書で対応する

## APIモデル（REST）

### 認証コンテキスト

全APIで `actorStaffId` を取得できる前提（JWT/Session/ヘッダ等）。
認可はUIだけでなくAPI層で強制する。

### 1. スタッフ

- `GET /api/staffs`
  - query: `staffCode`, `staffName`, `responsibility`, `groupId`, `roleLevel`, `activeOnly`
  - response: staff summary list
- `POST /api/staffs`
  - MEMBER登録時に初回ログイン情報を発行する
  - `memberLoginNotificationEnabled=true` かつSMTP送信可能な場合、登録メールへ送信する
  - 送信不可時はレスポンスの `initialLoginInformation` に初回ログイン情報を含める
- `PUT /api/staffs/{staffId}`（マスタのみ）
- `GET /api/staffs/{staffId}/calendar?yearMonth=YYYY-MM`
  - メンバ: 自分のみ
  - メンバ（オプション機能有効時）: 自分 + 許可済み相手
  - チーフ: 自分 + 同一グループのメンバのみ
  - マスタ: 全員

### 2. グループ

- `GET /api/groups`（チーフ/マスタ）
- `POST /api/groups`（マスタのみ）
- `PUT /api/groups/{groupId}`（マスタのみ）

### 3. 資格

- `GET /api/qualifications`
- `POST /api/qualifications`（マスタのみ）
- `PUT /api/qualifications/{id}`（マスタのみ）

### 4. シフト種類

- `GET /api/shift-types`
- `POST /api/shift-types`（マスタのみ）
- `PUT /api/shift-types/{id}`（マスタのみ）

### 5. 希望シフト

- `GET /api/staffs/{staffId}/shift-requests?yearMonth=YYYY-MM`
  - メンバ: 自分のみ
  - チーフ: 同一グループのメンバのみ
  - マスタ: 全員
- `PUT /api/staffs/{staffId}/shift-requests/bulk`
  - メンバ: 自分のみ更新可
  - チーフ: 原則更新不可（参照のみ）
  - マスタ: 必要時のみ更新可
- `POST /api/staffs/{staffId}/shift-requests/submit`
  - メンバ本人の希望を提出状態へ変更

### 8. メンバー間カレンダー閲覧申請/許可（オプション）

- `GET /api/calendar-view-permissions/settings`
  - 機能有効/無効を返す
- `POST /api/calendar-view-permissions/requests`
  - request: targetStaffId
  - メンバが同一グループの相手へ申請
- `POST /api/calendar-view-permissions/requests/{requestId}/approve`
- `POST /api/calendar-view-permissions/requests/{requestId}/reject`
- `POST /api/calendar-view-permissions/requests/{requestId}/cancel`
- `GET /api/calendar-view-permissions/approved`
  - 自分が閲覧可能な相手一覧

### 9. システム設定（管理者）

- `GET /api/system-settings`
- `GET /api/system-settings/{settingKey}`
- `PUT /api/system-settings/{settingKey}/boolean?value=true|false`
- `PUT /api/system-settings/{settingKey}/text?value=...`
  - 更新はマスタのみ
  - 初回ログイン通知のキーは `memberLoginNotificationEnabled` と `memberLoginNotificationBaseUrl`

### 10. パスワード変更

- `POST /api/password-reset-requests`
  - ログイン中の本人に対してワンタイムURLと確認コードを発行する
- `POST /api/password-resets/{staffId}/{token}`
  - 確認コードと8文字以上の新パスワードで変更を完了する

## DTO例

### StaffSummaryDto

- id
- staffCode
- staffName
- email
- responsibility
- group: { id, groupCode, groupName } | null
- roleLevel
- qualifications: string[]
- isActive

### ShiftCellDto

- staffId
- workDate (`YYYY-MM-DD`)
- shiftTypeCode
- shiftTypeName
- startTime (`HH:mm`) | null
- endTime (`HH:mm`) | null

### DesiredShiftCellDto

- staffId
- workDate (`YYYY-MM-DD`)
- desiredShiftTypeCode
- status (`DRAFT | SUBMITTED | APPLIED | REJECTED`)

### AutoGeneratePreviewRequest

- yearMonth
- targetScope
- requiredHeadcountByShift
- maxWorkDaysPerStaff
- maxConsecutiveWorkDays
- minHolidays
- desiredShiftPolicy (`REQUIRED | PREFERRED | IGNORE`)
- overwriteMode (`EMPTY_ONLY | OVERWRITE_ALL`)

### CalendarViewPermissionDto

- requestId
- requesterStaffId
- targetStaffId
- status (`PENDING | APPROVED | REJECTED | CANCELED | EXPIRED`)
- requestedAt
- respondedAt

### InitialLoginInformation

- emailSent
- message
- accessUrl（メール送信不可時のみ）
- loginCode（メール送信不可時のみ）
- initialPassword（メール送信不可時のみ）

### PasswordResetRequestResponse

- emailSent
- message
- accessUrl（メール送信不可時のみ）
- verificationCode（メール送信不可時のみ）

### BulkShiftUpdateRequest

- yearMonth (`YYYY-MM`)
- updates: ShiftUpdateItem[]

### ShiftUpdateItem

- staffId
- workDate
- shiftTypeCode
- note (optional)

## 認可ルール（サーバ実装）

### AccessControlService 擬似仕様

- `canViewCalendar(actor, target)`
  - actor.role = MASTER -> true
  - actor.role = CHIEF -> `target.role == MEMBER && actor.groupId == target.groupId`
  - actor.role = MEMBER ->
    - `actor.id == target.id` は常に true
    - `calendarViewPermissionEnabled == true` かつ APPROVED がある場合は他メンバも true
- `canEditShift(actor, target)`
  - actor.role = MASTER -> true
  - actor.role = CHIEF -> `target.role == MEMBER && actor.groupId == target.groupId`
  - actor.role = MEMBER -> false

- `canRequestCalendarView(actor, target)`
  - actor.role = MEMBER
  - target.role = MEMBER
  - actor.groupId = target.groupId
  - `calendarViewPermissionEnabled == true`

## Spring Boot 実装マッピング（推奨）

- package `domain`
  - `Staff`, `Group`, `ShiftAssignment`, `ShiftRequest`, `ShiftType`, `Qualification`, `CalendarViewPermission`, `SystemSetting`
  - `RoleLevel` enum
- package `application`
  - `ShiftAssignmentService`, `ShiftRequestService`, `AutoShiftGenerationService`, `CalendarViewPermissionService`, `SystemSettingService`, `AuthenticationService`, `StaffService`
- package `api`
  - `StaffApiController`, `ShiftAssignmentApiController`, `ShiftRequestApiController`, `CalendarViewPermissionApiController`, `SystemSettingApiController`, `AuthenticationApiController`
- package `infrastructure`
  - `Jdbc*Repository` または `Jpa*Repository`

## Flywayマイグレーション案

- `V001__001_initialize_schema.sql` と `V002__002_seed_dev_data.sql`

## 受け入れ条件

- チーフが異なるグループのメンバを取得/更新できない
- メンバが権限外のシフト割当APIにアクセスできない
- マスタが全件取得/更新できる
- `role_level=CHIEF|MEMBER` で `group_id` 未設定登録が失敗する
- `staff_id + work_date` 重複登録が失敗する
- 自動生成で希望シフト考慮モードが反映される
- 希望と充足要件が競合した場合、未反映希望と理由が返る
- メンバー間閲覧機能が無効時、申請APIは利用不可
- メンバー間閲覧機能が有効時、APPROVED な相手のみ閲覧可能
- メンバ登録時、SMTP送信可能かつ通知設定が有効なら初回ログイン情報メールが送信される
- SMTP送信不可時は初回ログイン情報が作成レスポンスに含まれる
- パスワード変更後は変更前のJWTが利用できない

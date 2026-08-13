# docs 目次

このディレクトリのドキュメントを、画面仕様と設計資料に分けて整理しています。

## 画面仕様

- `admin_screen.md` : 管理画面
- `admin_staff_form_screen.md` : スタッフ登録・編集画面
- `admin_qualification_screen.md` : 資格管理画面
- `admin_shift_type_screen.md` : シフト種類管理画面
- `shift_edit_screen.md` : シフト編集画面
- `staff_calendar_screen.md` : スタッフカレンダー画面
- `password_reset_screen.md` : パスワード変更画面
- `staff_edit_screen.md` : スタッフ編集画面
- `shift_request_flow.md` : シフト申請の状態フローと操作フロー

## 画面仕様とURL対応

実装ルーティング（`frontend/src/App.jsx`）を基準に、画面仕様とURLを対応付けています。

- `admin_screen.md` : `/admin`（ログイン済みかつ `CHIEF` / `MASTER` のみ）
- `admin_staff_form_screen.md` : 独立URLなし（`/admin` 内の「スタッフ管理」タブで表示）
- `admin_qualification_screen.md` : 独立URLなし（`/admin` 内の「資格管理」タブで表示）
- `admin_shift_type_screen.md` : 独立URLなし（`/admin` 内の「シフト種類管理」タブで表示）
- `shift_edit_screen.md` : `/admin/shifts`
- `staff_calendar_screen.md` : `/member`
- `password_reset_screen.md` : `/password-reset/:staffId/:token`（ワンタイムURL）
- `staff_edit_screen.md` : 独立URLなし（仕様書のみ。現行ルーティングには未割当）

## 設計資料（画面以外）

- `domain_model.md` : ドメインモデル（業務ルール、集約、権限制約）
- `api_db_model.md` : API/DBモデル設計（テーブル、制約、RESTモデル）
- `api_reference.md` : APIリファレンス（エンドポイント仕様）
- `auto_shift_generation_rules.md` : シフト自動生成ルール仕様（`autoShiftGenerationRules` の項目、判定順序、緩和ルール）

## 推奨参照順

1. `domain_model.md`
2. `api_db_model.md`
3. `api_reference.md`
4. `shift_request_flow.md`
5. 各画面仕様

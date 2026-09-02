# Shift Scheduler

勤務シフトを作成・閲覧できるクライアント/サーバー構成のアプリです。

## 構成

- Backend: Java (Spring Boot) + PostgreSQL
- Frontend: React + Vite

## 主な機能

- **ユーザー認証** - スタッフコードとパスワードによるログイン
- **同時ログイン制御** - 同じメンバーの同時ログインを拒否し、「同時ログイン不可」ポップアップを表示
- **自動ログアウト** - 30分間操作がない場合に自動ログアウト
- **ロールベースアクセス制御** - MEMBER / CHIEF / MASTER の3段階権限
- **初回ログイン情報の発行** - メンバー登録時に初期パスワードを発行し、設定済みSMTPでメール送信
- **パスワード変更** - 1時間有効・ワンタイムURLと確認コードによる変更
- シフト登録・編集
- シフト一覧表示
- メンバー間のカレンダー閲覧申請（システム設定で有効化した場合）
- PostgreSQL への永続化

## 管理者ページ

- URL: `/admin`
- 利用可能権限: `CHIEF` / `MASTER`
- 画面内タブ:
  - スタッフ管理
  - 資格管理
  - シフト種類管理
  - システム設定
- シフト編集画面: `/admin/shifts`
- 管理者ページから会員ページへ戻る場合は `/member` を使用します

## メンバー画面

- URL: `/member`
- メンバーは自分のシフト希望を登録・提出できます
- システム設定 `calendarViewPermissionEnabled` が有効な場合、同じグループのスタッフへカレンダー閲覧を申請できます
- 申請対象は、ログイン中のメンバー自身を除く同じグループの有効なスタッフです。`MEMBER`、`CHIEF`、`MASTER` のロールを問わず対象になります
- 申請中または承認済みの対象は「申請済」と表示され、再申請できません
- `CHIEF` / `MASTER` には「閲覧申請対象メンバー」は表示されません
- 機能が無効な場合は、申請対象・申請一覧・承認済み一覧を表示しません

### メンバー間カレンダー閲覧申請API

- `GET /api/staffs/permission-targets` - ログイン中のメンバーが申請できる対象一覧
- `POST /api/calendar-view-permissions` - カレンダー閲覧申請を作成
- `GET /api/calendar-view-permissions/requester/{staffId}/status/PENDING` - 自分が送信した申請中一覧
- `GET /api/calendar-view-permissions/target/{staffId}/status/PENDING` - 自分宛ての申請中一覧
- `GET /api/calendar-view-permissions/requester/{staffId}/status/APPROVED` - 自分が承認済みの閲覧対象一覧

申請APIでは、設定が有効であること、申請者が `MEMBER` であること、対象者が同じグループに所属していることをサーバー側でも検証します。

### システム設定の休業日 CSV 取込

管理者ページの「システム設定」タブでは、休業日設定の `CSV読込` ボタンから休業日一覧を取り込めます。

- 文字コードは UTF-8 を推奨
- 1列 CSV / 複数列 CSV のどちらでも可
- ヘッダー行があっても可
- CSV 内に含まれる日付文字列を抽出し、休業日入力欄へ反映
- 取り込み後は自動保存されないため、画面下部の `保存` ボタンで確定が必要

受け付ける日付形式:

- `YYYY-MM-DD`
- `YYYY/MM/DD`
- `YYYY年M月D日`

CSV 例:

```csv
date,name,notes
2026-01-01,元日,closed
2026/05/06,振替休日,closed
2026年8月13日,夏季休業,closed
```

### シフト自動生成ルール仕様

シフト自動生成の設定項目・判定順序・制約緩和の仕様は以下を参照してください。

- [docs/auto_shift_generation_rules.md](docs/auto_shift_generation_rules.md)

## 本番環境の構築

本番環境の構築、環境変数、Docker、PostgreSQL、SMTP、バックアップ、デプロイ後確認の手順は、[docs/production_setup.md](docs/production_setup.md) を参照してください。

## WSL実行ルール（必須）

このプロジェクトの開発・ビルド・テスト・起動は、すべて WSL ターミナル内で実行してください。
Windows の PowerShell / cmd での実行は規定違反とし、原則として使用しないでください。

開始前に次の確認コマンドを実行し、WSL 環境であることを必ず確認します。

```bash
uname -a
cat /proc/version
echo "$SHELL"
pwd
```

確認ポイント:

- `uname -a` または `cat /proc/version` に `microsoft` / `WSL` が含まれる
- `SHELL` が `/bin/bash` または `/bin/zsh`
- 作業ディレクトリが `/mnt/...` 配下（例: `/mnt/c/Users/.../Shift-Scheduler`）

上記のいずれかを満たさない場合は、WSL を開いてから再実行してください。

### 実行規約

- バックエンドの Maven コマンドは必ず WSL から実行する
- Docker / Compose のコマンドも必ず WSL から実行する
- VS Code のタスクやデバッグ起動も、WSL 側のコマンドを使う
- 変更確認やテスト実行も、Windows 側のシェルではなく WSL で行う

### Docker も WSL から実行する

開発・検証時の Docker 実行は、Windows の PowerShell / cmd ではなく、必ず WSL の Ubuntu ターミナルから行います。

```bash
cd /path/to/Shift-Scheduler
docker compose up -d
docker compose ps
docker compose logs -f
```

> 以後の起動・停止・ログ確認も、WSL 側の `docker compose` コマンドで行います。

## VS Code で WSL 上のバックエンドをデバッグする

VS Code から WSL 上の Spring Boot アプリをデバッグする場合は、以下の手順で実行します。

### 1. WSL でバックエンドを起動する

```bash
cd /path/to/Shift-Scheduler/backend
./mvnw spring-boot:run \
  -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" \
  -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### 2. VS Code のデバッガから接続する

- VS Code の「Terminal」→「Run Task」から `backend:run-wsl` を実行します
- デバッグ接続が必要な場合は、タスクのJVM起動引数にJDWP設定を追加してからVS CodeのJavaデバッガでポート `5005` へ接続します

### 3. タスクから起動する方法

- VS Code の「Terminal」→「Run Task」から「backend:run-wsl」を選択します
- これにより、[.vscode/tasks.json](.vscode/tasks.json) で定義した WSL 起動コマンドが実行されます

### 4. デバッグ時のポイント

- ブレークポイントは Java ソース上に設定します
- API リクエストを送信すると、デバッガが停止します
- 接続先ポートは `5005` を使用します

> WSL 外の PowerShell / cmd からではなく、必ず WSL ターミナル上で起動してください。

## 最短起動（Docker）

1. 起動

```bash
docker compose up --build
```

2. アクセス

- http://127.0.0.1:8000
- ログイン画面が表示されます
- テストパスワードを使用してログインしてください（例: staffCode=`STF-00001`, password=`test_stf-00001`）

3. 停止

```bash
docker compose down
```

補足: PostgreSQL データは `postgres_data` ボリュームに保存されます。

## ローカル起動（Dockerなし）

1. PostgreSQL を起動して環境変数を設定

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=shift_scheduler
export DB_USER=shift_user
export DB_PASSWORD=shift_password
```

### パスワード変更メール設定

会員画面からのパスワード変更では、登録済みメールアドレスへ再設定URLと確認コードを送信します。メンバー新規登録時の初回ログイン情報も同じSMTP設定を使用します。SMTP サーバーを使用する環境では、バックエンド起動前に次の環境変数を設定してください。

```bash
export SMTP_HOST=smtp.example.com
export SMTP_PORT=587
export SMTP_USERNAME=your-smtp-user
export SMTP_PASSWORD=your-smtp-password
export SMTP_FROM=no-reply@example.com
export SMTP_AUTH=true
export SMTP_STARTTLS=true
export PASSWORD_RESET_BASE_URL=https://scheduler.example.com/password-reset
```

- `SMTP_HOST`、`SMTP_USERNAME`、`SMTP_PASSWORD`、`SMTP_FROM` はメール送信に必要です。
- `SMTP_PORT` の既定値は `587`、`SMTP_AUTH` と `SMTP_STARTTLS` の既定値は `true` です。
- `PASSWORD_RESET_BASE_URL` はメールに記載する再設定画面のURLです。未設定時は `http://localhost:5173/password-reset` になります。
- Docker Compose で起動する場合も、これらの環境変数を `shift-scheduler` サービスへ渡してください。認証情報はリポジトリへ保存せず、`.env` またはデプロイ先のシークレット管理機能で設定してください。
- SMTP未設定または送信失敗時は、初回ログイン情報またはパスワード変更URL・確認コードを該当画面のダイアログに表示します。

2. React をビルド

```bash
cd frontend
npm install
npm run build
cd ..
```

3. Java サーバー起動

```bash
cd backend
./mvnw spring-boot:run
```

4. アクセス

- http://127.0.0.1:8000
- ログイン画面が表示されます
- テストパスワードを使用してログインしてください（例: staffCode=`STF-00001`, password=`test_stf-00001`）

## フロント開発モード

```bash
cd frontend
npm install
npm run dev
```

- 開発サーバー: http://127.0.0.1:5173
- `/api` は http://127.0.0.1:8000 にプロキシされます

## API

### 認証エンドポイント
- `POST /api/login` - ログイン (staffCode, password を入力)
  - レスポンス: `{ token, staffId, staffCode, staffName, roleLevel }`
- `POST /api/password-reset-requests` - ログイン中の本人がパスワード変更情報を発行
- `POST /api/password-resets/{staffId}/{token}` - 確認コードと新しいパスワードで変更を完了

### 保護されたエンドポイント（認証必須）

**全員がアクセス可能:**
- `GET /api/staffs` - スタッフ一覧
- `GET /api/shift-types` - シフト種類一覧
- `GET /api/shift-assignments` - シフト割り当て一覧（照会）

**CHIEF/MASTER のみ:**
- `POST /api/shift-assignments` - シフト割り当て作成
- `PUT /api/shift-assignments/{id}` - シフト割り当て更新
- `DELETE /api/shift-assignments/{id}` - シフト割り当て削除

**MASTER のみ:**
- `POST /api/shift-types` - シフト種類作成
- `PUT /api/shift-types/{id}` - シフト種類更新
- `DELETE /api/shift-types/{id}` - シフト種類削除

### テストユーザー

アプリケーション起動時に、以下のテストパスワードが自動生成されます：
- スタッフコード: `STF-00001` 形式
- パスワード: `test_stf-00001` 形式

例：
- staffCode: `STF-00001`, password: `test_stf-00001`
- staffCode: `STF-00002`, password: `test_stf-00002`

## DBマイグレーション

- Flyway を使用しています
- マイグレーションファイル配置先: `backend/src/main/resources/db/migration`
- 初期スキーマ: `V001__001_initialize_schema.sql`
- スキーマ追加・認証関連・初期設定: `V002__002_seed_dev_data.sql`
- マイグレーションは `V001`（基礎スキーマ）と `V002`（追加スキーマ・初期値）の2ファイルに整理しています。旧V003〜V010の履歴があるDBは、DB再作成またはFlyway履歴のリセットが必要です
- 既存DB導入時のため `spring.flyway.baseline-on-migrate=true` を設定済みです

ローカルでサーバー起動時（`mvnw spring-boot:run`）または Docker 起動時に、未適用のマイグレーションが自動適用されます。

## セキュリティ設定

- **JWT トークン** - 24時間有効期限で発行
- **パスワードハッシング** - SHA-256 + ランダムソルト
- **認証フィルター** - すべての保護APIでJWTを検証し、パスワード変更前に発行されたJWTを無効化
- **ロール認可** - メソッドレベルで @RequireRole アノテーション適用
- **本番環境では必須** - JWT秘密鍵の変更（JwtTokenUtil.java 参照）

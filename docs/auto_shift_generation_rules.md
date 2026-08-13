# シフト自動生成ルール仕様

最終更新: 2026-08-13

このドキュメントは、シフト自動生成で使用する `autoShiftGenerationRules` の仕様と、実際の判定順序をまとめたものです。
実装の基準は `backend/src/main/java/com/shiftscheduler/server/service/ShiftAssignmentService.java` です。

## 対象API

- `POST /api/shift-assignments/auto-generate?year=YYYY&month=M`
- 権限: `CHIEF` / `MASTER`
- 指定月が確定済みの場合はエラーになります

## 設定キー

- システム設定キー: `autoShiftGenerationRules`
- 保存形式: JSON（`system_settings.setting_value_text`）

### JSON例（デフォルト + グループ別）

```json
{
  "defaultRules": {
    "requiredCounts": {
      "10": 1,
      "20": 2
    },
    "monthlyMaxWorkdaysMode": "FIXED",
    "monthlyMaxWorkdays": 20,
    "maxConsecutiveWorkdays": 6,
    "minimumRestDays": 1,
    "minimumShiftGapHours": 8,
    "desiredShiftMode": "PRIORITY",
    "existingShiftHandling": "ONLY_EMPTY"
  },
  "groupRules": {
    "10": {
      "requiredCounts": {
        "10": 2
      },
      "monthlyMaxWorkdays": 18
    }
  }
}
```

補足:

- `defaultRules` が全体の基準値です。
- `groupRules.{groupId}` が存在する場合、そのグループだけ上書きされます。
- 従来形式（ルート直下にルール項目を持つJSON）も読み取り可能です。

## ルール項目

| 項目 | 型 | 説明 | 実装上の扱い |
|---|---|---|---|
| `requiredCounts` | object | シフトタイプIDごとの1日必要人数 | 必須充足対象。`<= 0` は無効扱い |
| `monthlyMaxWorkdaysMode` | string | `FIXED` / `CALCULATED` | `CALCULATED` のとき `ceil((月日数 / 7) * 5 + 1)` |
| `monthlyMaxWorkdays` | number | 固定上限勤務日数 | `mode=FIXED` のときのみ実質利用 |
| `maxConsecutiveWorkdays` | number | 連続勤務上限日数 | ハード制約（超過不可） |
| `minimumRestDays` | number | 最低休日日数 | 充足不可時に緩和される場合あり |
| `minimumShiftGapHours` | number | 最短連続シフト間隔（時間） | 充足不可時に緩和される場合あり |
| `desiredShiftMode` | string | `REQUIRED` / `PRIORITY` / `IGNORE` | `REQUIRED` のみ先行割当で必須扱い |
| `existingShiftHandling` | string | `ONLY_EMPTY` / `OVERWRITE` | `OVERWRITE` は対象月の編集可能スタッフ分を先に削除 |

## スタッフ側の関連データ

自動生成はスタッフごとの以下設定も参照します。

- `ngShiftTypeIds`: 避けるシフト（JSON推奨）
- `preferredShiftTypeIds`: 希望シフト（JSON推奨）

推奨JSON形式:

```json
{
  "shiftTypeIds": [10, 20],
  "weekdayIds": [1, 2, 3]
}
```

`weekdayIds` は `0=日, 1=月, ..., 6=土` です。

## 自動生成の処理フロー

1. 前提チェック
- 月の範囲チェック（1-12）
- 月確定チェック
- 編集可能スタッフ抽出
- スタッフをグループ単位に分割
- 有効な勤務シフト種類取得

2. 初期状態ロード（グループごと）
- 既存割当
- スタッフ別勤務日集合
- 月次勤務回数
- 日次シフト人数

3. 既存シフトの扱い（グループごと）
- `existingShiftHandling=OVERWRITE` の場合、対象月の既存割当を削除して再読込

4. 申請の先行割当（`desiredShiftMode=REQUIRED` のときのみ、グループごと）
- 申請シフトを先に埋める
- 埋められなかった件数は `unassignedRequiredCount` に加算

5. 必要人数充足（グループごと）
- 日付ごとに、必要人数が多いシフトから割当
- 候補がいない場合は不足条件に記録

6. リトライ（グループごと）
- 生成結果が不足ありの場合、最大回数まで再試行
- 最良結果（不足が少ない結果）を採用

補足:

- グループ間でスタッフは共有しません。
- 他グループの不足を別グループのスタッフで埋めることはしません。

## 制約判定（候補スタッフ選定時）

候補スタッフは次の順でチェックされます。

1. NGシフト制約
- NGシフトタイプ一致、または曜日条件一致なら割当不可（OR判定）

  具体例:
  - `shiftTypeIds=[10]` のみ設定: シフトID `10` は全曜日でNG
  - `weekdayIds=[0,6]` のみ設定: 土日なら全シフトNG
  - 両方設定: どちらか一致すればNG

2. 同日重複
- 同じスタッフ・同日セルに既存割当があれば不可

3. 連続勤務上限
- `maxConsecutiveWorkdays` を超える場合は不可

4. 最低休日日数
- `minimumRestDays` 条件違反は不可

5. 最短連続シフト間隔
- `minimumShiftGapHours` 未満なら不可

## 制約緩和の順序

必要人数を埋める過程で候補が見つからない場合、次の順で緩和します。

1. 通常判定（緩和なし）
2. `minimumRestDays` を無視
3. `minimumRestDays` と `minimumShiftGapHours` を無視

以下は緩和しません。

- NGシフト制約
- 連続勤務上限
- 同日重複

## 希望シフトの扱い

- `desiredShiftMode=REQUIRED`
  - 申請シフトを先に必須割当として試行
- `desiredShiftMode=PRIORITY` / `IGNORE`
  - 申請は候補重み付け上の参考情報

候補重みは概ね以下で加点されます。

- 申請一致: +10
- 希望シフト一致: +5
- 月間残キャパが大きいほど加点
- 最近の勤務回数が少ないほど加点
- 連続勤務が短いほど加点
- 最終勤務日から離れているほど加点

## 重要な注意点（現行実装）

1. 月間上限勤務日数はハード上限ではありません
- 現在は候補重み計算に使う「配分バランス用の指標」です
- 必要人数充足を優先するため、上限を超える割当が発生し得ます

2. 休暇申請（希望シフトなし）がある日の扱い
- その日の必要人数充足ループをスキップする実装になっています

3. `minimumShiftGapHours` は前日以前の割当との比較です
- 当日中の複数割当はそもそも同日重複禁止のため対象外です

## 結果レスポンスの見方

主な項目:

- `generatedCount`: 新規に生成された割当件数
- `unassignedRequiredCount`: 申請必須未充足 + 必要人数不足の合算
- `retryCount`: 再試行回数
- `unmetConditions`: 日付・シフト別の不足詳細

不足理由は `勤務可能な候補スタッフが見つかりませんでした。` として返されます。

## 運用の推奨

- まず `requiredCounts` を現実的な人数に設定する
- `maxConsecutiveWorkdays` は安全側に設定する
- `minimumRestDays` と `minimumShiftGapHours` は段階的に厳しくする
- 必要人数不足が頻発する場合は、先に `requiredCounts` または NG条件の見直しを行う

import React, { useEffect, useRef, useState } from "react";
import { parseHolidayDatesFromCsv, parseHolidayWeekdays } from "../../utils/holidayDates";
import { DEFAULT_ROLE_LABELS, parseRoleLabels } from "../../utils/roleLabels";

const WEEKDAY_OPTIONS = [
  { value: 0, label: "日" },
  { value: 1, label: "月" },
  { value: 2, label: "火" },
  { value: 3, label: "水" },
  { value: 4, label: "木" },
  { value: 5, label: "金" },
  { value: 6, label: "土" },
];

const SETTING_KEYS = {
  calendarViewPermissionEnabled: "calendarViewPermissionEnabled",
  memberLoginNotificationEnabled: "memberLoginNotificationEnabled",
  memberLoginNotificationBaseUrl: "memberLoginNotificationBaseUrl",
  holidayDates: "holidayDates",
  holidayWeekdays: "holidayWeekdays",
  roleLabels: "roleLabels",
};

export default function AdminSystemSettingTab({ onCancel }) {
  const holidayCsvInputRef = useRef(null);
  const [settings, setSettings] = useState({
    calendarViewPermissionEnabled: false,
    memberLoginNotificationEnabled: false,
    memberLoginNotificationBaseUrl: "https://example.com",
    holidayDates: "",
    holidayWeekdays: [],
    roleLabels: { ...DEFAULT_ROLE_LABELS },
  });
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadSettings();
  }, []);

  async function authFetchNoRedirect(url, options = {}) {
    const token = localStorage.getItem("authToken");
    const headers = {
      ...options.headers,
    };

    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }

    return fetch(url, {
      ...options,
      headers,
    });
  }

  async function loadSettings() {
    try {
      const res = await authFetchNoRedirect("/api/system-settings");
      if (!res.ok) {
        if (res.status === 401) {
          throw new Error("認証エラーです。再ログインしてから再試行してください。");
        }
        throw new Error("設定の取得に失敗しました。");
      }
      const data = await res.json();
      const settingMap = new Map((Array.isArray(data) ? data : []).map((item) => [item.settingKey, item]));
      setSettings({
        calendarViewPermissionEnabled:
          settingMap.get(SETTING_KEYS.calendarViewPermissionEnabled)?.settingValueBoolean ?? false,
        memberLoginNotificationEnabled:
          settingMap.get(SETTING_KEYS.memberLoginNotificationEnabled)?.settingValueBoolean ?? false,
        memberLoginNotificationBaseUrl:
          settingMap.get(SETTING_KEYS.memberLoginNotificationBaseUrl)?.settingValueText || "https://example.com",
        holidayDates:
          settingMap.get(SETTING_KEYS.holidayDates)?.settingValueText || "",
        holidayWeekdays:
          parseHolidayWeekdays(settingMap.get(SETTING_KEYS.holidayWeekdays)?.settingValueText || ""),
        roleLabels:
          parseRoleLabels(settingMap.get(SETTING_KEYS.roleLabels)?.settingValueText || ""),
      });
    } catch (e) {
      setMessage(e.message);
      setMessageType("error");
    }
  }

  function handleChange(e) {
    const { name, type, checked, value } = e.target;
    setSettings((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  }

  function handleHolidayWeekdayToggle(weekday) {
    setSettings((prev) => {
      const isSelected = prev.holidayWeekdays.includes(weekday);
      const nextWeekdays = isSelected
        ? prev.holidayWeekdays.filter((value) => value !== weekday)
        : [...prev.holidayWeekdays, weekday].sort((left, right) => left - right);

      return {
        ...prev,
        holidayWeekdays: nextWeekdays,
      };
    });
  }

  function handleRoleLabelChange(roleKey, value) {
    setSettings((prev) => ({
      ...prev,
      roleLabels: {
        ...prev.roleLabels,
        [roleKey]: value,
      },
    }));
  }

  async function handleHolidayCsvImport(e) {
    const file = e.target.files?.[0];

    if (!file) {
      return;
    }

    try {
      const rawValue = await file.text();
      const parsedDates = parseHolidayDatesFromCsv(rawValue);

      if (parsedDates.length === 0) {
        throw new Error("CSVファイルから有効な休業日を読み取れませんでした。YYYY-MM-DD 形式の日付を含めてください。");
      }

      setSettings((prev) => ({
        ...prev,
        holidayDates: parsedDates.join(","),
      }));
      setMessage(`${parsedDates.length}件の休業日をCSVから読み込みました。保存すると反映されます。`);
      setMessageType("success");
    } catch (error) {
      setMessage(error.message || "CSVファイルの読み込みに失敗しました。");
      setMessageType("error");
    } finally {
      e.target.value = "";
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    setMessage("");
    setMessageType("");

    try {
      const requests = [
        authFetchNoRedirect(
          `/api/system-settings/${SETTING_KEYS.calendarViewPermissionEnabled}/boolean?value=${settings.calendarViewPermissionEnabled}`,
          {
            method: "PUT",
          }
        ),
        authFetchNoRedirect(
          `/api/system-settings/${SETTING_KEYS.memberLoginNotificationEnabled}/boolean?value=${settings.memberLoginNotificationEnabled}`,
          {
            method: "PUT",
          }
        ),
        authFetchNoRedirect(
          `/api/system-settings/${SETTING_KEYS.memberLoginNotificationBaseUrl}/text?value=${encodeURIComponent(settings.memberLoginNotificationBaseUrl)}`,
          {
            method: "PUT",
          }
        ),
        authFetchNoRedirect(
          `/api/system-settings/${SETTING_KEYS.holidayDates}/text?value=${encodeURIComponent(settings.holidayDates)}`,
          {
            method: "PUT",
          }
        ),
        authFetchNoRedirect(
          `/api/system-settings/${SETTING_KEYS.holidayWeekdays}/text?value=${encodeURIComponent(settings.holidayWeekdays.join(","))}`,
          {
            method: "PUT",
          }
        ),
        authFetchNoRedirect(
          `/api/system-settings/${SETTING_KEYS.roleLabels}/text?value=${encodeURIComponent(JSON.stringify(settings.roleLabels))}`,
          {
            method: "PUT",
          }
        ),
      ];

      const responses = await Promise.all(requests);
      const failed = responses.find((res) => !res.ok);
      if (failed) {
        if (failed.status === 401) {
          throw new Error("認証エラーです。再ログインしてから再試行してください。");
        }
        const detail = await failed.text();
        throw new Error(detail || "設定の保存に失敗しました。");
      }

      setMessage("設定を保存しました。");
      setMessageType("success");
      await loadSettings();
      if (onCancel) {
        onCancel();
      }
    } catch (e) {
      setMessage(e.message);
      setMessageType("error");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="card">
      <h2>システム設定</h2>
      {message && <div className={messageType} style={{ marginBottom: "1rem", padding: "0.75rem", borderRadius: "4px" }}>{message}</div>}

      <form onSubmit={handleSubmit} style={{ display: "grid", gap: "2rem", marginTop: "1.5rem" }}>
        <div style={{ borderBottom: "1px solid var(--line)", paddingBottom: "1.5rem" }}>
          <h3 style={{ marginTop: 0, marginBottom: "1rem" }}>メンバー間カレンダー閲覧機能</h3>
          <label style={{ display: "flex", alignItems: "center", gap: "0.75rem", cursor: "pointer" }}>
            <input
              type="checkbox"
              name="calendarViewPermissionEnabled"
              checked={settings.calendarViewPermissionEnabled}
              onChange={handleChange}
              style={{ width: "20px", height: "20px", cursor: "pointer" }}
            />
            <div>
              <div style={{ fontWeight: 600 }}>機能の有効/無効</div>
              <div style={{ fontSize: "0.9rem", color: "#666", marginTop: "0.25rem" }}>
                有効時：メンバーが同一グループ内の他メンバーへ閲覧申請を送信できます
              </div>
            </div>
          </label>
          <div style={{ backgroundColor: "#f9f9f9", padding: "1rem", borderRadius: "4px", marginTop: "1rem" }}>
            <p style={{ margin: 0, fontSize: "0.85rem", color: "#666" }}>
              <strong>有効時の挙動：</strong> メンバーが同一グループのメンバーに対してカレンダー閲覧を申請でき、申請を受けたメンバーが許可/拒否を選択できます。
            </p>
            <p style={{ margin: "0.5rem 0 0 0", fontSize: "0.85rem", color: "#666" }}>
              <strong>無効時の挙動：</strong> 申請・許可・遷移UIを非表示にします。
            </p>
          </div>
        </div>

        <div style={{ borderBottom: "1px solid var(--line)", paddingBottom: "1.5rem" }}>
          <h3 style={{ marginTop: 0, marginBottom: "1rem" }}>メンバー初回ログイン通知</h3>
          <label style={{ display: "flex", alignItems: "center", gap: "0.75rem", cursor: "pointer" }}>
            <input
              type="checkbox"
              name="memberLoginNotificationEnabled"
              checked={settings.memberLoginNotificationEnabled}
              onChange={handleChange}
              style={{ width: "20px", height: "20px", cursor: "pointer" }}
            />
            <div>
              <div style={{ fontWeight: 600 }}>機能の有効/無効</div>
              <div style={{ fontSize: "0.9rem", color: "#666", marginTop: "0.25rem" }}>
                有効時：メンバー新規登録時に初回ログイン情報をメールで自動送信します
              </div>
            </div>
          </label>
          <div style={{ marginTop: "1rem" }}>
            <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: 600 }}>初回ログインアクセスURL（ベースURL）</label>
            <input
              type="url"
              name="memberLoginNotificationBaseUrl"
              value={settings.memberLoginNotificationBaseUrl}
              onChange={handleChange}
              placeholder="https://example.com"
              disabled={!settings.memberLoginNotificationEnabled}
              style={{
                width: "100%",
                padding: "0.75rem",
                border: "1px solid var(--line)",
                borderRadius: "4px",
                opacity: settings.memberLoginNotificationEnabled ? 1 : 0.6,
                cursor: settings.memberLoginNotificationEnabled ? "text" : "not-allowed",
              }}
            />
            <div style={{ fontSize: "0.85rem", color: "#666", marginTop: "0.5rem" }}>
              例: https://shift-scheduler.example.com
            </div>
          </div>
          <div style={{ backgroundColor: "#f9f9f9", padding: "1rem", borderRadius: "4px", marginTop: "1rem" }}>
            <p style={{ margin: 0, fontSize: "0.85rem", color: "#666" }}>
              <strong>有効時の挙動：</strong> メンバー新規登録時に登録メールアドレスへアクセスURL・ログインコード・初期パスワードを送信します。
            </p>
            <p style={{ margin: "0.5rem 0 0 0", fontSize: "0.85rem", color: "#666" }}>
              <strong>無効時の挙動：</strong> 新規登録時の自動送信を行いません。必要時は管理者が手動で再送できます。
            </p>
          </div>
        </div>

        <div style={{ borderBottom: "1px solid var(--line)", paddingBottom: "1.5rem" }}>
          <h3 style={{ marginTop: 0, marginBottom: "1rem" }}>休業日設定</h3>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "1rem", marginBottom: "0.5rem" }}>
            <label style={{ display: "block", marginBottom: 0, fontWeight: 600 }}>休業日（YYYY-MM-DD 形式、カンマ区切り）</label>
            <>
              <input
                ref={holidayCsvInputRef}
                type="file"
                accept=".csv,text/csv"
                onChange={handleHolidayCsvImport}
                style={{ display: "none" }}
              />
              <button
                type="button"
                onClick={() => holidayCsvInputRef.current?.click()}
                style={{
                  padding: "0.55rem 0.9rem",
                  backgroundColor: "#0f766e",
                  color: "#fff",
                  border: "none",
                  borderRadius: "4px",
                  cursor: "pointer",
                  fontWeight: 600,
                  whiteSpace: "nowrap",
                }}
              >
                CSV読込
              </button>
            </>
          </div>
          <textarea
            name="holidayDates"
            value={settings.holidayDates}
            onChange={handleChange}
            rows={4}
            placeholder="2026-01-01,2026-05-06"
            style={{
              width: "100%",
              padding: "0.75rem",
              border: "1px solid var(--line)",
              borderRadius: "4px",
              resize: "vertical",
            }}
          />
          <div style={{ fontSize: "0.85rem", color: "#666", marginTop: "0.5rem" }}>
            例: 2026-01-01,2026-05-06
          </div>
          <div style={{ fontSize: "0.85rem", color: "#666", marginTop: "0.35rem" }}>
            CSV は 1 列でも複数列でも構いません。含まれる日付を抽出して入力欄へ反映します。
          </div>
          <div style={{ marginTop: "1rem" }}>
            <div style={{ marginBottom: "0.5rem", fontWeight: 600 }}>曜日ごとの休業日</div>
            <div style={{ display: "flex", gap: "0.75rem", flexWrap: "nowrap", overflowX: "auto" }}>
              {WEEKDAY_OPTIONS.map((weekday) => (
                <label
                  key={weekday.value}
                  style={{
                    display: "inline-flex",
                    alignItems: "center",
                    gap: "0.35rem",
                    padding: "0.45rem 0.65rem",
                    border: "1px solid var(--line)",
                    borderRadius: "999px",
                    backgroundColor: settings.holidayWeekdays.includes(weekday.value) ? "#ecfeff" : "#fff",
                    whiteSpace: "nowrap",
                    cursor: "pointer",
                  }}
                >
                  <input
                    type="checkbox"
                    checked={settings.holidayWeekdays.includes(weekday.value)}
                    onChange={() => handleHolidayWeekdayToggle(weekday.value)}
                  />
                  <span>{weekday.label}</span>
                </label>
              ))}
            </div>
            <div style={{ fontSize: "0.85rem", color: "#666", marginTop: "0.35rem" }}>
              チェックした曜日は毎週の休業日として扱われます。保存時に DB へ登録されます。
            </div>
          </div>
        </div>

        <div style={{ borderBottom: "1px solid var(--line)", paddingBottom: "1.5rem" }}>
          <h3 style={{ marginTop: 0, marginBottom: "1rem" }}>ロール表示ラベル</h3>
          <div style={{ display: "grid", gap: "0.75rem" }}>
            <label style={{ display: "grid", gap: "0.35rem" }}>
              <span style={{ fontWeight: 600 }}>MASTER ラベル</span>
              <input
                type="text"
                value={settings.roleLabels.MASTER}
                onChange={(e) => handleRoleLabelChange("MASTER", e.target.value)}
                placeholder={DEFAULT_ROLE_LABELS.MASTER}
                style={{ width: "100%", padding: "0.65rem", border: "1px solid var(--line)", borderRadius: "4px" }}
              />
            </label>
            <label style={{ display: "grid", gap: "0.35rem" }}>
              <span style={{ fontWeight: 600 }}>CHIEF ラベル</span>
              <input
                type="text"
                value={settings.roleLabels.CHIEF}
                onChange={(e) => handleRoleLabelChange("CHIEF", e.target.value)}
                placeholder={DEFAULT_ROLE_LABELS.CHIEF}
                style={{ width: "100%", padding: "0.65rem", border: "1px solid var(--line)", borderRadius: "4px" }}
              />
            </label>
            <label style={{ display: "grid", gap: "0.35rem" }}>
              <span style={{ fontWeight: 600 }}>MEMBER ラベル</span>
              <input
                type="text"
                value={settings.roleLabels.MEMBER}
                onChange={(e) => handleRoleLabelChange("MEMBER", e.target.value)}
                placeholder={DEFAULT_ROLE_LABELS.MEMBER}
                style={{ width: "100%", padding: "0.65rem", border: "1px solid var(--line)", borderRadius: "4px" }}
              />
            </label>
          </div>
          <div style={{ fontSize: "0.85rem", color: "#666", marginTop: "0.5rem" }}>
            保存すると、ロール表示テキストに反映されます。
          </div>
        </div>

        <div style={{ display: "flex", gap: "1rem" }}>
          <button
            type="submit"
            disabled={saving}
            style={{
              flex: 1,
              padding: "0.75rem",
              backgroundColor: saving ? "#ccc" : "var(--accent)",
              color: "#fff",
              border: "none",
              borderRadius: "4px",
              cursor: saving ? "not-allowed" : "pointer",
              fontWeight: 600,
            }}
          >
            {saving ? "保存中..." : "保存"}
          </button>
          <button
            type="button"
            onClick={() => {
              if (onCancel) {
                onCancel();
                return;
              }
              loadSettings();
            }}
            style={{
              flex: 1,
              padding: "0.75rem",
              backgroundColor: "#999",
              color: "#fff",
              border: "none",
              borderRadius: "4px",
              cursor: "pointer",
              fontWeight: 600,
            }}
          >
            キャンセル
          </button>
        </div>
      </form>
    </div>
  );
}

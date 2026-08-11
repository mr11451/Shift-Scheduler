import React, { useState, useEffect } from "react";

const SETTING_KEYS = {
  calendarViewPermissionEnabled: "calendarViewPermissionEnabled",
  memberLoginNotificationEnabled: "memberLoginNotificationEnabled",
  memberLoginNotificationBaseUrl: "memberLoginNotificationBaseUrl",
  holidayDates: "holidayDates",
};

export default function AdminSystemSettingTab({ onCancel }) {
  const [settings, setSettings] = useState({
    calendarViewPermissionEnabled: false,
    memberLoginNotificationEnabled: false,
    memberLoginNotificationBaseUrl: "https://example.com",
    holidayDates: "",
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
                有効時：メンバが同一グループ内の他メンバへ閲覧申請を送信できます
              </div>
            </div>
          </label>
          <div style={{ backgroundColor: "#f9f9f9", padding: "1rem", borderRadius: "4px", marginTop: "1rem" }}>
            <p style={{ margin: 0, fontSize: "0.85rem", color: "#666" }}>
              <strong>有効時の挙動：</strong> メンバが同一グループのメンバに対してカレンダー閲覧を申請でき、申請を受けたメンバが許可/拒否を選択できます。
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
                有効時：メンバ新規登録時に初回ログイン情報をメールで自動送信します
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
              <strong>有効時の挙動：</strong> メンバ新規登録時に登録メールアドレスへアクセスURL・ログインコード・初期パスワードを送信します。
            </p>
            <p style={{ margin: "0.5rem 0 0 0", fontSize: "0.85rem", color: "#666" }}>
              <strong>無効時の挙動：</strong> 新規登録時の自動送信を行いません。必要時は管理者が手動で再送できます。
            </p>
          </div>
        </div>

        <div style={{ borderBottom: "1px solid var(--line)", paddingBottom: "1.5rem" }}>
          <h3 style={{ marginTop: 0, marginBottom: "1rem" }}>休業日設定</h3>
          <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: 600 }}>休業日（YYYY-MM-DD 形式、カンマ区切り）</label>
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

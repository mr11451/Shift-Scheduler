import React, { useEffect, useState } from "react";

const RULE_SETTING_KEY = "autoShiftGenerationRules";
const DEFAULT_SCOPE = "__default__";

const DEFAULT_RULES = {
  requiredCounts: {},
  monthlyMaxWorkdaysMode: "FIXED",
  monthlyMaxWorkdays: 20,
  maxConsecutiveWorkdays: 6,
  minimumRestDays: 1,
  minimumShiftGapHours: 8,
  desiredShiftMode: "PRIORITY",
  existingShiftHandling: "ONLY_EMPTY",
};

export default function AdminAutoShiftRuleTab({ onCancel, restrictToOwnGroup = false, ownGroupId = null }) {
  const [shiftTypes, setShiftTypes] = useState([]);
  const [groups, setGroups] = useState([]);
  const [selectedScope, setSelectedScope] = useState(DEFAULT_SCOPE);
  const [defaultRules, setDefaultRules] = useState(DEFAULT_RULES);
  const [groupRules, setGroupRules] = useState({});
  const [rules, setRules] = useState(DEFAULT_RULES);
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("");
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadData();
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

  async function loadData() {
    try {
      setLoading(true);
      const [shiftTypeRes, groupRes, settingRes] = await Promise.all([
        authFetchNoRedirect("/api/shift-types?active=true"),
        authFetchNoRedirect("/api/groups"),
        authFetchNoRedirect("/api/system-settings"),
      ]);

      if (shiftTypeRes.ok) {
        const types = await shiftTypeRes.json();
        setShiftTypes(Array.isArray(types) ? types : []);
      }

      let loadedGroups = [];
      if (groupRes.ok) {
        const data = await groupRes.json();
        loadedGroups = Array.isArray(data) ? data.filter((group) => group?.isActive !== false) : [];
      }
      setGroups(loadedGroups);

      let loadedDefaultRules = { ...DEFAULT_RULES };
      let loadedGroupRules = {};
      if (settingRes.ok) {
        const data = await settingRes.json();
        const setting = (Array.isArray(data) ? data : []).find((item) => item.settingKey === RULE_SETTING_KEY);
        if (setting?.settingValueText) {
          try {
            const parsed = JSON.parse(setting.settingValueText);
            const hasGroupedFormat = parsed && typeof parsed === "object" && (parsed.defaultRules || parsed.groupRules);

            if (hasGroupedFormat) {
              loadedDefaultRules = normalizeRules(parsed.defaultRules || {});
              const nextGroupRules = {};
              if (parsed.groupRules && typeof parsed.groupRules === "object") {
                Object.entries(parsed.groupRules).forEach(([groupId, groupRule]) => {
                  nextGroupRules[groupId] = normalizeRules(groupRule || {});
                });
              }
              loadedGroupRules = nextGroupRules;
            } else {
              loadedDefaultRules = normalizeRules(parsed || {});
            }
          } catch {
            // Keep defaults if the saved value is invalid.
          }
        }
      }

      setDefaultRules(loadedDefaultRules);
      setGroupRules(loadedGroupRules);

      const nextSelectedScope = restrictToOwnGroup
        ? (ownGroupId != null ? String(ownGroupId) : DEFAULT_SCOPE)
        : (selectedScope !== DEFAULT_SCOPE
          && loadedGroups.some((group) => String(group.id) === String(selectedScope))
            ? selectedScope
            : DEFAULT_SCOPE);
      setSelectedScope(nextSelectedScope);
      setRules(nextSelectedScope === DEFAULT_SCOPE ? loadedDefaultRules : (loadedGroupRules[String(nextSelectedScope)] || loadedDefaultRules));
      setMessage("");
      setMessageType("");
    } catch (e) {
      setMessage(e.message || "データの読み込みに失敗しました。" );
      setMessageType("error");
    } finally {
      setLoading(false);
    }
  }

  function normalizeRules(source) {
    return {
      ...DEFAULT_RULES,
      ...(source || {}),
      requiredCounts: {
        ...DEFAULT_RULES.requiredCounts,
        ...((source && source.requiredCounts) || {}),
      },
    };
  }

  function updateActiveRules(nextRules) {
    const normalized = normalizeRules(nextRules);
    setRules(normalized);
    if (selectedScope === DEFAULT_SCOPE) {
      setDefaultRules(normalized);
      return;
    }
    setGroupRules((prev) => ({
      ...prev,
      [String(selectedScope)]: normalized,
    }));
  }

  function buildPayloadRules() {
    const nextDefaultRules = selectedScope === DEFAULT_SCOPE ? rules : defaultRules;
    const nextGroupRules = { ...groupRules };

    if (selectedScope !== DEFAULT_SCOPE) {
      nextGroupRules[String(selectedScope)] = rules;
    }

    return {
      defaultRules: nextDefaultRules,
      groupRules: nextGroupRules,
    };
  }

  function handleScopeChange(e) {
    if (restrictToOwnGroup) {
      return;
    }

    const nextScope = e.target.value;
    setSelectedScope(nextScope);
    if (nextScope === DEFAULT_SCOPE) {
      setRules(defaultRules);
      return;
    }

    setRules(groupRules[nextScope] || defaultRules);
  }

  function handleChange(e) {
    const { name, value } = e.target;
    updateActiveRules({
      ...rules,
      [name]: value,
    });
  }

  function handleRequiredCountChange(shiftTypeId, value) {
    updateActiveRules({
      ...rules,
      requiredCounts: {
        ...rules.requiredCounts,
        [shiftTypeId]: value,
      },
    });
  }

  function validateRules(nextRules) {
    const warnings = [];
    const monthlyMaxMode = nextRules.monthlyMaxWorkdaysMode || "FIXED";
    const monthlyMax = Number(nextRules.monthlyMaxWorkdays || 0);
    const consecutiveMax = Number(nextRules.maxConsecutiveWorkdays || 0);
    const minimumRest = Number(nextRules.minimumRestDays || 0);
    const minimumGapHours = Number(nextRules.minimumShiftGapHours || 0);

    if (monthlyMaxMode !== "CALCULATED" && monthlyMax > 0 && minimumRest > monthlyMax) {
      warnings.push("最低休日日数が月間上限勤務日数を超えています。生成条件が矛盾する可能性があります。" );
    }

    if (consecutiveMax > 0 && minimumRest > consecutiveMax) {
      warnings.push("最低休日日数が連続勤務上限を超えています。生成条件が矛盾する可能性があります。" );
    }

    if (monthlyMax > 0 && consecutiveMax > 0 && consecutiveMax > monthlyMax) {
      warnings.push("連続勤務日数の上限が月間上限勤務日数を超えています。" );
    }

    if (minimumGapHours < 0) {
      warnings.push("最短連続シフト間隔は0以上で入力してください。" );
    }

    return warnings;
  }

  async function saveRules(allowWarnings = false) {
    setSaving(true);
    setMessage("");
    setMessageType("");

    const payloadRules = buildPayloadRules();
    const activeRules = selectedScope === DEFAULT_SCOPE
      ? payloadRules.defaultRules
      : (payloadRules.groupRules[String(selectedScope)] || payloadRules.defaultRules);

    const warnings = validateRules(activeRules);
    if (warnings.length > 0 && !allowWarnings) {
      setMessage(warnings.join("\n"));
      setMessageType("error");
      setSaving(false);
      return;
    }

    try {
      const res = await authFetchNoRedirect(
        `/api/system-settings/${RULE_SETTING_KEY}/text?value=${encodeURIComponent(JSON.stringify(payloadRules))}`,
        { method: "PUT" }
      );

      if (!res.ok) {
        const detail = await res.text();
        throw new Error(detail || "自動生成ルールの保存に失敗しました。" );
      }

      setMessage(allowWarnings ? "警告を承認して自動生成ルールを保存しました。" : "自動生成ルールを保存しました。" );
      setMessageType("success");
      await loadData();
    } catch (e) {
      setMessage(e.message || "自動生成ルールの保存に失敗しました。" );
      setMessageType("error");
    } finally {
      setSaving(false);
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();
    await saveRules();
  }

  async function handleConfirmWithWarnings(e) {
    e.preventDefault();
    await saveRules(true);
  }

  return (
    <div className="card">
      <h2>自動生成ルール</h2>
      <p style={{ color: "#6b7280", marginTop: "-0.25rem" }}>
        自動シフト生成に使う基本ルールを管理します。デフォルトルールに加えて、グループ別ルールも設定できます。
      </p>

      {restrictToOwnGroup && ownGroupId == null && (
        <div className="error" style={{ marginBottom: "1rem", padding: "0.75rem", borderRadius: "4px" }}>
          所属グループが設定されていないため、自動生成ルールを設定できません。
        </div>
      )}

      {message && (
        <div className={messageType} style={{ marginBottom: "1rem", padding: "0.75rem", borderRadius: "4px", whiteSpace: "pre-line" }}>
          {message}
        </div>
      )}

      {validateRules(rules).length > 0 && (
        <div style={{ marginBottom: "1rem", padding: "0.75rem", border: "1px solid #f59e0b", borderRadius: "4px", background: "#fffbeb" }}>
          <div style={{ fontWeight: 600, marginBottom: "0.5rem" }}>警告があります。内容を確認したうえで、下の確定ボタンで保存できます。</div>
          <button
            type="button"
            onClick={handleConfirmWithWarnings}
            disabled={saving}
            style={{ padding: "0.6rem 0.9rem", backgroundColor: saving ? "#ccc" : "#d97706", color: "#fff", border: "none", borderRadius: "4px", cursor: saving ? "not-allowed" : "pointer", fontWeight: 600 }}
          >
            {saving ? "処理中..." : "警告を承認して確定"}
          </button>
        </div>
      )}

      {loading ? (
        <p>読み込み中...</p>
      ) : (
        <form onSubmit={handleSubmit} style={{ display: "grid", gap: "1.5rem" }}>
          <div style={{ borderBottom: "1px solid var(--line)", paddingBottom: "1.25rem" }}>
            <h3 style={{ marginTop: 0, marginBottom: "0.75rem" }}>設定対象</h3>
            <label style={{ display: "grid", gap: "0.25rem", maxWidth: "320px" }}>
              <span style={{ fontWeight: 600 }}>ルール適用先</span>
              <select
                value={selectedScope}
                onChange={handleScopeChange}
                disabled={restrictToOwnGroup}
                style={{ width: "fit-content", minWidth: 0, display: "inline-block", padding: "0.6rem", border: "1px solid var(--line)", borderRadius: "4px" }}
              >
                {!restrictToOwnGroup && <option value={DEFAULT_SCOPE}>デフォルト（全体）</option>}
                {groups
                  .filter((group) => !restrictToOwnGroup || String(group.id) === String(ownGroupId))
                  .map((group) => (
                    <option key={group.id} value={String(group.id)}>
                      グループ: {group.groupName}
                    </option>
                  ))}
              </select>
              <span style={{ fontSize: "0.85rem", color: "#6b7280" }}>
                {restrictToOwnGroup
                  ? "チーフ権限では自分のグループのルールのみ設定できます。"
                  : "グループ別ルールが未設定の場合は、デフォルトルールが適用されます。"}
              </span>
            </label>
          </div>

          <div style={{ background: "#f9fafb", border: "1px solid var(--line)", borderRadius: "8px", padding: "1rem" }}>
            <h3 style={{ marginTop: 0, marginBottom: "0.5rem" }}>ルールの意味</h3>
            <ul style={{ margin: 0, paddingLeft: "1.2rem", lineHeight: 1.6 }}>
              <li><strong>1日あたり必要人数</strong>: 各シフト種類ごとに、その日に必要な人数を指定します。</li>
              <li><strong>月間上限勤務日数</strong>: 1人が1か月に働ける上限日数です。</li>
              <li><strong>連続勤務日数の上限</strong>: 連続して勤務できる最大日数です。</li>
              <li><strong>最低休日日数</strong>: 連続勤務の前後に最低限確保する休みの日数です。</li>
              <li><strong>最短連続シフト間隔</strong>: 連続して働くシフトの間に最低限空ける時間です。夜勤の後に早番を入れないなどの制約に使えます。</li>
              <li><strong>希望シフトの考慮</strong>: 希望シフトを「必須」「優先」「無視」のどれで扱うかを指定します。</li>
              <li><strong>既存シフトの扱い</strong>: 既に入力済みのセルを残すか、上書きして再生成するかを指定します。</li>
            </ul>
          </div>
          <div style={{ borderBottom: "1px solid var(--line)", paddingBottom: "1.25rem" }}>
            <h3 style={{ marginTop: 0, marginBottom: "0.75rem" }}>1日あたり必要人数（シフト種類ごと）</h3>
            <div style={{ display: "grid", gap: "0.75rem" }}>
              {shiftTypes.length === 0 ? (
                <p style={{ margin: 0, color: "#6b7280" }}>有効なシフト種類がありません。</p>
              ) : (
                shiftTypes.map((shiftType) => (
                  <label key={shiftType.id} style={{ display: "grid", gap: "0.25rem" }}>
                    <span style={{ fontWeight: 600 }}>{shiftType.shiftName}</span>
                    <input
                      type="number"
                      min="0"
                      value={rules.requiredCounts?.[shiftType.id] ?? 0}
                      onChange={(e) => handleRequiredCountChange(shiftType.id, Number(e.target.value))}
                      style={{ width: "fit-content", minWidth: 0, display: "inline-block", padding: "0.6rem", border: "1px solid var(--line)", borderRadius: "4px" }}
                    />
                  </label>
                ))
              )}
            </div>
          </div>

          <div style={{ borderBottom: "1px solid var(--line)", paddingBottom: "1.25rem" }}>
            <h3 style={{ marginTop: 0, marginBottom: "0.75rem" }}>勤務制約</h3>
            <div style={{ display: "grid", gap: "0.75rem", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))" }}>
              <label style={{ display: "grid", gap: "0.25rem", gridColumn: "1 / -1" }}>
                <span style={{ fontWeight: 600 }}>月間上限勤務日数の設定</span>
                <select
                  name="monthlyMaxWorkdaysMode"
                  value={rules.monthlyMaxWorkdaysMode || "FIXED"}
                  onChange={handleChange}
                  style={{ width: "fit-content", minWidth: 0, display: "inline-block", padding: "0.6rem", border: "1px solid var(--line)", borderRadius: "4px" }}
                >
                  <option value="FIXED">固定</option>
                  <option value="CALCULATED">月毎変動</option>
                </select>
              </label>
              <label style={{ display: "grid", gap: "0.25rem" }}>
                <span style={{ fontWeight: 600 }}>月間上限勤務日数</span>
                <input
                  type="number"
                  min="0"
                  name="monthlyMaxWorkdays"
                  value={rules.monthlyMaxWorkdays}
                  onChange={handleChange}
                  disabled={(rules.monthlyMaxWorkdaysMode || "FIXED") === "CALCULATED"}
                  style={{ padding: "0.6rem", border: "1px solid var(--line)", borderRadius: "4px" }}
                />
                {(rules.monthlyMaxWorkdaysMode || "FIXED") === "CALCULATED" && (
                  <span style={{ fontSize: "0.85rem", color: "#6b7280" }}>
                    月毎変動を選択中です。自動生成時にその月の日数 ÷ 7 × 5 + 1 で上限を計算します。
                  </span>
                )}
              </label>
              <label style={{ display: "grid", gap: "0.25rem" }}>
                <span style={{ fontWeight: 600 }}>連続勤務日数の上限</span>
                <input
                  type="number"
                  min="0"
                  name="maxConsecutiveWorkdays"
                  value={rules.maxConsecutiveWorkdays}
                  onChange={handleChange}
                  style={{ padding: "0.6rem", border: "1px solid var(--line)", borderRadius: "4px" }}
                />
              </label>
              <label style={{ display: "grid", gap: "0.25rem" }}>
                <span style={{ fontWeight: 600 }}>最低休日日数</span>
                <input
                  type="number"
                  min="0"
                  name="minimumRestDays"
                  value={rules.minimumRestDays}
                  onChange={handleChange}
                  style={{ padding: "0.6rem", border: "1px solid var(--line)", borderRadius: "4px" }}
                />
              </label>
              <label style={{ display: "grid", gap: "0.25rem" }}>
                <span style={{ fontWeight: 600 }}>最短連続シフト間隔（時間）</span>
                <input
                  type="number"
                  min="0"
                  name="minimumShiftGapHours"
                  value={rules.minimumShiftGapHours}
                  onChange={handleChange}
                  style={{ padding: "0.6rem", border: "1px solid var(--line)", borderRadius: "4px" }}
                />
                <span style={{ fontSize: "0.85rem", color: "#6b7280" }}>
                  次のシフトまでに最低何時間空けるかを指定します。たとえば、夜勤の後に早番を入れないようにする際に使います。
                </span>
              </label>
            </div>
          </div>

          <div style={{ borderBottom: "1px solid var(--line)", paddingBottom: "1.25rem" }}>
            <h3 style={{ marginTop: 0, marginBottom: "0.75rem" }}>希望シフトの考慮</h3>
            <label style={{ display: "grid", gap: "0.25rem" }}>
              <span style={{ fontWeight: 600 }}>考慮方法</span>
              <select
                name="desiredShiftMode"
                value={rules.desiredShiftMode}
                onChange={handleChange}
                style={{ width: "fit-content", minWidth: 0, display: "inline-block", padding: "0.6rem", border: "1px solid var(--line)", borderRadius: "4px" }}
              >
                <option value="REQUIRED">必須考慮</option>
                <option value="PRIORITY">優先考慮</option>
                <option value="IGNORE">考慮しない</option>
              </select>
            </label>
          </div>

          <div>
            <h3 style={{ marginTop: 0, marginBottom: "0.75rem" }}>既存シフトの扱い</h3>
            <label style={{ display: "grid", gap: "0.25rem" }}>
              <span style={{ fontWeight: 600 }}>生成方法</span>
              <select
                name="existingShiftHandling"
                value={rules.existingShiftHandling}
                onChange={handleChange}
                style={{ width: "fit-content", minWidth: 0, display: "inline-block", padding: "0.6rem", border: "1px solid var(--line)", borderRadius: "4px" }}
              >
                <option value="ONLY_EMPTY">未入力セルのみ生成</option>
                <option value="OVERWRITE">既存値を上書きして再生成</option>
              </select>
            </label>
          </div>

          <div style={{ display: "flex", gap: "1rem" }}>
            <button
              type="submit"
              disabled={saving}
              style={{
                flex: 1,
                padding: "0.75rem 1rem",
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
                loadData();
              }}
              style={{
                flex: 1,
                padding: "0.75rem 1rem",
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
      )}
    </div>
  );
}

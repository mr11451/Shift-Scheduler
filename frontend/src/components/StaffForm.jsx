import React, { useState, useEffect, useContext } from "react";
import "./StaffForm.css";
import { fetchWithAuth } from "../utils/fetchWithAuth";
import { AuthContext } from "../context/AuthContext";

const WEEKDAY_OPTIONS = [
  { value: 0, label: "日" },
  { value: 1, label: "月" },
  { value: 2, label: "火" },
  { value: 3, label: "水" },
  { value: 4, label: "木" },
  { value: 5, label: "金" },
  { value: 6, label: "土" },
];

export default function StaffForm({ staffId, onSuccess, onCancel }) {
  const { auth } = useContext(AuthContext);
  const canEditGroup = auth?.roleLevel === "MASTER";
  const [form, setForm] = useState({
    staffName: "",
    email: "",
    phone: "",
    ngShiftTypeIdsRaw: "",
    preferredShiftTypeIdsRaw: "",
    responsibility: "",
    roleLevel: "MEMBER",
    groupId: null,
    qualificationIds: [],
    ngShiftTypeIds: [],
    preferredShiftTypeIds: [],
    ngShiftWeekdayIds: [],
    preferredShiftWeekdayIds: [],
    isActive: true,
  });

  const [groups, setGroups] = useState([]);
  const [qualifications, setQualifications] = useState([]);
  const [shiftTypes, setShiftTypes] = useState([]);
  const [groupLoadError, setGroupLoadError] = useState("");
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("");
  const [loading, setLoading] = useState(false);
  const [staffCode, setStaffCode] = useState("");
  const [isEditMode, setIsEditMode] = useState(false);
  const [showGroupModal, setShowGroupModal] = useState(false);
  const [newGroupName, setNewGroupName] = useState("");
  const [currentGroupName, setCurrentGroupName] = useState("");

  useEffect(() => {
    loadGroups();
    loadQualifications();
    loadShiftTypes();
    if (staffId) {
      loadStaff(staffId);
      setIsEditMode(true);
    } else {
      setIsEditMode(false);
      setCurrentGroupName("");
      setGroupLoadError("");
    }
  }, [staffId]);

  async function loadGroups() {
    try {
      const response = await fetchWithAuth("/api/groups");
      if (response.ok) {
        const data = await response.json();
        setGroups(data);
        setGroupLoadError("");
      } else {
        setGroupLoadError("グループ一覧の読み込みに失敗しました。");
      }
    } catch (error) {
      setGroupLoadError("グループ一覧の読み込みに失敗しました。");
      console.error("Failed to load groups:", error);
    }
  }

  async function createNewGroup() {
    if (!newGroupName.trim()) {
      setMessage("グループ名を入力してください。");
      setMessageType("error");
      return;
    }

    try {
      // Generate group code: GRP-{timestamp}
      const timestamp = Date.now().toString().slice(-6);
      const groupCode = `GRP-${timestamp}`;

      const response = await fetchWithAuth("/api/groups", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ 
          groupCode: groupCode,
          groupName: newGroupName 
        }),
      });

      if (response.ok) {
        const newGroup = await response.json();
        setGroups([...groups, newGroup]);
        setForm((prev) => ({ ...prev, groupId: newGroup.id }));
        setShowGroupModal(false);
        setNewGroupName("");
        setMessage("グループを作成しました。");
        setMessageType("success");
      } else {
        setMessage("グループの作成に失敗しました。");
        setMessageType("error");
      }
    } catch (error) {
      setMessage("グループの作成に失敗しました。");
      setMessageType("error");
    }
  }

  async function loadQualifications() {
    try {
      const response = await fetchWithAuth("/api/qualifications");
      if (response.ok) {
        const data = await response.json();
        setQualifications(data);
      }
    } catch (error) {
      console.error("Failed to load qualifications:", error);
    }
  }

  async function loadShiftTypes() {
    try {
      const response = await fetchWithAuth("/api/shift-types?active=true");
      if (response.ok) {
        const data = await response.json();
        setShiftTypes(data);
      }
    } catch (error) {
      console.error("Failed to load shift types:", error);
    }
  }

  function parseNgShiftTypeIds(value) {
    if (!value) {
      return [];
    }

    if (typeof value === "string") {
      const trimmed = value.trim();
      if (!trimmed) {
        return [];
      }

      try {
        const parsed = JSON.parse(trimmed);
        if (Array.isArray(parsed)) {
          return parsed.map((item) => Number(item)).filter((item) => Number.isFinite(item));
        }
        if (parsed && Array.isArray(parsed.shiftTypeIds)) {
          return parsed.shiftTypeIds.map((item) => Number(item)).filter((item) => Number.isFinite(item));
        }
      } catch (error) {
        // Legacy text format is ignored here and remains editable through the text field payload.
      }
    }

    return [];
  }

  function parseShiftWeekdayIds(value) {
    if (!value || typeof value !== "string") {
      return [];
    }

    const trimmed = value.trim();
    if (!trimmed) {
      return [];
    }

    try {
      const parsed = JSON.parse(trimmed);
      const weekdayIds = Array.isArray(parsed?.weekdayIds) ? parsed.weekdayIds : [];
      return weekdayIds
        .map((item) => Number(item))
        .filter((item) => Number.isInteger(item) && item >= 0 && item <= 6);
    } catch (error) {
      return [];
    }
  }

  function buildShiftPreferenceValue(shiftTypeIds, weekdayIds, fallbackValue) {
    const normalizedShiftTypeIds = (shiftTypeIds || []).map((id) => Number(id)).filter((id) => Number.isFinite(id));
    const normalizedWeekdayIds = (weekdayIds || []).map((id) => Number(id)).filter((id) => Number.isInteger(id) && id >= 0 && id <= 6);

    if (normalizedShiftTypeIds.length === 0 && normalizedWeekdayIds.length === 0) {
      return fallbackValue;
    }

    return JSON.stringify({
      shiftTypeIds: normalizedShiftTypeIds,
      weekdayIds: normalizedWeekdayIds,
    });
  }

  async function loadStaff(id) {
    try {
      const response = await fetchWithAuth(`/api/staffs/${id}`);
      if (response.ok) {
        const data = await response.json();
        setStaffCode(data.staffCode);
        setCurrentGroupName(data.groupName || "");
        setForm({
          staffName: data.staffName,
          email: data.email || "",
          phone: data.phone || "",
          ngShiftTypeIdsRaw: data.ngShiftTypeIds || "",
          preferredShiftTypeIdsRaw: data.preferredShiftTypeIds || "",
          responsibility: data.responsibility,
          roleLevel: data.roleLevel,
          groupId: data.groupId ?? null,
          qualificationIds: data.qualificationIds || [],
          ngShiftTypeIds: parseNgShiftTypeIds(data.ngShiftTypeIds),
          preferredShiftTypeIds: parseNgShiftTypeIds(data.preferredShiftTypeIds),
          ngShiftWeekdayIds: parseShiftWeekdayIds(data.ngShiftTypeIds),
          preferredShiftWeekdayIds: parseShiftWeekdayIds(data.preferredShiftTypeIds),
          isActive: data.isActive,
        });
      }
    } catch (error) {
      setMessage("スタッフ情報の読み込みに失敗しました。");
      setMessageType("error");
    }
  }

  const selectedGroupId = form.groupId ?? "";
  const hasCurrentGroupOption = currentGroupName && selectedGroupId !== "" && groups.some((group) => String(group.id) === String(selectedGroupId));

  function onChange(event) {
    const { name, value, type, checked } = event.target;
    const fieldValue = type === "checkbox" ? checked : value;

    // Handle new group creation
    if (name === "groupId" && value === "new") {
      setShowGroupModal(true);
      return;
    }

    // Convert numeric fields
    if (name === "groupId" && value) {
      setForm((prev) => ({ ...prev, [name]: parseInt(value, 10) || null }));
    } else if (name === "qualificationIds") {
      // Handle qualification checkbox
      setForm((prev) => {
        const qId = parseInt(value, 10);
        const newIds = checked
          ? [...prev.qualificationIds, qId]
          : prev.qualificationIds.filter((id) => id !== qId);
        return { ...prev, qualificationIds: newIds };
      });
    } else if (name === "ngShiftTypeIds") {
      const selectedValue = Number(value);
      setForm((prev) => {
        const currentIds = prev.ngShiftTypeIds || [];
        const nextIds = checked
          ? [...currentIds, selectedValue]
          : currentIds.filter((id) => id !== selectedValue);
        return { ...prev, ngShiftTypeIds: nextIds };
      });
    } else if (name === "preferredShiftTypeIds") {
      const selectedValue = Number(value);
      setForm((prev) => {
        const currentIds = prev.preferredShiftTypeIds || [];
        const nextIds = checked
          ? [...currentIds, selectedValue]
          : currentIds.filter((id) => id !== selectedValue);
        return { ...prev, preferredShiftTypeIds: nextIds };
      });
    } else if (name === "ngShiftWeekdayIds") {
      const selectedValue = Number(value);
      setForm((prev) => {
        const currentIds = prev.ngShiftWeekdayIds || [];
        const nextIds = checked
          ? [...currentIds, selectedValue].sort((left, right) => left - right)
          : currentIds.filter((id) => id !== selectedValue);
        return { ...prev, ngShiftWeekdayIds: nextIds };
      });
    } else if (name === "preferredShiftWeekdayIds") {
      const selectedValue = Number(value);
      setForm((prev) => {
        const currentIds = prev.preferredShiftWeekdayIds || [];
        const nextIds = checked
          ? [...currentIds, selectedValue].sort((left, right) => left - right)
          : currentIds.filter((id) => id !== selectedValue);
        return { ...prev, preferredShiftWeekdayIds: nextIds };
      });
    } else {
      setForm((prev) => ({ ...prev, [name]: fieldValue }));
    }
  }

  async function onSubmit(event) {
    event.preventDefault();
    setLoading(true);
    setMessage("");
    setMessageType("");

    const payload = {
      staffName: form.staffName,
      email: form.email,
      phone: form.phone,
      responsibility: form.responsibility,
      roleLevel: form.roleLevel,
      groupId: form.groupId,
      qualificationIds: form.qualificationIds,
      isActive: form.isActive,
      ngShiftTypeIds: buildShiftPreferenceValue(form.ngShiftTypeIds, form.ngShiftWeekdayIds, form.ngShiftTypeIdsRaw),
      preferredShiftTypeIds: buildShiftPreferenceValue(form.preferredShiftTypeIds, form.preferredShiftWeekdayIds, form.preferredShiftTypeIdsRaw),
    };

    // Validation
    if (!form.staffName.trim()) {
      setMessage("氏名は必須です。");
      setMessageType("error");
      setLoading(false);
      return;
    }

    if (!form.responsibility.trim()) {
      setMessage("担当は必須です。");
      setMessageType("error");
      setLoading(false);
      return;
    }

    if ((form.roleLevel === "MEMBER" || form.roleLevel === "CHIEF") && !form.groupId) {
      setMessage("メンバ/チーフ選択時はグループが必須です。");
      setMessageType("error");
      setLoading(false);
      return;
    }

    if (form.roleLevel === "MEMBER" && !form.email.trim()) {
      setMessage("メンバ選択時はメールアドレスが必須です。");
      setMessageType("error");
      setLoading(false);
      return;
    }

    try {
      const url = isEditMode ? `/api/staffs/${staffId}` : "/api/staffs";
      const method = isEditMode ? "PUT" : "POST";

      const response = await fetchWithAuth(url, {
        method: method,
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      if (response.ok) {
        setMessage(isEditMode ? "スタッフ情報を更新しました。" : "スタッフを登録しました。");
        setMessageType("success");
        if (onSuccess) {
          setTimeout(() => onSuccess(), 1500);
        }
      } else {
        const errorData = await response.json().catch(() => ({}));
        setMessage(errorData.message || "エラーが発生しました。");
        setMessageType("error");
      }
    } catch (error) {
      setMessage("保存に失敗しました。");
      setMessageType("error");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="staff-form-container">
      <h2>{isEditMode ? "スタッフ編集" : "スタッフ登録"}</h2>

      {message && (
        <div className={`message message-${messageType}`}>
          {message}
        </div>
      )}

      <form onSubmit={onSubmit} className="staff-form">
        <div className="form-row">
          <div className="form-group">
            <label htmlFor="staffCode">登録番号</label>
            <input
              type="text"
              id="staffCode"
              value={staffCode || "（自動割り付け）"}
              disabled
              className="form-control"
            />
          </div>

          <div className="form-group">
            <label htmlFor="staffName">氏名 *</label>
            <input
              type="text"
              id="staffName"
              name="staffName"
              value={form.staffName}
              onChange={onChange}
              className="form-control"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="email">メールアドレス</label>
            <input
              type="email"
              id="email"
              name="email"
              value={form.email}
              onChange={onChange}
              className="form-control"
            />
            <small>メンバ選択時は必須</small>
          </div>
        </div>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="phone">電話番号</label>
            <input
              type="tel"
              id="phone"
              name="phone"
              value={form.phone}
              onChange={onChange}
              className="form-control"
              pattern="[0-9\-]*"
              placeholder="例: 090-1234-5678"
            />
          </div>

          <div className="form-group">
            <label htmlFor="responsibility">担当 *</label>
            <input
              type="text"
              id="responsibility"
              name="responsibility"
              value={form.responsibility}
              onChange={onChange}
              className="form-control"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="roleLevel">権限 *</label>
            <select
              id="roleLevel"
              name="roleLevel"
              value={form.roleLevel}
              onChange={onChange}
              className="form-control"
              required
            >
              <option value="MEMBER">メンバ</option>
              <option value="CHIEF">チーフ</option>
              <option value="MASTER">マスタ</option>
            </select>
          </div>
        </div>

        <div className="form-row full">
          <div className="form-group">
            <label>避けるシフト</label>
            <div style={{ display: "grid", gap: "0.6rem", marginTop: "0.5rem" }}>
              {shiftTypes.length === 0 ? (
                <p style={{ color: "#999", fontSize: "0.9rem", margin: 0 }}>登録済みシフトタイプがありません</p>
              ) : (
                shiftTypes.map((shiftType) => (
                  <label key={shiftType.id} className="shift-option-label">
                    <input
                      type="checkbox"
                      name="ngShiftTypeIds"
                      value={shiftType.id}
                      checked={(form.ngShiftTypeIds || []).includes(Number(shiftType.id))}
                      onChange={onChange}
                      className="shift-option-checkbox"
                    />
                    <span className="shift-option-text">{shiftType.shiftName}{shiftType.startTime ? ` (${shiftType.startTime}〜${shiftType.endTime})` : ""}</span>
                  </label>
                ))
              )}
            </div>
            <div className="shift-weekday-row">
              {WEEKDAY_OPTIONS.map((weekday) => (
                <label key={`ng-weekday-${weekday.value}`} className="shift-weekday-label">
                  <input
                    type="checkbox"
                    name="ngShiftWeekdayIds"
                    value={weekday.value}
                    checked={(form.ngShiftWeekdayIds || []).includes(weekday.value)}
                    onChange={onChange}
                    className="shift-option-checkbox"
                  />
                  <span>{weekday.label}</span>
                </label>
              ))}
            </div>
            <small>避けたいシフトタイプを選択してください。これは自動生成時の強い制約になります。</small>
          </div>
        </div>

        <div className="form-row full">
          <div className="form-group">
            <label>希望シフト</label>
            <div style={{ display: "grid", gap: "0.6rem", marginTop: "0.5rem" }}>
              {shiftTypes.length === 0 ? (
                <p style={{ color: "#999", fontSize: "0.9rem", margin: 0 }}>登録済みシフトタイプがありません</p>
              ) : (
                shiftTypes.map((shiftType) => (
                  <label key={`preferred-${shiftType.id}`} className="shift-option-label">
                    <input
                      type="checkbox"
                      name="preferredShiftTypeIds"
                      value={shiftType.id}
                      checked={(form.preferredShiftTypeIds || []).includes(Number(shiftType.id))}
                      onChange={onChange}
                      className="shift-option-checkbox"
                    />
                    <span className="shift-option-text">{shiftType.shiftName}{shiftType.startTime ? ` (${shiftType.startTime}〜${shiftType.endTime})` : ""}</span>
                  </label>
                ))
              )}
            </div>
            <div className="shift-weekday-row">
              {WEEKDAY_OPTIONS.map((weekday) => (
                <label key={`preferred-weekday-${weekday.value}`} className="shift-weekday-label">
                  <input
                    type="checkbox"
                    name="preferredShiftWeekdayIds"
                    value={weekday.value}
                    checked={(form.preferredShiftWeekdayIds || []).includes(weekday.value)}
                    onChange={onChange}
                    className="shift-option-checkbox"
                  />
                  <span>{weekday.label}</span>
                </label>
              ))}
            </div>
            <small>希望するシフトタイプを選択してください。これは自動生成時の優先度を少し上げるだけの弱い好みです。</small>
          </div>
        </div>

        {canEditGroup && (
          <div className="form-row full">
            <div className="form-group">
              <label htmlFor="groupId">グループ</label>
              <select
                id="groupId"
                name="groupId"
                value={selectedGroupId}
                onChange={onChange}
                className="form-control"
              >
                <option value="">グループを選択</option>
                {isEditMode && currentGroupName && !hasCurrentGroupOption && (
                  <option value={selectedGroupId} disabled>
                    現在のグループ: {currentGroupName}
                  </option>
                )}
                {groups.map((group) => (
                  <option key={group.id} value={group.id}>
                    {group.groupName}
                  </option>
                ))}
                <option value="new" style={{ color: "var(--accent)", fontWeight: "bold" }}>
                  ➕ 新規グループ作成
                </option>
              </select>
              {groupLoadError && (
                <small style={{ color: "#b45309" }}>{groupLoadError}</small>
              )}
            </div>
          </div>
        )}

        <div className="form-row full">
          <div className="form-group">
            <label>資格（複数選択可）</label>
            <div style={{ display: "flex", flexWrap: "wrap", gap: "1rem", marginTop: "0.5rem" }}>
              {qualifications.length === 0 ? (
                <p style={{ color: "#999", fontSize: "0.9rem" }}>登録済み資格がありません</p>
              ) : (
                qualifications.map((qual) => (
                  <label key={qual.id} style={{ display: "flex", alignItems: "center", gap: "0.5rem", cursor: "pointer" }}>
                    <input
                      type="checkbox"
                      name="qualificationIds"
                      value={qual.id}
                      checked={form.qualificationIds.includes(qual.id)}
                      onChange={onChange}
                    />
                    <span>{qual.qualificationName}</span>
                  </label>
                ))
              )}
            </div>
          </div>
        </div>

        {isEditMode && (
          <div className="form-row full">
            <div className="form-group checkbox">
              <input
                type="checkbox"
                id="isActive"
                name="isActive"
                checked={form.isActive}
                onChange={onChange}
                className="form-control"
              />
              <label htmlFor="isActive">有効</label>
            </div>
          </div>
        )}

        <div className="form-actions">
          <button type="submit" disabled={loading} className="btn btn-primary">
            {loading ? "保存中..." : "保存"}
          </button>
          <button type="button" onClick={onCancel} className="btn btn-secondary">
            キャンセル
          </button>
        </div>
      </form>

      {showGroupModal && (
        <div style={{
          position: "fixed",
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: "rgba(0, 0, 0, 0.5)",
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          zIndex: 1000,
        }}>
          <div style={{
            backgroundColor: "#fff",
            padding: "2rem",
            borderRadius: "8px",
            boxShadow: "0 4px 12px rgba(0, 0, 0, 0.15)",
            minWidth: "400px",
          }}>
            <h3 style={{ marginTop: 0, marginBottom: "1rem" }}>新規グループ作成</h3>
            
            <div style={{ marginBottom: "1.5rem" }}>
              <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: 600 }}>
                グループ名
              </label>
              <input
                type="text"
                value={newGroupName}
                onChange={(e) => setNewGroupName(e.target.value)}
                onKeyPress={(e) => e.key === "Enter" && createNewGroup()}
                placeholder="グループ名を入力してください"
                style={{
                  width: "100%",
                  padding: "0.5rem",
                  border: "1px solid #ccc",
                  borderRadius: "4px",
                  fontSize: "1rem",
                  boxSizing: "border-box",
                }}
                autoFocus
              />
            </div>

            <div style={{ display: "flex", gap: "0.5rem", justifyContent: "flex-end" }}>
              <button
                type="button"
                onClick={() => {
                  setShowGroupModal(false);
                  setNewGroupName("");
                }}
                style={{
                  padding: "0.5rem 1rem",
                  backgroundColor: "#ddd",
                  border: "none",
                  borderRadius: "4px",
                  cursor: "pointer",
                  fontWeight: 600,
                }}
              >
                キャンセル
              </button>
              <button
                type="button"
                onClick={createNewGroup}
                style={{
                  padding: "0.5rem 1rem",
                  backgroundColor: "var(--accent)",
                  color: "#fff",
                  border: "none",
                  borderRadius: "4px",
                  cursor: "pointer",
                  fontWeight: 600,
                }}
              >
                作成
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

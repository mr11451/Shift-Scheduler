import React, { useState, useEffect, useContext } from "react";
import { fetchWithAuth } from "../../utils/fetchWithAuth";
import { AuthContext } from "../../context/AuthContext";

// Admin tab for managing shift types. CHIEF can only add; edit/deactivate is limited to
// MASTER or the CHIEF who created the entry.
export default function AdminShiftTypeTab() {
  const { auth } = useContext(AuthContext);
  const isChiefRole = String(auth?.roleLevel || "").toUpperCase() === "CHIEF";
  const [shiftTypes, setShiftTypes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("");
  const [view, setView] = useState("list");
  const [form, setForm] = useState({
    shiftCode: "",
    shiftName: "",
    startTime: "",
    endTime: "",
    sortOrder: 0,
    isActive: true,
  });
  const [editingId, setEditingId] = useState(null);

  useEffect(() => {
    loadShiftTypes();
  }, []);

  // Fetch the full shift type list (active and inactive) from the API.
  async function loadShiftTypes() {
    try {
      setLoading(true);
      const res = await fetchWithAuth("/api/shift-types", { redirectOnUnauthorized: false });
      if (!res.ok) {
        if (res.status === 401 || res.status === 403) {
          throw new Error("権限が不足しているため、シフト種類を取得できません。");
        }
        throw new Error("シフト種類の取得に失敗しました。");
      }
      setShiftTypes(await res.json());
    } catch (e) {
      setMessage(e.message);
      setMessageType("error");
    } finally {
      setLoading(false);
    }
  }

  // Populate the form with an existing shift type's values and switch to edit view (blocked for CHIEF).
  function handleEdit(shiftType) {
    if (isChiefRole) {
      return;
    }

    setForm({
      shiftCode: shiftType.shiftCode,
      shiftName: shiftType.shiftName,
      startTime: shiftType.startTime || "",
      endTime: shiftType.endTime || "",
      sortOrder: shiftType.sortOrder ?? 0,
      isActive: shiftType.isActive,
    });
    setEditingId(shiftType.id);
    setView("form");
  }

  // Reset the form for creating a brand-new shift type.
  function handleNewShiftType() {
    setForm({
      shiftCode: "",
      shiftName: "",
      startTime: "",
      endTime: "",
      sortOrder: shiftTypes.length,
      isActive: true,
    });
    setEditingId(null);
    setView("form");
  }

  // Sync a form field's value (or checkbox state) into local form state.
  function handleChange(e) {
    const { name, value, type, checked } = e.target;
    setForm((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : name === "sortOrder" ? parseInt(value, 10) || 0 : value,
    }));
  }

  // Validate and submit the create/update form to the API.
  async function handleSubmit(e) {
    e.preventDefault();
    setMessage("");
    setMessageType("");

    if (isChiefRole && editingId) {
      setMessage("チーフ権限では新規追加のみ行えます。");
      setMessageType("error");
      return;
    }

    if (!form.shiftCode.trim()) {
      setMessage("シフト記号は必須です。");
      setMessageType("error");
      return;
    }

    if (!form.shiftName.trim()) {
      setMessage("シフト名称は必須です。");
      setMessageType("error");
      return;
    }

    try {
      const method = editingId ? "PUT" : "POST";
      const url = editingId ? `/api/shift-types/${editingId}` : "/api/shift-types";
      const res = await fetchWithAuth(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form),
        redirectOnUnauthorized: false,
      });

      if (!res.ok) {
        if (res.status === 401 || res.status === 403) {
          throw new Error("権限が不足しているため、保存できません。");
        }
        const detail = await res.text();
        throw new Error(detail || "保存に失敗しました。");
      }
      setMessage("保存しました。");
      setMessageType("success");
      setView("list");
      await loadShiftTypes();
    } catch (e) {
      setMessage(e.message);
      setMessageType("error");
    }
  }

  // Toggle a shift type's active/inactive status (logical delete/restore), blocked for CHIEF.
  async function handleToggleActive(shiftType) {
    if (isChiefRole) {
      return;
    }

    const isActive = shiftType.isActive !== false;
    const actionLabel = isActive ? "無効化" : "有効化";
    if (!window.confirm(`このシフト種類を${actionLabel}してもよろしいですか？`)) return;

    try {
      const res = isActive
        ? await fetchWithAuth(`/api/shift-types/${shiftType.id}`, {
            method: "DELETE",
            redirectOnUnauthorized: false,
          })
        : await fetchWithAuth(`/api/shift-types/${shiftType.id}/reactivate`, {
            method: "POST",
            redirectOnUnauthorized: false,
          });

      if (!res.ok) {
        if (res.status === 401 || res.status === 403) {
          throw new Error(`権限が不足しているため、${actionLabel}できません。`);
        }
        const detail = await res.text();
        throw new Error(detail || `${actionLabel}に失敗しました。`);
      }
      setMessage(`シフト種類を${actionLabel}しました。`);
      setMessageType("success");
      await loadShiftTypes();
    } catch (e) {
      setMessage(e.message);
      setMessageType("error");
    }
  }

  if (loading && view === "list") return <div className="card">読み込み中...</div>;

  return (
    <div>
      {message && <div className={messageType} style={{ marginBottom: "1rem", padding: "0.75rem", borderRadius: "4px" }}>{message}</div>}

      {view === "list" ? (
        <div className="card">
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1rem" }}>
            <h2>シフト種類管理</h2>
            <button
              type="button"
              onClick={handleNewShiftType}
              style={{
                padding: "0.5rem 1rem",
                backgroundColor: "var(--accent)",
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                cursor: "pointer",
              }}
            >
              + 新規追加
            </button>
          </div>

          <table style={{ width: "100%" }}>
            <thead>
              <tr>
                <th style={{ textAlign: "left", padding: "0.5rem" }}>記号</th>
                <th style={{ textAlign: "left", padding: "0.5rem" }}>シフト名</th>
                <th style={{ textAlign: "left", padding: "0.5rem" }}>開始時刻</th>
                <th style={{ textAlign: "left", padding: "0.5rem" }}>終了時刻</th>
                <th style={{ textAlign: "center", padding: "0.5rem" }}>順序</th>
                <th style={{ textAlign: "center", padding: "0.5rem" }}>状態</th>
                <th style={{ textAlign: "center", padding: "0.5rem" }}>操作</th>
              </tr>
            </thead>
            <tbody>
              {shiftTypes.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: "center", padding: "1rem", color: "#999" }}>
                    シフト種類はまだ登録されていません。
                  </td>
                </tr>
              ) : (
                shiftTypes.map((st) => (
                  <tr key={st.id} style={{ borderBottom: "1px solid var(--line)" }}>
                    <td style={{ padding: "0.5rem", fontWeight: 600 }}>{st.shiftCode}</td>
                    <td style={{ padding: "0.5rem" }}>{st.shiftName}</td>
                    <td style={{ padding: "0.5rem" }}>{st.startTime || "-"}</td>
                    <td style={{ padding: "0.5rem" }}>{st.endTime || "-"}</td>
                    <td style={{ textAlign: "center", padding: "0.5rem" }}>{st.sortOrder}</td>
                    <td style={{ textAlign: "center", padding: "0.5rem" }}>{st.isActive ? "有効" : "無効"}</td>
                    <td style={{ textAlign: "center", padding: "0.5rem" }}>
                      {isChiefRole ? (
                        "-"
                      ) : (
                        <>
                          <button
                            type="button"
                            onClick={() => handleEdit(st)}
                            style={{
                              padding: "0.4rem 0.8rem",
                              marginRight: "0.5rem",
                              backgroundColor: "var(--accent)",
                              color: "#fff",
                              border: "none",
                              borderRadius: "4px",
                              cursor: "pointer",
                              fontSize: "0.9rem",
                            }}
                          >
                            編集
                          </button>
                          <button
                            type="button"
                            onClick={() => handleToggleActive(st)}
                            style={{
                              padding: "0.4rem 0.8rem",
                              backgroundColor: st.isActive ? "#d9534f" : "#198754",
                              color: "#fff",
                              border: "none",
                              borderRadius: "4px",
                              cursor: "pointer",
                              fontSize: "0.9rem",
                            }}
                          >
                            {st.isActive ? "無効化" : "有効化"}
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="card">
          <h2>{editingId ? "シフト種類編集" : "シフト種類新規追加"}</h2>
          <form onSubmit={handleSubmit} style={{ display: "grid", gap: "1rem", marginTop: "1rem" }}>
            <div>
              <label style={{ display: "block", marginBottom: "0.25rem", fontWeight: 600 }}>シフト記号</label>
              <input
                type="text"
                name="shiftCode"
                value={form.shiftCode}
                onChange={handleChange}
                placeholder="例: A, P, -"
                style={{ width: "100%", padding: "0.5rem", border: "1px solid var(--line)", borderRadius: "4px" }}
              />
            </div>
            <div>
              <label style={{ display: "block", marginBottom: "0.25rem", fontWeight: 600 }}>シフト名称</label>
              <input
                type="text"
                name="shiftName"
                value={form.shiftName}
                onChange={handleChange}
                placeholder="例: 早番、遅番、休み"
                style={{ width: "100%", padding: "0.5rem", border: "1px solid var(--line)", borderRadius: "4px" }}
              />
            </div>
            <div>
              <label style={{ display: "block", marginBottom: "0.25rem", fontWeight: 600 }}>開始時刻</label>
              <input
                type="time"
                name="startTime"
                value={form.startTime}
                onChange={handleChange}
                style={{ width: "100%", padding: "0.5rem", border: "1px solid var(--line)", borderRadius: "4px" }}
              />
            </div>
            <div>
              <label style={{ display: "block", marginBottom: "0.25rem", fontWeight: 600 }}>終了時刻</label>
              <input
                type="time"
                name="endTime"
                value={form.endTime}
                onChange={handleChange}
                style={{ width: "100%", padding: "0.5rem", border: "1px solid var(--line)", borderRadius: "4px" }}
              />
            </div>
            <div>
              <label style={{ display: "block", marginBottom: "0.25rem", fontWeight: 600 }}>表示順序</label>
              <input
                type="number"
                name="sortOrder"
                value={form.sortOrder}
                onChange={handleChange}
                style={{ width: "100%", padding: "0.5rem", border: "1px solid var(--line)", borderRadius: "4px" }}
              />
            </div>
            <div>
              <label style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                <input
                  type="checkbox"
                  name="isActive"
                  checked={form.isActive}
                  onChange={handleChange}
                />
                <span>有効</span>
              </label>
            </div>
            <div style={{ display: "flex", gap: "1rem", marginTop: "1rem" }}>
              <button
                type="submit"
                style={{
                  flex: 1,
                  padding: "0.75rem",
                  backgroundColor: "var(--accent)",
                  color: "#fff",
                  border: "none",
                  borderRadius: "4px",
                  cursor: "pointer",
                  fontWeight: 600,
                }}
              >
                保存
              </button>
              <button
                type="button"
                onClick={() => setView("list")}
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
      )}
    </div>
  );
}

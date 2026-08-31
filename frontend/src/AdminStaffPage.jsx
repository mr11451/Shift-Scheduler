import React, { useState, useEffect, useMemo } from "react";
import { Link } from "react-router-dom";
import StaffForm from "./components/StaffForm";
import { DEFAULT_ROLE_LABELS, parseRoleLabels } from "./utils/roleLabels";

// Sortable list of all staff with edit/delete actions, feeding into StaffForm for create/edit.
function StaffListView({ onEdit }) {
  const [staffs, setStaffs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("");
  const [sortKey, setSortKey] = useState("staffCode");
  const [sortDirection, setSortDirection] = useState("asc");
  const [roleLabelMap, setRoleLabelMap] = useState({ ...DEFAULT_ROLE_LABELS });

  useEffect(() => {
    loadStaffs();
    loadRoleLabels();
  }, []);

  // Fetch the staff list visible to the current requester.
  async function loadStaffs() {
    try {
      setLoading(true);
      const res = await fetch("/api/staffs");
      if (!res.ok) throw new Error("スタッフ一覧の取得に失敗しました。");
      setStaffs(await res.json());
    } catch (e) {
      setMessage(e.message);
      setMessageType("error");
    } finally {
      setLoading(false);
    }
  }

  // Fetch the configured role display labels, falling back to defaults on failure.
  async function loadRoleLabels() {
    try {
      const res = await fetch("/api/system-settings/roleLabels");
      if (!res.ok) {
        setRoleLabelMap({ ...DEFAULT_ROLE_LABELS });
        return;
      }

      const setting = await res.json();
      setRoleLabelMap(parseRoleLabels(setting?.settingValueText || ""));
    } catch {
      setRoleLabelMap({ ...DEFAULT_ROLE_LABELS });
    }
  }

  // Deactivate (logically delete) a staff member after confirmation.
  async function handleDelete(staffId) {
    if (!window.confirm("このスタッフを削除してもよろしいですか？")) return;
    try {
      const res = await fetch(`/api/staffs/${staffId}`, { method: "DELETE" });
      if (!res.ok) throw new Error("削除に失敗しました。");
      setMessage("スタッフを削除しました。");
      setMessageType("success");
      await loadStaffs();
    } catch (e) {
      setMessage(e.message);
      setMessageType("error");
    }
  }

  // Switch the sort column, or flip direction if the same column is clicked again.
  function toggleSort(nextKey) {
    if (sortKey === nextKey) {
      setSortDirection((prev) => (prev === "asc" ? "desc" : "asc"));
      return;
    }
    setSortKey(nextKey);
    setSortDirection("asc");
  }

  const sortedStaffs = useMemo(() => {
    const direction = sortDirection === "asc" ? 1 : -1;
    return [...staffs].sort((a, b) => {
      const left = String(a?.[sortKey] ?? "");
      const right = String(b?.[sortKey] ?? "");
      const compared = left.localeCompare(right, "ja");
      if (compared !== 0) {
        return compared * direction;
      }
      return String(a?.staffCode ?? "").localeCompare(String(b?.staffCode ?? ""), "ja") * direction;
    });
  }, [staffs, sortDirection, sortKey]);

  // Render the ascending/descending arrow for the active sort column header.
  function sortIndicator(key) {
    if (sortKey !== key) {
      return "";
    }
    return sortDirection === "asc" ? " ▲" : " ▼";
  }

  if (loading) return <div className="card">読み込み中...</div>;

  return (
    <div className="card">
      <h2>スタッフ一覧</h2>
      {message && <div className={messageType} style={{ marginBottom: "1rem" }}>{message}</div>}
      <table style={{ width: "100%" }}>
        <thead>
          <tr>
            <th style={{ textAlign: "left", padding: "0.5rem" }}>
              <button
                type="button"
                onClick={() => toggleSort("staffCode")}
                style={{
                  border: "none",
                  background: "transparent",
                  cursor: "pointer",
                  padding: 0,
                  font: "inherit",
                  color: "inherit",
                  fontWeight: 600,
                }}
              >
                スタッフコード{sortIndicator("staffCode")}
              </button>
            </th>
            <th style={{ textAlign: "left", padding: "0.5rem" }}>
              <button
                type="button"
                onClick={() => toggleSort("staffName")}
                style={{
                  border: "none",
                  background: "transparent",
                  cursor: "pointer",
                  padding: 0,
                  font: "inherit",
                  color: "inherit",
                  fontWeight: 600,
                }}
              >
                氏名{sortIndicator("staffName")}
              </button>
            </th>
            <th style={{ textAlign: "left", padding: "0.5rem" }}>担当</th>
            <th style={{ textAlign: "left", padding: "0.5rem" }}>グループ</th>
            <th style={{ textAlign: "left", padding: "0.5rem" }}>権限</th>
            <th style={{ textAlign: "left", padding: "0.5rem" }}>メール</th>
            <th style={{ textAlign: "center", padding: "0.5rem" }}>操作</th>
          </tr>
        </thead>
        <tbody>
          {sortedStaffs.length === 0 ? (
            <tr>
              <td colSpan={7} style={{ textAlign: "center", padding: "1rem", color: "#999" }}>
                スタッフはまだ登録されていません。
              </td>
            </tr>
          ) : (
            sortedStaffs.map((staff) => (
              <tr key={staff.id} style={{ borderBottom: "1px solid var(--line)" }}>
                <td style={{ padding: "0.5rem" }}>{staff.staffCode}</td>
                <td style={{ padding: "0.5rem" }}>{staff.staffName}</td>
                <td style={{ padding: "0.5rem" }}>{staff.responsibility || "-"}</td>
                <td style={{ padding: "0.5rem" }}>{staff.groupName || "-"}</td>
                <td style={{ padding: "0.5rem" }}>{roleLabelMap[staff.roleLevel] || staff.roleLevel || "-"}</td>
                <td style={{ padding: "0.5rem" }}>{staff.email || "-"}</td>
                <td style={{ textAlign: "center", padding: "0.5rem" }}>
                  <button
                    type="button"
                    onClick={() => onEdit(staff.id)}
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
                    onClick={() => handleDelete(staff.id)}
                    style={{
                      padding: "0.4rem 0.8rem",
                      backgroundColor: "#d9534f",
                      color: "#fff",
                      border: "none",
                      borderRadius: "4px",
                      cursor: "pointer",
                      fontSize: "0.9rem",
                    }}
                  >
                    削除
                  </button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

// Standalone page wrapper toggling between the staff list and the create/edit form.
export default function AdminStaffPage() {
  const [view, setView] = useState("list");
  const [editingStaffId, setEditingStaffId] = useState(null);

  const handleFormSubmit = () => {
    setView("list");
    setEditingStaffId(null);
  };

  const handleEdit = (staffId) => {
    setEditingStaffId(staffId);
    setView("form");
  };

  const handleNewStaff = () => {
    setEditingStaffId(null);
    setView("form");
  };

  return (
    <main className="shell">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1.5rem" }}>
        <div>
          <h1>スタッフ管理</h1>
          <p className="subtitle">スタッフ情報の登録・編集・削除</p>
        </div>
        <Link to="/admin" style={{ textDecoration: "none" }}>
          <button type="button" style={{
            padding: "0.5rem 1rem",
            backgroundColor: "#999",
            color: "#fff",
            border: "none",
            borderRadius: "8px",
            cursor: "pointer",
          }}>
            ← 管理画面へ戻る
          </button>
        </Link>
      </div>

      <div style={{ display: "flex", gap: "0.5rem", marginBottom: "1rem" }}>
        <button
          type="button"
          onClick={() => setView("list")}
          style={{
            flex: 1,
            padding: "0.75rem",
            backgroundColor: view === "list" ? "var(--accent)" : "#ddd",
            color: view === "list" ? "#fff" : "#000",
            border: "none",
            borderRadius: "8px",
            cursor: "pointer",
            fontWeight: 600,
          }}
        >
          一覧表示
        </button>
        <button
          type="button"
          onClick={handleNewStaff}
          style={{
            flex: 1,
            padding: "0.75rem",
            backgroundColor: view === "form" && !editingStaffId ? "var(--accent)" : "#ddd",
            color: view === "form" && !editingStaffId ? "#fff" : "#000",
            border: "none",
            borderRadius: "8px",
            cursor: "pointer",
            fontWeight: 600,
          }}
        >
          新規作成
        </button>
      </div>

      <section className="layout">
        {view === "list" ? (
          <StaffListView onEdit={handleEdit} />
        ) : (
          <StaffForm
            staffId={editingStaffId}
            onSuccess={handleFormSubmit}
            onCancel={handleFormSubmit}
          />
        )}
      </section>
    </main>
  );
}

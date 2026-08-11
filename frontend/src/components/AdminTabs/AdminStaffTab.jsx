import React, { useState, useEffect } from "react";
import StaffForm from "../StaffForm";
import { fetchWithAuth } from "../../utils/fetchWithAuth";

function StaffListView({ onEdit }) {
  const [staffs, setStaffs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("");

  useEffect(() => {
    loadStaffs();
  }, []);

  async function loadStaffs() {
    try {
      setLoading(true);
      const res = await fetchWithAuth("/api/staffs");
      if (!res.ok) throw new Error("スタッフ一覧の取得に失敗しました。");
      setStaffs(await res.json());
    } catch (e) {
      setMessage(e.message);
      setMessageType("error");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete(staffId) {
    if (!window.confirm("このスタッフを削除してもよろしいですか？")) return;
    try {
      const res = await fetchWithAuth(`/api/staffs/${staffId}`, { method: "DELETE" });
      if (!res.ok) throw new Error("削除に失敗しました。");
      setMessage("スタッフを削除しました。");
      setMessageType("success");
      await loadStaffs();
    } catch (e) {
      setMessage(e.message);
      setMessageType("error");
    }
  }

  if (loading) return <div className="card">読み込み中...</div>;

  return (
    <div className="card">
      <h2>スタッフ一覧</h2>
      {message && <div className={messageType} style={{ marginBottom: "1rem" }}>{message}</div>}
      <table style={{ width: "100%" }}>
        <thead>
          <tr>
            <th style={{ textAlign: "left", padding: "0.5rem" }}>スタッフコード</th>
            <th style={{ textAlign: "left", padding: "0.5rem" }}>氏名</th>
            <th style={{ textAlign: "left", padding: "0.5rem" }}>メール</th>
            <th style={{ textAlign: "center", padding: "0.5rem" }}>操作</th>
          </tr>
        </thead>
        <tbody>
          {staffs.length === 0 ? (
            <tr>
              <td colSpan={4} style={{ textAlign: "center", padding: "1rem", color: "#999" }}>
                スタッフはまだ登録されていません。
              </td>
            </tr>
          ) : (
            staffs.map((staff) => (
              <tr key={staff.id} style={{ borderBottom: "1px solid var(--line)" }}>
                <td style={{ padding: "0.5rem" }}>{staff.staffCode}</td>
                <td style={{ padding: "0.5rem" }}>{staff.staffName}</td>
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

export default function AdminStaffTab() {
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
    <div>
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

      <section className="tab-content">
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
    </div>
  );
}

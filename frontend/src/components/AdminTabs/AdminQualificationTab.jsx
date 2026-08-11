import React, { useState, useEffect } from "react";
import { fetchWithAuth } from "../../utils/fetchWithAuth";

export default function AdminQualificationTab() {
  const [qualifications, setQualifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("");
  const [view, setView] = useState("list");
  const [form, setForm] = useState({
    qualificationCode: "",
    qualificationName: "",
    description: "",
    displayOrder: 0,
    isActive: true,
  });
  const [editingId, setEditingId] = useState(null);

  useEffect(() => {
    loadQualifications();
  }, []);

  async function loadQualifications() {
    try {
      setLoading(true);
      const res = await fetchWithAuth("/api/qualifications");
      if (!res.ok) throw new Error("資格一覧の取得に失敗しました。");
      setQualifications(await res.json());
    } catch (e) {
      setMessage(e.message);
      setMessageType("error");
    } finally {
      setLoading(false);
    }
  }

  function handleEdit(qualification) {
    setForm({
      qualificationCode: qualification.qualificationCode,
      qualificationName: qualification.qualificationName,
      description: qualification.description || "",
      displayOrder: qualification.displayOrder || 0,
      isActive: qualification.isActive,
    });
    setEditingId(qualification.id);
    setView("form");
  }

  function handleNewQualification() {
    setForm({
      qualificationCode: "",
      qualificationName: "",
      description: "",
      displayOrder: qualifications.length,
      isActive: true,
    });
    setEditingId(null);
    setView("form");
  }

  function handleChange(e) {
    const { name, value, type, checked } = e.target;
    setForm((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : name === "displayOrder" ? parseInt(value) : value,
    }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setMessage("");
    setMessageType("");

    if (!form.qualificationCode.trim()) {
      setMessage("資格コードは必須です。");
      setMessageType("error");
      return;
    }

    if (!form.qualificationName.trim()) {
      setMessage("資格名は必須です。");
      setMessageType("error");
      return;
    }

    try {
      const method = editingId ? "PUT" : "POST";
      const url = editingId ? `/api/qualifications/${editingId}` : "/api/qualifications";
      const res = await fetchWithAuth(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form),
      });

      if (!res.ok) throw new Error("保存に失敗しました。");
      setMessage("保存しました。");
      setMessageType("success");
      setView("list");
      await loadQualifications();
    } catch (e) {
      setMessage(e.message);
      setMessageType("error");
    }
  }

  async function handleDelete(id) {
    if (!window.confirm("この資格を削除してもよろしいですか？")) return;
    try {
      const res = await fetchWithAuth(`/api/qualifications/${id}`, { method: "DELETE" });
      if (!res.ok) throw new Error("削除に失敗しました。");
      setMessage("資格を削除しました。");
      setMessageType("success");
      await loadQualifications();
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
            <h2>資格管理</h2>
            <button
              type="button"
              onClick={handleNewQualification}
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
                <th style={{ textAlign: "left", padding: "0.5rem" }}>コード</th>
                <th style={{ textAlign: "left", padding: "0.5rem" }}>資格名</th>
                <th style={{ textAlign: "left", padding: "0.5rem" }}>説明</th>
                <th style={{ textAlign: "center", padding: "0.5rem" }}>順序</th>
                <th style={{ textAlign: "center", padding: "0.5rem" }}>状態</th>
                <th style={{ textAlign: "center", padding: "0.5rem" }}>操作</th>
              </tr>
            </thead>
            <tbody>
              {qualifications.length === 0 ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: "center", padding: "1rem", color: "#999" }}>
                    資格はまだ登録されていません。
                  </td>
                </tr>
              ) : (
                qualifications.map((qual) => (
                  <tr key={qual.id} style={{ borderBottom: "1px solid var(--line)" }}>
                    <td style={{ padding: "0.5rem" }}>{qual.qualificationCode}</td>
                    <td style={{ padding: "0.5rem" }}>{qual.qualificationName}</td>
                    <td style={{ padding: "0.5rem", fontSize: "0.9rem" }}>{qual.description || "-"}</td>
                    <td style={{ textAlign: "center", padding: "0.5rem" }}>{qual.displayOrder}</td>
                    <td style={{ textAlign: "center", padding: "0.5rem" }}>{qual.isActive ? "有効" : "無効"}</td>
                    <td style={{ textAlign: "center", padding: "0.5rem" }}>
                      <button
                        type="button"
                        onClick={() => handleEdit(qual)}
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
                        onClick={() => handleDelete(qual.id)}
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
      ) : (
        <div className="card">
          <h2>{editingId ? "資格編集" : "資格新規追加"}</h2>
          <form onSubmit={handleSubmit} style={{ display: "grid", gap: "1rem", marginTop: "1rem" }}>
            <div>
              <label style={{ display: "block", marginBottom: "0.25rem", fontWeight: 600 }}>資格コード</label>
              <input
                type="text"
                name="qualificationCode"
                value={form.qualificationCode}
                onChange={handleChange}
                style={{ width: "100%", padding: "0.5rem", border: "1px solid var(--line)", borderRadius: "4px" }}
              />
            </div>
            <div>
              <label style={{ display: "block", marginBottom: "0.25rem", fontWeight: 600 }}>資格名</label>
              <input
                type="text"
                name="qualificationName"
                value={form.qualificationName}
                onChange={handleChange}
                style={{ width: "100%", padding: "0.5rem", border: "1px solid var(--line)", borderRadius: "4px" }}
              />
            </div>
            <div>
              <label style={{ display: "block", marginBottom: "0.25rem", fontWeight: 600 }}>説明</label>
              <textarea
                name="description"
                value={form.description}
                onChange={handleChange}
                style={{ width: "100%", padding: "0.5rem", border: "1px solid var(--line)", borderRadius: "4px", minHeight: "80px" }}
              />
            </div>
            <div>
              <label style={{ display: "block", marginBottom: "0.25rem", fontWeight: 600 }}>表示順序</label>
              <input
                type="number"
                name="displayOrder"
                value={form.displayOrder}
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

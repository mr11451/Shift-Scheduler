import React from "react";
import { Link } from "react-router-dom";
import AdminShiftEditTab from "./components/AdminTabs/AdminShiftEditTab";

export default function AdminShiftEditPage() {
  return (
    <main className="shell">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1.5rem" }}>
        <div>
          <h1>シフト編集</h1>
          <p className="subtitle">スタッフのシフト予定を編集します。</p>
        </div>
        <Link to="/admin" style={{ textDecoration: "none" }}>
          <button
            type="button"
            style={{
              padding: "0.5rem 1rem",
              backgroundColor: "#999",
              color: "#fff",
              border: "none",
              borderRadius: "8px",
              cursor: "pointer",
            }}
          >
            ← 管理画面へ
          </button>
        </Link>
      </div>

      <section className="admin-content">
        <AdminShiftEditTab />
      </section>
    </main>
  );
}

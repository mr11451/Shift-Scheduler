import React, { useContext, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import AdminStaffTab from "./components/AdminTabs/AdminStaffTab";
import AdminQualificationTab from "./components/AdminTabs/AdminQualificationTab";
import AdminShiftTypeTab from "./components/AdminTabs/AdminShiftTypeTab";
import AdminSystemSettingTab from "./components/AdminTabs/AdminSystemSettingTab";
import AdminAutoShiftRuleTab from "./components/AdminTabs/AdminAutoShiftRuleTab";
import { AuthContext } from "./context/AuthContext";
import { fetchWithAuth } from "./utils/fetchWithAuth";

export default function AdminPage() {
  const [activeTab, setActiveTab] = useState("staff");
  const { auth } = useContext(AuthContext);
  const [loginProfile, setLoginProfile] = useState(null);

  useEffect(() => {
    let mounted = true;

    async function loadLoginProfile() {
      if (!auth?.staffId || !auth?.token || auth.token === 'null' || auth.token === 'undefined') {
        setLoginProfile(null);
        return;
      }

      try {
        const response = await fetchWithAuth(`/api/staffs/${auth.staffId}`);
        if (!response.ok) {
          return;
        }

        const staff = await response.json();
        if (!mounted) {
          return;
        }

        setLoginProfile({
          staffName: staff.staffName,
          roleLevel: staff.roleLevel,
        });
      } catch {
        // Keep existing auth display when profile fetch fails.
      }
    }

    loadLoginProfile();

    return () => {
      mounted = false;
    };
  }, [auth?.staffId, auth?.token]);

  const roleLabelMap = {
    MASTER: "マスター",
    CHIEF: "チーフ",
    MEMBER: "メンバー",
  };

  const loginSource = loginProfile || auth;

  const loginSummary = loginSource
    ? `${loginSource.staffName || "不明"} / ${roleLabelMap[loginSource.roleLevel] || loginSource.roleLevel || "不明"}`
    : null;

  const tabs = [
    { id: "staff", label: "スタッフ管理" },
    { id: "qualification", label: "資格管理" },
    { id: "shiftType", label: "シフト種類管理" },
    { id: "autoShiftRule", label: "自動生成ルール" },
    { id: "systemSetting", label: "システム設定" },
  ];

  const renderTabContent = () => {
    switch (activeTab) {
      case "staff":
        return <AdminStaffTab />;
      case "qualification":
        return <AdminQualificationTab />;
      case "shiftType":
        return <AdminShiftTypeTab />;
      case "autoShiftRule":
        return <AdminAutoShiftRuleTab onCancel={() => setActiveTab("staff")} />;
      case "systemSetting":
        return <AdminSystemSettingTab onCancel={() => setActiveTab("staff")} />;
      default:
        return <AdminStaffTab />;
    }
  };

  return (
    <main className="shell">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1.5rem" }}>
        <div>
          <h1>管理画面</h1>
          <p className="subtitle">シフト管理システムの管理機能を設定します。</p>
          {loginSummary && (
            <p style={{ margin: "-0.75rem 0 0", fontSize: "0.8rem", color: "#6b7280" }}>
              ログイン中: {loginSummary}
            </p>
          )}
        </div>
        <Link to="/member" style={{ textDecoration: "none" }}>
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
            ← 会員ページへ
          </button>
        </Link>
      </div>

      <div style={{ marginBottom: "1.5rem" }}>
        <div className="admin-tabs">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`admin-tab-button ${activeTab === tab.id ? "is-active" : ""}`}
            >
              {tab.label}
            </button>
          ))}
        </div>
        <div style={{ marginTop: "1rem" }}>
          <Link to="/admin/shifts" style={{ textDecoration: "none" }}>
            <button
              type="button"
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
              → シフト編集画面へ
            </button>
          </Link>
        </div>
      </div>

      <section className="admin-content">{renderTabContent()}</section>
    </main>
  );
}

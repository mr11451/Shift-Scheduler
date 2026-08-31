import React, { useContext } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';

// Whether a stored auth token value looks like a real (non-empty, non-placeholder) token.
function hasUsableToken(token) {
  return typeof token === 'string' && token.trim() !== '' && token !== 'null' && token !== 'undefined';
}

/**
 * ProtectedRoute component that checks authentication and optionally checks role permissions
 * @param {Object} props
 * @param {JSX.Element} props.element - The component to render if authenticated
 * @param {Array<string>} props.allowedRoles - Array of role levels allowed to access (optional)
 * @returns {JSX.Element}
 */
export default function ProtectedRoute({ element, allowedRoles }) {
  const { auth, loading } = useContext(AuthContext);
  const location = useLocation();

  if (loading) {
    return <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
      <p>読み込み中...</p>
    </div>;
  }

  if (!auth || !hasUsableToken(auth.token)) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  }

  if (allowedRoles) {
    const normalizedRole = String(auth.roleLevel || "").toUpperCase();
    const allowedRoleSet = new Set(allowedRoles.map((role) => String(role).toUpperCase()));
    if (!allowedRoleSet.has(normalizedRole)) {
      return (
        <div style={{ padding: "2rem", fontFamily: "sans-serif" }}>
          <h2>アクセスできません</h2>
          <p>この画面には管理者権限が必要です。</p>
          <p style={{ color: "#666" }}>現在の権限: {auth.roleLevel || "未設定"}</p>
        </div>
      );
    }
  }

  return element;
}

import React from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import LoginPage from "./pages/LoginPage";
import PasswordResetPage from "./pages/PasswordResetPage";
import MemberPage from "./MemberPage";
import AdminPage from "./AdminPage";
import AdminShiftEditPage from "./AdminShiftEditPage";

// Top-level router: login, password reset, member calendar, and admin screens.
export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/" element={<LoginPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/password-reset/:staffId/:token" element={<PasswordResetPage />} />
        <Route path="/member" element={<ProtectedRoute element={<MemberPage />} />} />
        <Route path="/admin" element={<ProtectedRoute element={<AdminPage />} allowedRoles={["CHIEF", "MASTER"]} />} />
        <Route path="/admin/shifts" element={<ProtectedRoute element={<AdminShiftEditPage />} allowedRoles={["CHIEF", "MASTER"]} />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  );
}

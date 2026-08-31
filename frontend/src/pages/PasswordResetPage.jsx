import React, { useState } from "react";
import { Link, useParams } from "react-router-dom";

// Confirms a password reset using the token/verification code from the emailed link.
export default function PasswordResetPage() {
  const { staffId, token } = useParams();
  const [verificationCode, setVerificationCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  // Validate the new password confirmation and submit the reset to the API.
  async function handleSubmit(event) {
    event.preventDefault();
    setMessage("");
    setError("");
    if (newPassword !== confirmPassword) {
      setError("新しいパスワードが一致しません。");
      return;
    }

    setLoading(true);
    try {
      const response = await fetch(`/api/password-resets/${encodeURIComponent(staffId)}/${encodeURIComponent(token)}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ verificationCode, newPassword }),
      });
      if (!response.ok) {
        throw new Error((await response.text()) || "パスワードの変更に失敗しました。");
      }
      setMessage("パスワードを変更しました。新しいパスワードでログインしてください。");
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-container">
      <div className="login-card">
        <h1>パスワード変更</h1>
        <p className="login-subtitle">メールに届いた確認コードを入力してください。</p>
        {message && <div className="success-message">{message}</div>}
        {error && <div className="error-message">{error}</div>}
        {!message && (
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="verificationCode">確認コード</label>
              <input id="verificationCode" value={verificationCode} onChange={(event) => setVerificationCode(event.target.value)} inputMode="numeric" maxLength="6" required disabled={loading} />
            </div>
            <div className="form-group">
              <label htmlFor="newPassword">新しいパスワード</label>
              <input id="newPassword" type="password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} minLength="8" required disabled={loading} />
            </div>
            <div className="form-group">
              <label htmlFor="confirmPassword">新しいパスワード（確認）</label>
              <input id="confirmPassword" type="password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} minLength="8" required disabled={loading} />
            </div>
            <button type="submit" disabled={loading} className="login-button">{loading ? "変更中..." : "パスワードを変更"}</button>
          </form>
        )}
        <p className="login-subtitle"><Link to="/login">ログイン画面へ</Link></p>
      </div>
    </div>
  );
}
import React, { useContext, useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import './LoginPage.css';

function resolveRedirectPath(rawFrom) {
  if (typeof rawFrom !== 'string' || !rawFrom.startsWith('/')) {
    return '/member';
  }
  if (rawFrom === '/login' || rawFrom === '/') {
    return '/member';
  }
  return rawFrom;
}

export default function LoginPage() {
  const [staffCode, setStaffCode] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const location = useLocation();
  const { auth, loading: authLoading, login } = useContext(AuthContext);
  const redirectPath = resolveRedirectPath(location.state?.from);

  useEffect(() => {
    if (!authLoading && auth) {
      navigate(redirectPath, { replace: true });
    }
  }, [auth, authLoading, navigate, redirectPath]);

  async function handleLogin(e) {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await fetch('/api/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ staffCode, password })
      });

      if (response.ok) {
        const data = await response.json();
        login({
          token: data.token,
          staffId: data.staffId,
          staffCode: data.staffCode,
          staffName: data.staffName,
          roleLevel: data.roleLevel,
        });
        
        // Redirect back to originally requested page when available.
        navigate(redirectPath, { replace: true });
      } else {
        const errorData = await response.text();
        setError(errorData || 'ログインに失敗しました。');
      }
    } catch (err) {
      setError('ネットワークエラーが発生しました。');
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-container">
      <div className="login-card">
        <h1>シフト管理システム</h1>
        <p className="login-subtitle">スタッフコードとパスワードでログイン</p>

        <form onSubmit={handleLogin}>
          {error && <div className="error-message">{error}</div>}

          <div className="form-group">
            <label htmlFor="staffCode">スタッフコード</label>
            <input
              type="text"
              id="staffCode"
              value={staffCode}
              onChange={(e) => setStaffCode(e.target.value)}
              placeholder="STF-00001"
              required
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">パスワード</label>
            <input
              type="password"
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="パスワード"
              required
              disabled={loading}
            />
          </div>

          <button type="submit" disabled={loading} className="login-button">
            {loading ? 'ログイン中...' : 'ログイン'}
          </button>
        </form>
      </div>
    </div>
  );
}

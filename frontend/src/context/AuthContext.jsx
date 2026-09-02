import React, { createContext, useState, useEffect } from 'react';

export const AuthContext = createContext();
const INACTIVITY_TIMEOUT_MS = 30 * 60 * 1000;

// Whether a stored auth token value looks like a real (non-empty, non-placeholder) token.
function hasUsableToken(token) {
  return typeof token === 'string' && token.trim() !== '' && token !== 'null' && token !== 'undefined';
}

// Provides the current auth session (from localStorage) plus login/logout actions to the app.
export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Check if user is already logged in (token in localStorage)
    const token = localStorage.getItem('authToken');
    if (hasUsableToken(token)) {
      setAuth({
        token,
        staffId: localStorage.getItem('staffId'),
        staffCode: localStorage.getItem('staffCode'),
        staffName: localStorage.getItem('staffName'),
        roleLevel: localStorage.getItem('roleLevel')
      });
    } else {
      localStorage.removeItem('authToken');
      localStorage.removeItem('staffId');
      localStorage.removeItem('staffCode');
      localStorage.removeItem('staffName');
      localStorage.removeItem('roleLevel');
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    if (!auth) return undefined;

    let timeoutId;
    const resetInactivityTimer = () => {
      window.clearTimeout(timeoutId);
      timeoutId = window.setTimeout(() => {
        logout();
        window.location.href = '/login?reason=timeout';
      }, INACTIVITY_TIMEOUT_MS);
    };
    const activityEvents = ['click', 'keydown', 'pointermove', 'touchstart'];

    activityEvents.forEach((eventName) => window.addEventListener(eventName, resetInactivityTimer));
    resetInactivityTimer();

    return () => {
      window.clearTimeout(timeoutId);
      activityEvents.forEach((eventName) => window.removeEventListener(eventName, resetInactivityTimer));
    };
  }, [auth]);

  // Persist the authenticated session to localStorage and update context state.
  function login(authData) {
    if (!hasUsableToken(authData?.token)) {
      throw new Error('トークンが無効です。');
    }

    localStorage.setItem('authToken', authData.token);
    localStorage.setItem('staffId', authData.staffId);
    localStorage.setItem('staffCode', authData.staffCode);
    localStorage.setItem('staffName', authData.staffName);
    localStorage.setItem('roleLevel', authData.roleLevel);
    setAuth(authData);
  }

  // Clear the persisted session and reset context state.
  function logout() {
    const token = localStorage.getItem('authToken');
    if (token) {
      fetch('/api/logout', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        keepalive: true,
      }).catch(() => {});
    }
    localStorage.removeItem('authToken');
    localStorage.removeItem('staffId');
    localStorage.removeItem('staffCode');
    localStorage.removeItem('staffName');
    localStorage.removeItem('roleLevel');
    setAuth(null);
  }

  return (
    <AuthContext.Provider value={{ auth, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

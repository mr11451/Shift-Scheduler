import React, { createContext, useState, useEffect } from 'react';

export const AuthContext = createContext();

function hasUsableToken(token) {
  return typeof token === 'string' && token.trim() !== '' && token !== 'null' && token !== 'undefined';
}

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

  function logout() {
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

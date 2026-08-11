/**
 * Custom fetch wrapper that automatically adds JWT token to requests
 */
export async function fetchWithAuth(url, options = {}) {
  const { redirectOnUnauthorized = true, ...fetchOptions } = options;
  const token = localStorage.getItem('authToken');
  
  const headers = {
    ...fetchOptions.headers,
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(url, {
    ...fetchOptions,
    headers,
  });

  // If unauthorized, redirect to login
  if (response.status === 401 && redirectOnUnauthorized) {
    localStorage.removeItem('authToken');
    localStorage.removeItem('staffId');
    localStorage.removeItem('staffCode');
    localStorage.removeItem('staffName');
    localStorage.removeItem('roleLevel');
    window.location.href = '/login';
  }

  return response;
}

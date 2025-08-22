import { createContext, useState, useEffect } from 'react';

export const AuthContext = createContext();

/**
 * Authentication Provider Component
 * Manages the authentication token state across the application
 * Persists the token in localStorage for session persistence
 *
 * @param {object} props - Component properties
 * @param {React.ReactNode} props.children - Child components
 */
export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(() => localStorage.getItem('token'));

  useEffect(() => {
    if (token) {
      localStorage.setItem('token', token);
    } else {
      localStorage.removeItem('token');
    }
  }, [token]);

  return (
    <AuthContext.Provider value={{ token, setToken }}>
      {children}
    </AuthContext.Provider>
  );
};
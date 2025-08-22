import { createContext, useState } from 'react';

export const SessionMapContext = createContext();

/**
 * Session Map Provider Component
 * Manages the relationship between game IDs and session IDs
 * Useful for navigating between related game and session screens
 *
 * @param {object} props - Component properties
 * @param {React.ReactNode} props.children - Child components
 */
export const SessionMapProvider = ({ children }) => {
  const [sessionMap, setSessionMap] = useState({});

  const addSessionMapping = (gameId, sessionId) => {
    setSessionMap(prev => ({ ...prev, [sessionId]: gameId }));
  };

  const getGameIdBySession = (sessionId) => {
    return sessionMap[sessionId];
  };

  const getSessionIdByGame = (gameId) => {
    const entry = Object.entries(sessionMap).find(([_sid, gid]) => gid === gameId);
    return entry?.[0] || null;
  };

  return (
    <SessionMapContext.Provider
      value={{ sessionMap, addSessionMapping, getGameIdBySession, getSessionIdByGame }}
    >
      {children}
    </SessionMapContext.Provider>
  );
};
import { useContext } from 'react';
import { SessionMapContext } from './SessionMapContext';

/**
 * Custom hook to access the SessionMapContext
 * Provides a convenient way to use session mapping functionality
 * from any component without directly importing the context
 *
 * @returns {object} - The session map context value
 * @returns {object} returns.sessionMap - The current session-to-game mappings
 * @returns {function} returns.addSessionMapping - Function to add a new mapping
 * @returns {function} returns.getGameIdBySession - Function to get game ID from session ID
 * @returns {function} returns.getSessionIdByGame - Function to get session ID from game ID
 */
export const useSessionMap = () => useContext(SessionMapContext);
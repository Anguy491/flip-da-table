import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login.jsx';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Lobby from './pages/Lobby';
import UnoPlayScreen from './pages/UnoPlayScreen';
import DVCPlayScreen from './pages/DVCPlayScreen';
import SessionSummary from './pages/SessionSummary';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/login" />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/lobby/:sessionid" element={<Lobby />} />
        <Route path="/unoplayscreen/:sessionid" element={<UnoPlayScreen />} />
        <Route path="/dvcplayscreen/:sessionid" element={<DVCPlayScreen />} />
        <Route path="/sessionsum/:sessionid" element={<SessionSummary />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App
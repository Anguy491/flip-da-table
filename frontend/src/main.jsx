import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { AuthProvider } from './context/AuthContext'
import { SessionMapProvider } from './context/SessionMapContext';
import App from './App.jsx'
import './index.css'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <AuthProvider>
      <SessionMapProvider>
        <App />
      </SessionMapProvider>
    </AuthProvider>
  </StrictMode>,
)
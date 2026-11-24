import { Navigate, Route, Routes } from 'react-router-dom'

import { AppLayout } from './components/layout/AppLayout'
import { useAuth } from './auth/authContext'
import { Account } from './pages/Account'
import { Alerts } from './pages/Alerts'
import { Dashboard } from './pages/Dashboard'
import { Markets } from './pages/Markets'
import { PairDetails } from './pages/PairDetails'
import { Settings } from './pages/Settings'

function PrivateAlerts() {
  const { userId, loading } = useAuth()
  if (loading) return <p role="status">Restoring your session...</p>
  return userId ? <Alerts /> : <Account />
}

function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<Dashboard />} />
        <Route path="markets" element={<Markets />} />
        <Route path="markets/:symbol" element={<PairDetails />} />
        <Route path="account" element={<Account />} />
        <Route path="alerts" element={<PrivateAlerts />} />
        <Route path="settings" element={<Settings />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}

export default App

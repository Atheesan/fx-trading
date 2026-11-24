import { useState, type FormEvent } from 'react'
import { authenticate, logout } from '../api/client'
import { useAuth } from '../auth/authContext'

export function Account() {
  const { userId, loading } = useAuth()
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true); setError(null)
    try { await authenticate(mode, email, password); setPassword('') }
    catch { setError('Authentication failed. Check your details and try again.') }
    finally { setBusy(false) }
  }
  if (loading) return <p role="status">Restoring your session...</p>
  if (userId) return <section className="space-y-4"><h1 className="text-2xl font-bold">Your account</h1><p>Your watchlist, alerts, and display settings are saved to your account.</p><button disabled={busy} onClick={() => {
    setBusy(true); setError(null)
    void logout().catch(() => setError('Sign out failed. Please try again.')).finally(() => setBusy(false))
  }}>Sign out</button>{error && <p role="alert">{error}</p>}</section>
  return <section className="mx-auto max-w-md space-y-5"><h1 className="text-2xl font-bold">{mode === 'login' ? 'Sign in' : 'Create account'}</h1>
    <form onSubmit={event => void submit(event)} className="space-y-4">
      <label className="block">Email<input className="block w-full rounded border p-2" type="email" autoComplete="username" required maxLength={254} value={email} onChange={e => setEmail(e.target.value)} /></label>
      <label className="block">Password<input className="block w-full rounded border p-2" type="password" autoComplete={mode === 'login' ? 'current-password' : 'new-password'} required minLength={mode === 'register' ? 12 : 1} maxLength={128} value={password} onChange={e => setPassword(e.target.value)} /></label>
      {mode === 'register' && <p>Use at least 12 characters.</p>}
      {error && <p role="alert">{error}</p>}
      <button className="rounded bg-cyan-700 px-4 py-2 text-white" disabled={busy}>{busy ? 'Please wait...' : mode === 'login' ? 'Sign in' : 'Register'}</button>
    </form>
    <button onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError(null); setPassword('') }}>{mode === 'login' ? 'Create an account' : 'Use an existing account'}</button>
  </section>
}

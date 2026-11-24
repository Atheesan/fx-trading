export const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
).replace(/\/$/, '')

export interface AuthSession { accessToken: string; userId: string }
let activeUser: string | null = null
export function currentUserId() { return activeUser }
let accessToken: string | null = null
let csrf: Promise<{ token: string; headerName: string }> | null = null
let refreshing: Promise<AuthSession> | null = null
let generation = 0
const listeners = new Set<(session: AuthSession | null) => void>()

export function subscribeSession(listener: (session: AuthSession | null) => void) {
  listeners.add(listener)
  return () => { listeners.delete(listener) }
}
function updateSession(session: AuthSession | null) {
  if (activeUser !== (session?.userId ?? null)) generation++
  activeUser = session?.userId ?? null
  accessToken = session?.accessToken ?? null
  listeners.forEach(listener => listener(session))
}
async function csrfHeaders(): Promise<Record<string, string>> {
  csrf ??= fetch(`${API_BASE_URL}/api/v1/auth/csrf`, { credentials: 'include' })
    .then(async response => {
      if (!response.ok) throw new Error('Unable to establish a secure request')
      return response.json() as Promise<{ token: string; headerName: string }>
    }).catch(error => { csrf = null; throw error })
  const value = await csrf
  return { [value.headerName]: value.token }
}
export function refreshSession(): Promise<AuthSession> {
  if (!refreshing) {
    const currentGeneration = generation
    refreshing = request<AuthSession>('/api/v1/auth/refresh', 'POST', undefined, undefined, false)
      .then(session => {
        if (generation !== currentGeneration) throw new Error('Session changed')
        updateSession(session)
        return session
      }).catch(error => {
        if (generation === currentGeneration) updateSession(null)
        throw error
      }).finally(() => { refreshing = null })
  }
  return refreshing
}
export async function authenticate(mode: 'login' | 'register', email: string, password: string) {
  if (refreshing) await refreshing.catch(() => undefined)
  generation++
  const session = await request<AuthSession>(`/api/v1/auth/${mode}`, 'POST', { email, password }, undefined, false)
  updateSession(session)
}
export async function logout() {
  if (refreshing) await refreshing.catch(() => undefined)
  await request<void>('/api/v1/auth/logout', 'POST', undefined, undefined, false)
  generation++
  updateSession(null)
}
async function request<T>(path: string, method = 'GET', body?: unknown, signal?: AbortSignal, retry = true): Promise<T> {
  const requestGeneration = generation
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (accessToken && !path.startsWith('/api/v1/auth/')) headers.Authorization = `Bearer ${accessToken}`
  if (method !== 'GET') Object.assign(headers, await csrfHeaders())
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (requestGeneration !== generation) throw new Error('Session changed')
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method, headers, signal,
    ...(method !== 'GET' ? { credentials: 'include' as const } : {}),
    ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
  })
  if (requestGeneration !== generation) throw new Error('Session changed')
  if (response.status === 401 && retry && !path.startsWith('/api/v1/auth/')) {
    await refreshSession()
    return request<T>(path, method, body, signal, false)
  }
  if (!response.ok) throw new Error(path.startsWith('/api/v1/auth/') ? 'Authentication failed' : `Request failed with status ${response.status}`)
  if (response.status === 204 || method === 'DELETE') return undefined as T
  return response.json() as Promise<T>
}
export const apiClient = {
  get: <T>(path: string, signal?: AbortSignal) => request<T>(path, 'GET', undefined, signal),
  post: <T>(path: string, body: unknown) => request<T>(path, 'POST', body),
  put: <T>(path: string, body: unknown) => request<T>(path, 'PUT', body),
  delete: (path: string) => request<void>(path, 'DELETE'),
}

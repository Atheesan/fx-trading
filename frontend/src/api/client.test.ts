import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

function response(data: unknown, status = 200) {
  return { ok: status >= 200 && status < 300, status, json: async () => data }
}
beforeEach(() => { vi.resetModules(); localStorage.clear(); sessionStorage.clear() })
afterEach(() => vi.unstubAllGlobals())

describe('authentication client', () => {
  it('keeps tokens in memory and attaches bearer and CSRF headers', async () => {
    const fetchMock = vi.fn((url: string) => Promise.resolve(response(url.endsWith('/csrf')
      ? { token: 'csrf-token', headerName: 'X-XSRF-TOKEN' }
      : url.endsWith('/login') ? { accessToken: 'access-secret', userId: 'alice' } : { theme: 'dark' })))
    vi.stubGlobal('fetch', fetchMock)
    const { authenticate, apiClient } = await import('./client')
    await authenticate('login', 'alice@example.com', 'long-password')
    await apiClient.put('/api/v1/settings', { theme: 'dark' })
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/login'), expect.objectContaining({
      credentials: 'include', headers: expect.objectContaining({ 'X-XSRF-TOKEN': 'csrf-token' }),
    }))
    expect(fetchMock).toHaveBeenLastCalledWith(expect.stringContaining('/settings'), expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer access-secret' }),
    }))
    expect(localStorage.length).toBe(0); expect(sessionStorage.length).toBe(0)
  })
  it('shares concurrent refresh requests and retries unauthorized requests once', async () => {
    let reads = 0
    const fetchMock = vi.fn((url: string) => Promise.resolve(response(url.endsWith('/csrf')
      ? { token: 'csrf', headerName: 'X-XSRF-TOKEN' }
      : url.endsWith('/refresh') ? { accessToken: 'fresh', userId: 'alice' }
        : { theme: 'dark' }, url.endsWith('/settings') && reads++ < 2 ? 401 : 200)))
    vi.stubGlobal('fetch', fetchMock)
    const { apiClient } = await import('./client')
    await Promise.all([apiClient.get('/api/v1/settings'), apiClient.get('/api/v1/settings')])
    expect(fetchMock.mock.calls.filter(([url]) => url.endsWith('/refresh'))).toHaveLength(1)
    expect(fetchMock.mock.calls.filter(([url]) => url.endsWith('/settings'))).toHaveLength(4)
  })
  it('clears memory on logout and rejects writes from an old account', async () => {
    let user = 'alice'
    const fetchMock = vi.fn((url: string) => Promise.resolve(response(url.endsWith('/csrf')
      ? { token: 'csrf', headerName: 'X-XSRF-TOKEN' } : { accessToken: `${user}-token`, userId: user }, url.endsWith('/logout') ? 204 : 200)))
    vi.stubGlobal('fetch', fetchMock)
    const client = await import('./client')
    await client.authenticate('login', 'alice@example.com', 'long-password')
    const { createBackendWatchlistRepository } = await import('../watchlist/backendWatchlistRepository')
    const oldRepository = createBackendWatchlistRepository()
    await client.logout()
    expect(client.currentUserId()).toBeNull()
    user = 'bob'
    await client.authenticate('login', 'bob@example.com', 'long-password')
    await expect(oldRepository.save({ symbols: ['EURUSD'], selectedSymbol: 'EURUSD' })).rejects.toThrow('Session changed')
    expect(fetchMock.mock.calls.some(([url]) => url.endsWith('/watchlist'))).toBe(false)
  })
})

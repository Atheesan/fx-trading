import { apiClient, currentUserId } from '../api/client'
import type { WatchlistRepository, WatchlistState } from './watchlist'

// Serialize writes so rapid reordering cannot persist requests out of order.
export function createBackendWatchlistRepository(): WatchlistRepository {
  const owner = currentUserId()
  let pending = Promise.resolve()
  return {
    load: () => apiClient.get<WatchlistState>('/api/v1/watchlist'),
    save(state) {
      pending = pending.catch(() => undefined).then(async () => {
        if (!owner || currentUserId() !== owner) throw new Error('Session changed')
        await apiClient.put('/api/v1/watchlist', state) })
      return pending
    },
  }
}

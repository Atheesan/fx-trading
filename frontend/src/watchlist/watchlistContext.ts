import { createContext, useContext } from 'react'

import type { WatchlistState } from './watchlist'

export interface WatchlistContextValue {
  state: WatchlistState
  isHydrated: boolean
  persistenceError: string | null
  add: (symbol: string) => void
  remove: (symbol: string) => void
  move: (symbol: string, offset: -1 | 1) => void
  select: (symbol: string) => void
}

export const WatchlistContext = createContext<WatchlistContextValue | null>(null)

export function useWatchlist() {
  const context = useContext(WatchlistContext)
  if (context === null) {
    throw new Error('useWatchlist must be used within a WatchlistProvider')
  }
  return context
}

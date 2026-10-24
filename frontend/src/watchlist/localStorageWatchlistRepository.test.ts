import { beforeEach, describe, expect, it } from 'vitest'

import {
  LocalStorageWatchlistRepository,
  WATCHLIST_STORAGE_KEY,
} from './localStorageWatchlistRepository'

describe('LocalStorageWatchlistRepository', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('returns null when no valid watchlist has been stored', async () => {
    const repository = new LocalStorageWatchlistRepository(localStorage)

    expect(await repository.load()).toBeNull()

    localStorage.setItem(WATCHLIST_STORAGE_KEY, '{invalid json')
    expect(await repository.load()).toBeNull()
  })

  it('saves and loads a sanitized watchlist', async () => {
    const repository = new LocalStorageWatchlistRepository(localStorage)

    await repository.save({
      symbols: ['eur/usd', 'EURUSD', 'usdjpy'],
      selectedSymbol: 'usdjpy',
    })

    await expect(repository.load()).resolves.toEqual({
      symbols: ['EURUSD', 'USDJPY'],
      selectedSymbol: 'USDJPY',
    })
  })

  it('falls back to the first pair when the stored selection is unavailable', async () => {
    localStorage.setItem(WATCHLIST_STORAGE_KEY, JSON.stringify({
      symbols: ['EURUSD', 'GBPUSD'],
      selectedSymbol: 'USDJPY',
    }))
    const repository = new LocalStorageWatchlistRepository(localStorage)

    await expect(repository.load()).resolves.toEqual({
      symbols: ['EURUSD', 'GBPUSD'],
      selectedSymbol: 'EURUSD',
    })
  })
})

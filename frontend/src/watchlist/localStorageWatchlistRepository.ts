import {
  isCurrencyPairSymbol,
  sanitizeWatchlistState,
  type WatchlistRepository,
  type WatchlistState,
} from './watchlist'

export const WATCHLIST_STORAGE_KEY = 'fxai-watchlist-v1'

function isStoredWatchlist(value: unknown): value is WatchlistState {
  if (typeof value !== 'object' || value === null) {
    return false
  }

  const candidate = value as Partial<WatchlistState>
  return Array.isArray(candidate.symbols)
    && candidate.symbols.every(isCurrencyPairSymbol)
    && (candidate.selectedSymbol === null
      || isCurrencyPairSymbol(candidate.selectedSymbol))
}

export class LocalStorageWatchlistRepository implements WatchlistRepository {
  private readonly storage: Storage
  private readonly storageKey: string

  constructor(
    storage: Storage,
    storageKey = WATCHLIST_STORAGE_KEY,
  ) {
    this.storage = storage
    this.storageKey = storageKey
  }

  async load(): Promise<WatchlistState | null> {
    const serialized = this.storage.getItem(this.storageKey)
    if (serialized === null) {
      return null
    }

    try {
      const parsed: unknown = JSON.parse(serialized)
      return isStoredWatchlist(parsed)
        ? sanitizeWatchlistState(parsed)
        : null
    } catch {
      return null
    }
  }

  async save(state: WatchlistState): Promise<void> {
    this.storage.setItem(
      this.storageKey,
      JSON.stringify(sanitizeWatchlistState(state)),
    )
  }
}

export const browserWatchlistRepository = new LocalStorageWatchlistRepository(
  window.localStorage,
)

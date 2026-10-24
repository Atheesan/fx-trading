export const DEFAULT_WATCHLIST_SYMBOLS = [
  'EURUSD',
  'GBPUSD',
  'USDJPY',
  'AUDUSD',
  'NZDUSD',
] as const

export interface WatchlistState {
  symbols: string[]
  selectedSymbol: string | null
}

export interface WatchlistRepository {
  load(): Promise<WatchlistState | null>
  save(state: WatchlistState): Promise<void>
}

export const defaultWatchlistState: WatchlistState = {
  symbols: [...DEFAULT_WATCHLIST_SYMBOLS],
  selectedSymbol: DEFAULT_WATCHLIST_SYMBOLS[0],
}

export function normalizeSymbol(symbol: string) {
  return symbol.trim().toUpperCase().replaceAll('/', '').replaceAll('-', '')
}

export function isCurrencyPairSymbol(symbol: unknown): symbol is string {
  return typeof symbol === 'string' && /^[A-Z]{6}$/.test(symbol)
}

export function sanitizeWatchlistState(
  state: WatchlistState,
): WatchlistState {
  const symbols = [...new Set(
    state.symbols.map(normalizeSymbol).filter(isCurrencyPairSymbol),
  )]
  const selectedSymbol = state.selectedSymbol === null
    ? null
    : normalizeSymbol(state.selectedSymbol)

  return {
    symbols,
    selectedSymbol: selectedSymbol && symbols.includes(selectedSymbol)
      ? selectedSymbol
      : (symbols[0] ?? null),
  }
}

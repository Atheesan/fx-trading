import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'

import { browserWatchlistRepository } from './localStorageWatchlistRepository'
import {
  defaultWatchlistState,
  isCurrencyPairSymbol,
  normalizeSymbol,
  sanitizeWatchlistState,
  type WatchlistRepository,
  type WatchlistState,
} from './watchlist'
import { WatchlistContext } from './watchlistContext'

interface WatchlistProviderProps {
  children: ReactNode
  repository?: WatchlistRepository
}

export function WatchlistProvider({
  children,
  repository = browserWatchlistRepository,
}: WatchlistProviderProps) {
  const [state, setState] = useState<WatchlistState>(defaultWatchlistState)
  const [isHydrated, setIsHydrated] = useState(false)
  const [persistenceError, setPersistenceError] = useState<string | null>(null)

  useEffect(() => {
    let active = true

    repository.load()
      .then((storedState) => {
        if (active && storedState !== null) {
          setState(sanitizeWatchlistState(storedState))
        }
      })
      .catch(() => {
        if (active) {
          setPersistenceError('Watchlist changes cannot be loaded from this browser.')
        }
      })
      .finally(() => {
        if (active) {
          setIsHydrated(true)
        }
      })

    return () => {
      active = false
    }
  }, [repository])

  useEffect(() => {
    if (!isHydrated) {
      return
    }

    repository.save(state)
      .then(() => setPersistenceError(null))
      .catch(() => {
        setPersistenceError('Watchlist changes cannot be saved in this browser.')
      })
  }, [isHydrated, repository, state])

  const add = useCallback((symbol: string) => {
    const normalizedSymbol = normalizeSymbol(symbol)
    if (!isCurrencyPairSymbol(normalizedSymbol)) {
      return
    }
    setState((current) => current.symbols.includes(normalizedSymbol)
      ? current
      : {
          symbols: [...current.symbols, normalizedSymbol],
          selectedSymbol: current.selectedSymbol ?? normalizedSymbol,
        })
  }, [])

  const remove = useCallback((symbol: string) => {
    const normalizedSymbol = normalizeSymbol(symbol)
    setState((current) => {
      const removedIndex = current.symbols.indexOf(normalizedSymbol)
      if (removedIndex < 0) {
        return current
      }

      const symbols = current.symbols.filter(
        (candidate) => candidate !== normalizedSymbol,
      )
      return {
        symbols,
        selectedSymbol: current.selectedSymbol === normalizedSymbol
          ? (symbols[Math.min(removedIndex, symbols.length - 1)] ?? null)
          : current.selectedSymbol,
      }
    })
  }, [])

  const move = useCallback((symbol: string, offset: -1 | 1) => {
    const normalizedSymbol = normalizeSymbol(symbol)
    setState((current) => {
      const currentIndex = current.symbols.indexOf(normalizedSymbol)
      const targetIndex = currentIndex + offset
      if (currentIndex < 0 || targetIndex < 0 || targetIndex >= current.symbols.length) {
        return current
      }

      const symbols = [...current.symbols]
      ;[symbols[currentIndex], symbols[targetIndex]] = [
        symbols[targetIndex],
        symbols[currentIndex],
      ]
      return { ...current, symbols }
    })
  }, [])

  const select = useCallback((symbol: string) => {
    const normalizedSymbol = normalizeSymbol(symbol)
    setState((current) => current.symbols.includes(normalizedSymbol)
      ? { ...current, selectedSymbol: normalizedSymbol }
      : current)
  }, [])

  const value = useMemo(() => ({
    state,
    isHydrated,
    persistenceError,
    add,
    remove,
    move,
    select,
  }), [add, isHydrated, move, persistenceError, remove, select, state])

  return (
    <WatchlistContext.Provider value={value}>
      {children}
    </WatchlistContext.Provider>
  )
}

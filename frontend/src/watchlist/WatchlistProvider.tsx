import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  useRef,
  type ReactNode,
} from 'react'

import { createBackendWatchlistRepository } from './backendWatchlistRepository'
import { useAuth } from '../auth/authContext'
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
  repository: suppliedRepository,
}: WatchlistProviderProps) {
  const { userId } = useAuth()
  const repository = useMemo(() => suppliedRepository ?? (userId ? createBackendWatchlistRepository() : null), [suppliedRepository, userId])
  const loadedRepository = useRef<WatchlistRepository | null>(null)
  const loadedState = useRef<WatchlistState | null>(null)
  const [state, setState] = useState<WatchlistState>(repository ? defaultWatchlistState : { symbols: [], selectedSymbol: null })
  const [isHydrated, setIsHydrated] = useState(!repository)
  const [persistenceError, setPersistenceError] = useState<string | null>(repository ? null : 'Sign in to save your watchlist.')

  useEffect(() => {
    loadedRepository.current = null
    let active = true
    if (!repository) return

    repository.load()
      .then((storedState) => {
        if (active && storedState !== null) {
          loadedRepository.current = repository
          const next = sanitizeWatchlistState(storedState)
          loadedState.current = next
          setState(next)
        }
      })
      .catch(() => {
        if (active) {
          setPersistenceError('Your watchlist could not be loaded. Reload to try again.')
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
    if (!isHydrated || !repository || loadedRepository.current !== repository || state === loadedState.current || persistenceError) {
      return
    }

    repository.save(state)
      .then(() => setPersistenceError(null))
      .catch(() => {
        setPersistenceError('Watchlist changes could not be saved. Reload to try again.')
      })
  }, [isHydrated, repository, state, persistenceError])

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

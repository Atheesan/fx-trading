import { CloudOff, Plus, Rows3 } from 'lucide-react'
import { useState, type FormEvent } from 'react'

import type { CurrencyPair } from '../../api/market'
import { useHealthQuery } from '../../queries/useHealthQuery'
import {
  useCurrencyPairsQuery,
  usePriceQueries,
} from '../../queries/useMarketQueries'
import { useWatchlist } from '../../watchlist/watchlistContext'
import { WatchlistRow } from './WatchlistRow'

function pairFromSymbol(symbol: string): CurrencyPair {
  const baseCurrency = symbol.slice(0, 3)
  const quoteCurrency = symbol.slice(3)
  return {
    baseCurrency,
    quoteCurrency,
    symbol,
    displayName: `${baseCurrency}/${quoteCurrency}`,
  }
}

export function Watchlist() {
  const {
    state,
    isHydrated,
    persistenceError,
    add,
    remove,
    move,
    select,
  } = useWatchlist()
  const [addSymbol, setAddSymbol] = useState('')
  const healthQuery = useHealthQuery()
  const backendConnected = healthQuery.isSuccess
    && healthQuery.data.status === 'UP'
  const pairsQuery = useCurrencyPairsQuery(backendConnected)
  const availablePairs = pairsQuery.data ?? []
  const pairsBySymbol = new Map(
    availablePairs.map((pair) => [pair.symbol, pair]),
  )
  const watchedPairs = state.symbols.map(
    (symbol) => pairsBySymbol.get(symbol) ?? pairFromSymbol(symbol),
  )
  const priceQueries = usePriceQueries(state.symbols, backendConnected)
  const addablePairs = availablePairs.filter(
    (pair) => !state.symbols.includes(pair.symbol),
  )
  const selectedAddSymbol = addablePairs.some(
    (pair) => pair.symbol === addSymbol,
  ) ? addSymbol : (addablePairs[0]?.symbol ?? '')

  function handleAdd(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (selectedAddSymbol) {
      add(selectedAddSymbol)
      setAddSymbol('')
    }
  }

  let statusMessage: string | null = null
  if (healthQuery.isError || (healthQuery.isSuccess && !backendConnected)) {
    statusMessage = 'Backend unavailable. Watchlist editing is still available.'
  } else if (pairsQuery.isError) {
    statusMessage = 'Currency pairs could not be loaded. Existing watchlist items remain available.'
  }

  return (
    <section
      aria-labelledby="watchlist-heading"
      className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-white/8 dark:bg-white/[0.035]"
    >
      <div className="flex flex-col gap-4 border-b border-slate-100 px-5 py-4 dark:border-white/8 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h2 id="watchlist-heading" className="font-semibold text-slate-950 dark:text-white">
              Watchlist
            </h2>
            <span className="rounded-full bg-cyan-500/10 px-2.5 py-1 text-xs font-semibold text-cyan-700 dark:text-cyan-300">
              {state.symbols.length} {state.symbols.length === 1 ? 'pair' : 'pairs'}
            </span>
          </div>
          <p className="mt-1 text-xs text-slate-500">Your monitored currency pairs</p>
        </div>
        <form onSubmit={handleAdd} className="flex items-end gap-2">
          <label className="grid gap-1 text-xs font-medium text-slate-500 dark:text-slate-400">
            Add currency pair
            <select
              aria-label="Currency pair to add"
              value={selectedAddSymbol}
              disabled={!isHydrated || addablePairs.length === 0}
              onChange={(event) => setAddSymbol(event.target.value)}
              className="min-w-36 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-800 outline-none focus:border-cyan-500 disabled:cursor-not-allowed disabled:opacity-50 dark:border-white/10 dark:bg-slate-900 dark:text-slate-100"
            >
              {addablePairs.length === 0 && (
                <option value="">
                  {pairsQuery.isPending ? 'Loading pairs…' : 'No pairs available'}
                </option>
              )}
              {addablePairs.map((pair) => (
                <option key={pair.symbol} value={pair.symbol}>
                  {pair.displayName}
                </option>
              ))}
            </select>
          </label>
          <button
            type="submit"
            disabled={!isHydrated || !selectedAddSymbol}
            className="inline-flex items-center gap-1.5 rounded-lg bg-cyan-500 px-3 py-2 text-sm font-semibold text-slate-950 transition hover:bg-cyan-400 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <Plus aria-hidden="true" className="size-4" />
            Add
          </button>
        </form>
      </div>

      {(statusMessage || persistenceError) && (
        <div role="note" className="flex items-center gap-2 border-b border-amber-200 bg-amber-50 px-5 py-2.5 text-xs text-amber-800 dark:border-amber-300/10 dark:bg-amber-300/5 dark:text-amber-200">
          <CloudOff aria-hidden="true" className="size-4 shrink-0" />
          {persistenceError ?? statusMessage}
        </div>
      )}

      {watchedPairs.length > 0 ? (
        <>
          <div className="hidden grid-cols-[minmax(9rem,1.4fr)_repeat(3,minmax(5.5rem,1fr))_minmax(9rem,1fr)_8rem] gap-4 bg-slate-50/80 px-5 py-2.5 text-[0.65rem] font-bold uppercase tracking-wider text-slate-400 dark:bg-white/[0.025] md:grid">
            <span>Symbol</span>
            <span>Bid</span>
            <span>Ask</span>
            <span>Spread</span>
            <span>Last update</span>
            <span className="text-right">Actions</span>
          </div>
          {watchedPairs.map((pair, index) => (
            <WatchlistRow
              key={pair.symbol}
              pair={pair}
              priceQuery={priceQueries[index]}
              marketDataAvailable={backendConnected}
              selected={state.selectedSymbol === pair.symbol}
              first={index === 0}
              last={index === watchedPairs.length - 1}
              onSelect={() => select(pair.symbol)}
              onMoveUp={() => move(pair.symbol, -1)}
              onMoveDown={() => move(pair.symbol, 1)}
              onRemove={() => remove(pair.symbol)}
            />
          ))}
        </>
      ) : (
        <div className="grid min-h-52 place-items-center px-6 py-10 text-center">
          <div>
            <Rows3 aria-hidden="true" className="mx-auto size-9 text-slate-400" />
            <h3 className="mt-4 font-semibold text-slate-800 dark:text-slate-100">
              Your watchlist is empty
            </h3>
            <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
              Add a currency pair to start monitoring it.
            </p>
          </div>
        </div>
      )}
    </section>
  )
}

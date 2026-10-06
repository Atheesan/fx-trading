import { CloudOff, RefreshCw, Rows3 } from 'lucide-react'

import { useHealthQuery } from '../../queries/useHealthQuery'
import {
  useCurrencyPairsQuery,
  usePriceQueries,
} from '../../queries/useMarketQueries'
import { WatchlistRow } from './WatchlistRow'

function WatchlistSkeleton() {
  return (
    <div aria-label="Loading market watchlist" className="divide-y divide-slate-100 dark:divide-white/8">
      {Array.from({ length: 5 }, (_, index) => (
        <div
          key={index}
          className="grid gap-4 px-5 py-5 md:grid-cols-[minmax(9rem,1.4fr)_repeat(3,minmax(5.5rem,1fr))_minmax(9rem,1fr)_1.5rem]"
        >
          <span className="h-6 w-24 animate-pulse rounded bg-slate-200 dark:bg-white/10" />
          {Array.from({ length: 4 }, (__, cellIndex) => (
            <span
              key={cellIndex}
              className="h-5 w-16 animate-pulse rounded bg-slate-200 dark:bg-white/10"
            />
          ))}
        </div>
      ))}
    </div>
  )
}

interface MessageStateProps {
  icon: typeof CloudOff
  title: string
  message: string
  onRetry?: () => void
}

function MessageState({ icon: Icon, title, message, onRetry }: MessageStateProps) {
  return (
    <div className="grid min-h-64 place-items-center px-6 py-10 text-center">
      <div>
        <Icon aria-hidden="true" className="mx-auto size-9 text-slate-400" />
        <h3 className="mt-4 font-semibold text-slate-800 dark:text-slate-100">{title}</h3>
        <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">{message}</p>
        {onRetry && (
          <button
            type="button"
            onClick={onRetry}
            className="mt-5 inline-flex items-center gap-2 rounded-lg bg-slate-900 px-3.5 py-2 text-sm font-semibold text-white transition hover:bg-slate-700 dark:bg-cyan-400 dark:text-slate-950 dark:hover:bg-cyan-300"
          >
            <RefreshCw aria-hidden="true" className="size-4" />
            Try again
          </button>
        )}
      </div>
    </div>
  )
}

export function Watchlist() {
  const healthQuery = useHealthQuery()
  const backendConnected =
    healthQuery.isSuccess && healthQuery.data.status === 'UP'
  const pairsQuery = useCurrencyPairsQuery(backendConnected)
  const pairs = pairsQuery.data ?? []
  const priceQueries = usePriceQueries(
    pairs.map((pair) => pair.symbol),
    pairsQuery.isSuccess,
  )

  let content
  if (healthQuery.isPending) {
    content = <WatchlistSkeleton />
  } else if (healthQuery.isError || !backendConnected) {
    content = (
      <MessageState
        icon={CloudOff}
        title="Backend unavailable"
        message="FXAI cannot reach the market data service. Check the backend connection and try again."
        onRetry={() => void healthQuery.refetch()}
      />
    )
  } else if (pairsQuery.isPending) {
    content = <WatchlistSkeleton />
  } else if (pairsQuery.isError) {
    content = (
      <MessageState
        icon={CloudOff}
        title="Market data unavailable"
        message="The currency-pair list could not be loaded."
        onRetry={() => void pairsQuery.refetch()}
      />
    )
  } else if (pairs.length === 0) {
    content = (
      <MessageState
        icon={Rows3}
        title="No currency pairs"
        message="There are no currency pairs available to monitor."
      />
    )
  } else {
    content = pairs.map((pair, index) => (
      <WatchlistRow
        key={pair.symbol}
        pair={pair}
        priceQuery={priceQueries[index]}
      />
    ))
  }

  return (
    <section
      aria-labelledby="watchlist-heading"
      className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-white/8 dark:bg-white/[0.035]"
    >
      <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4 dark:border-white/8">
        <div>
          <h2 id="watchlist-heading" className="font-semibold text-slate-950 dark:text-white">
            Watchlist
          </h2>
          <p className="mt-0.5 text-xs text-slate-500">Major currency pairs</p>
        </div>
        <span className="rounded-full bg-cyan-500/10 px-2.5 py-1 text-xs font-semibold text-cyan-700 dark:text-cyan-300">
          {pairsQuery.isSuccess ? `${pairs.length} pairs` : 'Live'}
        </span>
      </div>
      {pairs.length > 0 && (
        <div className="hidden grid-cols-[minmax(9rem,1.4fr)_repeat(3,minmax(5.5rem,1fr))_minmax(9rem,1fr)_1.5rem] gap-4 bg-slate-50/80 px-5 py-2.5 text-[0.65rem] font-bold uppercase tracking-wider text-slate-400 dark:bg-white/[0.025] md:grid">
          <span>Symbol</span>
          <span>Bid</span>
          <span>Ask</span>
          <span>Spread</span>
          <span>Last update</span>
          <span />
        </div>
      )}
      {content}
    </section>
  )
}

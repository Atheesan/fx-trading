import type { UseQueryResult } from '@tanstack/react-query'
import { ArrowUpRight } from 'lucide-react'
import { Link } from 'react-router-dom'

import type { CurrencyPair, PriceQuote } from '../../api/market'
import { PriceDisplay } from './PriceDisplay'

interface WatchlistRowProps {
  pair: CurrencyPair
  priceQuery: UseQueryResult<PriceQuote, Error>
}

function PriceSkeleton() {
  return <span className="block h-5 w-16 animate-pulse rounded bg-slate-200 dark:bg-white/10" />
}

export function WatchlistRow({ pair, priceQuery }: WatchlistRowProps) {
  return (
    <Link
      to={`/markets/${pair.symbol}`}
      aria-label={`View ${pair.displayName} market`}
      className="group grid gap-4 border-t border-slate-100 px-5 py-4 transition first:border-t-0 hover:bg-cyan-500/[0.045] focus-visible:outline-2 focus-visible:outline-inset focus-visible:outline-cyan-500 dark:border-white/8 dark:hover:bg-cyan-400/[0.045] md:grid-cols-[minmax(9rem,1.4fr)_repeat(3,minmax(5.5rem,1fr))_minmax(9rem,1fr)_1.5rem] md:items-center"
    >
      <div className="flex items-center justify-between md:block">
        <div>
          <p className="font-bold tracking-tight text-slate-950 dark:text-white">
            {pair.displayName}
          </p>
          <p className="mt-0.5 text-xs text-slate-400">{pair.symbol}</p>
        </div>
        <ArrowUpRight
          aria-hidden="true"
          className="size-5 text-slate-300 transition group-hover:text-cyan-500 md:hidden"
        />
      </div>

      {priceQuery.isPending ? (
        <>
          <PriceSkeleton />
          <PriceSkeleton />
          <PriceSkeleton />
          <PriceSkeleton />
        </>
      ) : priceQuery.isError ? (
        <p className="text-sm text-rose-600 dark:text-rose-400 md:col-span-4">
          Price unavailable
        </p>
      ) : (
        <>
          <PriceDisplay label="Bid" value={priceQuery.data.bid} symbol={pair.symbol} />
          <PriceDisplay label="Ask" value={priceQuery.data.ask} symbol={pair.symbol} />
          <PriceDisplay label="Spread" value={priceQuery.data.spread} symbol={pair.symbol} />
          <div>
            <span className="mb-1 block text-[0.65rem] font-semibold uppercase tracking-wider text-slate-400 md:hidden">
              Last update
            </span>
            <time
              dateTime={priceQuery.data.timestamp}
              className="text-sm text-slate-500 dark:text-slate-400"
            >
              {new Date(priceQuery.data.timestamp).toLocaleTimeString([], {
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
              })}
            </time>
          </div>
        </>
      )}

      <ArrowUpRight
        aria-hidden="true"
        className="hidden size-4 text-slate-300 transition group-hover:text-cyan-500 md:block"
      />
    </Link>
  )
}

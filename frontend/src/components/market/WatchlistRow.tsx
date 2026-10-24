import type { UseQueryResult } from '@tanstack/react-query'
import {
  ArrowDown,
  ArrowUp,
  ArrowUpRight,
  Check,
  Trash2,
} from 'lucide-react'
import { type ReactNode } from 'react'
import { Link } from 'react-router-dom'

import type { CurrencyPair, PriceQuote } from '../../api/market'
import { PriceDisplay } from './PriceDisplay'

interface WatchlistRowProps {
  pair: CurrencyPair
  priceQuery: UseQueryResult<PriceQuote, Error>
  marketDataAvailable: boolean
  selected: boolean
  first: boolean
  last: boolean
  onSelect: () => void
  onMoveUp: () => void
  onMoveDown: () => void
  onRemove: () => void
}

function PriceSkeleton() {
  return <span className="block h-5 w-16 animate-pulse rounded bg-slate-200 dark:bg-white/10" />
}

function ActionButton({
  label,
  disabled = false,
  onClick,
  children,
}: {
  label: string
  disabled?: boolean
  onClick: () => void
  children: ReactNode
}) {
  return (
    <button
      type="button"
      aria-label={label}
      disabled={disabled}
      onClick={onClick}
      className="rounded-md p-1.5 text-slate-400 transition hover:bg-slate-200 hover:text-slate-700 disabled:cursor-not-allowed disabled:opacity-30 dark:hover:bg-white/10 dark:hover:text-slate-100"
    >
      {children}
    </button>
  )
}

export function WatchlistRow({
  pair,
  priceQuery,
  marketDataAvailable,
  selected,
  first,
  last,
  onSelect,
  onMoveUp,
  onMoveDown,
  onRemove,
}: WatchlistRowProps) {
  return (
    <article
      data-symbol={pair.symbol}
      className={`grid gap-4 border-t px-5 py-4 transition first:border-t-0 md:grid-cols-[minmax(9rem,1.4fr)_repeat(3,minmax(5.5rem,1fr))_minmax(9rem,1fr)_8rem] md:items-center ${
        selected
          ? 'border-cyan-100 bg-cyan-500/[0.065] dark:border-cyan-400/15 dark:bg-cyan-400/[0.065]'
          : 'border-slate-100 dark:border-white/8'
      }`}
    >
      <button
        type="button"
        aria-label={`Select ${pair.displayName}`}
        aria-pressed={selected}
        onClick={onSelect}
        className="flex items-center justify-between rounded-lg text-left outline-none focus-visible:ring-2 focus-visible:ring-cyan-500 md:block"
      >
        <div className="flex items-center gap-2">
          <div>
            <p className="font-bold tracking-tight text-slate-950 dark:text-white">
              {pair.displayName}
            </p>
            <p className="mt-0.5 text-xs text-slate-400">{pair.symbol}</p>
          </div>
          {selected && (
            <span className="inline-flex items-center gap-1 rounded-full bg-cyan-500/10 px-2 py-1 text-[0.65rem] font-bold uppercase tracking-wide text-cyan-700 dark:text-cyan-300">
              <Check aria-hidden="true" className="size-3" />
              Selected
            </span>
          )}
        </div>
      </button>

      {!marketDataAvailable ? (
        <p className="text-sm text-slate-400 md:col-span-4">Price unavailable</p>
      ) : priceQuery.isPending ? (
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

      <div className="flex items-center justify-end gap-0.5">
        <ActionButton
          label={`Move ${pair.displayName} up`}
          disabled={first}
          onClick={onMoveUp}
        >
          <ArrowUp aria-hidden="true" className="size-4" />
        </ActionButton>
        <ActionButton
          label={`Move ${pair.displayName} down`}
          disabled={last}
          onClick={onMoveDown}
        >
          <ArrowDown aria-hidden="true" className="size-4" />
        </ActionButton>
        <ActionButton label={`Remove ${pair.displayName}`} onClick={onRemove}>
          <Trash2 aria-hidden="true" className="size-4" />
        </ActionButton>
        <Link
          to={`/markets/${pair.symbol}`}
          aria-label={`View ${pair.displayName} market`}
          className="rounded-md p-1.5 text-slate-400 transition hover:bg-cyan-500/10 hover:text-cyan-600 focus-visible:outline-2 focus-visible:outline-cyan-500 dark:hover:text-cyan-300"
        >
          <ArrowUpRight aria-hidden="true" className="size-4" />
        </Link>
      </div>
    </article>
  )
}

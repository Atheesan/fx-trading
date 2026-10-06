import { ArrowLeft, ChartNoAxesCombined } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'

import { PageHeader } from '../components/PageHeader'
import { PriceDisplay } from '../components/market/PriceDisplay'
import { usePriceQuery } from '../queries/useMarketQueries'

export function PairDetails() {
  const { symbol = '' } = useParams()
  const normalizedSymbol = symbol.toUpperCase().replace('-', '')
  const displaySymbol = `${normalizedSymbol.slice(0, 3)}/${normalizedSymbol.slice(3)}`
  const priceQuery = usePriceQuery(normalizedSymbol)

  return (
    <div className="space-y-8">
      <Link to="/markets" className="inline-flex items-center gap-2 text-sm font-medium text-slate-500 transition hover:text-cyan-600 dark:text-slate-400 dark:hover:text-cyan-400">
        <ArrowLeft aria-hidden="true" className="size-4" />
        Back to markets
      </Link>
      <PageHeader
        eyebrow="Pair details"
        title={`${displaySymbol} market details`}
        description="Live pricing and historical chart data will be presented in this workspace."
      />
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-white/8 dark:bg-white/[0.035]">
        {priceQuery.isPending ? (
          <div aria-label="Loading price" className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            {Array.from({ length: 4 }, (_, index) => (
              <span key={index} className="h-16 animate-pulse rounded-xl bg-slate-100 dark:bg-white/10" />
            ))}
          </div>
        ) : priceQuery.isError ? (
          <div className="py-10 text-center">
            <ChartNoAxesCombined aria-hidden="true" className="mx-auto size-9 text-rose-400" />
            <h2 className="mt-4 font-semibold text-slate-800 dark:text-slate-100">Price unavailable</h2>
            <button type="button" onClick={() => void priceQuery.refetch()} className="mt-3 text-sm font-semibold text-cyan-600 dark:text-cyan-400">
              Try again
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-6 sm:grid-cols-4">
            <PriceDisplay label="Bid" value={priceQuery.data.bid} symbol={normalizedSymbol} />
            <PriceDisplay label="Ask" value={priceQuery.data.ask} symbol={normalizedSymbol} />
            <PriceDisplay label="Midpoint" value={priceQuery.data.midpoint} symbol={normalizedSymbol} />
            <PriceDisplay label="Spread" value={priceQuery.data.spread} symbol={normalizedSymbol} />
          </div>
        )}
      </section>
    </div>
  )
}

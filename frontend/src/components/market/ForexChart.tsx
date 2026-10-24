import { ChartNoAxesCombined, RefreshCw } from 'lucide-react'
import { useMemo, useState } from 'react'

import {
  candleTimeframes,
  technicalIndicatorNames,
  type CandleTimeframe,
} from '../../api/market'
import {
  useCandlesQuery,
  useIndicatorsQuery,
} from '../../queries/useMarketQueries'
import { IndicatorPanel } from './IndicatorPanel'
import {
  LightweightCandlestickChart,
  type PriceIndicatorOverlay,
} from './LightweightCandlestickChart'

interface ForexChartProps {
  symbol: string
}

function displayPair(symbol: string) {
  return `${symbol.slice(0, 3)}/${symbol.slice(3)}`
}

export function ForexChart({ symbol }: ForexChartProps) {
  const [timeframe, setTimeframe] = useState<CandleTimeframe>('1h')
  const candlesQuery = useCandlesQuery(symbol, timeframe)
  const indicatorsQuery = useIndicatorsQuery(
    symbol,
    timeframe,
    technicalIndicatorNames,
  )
  const overlays: PriceIndicatorOverlay[] = useMemo(
    () => (indicatorsQuery.data?.indicators ?? [])
      .filter((indicator) => (
        indicator.status === 'AVAILABLE'
        && ['sma20', 'sma50', 'ema20'].includes(indicator.name)
      ))
      .map((indicator) => ({
        name: indicator.name.toUpperCase() as PriceIndicatorOverlay['name'],
        points: indicator.points.map((point) => ({
          timestamp: point.timestamp,
          value: point.value,
        })),
      })),
    [indicatorsQuery.data],
  )

  return (
    <section className="min-w-0 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-6 dark:border-white/8 dark:bg-white/[0.035]">
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-cyan-600 dark:text-cyan-400">
            {displayPair(symbol)} · {timeframe}
          </p>
          <h2 className="mt-1 text-lg font-semibold text-slate-900 dark:text-white">
            Price chart
          </h2>
        </div>
        <div aria-label="Chart timeframe" className="flex flex-wrap gap-2">
          {candleTimeframes.map((option) => (
            <button
              key={option}
              type="button"
              aria-pressed={timeframe === option}
              onClick={() => setTimeframe(option)}
              className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition ${
                timeframe === option
                  ? 'bg-cyan-600 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200 dark:bg-white/8 dark:text-slate-300 dark:hover:bg-white/12'
              }`}
            >
              {option}
            </button>
          ))}
        </div>
      </div>

      {candlesQuery.isPending ? (
        <div role="status" aria-label="Loading chart data" className="h-[22rem] animate-pulse rounded-xl bg-slate-100 md:h-[28rem] dark:bg-white/8">
          <span className="sr-only">Loading chart data</span>
        </div>
      ) : candlesQuery.isError ? (
        <div role="alert" className="flex h-[22rem] flex-col items-center justify-center rounded-xl border border-dashed border-rose-200 text-center md:h-[28rem] dark:border-rose-400/20">
          <ChartNoAxesCombined aria-hidden="true" className="size-9 text-rose-400" />
          <h3 className="mt-4 font-semibold text-slate-800 dark:text-slate-100">Chart unavailable</h3>
          <p className="mt-1 max-w-md text-sm text-slate-500 dark:text-slate-400">
            Historical candle data could not be loaded.
          </p>
          <button type="button" onClick={() => void candlesQuery.refetch()} className="mt-4 inline-flex items-center gap-2 text-sm font-semibold text-cyan-600 dark:text-cyan-400">
            <RefreshCw aria-hidden="true" className="size-4" />
            Try again
          </button>
        </div>
      ) : candlesQuery.data.length === 0 ? (
        <div className="flex h-[22rem] flex-col items-center justify-center rounded-xl border border-dashed border-slate-200 text-center md:h-[28rem] dark:border-white/10">
          <ChartNoAxesCombined aria-hidden="true" className="size-9 text-slate-300 dark:text-slate-600" />
          <h3 className="mt-4 font-semibold text-slate-800 dark:text-slate-100">No candle data</h3>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            No {timeframe} candles are available for {displayPair(symbol)}.
          </p>
        </div>
      ) : (
        <LightweightCandlestickChart
          candles={candlesQuery.data}
          overlays={overlays}
        />
      )}

      <IndicatorPanel
        timeframe={timeframe}
        indicators={indicatorsQuery.data?.indicators}
        loading={indicatorsQuery.isPending}
        error={indicatorsQuery.isError}
        onRetry={() => void indicatorsQuery.refetch()}
      />
    </section>
  )
}

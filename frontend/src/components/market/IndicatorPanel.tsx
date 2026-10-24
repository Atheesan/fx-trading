import { Activity, RefreshCw } from 'lucide-react'

import type {
  CandleTimeframe,
  TechnicalIndicator,
} from '../../api/market'

interface IndicatorPanelProps {
  timeframe: CandleTimeframe
  indicators?: TechnicalIndicator[]
  loading: boolean
  error: boolean
  onRetry: () => void
}

const displayNames = {
  sma20: 'SMA20',
  sma50: 'SMA50',
  ema20: 'EMA20',
  rsi14: 'RSI14',
  macd: 'MACD',
} as const

function formatValue(value: number | null) {
  if (value === null) return '—'
  return value.toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 5,
  })
}

function IndicatorValue({ indicator }: { indicator: TechnicalIndicator }) {
  if (indicator.status === 'INSUFFICIENT_DATA') {
    return (
      <p className="mt-2 text-xs leading-5 text-amber-700 dark:text-amber-300">
        Insufficient data ({indicator.availableDataPoints}/{indicator.requiredDataPoints})
      </p>
    )
  }

  if (indicator.name === 'macd') {
    return (
      <dl className="mt-3 grid grid-cols-3 gap-2 text-xs">
        <div>
          <dt className="text-slate-400">MACD</dt>
          <dd className="mt-1 font-mono font-semibold text-slate-800 dark:text-slate-100">
            {formatValue(indicator.value)}
          </dd>
        </div>
        <div>
          <dt className="text-slate-400">Signal</dt>
          <dd className="mt-1 font-mono font-semibold text-slate-800 dark:text-slate-100">
            {formatValue(indicator.signal)}
          </dd>
        </div>
        <div>
          <dt className="text-slate-400">Histogram</dt>
          <dd className="mt-1 font-mono font-semibold text-slate-800 dark:text-slate-100">
            {formatValue(indicator.histogram)}
          </dd>
        </div>
      </dl>
    )
  }

  return (
    <p className="mt-3 font-mono text-lg font-semibold text-slate-900 dark:text-white">
      {formatValue(indicator.value)}
    </p>
  )
}

export function IndicatorPanel({
  timeframe,
  indicators,
  loading,
  error,
  onRetry,
}: IndicatorPanelProps) {
  return (
    <section aria-labelledby="indicator-panel-heading" className="mt-6 border-t border-slate-100 pt-5 dark:border-white/8">
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h3 id="indicator-panel-heading" className="font-semibold text-slate-900 dark:text-white">
            Technical indicators
          </h3>
          <p className="mt-0.5 text-xs text-slate-500">
            Descriptive values for the {timeframe} timeframe
          </p>
        </div>
        <Activity aria-hidden="true" className="size-5 text-cyan-500" />
      </div>

      {loading ? (
        <div aria-label="Loading technical indicators" className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
          {Array.from({ length: 5 }, (_, index) => (
            <span key={index} className="h-24 animate-pulse rounded-xl bg-slate-100 dark:bg-white/8" />
          ))}
        </div>
      ) : error ? (
        <div role="alert" className="flex items-center justify-between gap-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:border-rose-400/20 dark:bg-rose-400/5 dark:text-rose-300">
          <span>Technical indicators could not be loaded.</span>
          <button type="button" onClick={onRetry} className="inline-flex shrink-0 items-center gap-1.5 font-semibold">
            <RefreshCw aria-hidden="true" className="size-4" />
            Try again
          </button>
        </div>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
          {(indicators ?? []).map((indicator) => (
            <article key={indicator.name} className="rounded-xl border border-slate-200 bg-slate-50/70 p-4 dark:border-white/8 dark:bg-white/[0.025]">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                {displayNames[indicator.name]}
              </h4>
              <IndicatorValue indicator={indicator} />
            </article>
          ))}
        </div>
      )}
    </section>
  )
}

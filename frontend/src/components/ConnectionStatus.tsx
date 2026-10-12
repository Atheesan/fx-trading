import { useHealthQuery } from '../queries/useHealthQuery'
import { usePriceStreamStatus } from './priceStreamContext'

export function ConnectionStatus() {
  const healthQuery = useHealthQuery()
  const priceStreamStatus = usePriceStreamStatus()
  const connected = healthQuery.isSuccess && healthQuery.data.status === 'UP'
  const status = healthQuery.isPending
    ? 'Checking'
    : connected
      ? 'Connected'
      : 'Unavailable'

  return (
    <div
      className="flex items-center gap-2 rounded-full border border-slate-200 bg-slate-50 px-3 py-2 dark:border-white/10 dark:bg-white/5"
      role="status"
      aria-live="polite"
    >
      <span
        aria-hidden="true"
        className={`size-2 rounded-full ${
          healthQuery.isPending
            ? 'animate-pulse bg-amber-400'
            : connected
              ? 'bg-emerald-400 shadow-[0_0_10px_rgba(52,211,153,0.6)]'
              : 'bg-rose-400'
        }`}
      />
      <span className="text-xs font-medium text-slate-600 dark:text-slate-300 sm:text-sm">
        Backend: {status}
      </span>
      <span aria-hidden="true" className="text-slate-300 dark:text-slate-600">·</span>
      <span
        className={`text-xs font-bold sm:text-sm ${
          priceStreamStatus === 'LIVE'
            ? 'text-emerald-600 dark:text-emerald-400'
            : priceStreamStatus === 'RECONNECTING'
              ? 'text-amber-600 dark:text-amber-400'
              : 'text-rose-600 dark:text-rose-400'
        }`}
      >
        {priceStreamStatus}
      </span>
    </div>
  )
}

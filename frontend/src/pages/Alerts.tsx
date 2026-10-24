import { BellRing, Plus, RefreshCw, Trash2 } from 'lucide-react'
import { useState, type FormEvent } from 'react'

import type {
  AlertCondition,
  PriceAlert,
} from '../api/alerts'
import { PageHeader } from '../components/PageHeader'
import {
  useAlertsQuery,
  useCreateAlertMutation,
  useDeleteAlertMutation,
} from '../queries/useAlertQueries'
import { useCurrencyPairsQuery } from '../queries/useMarketQueries'

function displaySymbol(symbol: string) {
  return `${symbol.slice(0, 3)}/${symbol.slice(3)}`
}

function displayCondition(condition: AlertCondition) {
  return condition === 'PRICE_ABOVE' ? 'Price above' : 'Price below'
}

function formatPrice(price: number) {
  return price.toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 5,
  })
}

function AlertList({
  title,
  description,
  alerts,
  deletingId,
  onDelete,
}: {
  title: string
  description: string
  alerts: PriceAlert[]
  deletingId?: string
  onDelete: (id: string) => void
}) {
  return (
    <section aria-labelledby={`${title.toLowerCase()}-alerts-heading`} className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-white/8 dark:bg-white/[0.035]">
      <div className="border-b border-slate-100 px-5 py-4 dark:border-white/8">
        <div className="flex items-center justify-between gap-3">
          <div>
            <h2 id={`${title.toLowerCase()}-alerts-heading`} className="font-semibold text-slate-950 dark:text-white">
              {title} alerts
            </h2>
            <p className="mt-1 text-xs text-slate-500">{description}</p>
          </div>
          <span className="rounded-full bg-cyan-500/10 px-2.5 py-1 text-xs font-semibold text-cyan-700 dark:text-cyan-300">
            {alerts.length}
          </span>
        </div>
      </div>

      {alerts.length === 0 ? (
        <div className="px-5 py-10 text-center text-sm text-slate-500">
          No {title.toLowerCase()} alerts.
        </div>
      ) : (
        <div className="divide-y divide-slate-100 dark:divide-white/8">
          {alerts.map((alert) => (
            <article key={alert.id} className="flex flex-col gap-4 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
              <div className="grid flex-1 gap-3 sm:grid-cols-3">
                <div>
                  <p className="font-bold text-slate-900 dark:text-white">
                    {displaySymbol(alert.symbol)}
                  </p>
                  <p className="mt-0.5 text-xs text-slate-400">{alert.symbol}</p>
                </div>
                <div>
                  <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Condition</p>
                  <p className="mt-1 text-sm text-slate-700 dark:text-slate-200">
                    {displayCondition(alert.condition)}
                  </p>
                </div>
                <div>
                  <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Target</p>
                  <p className="mt-1 font-mono text-sm font-semibold text-slate-800 dark:text-slate-100">
                    {formatPrice(alert.targetPrice)}
                  </p>
                  {alert.triggeredAt && (
                    <time dateTime={alert.triggeredAt} className="mt-1 block text-xs text-slate-400">
                      Triggered {new Date(alert.triggeredAt).toLocaleString()}
                    </time>
                  )}
                </div>
              </div>
              <button
                type="button"
                aria-label={`Delete ${displaySymbol(alert.symbol)} ${displayCondition(alert.condition).toLowerCase()} alert`}
                disabled={deletingId === alert.id}
                onClick={() => onDelete(alert.id)}
                className="inline-flex items-center justify-center gap-1.5 rounded-lg px-3 py-2 text-sm font-semibold text-rose-600 transition hover:bg-rose-50 disabled:opacity-50 dark:text-rose-400 dark:hover:bg-rose-400/10"
              >
                <Trash2 aria-hidden="true" className="size-4" />
                Delete
              </button>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}

export function Alerts() {
  const [symbol, setSymbol] = useState('EURUSD')
  const [condition, setCondition] = useState<AlertCondition>('PRICE_ABOVE')
  const [targetPrice, setTargetPrice] = useState('')
  const [validationError, setValidationError] = useState<string | null>(null)
  const pairsQuery = useCurrencyPairsQuery()
  const alertsQuery = useAlertsQuery()
  const createMutation = useCreateAlertMutation()
  const deleteMutation = useDeleteAlertMutation()
  const alerts = alertsQuery.data ?? []
  const activeAlerts = alerts.filter((alert) => alert.status === 'ACTIVE')
  const triggeredAlerts = alerts.filter((alert) => alert.status === 'TRIGGERED')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const parsedTarget = Number(targetPrice)
    if (!Number.isFinite(parsedTarget) || parsedTarget <= 0) {
      setValidationError('Enter a target price greater than zero.')
      return
    }

    setValidationError(null)
    createMutation.mutate(
      { symbol, condition, targetPrice: parsedTarget },
      { onSuccess: () => setTargetPrice('') },
    )
  }

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Alerts"
        title="Price alerts"
        description="Track when a currency pair reaches a price above or below your target."
      />

      <section aria-labelledby="create-alert-heading" className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-white/8 dark:bg-white/[0.035] sm:p-6">
        <div className="flex items-start gap-3">
          <span className="rounded-xl bg-cyan-500/10 p-2.5 text-cyan-600 dark:text-cyan-400">
            <BellRing aria-hidden="true" className="size-5" />
          </span>
          <div>
            <h2 id="create-alert-heading" className="font-semibold text-slate-950 dark:text-white">
              Create alert
            </h2>
            <p className="mt-1 text-sm text-slate-500">
              Alerts use the live midpoint price and trigger once.
            </p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="mt-6 grid gap-4 md:grid-cols-[1fr_1fr_1fr_auto] md:items-end">
          <label className="grid gap-1.5 text-sm font-medium text-slate-600 dark:text-slate-300">
            Currency pair
            <select
              value={symbol}
              disabled={pairsQuery.isPending || pairsQuery.isError}
              onChange={(event) => setSymbol(event.target.value)}
              className="rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 outline-none focus:border-cyan-500 disabled:opacity-50 dark:border-white/10 dark:bg-slate-900 dark:text-white"
            >
              {(pairsQuery.data ?? []).map((pair) => (
                <option key={pair.symbol} value={pair.symbol}>{pair.displayName}</option>
              ))}
            </select>
          </label>
          <label className="grid gap-1.5 text-sm font-medium text-slate-600 dark:text-slate-300">
            Condition
            <select
              value={condition}
              onChange={(event) => setCondition(event.target.value as AlertCondition)}
              className="rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 outline-none focus:border-cyan-500 dark:border-white/10 dark:bg-slate-900 dark:text-white"
            >
              <option value="PRICE_ABOVE">Price above</option>
              <option value="PRICE_BELOW">Price below</option>
            </select>
          </label>
          <label className="grid gap-1.5 text-sm font-medium text-slate-600 dark:text-slate-300">
            Target price
            <input
              type="number"
              min="0"
              step="any"
              required
              value={targetPrice}
              onChange={(event) => setTargetPrice(event.target.value)}
              placeholder="1.12500"
              className="rounded-lg border border-slate-200 bg-white px-3 py-2.5 font-mono text-slate-900 outline-none placeholder:text-slate-300 focus:border-cyan-500 dark:border-white/10 dark:bg-slate-900 dark:text-white dark:placeholder:text-slate-600"
            />
          </label>
          <button
            type="submit"
            disabled={createMutation.isPending || pairsQuery.isPending || pairsQuery.isError}
            className="inline-flex items-center justify-center gap-2 rounded-lg bg-cyan-500 px-4 py-2.5 text-sm font-semibold text-slate-950 transition hover:bg-cyan-400 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <Plus aria-hidden="true" className="size-4" />
            Create alert
          </button>
        </form>
        {(validationError || createMutation.isError) && (
          <p role="alert" className="mt-3 text-sm text-rose-600 dark:text-rose-400">
            {validationError ?? 'The alert could not be created.'}
          </p>
        )}
      </section>

      {alertsQuery.isPending ? (
        <div aria-label="Loading price alerts" className="h-56 animate-pulse rounded-2xl bg-slate-100 dark:bg-white/8" />
      ) : alertsQuery.isError ? (
        <div role="alert" className="grid min-h-56 place-items-center rounded-2xl border border-rose-200 bg-white p-8 text-center dark:border-rose-400/20 dark:bg-white/[0.035]">
          <div>
            <p className="font-semibold text-slate-800 dark:text-slate-100">Alerts unavailable</p>
            <button type="button" onClick={() => void alertsQuery.refetch()} className="mt-3 inline-flex items-center gap-2 text-sm font-semibold text-cyan-600 dark:text-cyan-400">
              <RefreshCw aria-hidden="true" className="size-4" />
              Try again
            </button>
          </div>
        </div>
      ) : (
        <div className="grid gap-6 xl:grid-cols-2">
          <AlertList
            title="Active"
            description="Waiting for their target price."
            alerts={activeAlerts}
            deletingId={deleteMutation.isPending ? deleteMutation.variables : undefined}
            onDelete={(id) => deleteMutation.mutate(id)}
          />
          <AlertList
            title="Triggered"
            description="Targets that have already been reached."
            alerts={triggeredAlerts}
            deletingId={deleteMutation.isPending ? deleteMutation.variables : undefined}
            onDelete={(id) => deleteMutation.mutate(id)}
          />
        </div>
      )}

      {deleteMutation.isError && (
        <p role="alert" className="text-sm text-rose-600 dark:text-rose-400">
          The alert could not be deleted.
        </p>
      )}
    </div>
  )
}

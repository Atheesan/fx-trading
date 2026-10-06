import { Activity, Clock3, Radio, TrendingUp } from 'lucide-react'
import { Link } from 'react-router-dom'

import { PageHeader } from '../components/PageHeader'
import { Watchlist } from '../components/market/Watchlist'

const metrics = [
  { label: 'Market sessions', value: '3', note: 'London · New York · Asia', icon: Clock3 },
  { label: 'Pairs monitored', value: '8', note: 'Major currency pairs', icon: Radio },
  { label: 'Market state', value: 'Live', note: 'Monitoring enabled', icon: Activity },
]

export function Dashboard() {
  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Dashboard"
        title="Market overview"
        description="A focused view of major forex markets and system availability."
        action={
          <Link
            to="/markets"
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-cyan-500 px-4 py-2.5 text-sm font-semibold text-slate-950 shadow-lg shadow-cyan-500/20 transition hover:bg-cyan-400"
          >
            <TrendingUp aria-hidden="true" className="size-4" />
            Explore markets
          </Link>
        }
      />

      <section aria-label="Market summary" className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {metrics.map(({ label, value, note, icon: Icon }) => (
          <article
            key={label}
            className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-white/8 dark:bg-white/[0.035]"
          >
            <div className="flex items-start justify-between">
              <div>
                <p className="text-sm text-slate-500 dark:text-slate-400">{label}</p>
                <p className="mt-3 text-3xl font-bold tracking-tight text-slate-950 dark:text-white">
                  {value}
                </p>
              </div>
              <div className="rounded-xl bg-cyan-500/10 p-2.5 text-cyan-600 dark:text-cyan-400">
                <Icon aria-hidden="true" className="size-5" />
              </div>
            </div>
            <p className="mt-4 text-xs text-slate-400 dark:text-slate-500">{note}</p>
          </article>
        ))}
      </section>

      <Watchlist />
    </div>
  )
}

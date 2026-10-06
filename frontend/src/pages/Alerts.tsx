import { BellRing } from 'lucide-react'

import { PageHeader } from '../components/PageHeader'

export function Alerts() {
  return (
    <div className="space-y-8">
      <PageHeader eyebrow="Alerts" title="Market alerts" description="Review monitoring alerts for the currency pairs you follow." />
      <section className="grid min-h-72 place-items-center rounded-2xl border border-slate-200 bg-white p-8 text-center shadow-sm dark:border-white/8 dark:bg-white/[0.035]">
        <div>
          <BellRing aria-hidden="true" className="mx-auto size-9 text-cyan-500" />
          <h2 className="mt-4 font-semibold text-slate-800 dark:text-slate-100">No alerts to show</h2>
          <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">Market monitoring alerts will appear here.</p>
        </div>
      </section>
    </div>
  )
}

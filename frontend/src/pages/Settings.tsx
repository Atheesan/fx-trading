import { SlidersHorizontal } from 'lucide-react'

import { PageHeader } from '../components/PageHeader'

export function Settings() {
  return (
    <div className="space-y-8">
      <PageHeader eyebrow="Settings" title="Workspace settings" description="Manage local preferences for your market monitoring workspace." />
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-white/8 dark:bg-white/[0.035]">
        <div className="flex items-start gap-4">
          <div className="rounded-xl bg-cyan-500/10 p-3 text-cyan-600 dark:text-cyan-400">
            <SlidersHorizontal aria-hidden="true" className="size-5" />
          </div>
          <div>
            <h2 className="font-semibold text-slate-900 dark:text-white">Display preferences</h2>
            <p className="mt-1 text-sm leading-6 text-slate-500 dark:text-slate-400">Use the theme control in the top bar to switch between light and dark modes.</p>
          </div>
        </div>
      </section>
    </div>
  )
}

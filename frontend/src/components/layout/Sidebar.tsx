import {
  Bell,
  ChartCandlestick,
  LayoutDashboard,
  Settings,
} from 'lucide-react'
import { NavLink } from 'react-router-dom'

const navigation = [
  { label: 'Dashboard', to: '/', icon: LayoutDashboard, end: true },
  { label: 'Markets', to: '/markets', icon: ChartCandlestick },
  { label: 'Alerts', to: '/alerts', icon: Bell },
  { label: 'Settings', to: '/settings', icon: Settings },
]

function NavigationLinks() {
  return navigation.map(({ label, to, icon: Icon, end }) => (
    <NavLink
      key={to}
      to={to}
      end={end}
      className={({ isActive }) =>
        `group flex flex-1 items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors md:flex-none ${
          isActive
            ? 'bg-cyan-400/12 text-cyan-300'
            : 'text-slate-400 hover:bg-white/5 hover:text-slate-100'
        }`
      }
    >
      <Icon aria-hidden="true" className="size-5 shrink-0" />
      <span className="max-md:text-xs">{label}</span>
    </NavLink>
  ))
}

export function Sidebar() {
  return (
    <>
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 border-r border-white/8 bg-[#0b1220] md:flex md:flex-col">
        <div className="flex h-20 items-center gap-3 px-6">
          <div className="grid size-10 place-items-center rounded-xl bg-cyan-400 text-slate-950 shadow-lg shadow-cyan-500/20">
            <ChartCandlestick aria-hidden="true" className="size-5" />
          </div>
          <div>
            <p className="font-bold tracking-tight text-white">FXAI</p>
            <p className="text-xs text-slate-500">Market monitor</p>
          </div>
        </div>
        <nav aria-label="Primary navigation" className="flex flex-col gap-1 px-3 py-4">
          <NavigationLinks />
        </nav>
        <p className="mt-auto px-6 pb-6 text-xs leading-5 text-slate-600">
          Real-time forex intelligence
        </p>
      </aside>

      <nav
        aria-label="Mobile navigation"
        className="fixed inset-x-3 bottom-3 z-40 flex gap-1 rounded-2xl border border-white/10 bg-[#0b1220]/95 p-1.5 shadow-2xl backdrop-blur md:hidden"
      >
        <NavigationLinks />
      </nav>
    </>
  )
}

import { Outlet } from 'react-router-dom'

import { Sidebar } from './Sidebar'
import { TopBar } from './TopBar'

export function AppLayout() {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 transition-colors dark:bg-[#080d18] dark:text-slate-100">
      <Sidebar />
      <div className="md:pl-64">
        <TopBar />
        <main className="mx-auto max-w-7xl px-4 py-6 pb-24 sm:px-6 sm:py-8 md:pb-8 lg:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

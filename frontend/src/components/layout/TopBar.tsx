import { Moon, Sun } from 'lucide-react'
import { useEffect, useState } from 'react'

import { ConnectionStatus } from '../ConnectionStatus'

type Theme = 'light' | 'dark'

function initialTheme(): Theme {
  const savedTheme = localStorage.getItem('fxai-theme')
  if (savedTheme === 'light' || savedTheme === 'dark') return savedTheme

  return window.matchMedia?.('(prefers-color-scheme: dark)').matches
    ? 'dark'
    : 'light'
}

export function TopBar() {
  const [theme, setTheme] = useState<Theme>(initialTheme)

  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark')
    localStorage.setItem('fxai-theme', theme)
  }, [theme])

  return (
    <header className="sticky top-0 z-20 border-b border-slate-200/80 bg-white/85 backdrop-blur-xl dark:border-white/8 dark:bg-[#080d18]/85">
      <div className="flex h-16 items-center justify-between px-4 sm:px-6 md:h-20 lg:px-8">
        <div>
          <p className="text-lg font-bold tracking-tight text-slate-950 dark:text-white">
            FXAI
          </p>
          <p className="hidden text-xs text-slate-500 sm:block">
            Forex market intelligence
          </p>
        </div>

        <div className="flex items-center gap-2 sm:gap-4">
          <ConnectionStatus />

          <button
            type="button"
            onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}
            className="grid size-10 place-items-center rounded-xl border border-slate-200 bg-white text-slate-600 transition hover:border-cyan-400 hover:text-cyan-600 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-cyan-500 dark:border-white/10 dark:bg-white/5 dark:text-slate-300 dark:hover:text-cyan-300"
            aria-label={theme === 'light' ? 'Use dark theme' : 'Use light theme'}
          >
            {theme === 'light' ? (
              <Moon aria-hidden="true" className="size-5" />
            ) : (
              <Sun aria-hidden="true" className="size-5" />
            )}
          </button>
        </div>
      </div>
    </header>
  )
}

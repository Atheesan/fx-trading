import '@testing-library/jest-dom/vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import App from './App'

function jsonResponse(data: unknown, ok = true) {
  return {
    ok,
    status: ok ? 200 : 500,
    json: async () => data,
  }
}

function renderApp(route = '/') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[route]}>
        <App />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('App', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.classList.remove('dark')
  })

  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('shows a loading state while checking backend health', () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => undefined)))

    renderApp()

    expect(screen.getByRole('status')).toHaveTextContent('Backend: Checking')
  })

  it('shows the dashboard and connected backend state', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((input: string | URL | Request) => {
        const url = input.toString()
        if (url.endsWith('/api/v1/health')) {
          return Promise.resolve(
            jsonResponse({ status: 'UP', application: 'fxai-backend' }),
          )
        }
        if (url.endsWith('/api/v1/market/pairs')) {
          return Promise.resolve(jsonResponse([]))
        }
        return Promise.resolve(jsonResponse({}, false))
      }),
    )

    renderApp()

    expect(
      screen.getByRole('heading', { name: /market overview/i }),
    ).toBeInTheDocument()
    await screen.findByText('Backend: Connected')
    expect(screen.getByRole('status')).toHaveTextContent('Backend: Connected')
    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/v1/health',
      expect.objectContaining({ headers: { Accept: 'application/json' } }),
    )
  })

  it('shows an unavailable state when the health request fails', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('offline')))

    renderApp()

    await screen.findByText('Backend: Unavailable')
    expect(screen.getByRole('status')).toHaveTextContent('Backend: Unavailable')
  })

  it('renders pair details from the symbol route', () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => undefined)))

    renderApp('/markets/EURUSD')

    expect(
      screen.getByRole('heading', { name: 'EUR/USD market details' }),
    ).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: 'Markets' })[0]).toHaveAttribute(
      'aria-current',
      'page',
    )
  })

  it('toggles and persists the dark theme', () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => undefined)))

    renderApp()
    fireEvent.click(screen.getByRole('button', { name: 'Use dark theme' }))

    expect(document.documentElement).toHaveClass('dark')
    expect(localStorage.getItem('fxai-theme')).toBe('dark')
  })
})

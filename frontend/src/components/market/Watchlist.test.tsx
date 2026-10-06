import '@testing-library/jest-dom/vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { Watchlist } from './Watchlist'

const pairs = [
  {
    baseCurrency: 'EUR',
    quoteCurrency: 'USD',
    symbol: 'EURUSD',
    displayName: 'EUR/USD',
  },
  {
    baseCurrency: 'USD',
    quoteCurrency: 'JPY',
    symbol: 'USDJPY',
    displayName: 'USD/JPY',
  },
]

const prices = {
  EURUSD: {
    symbol: 'EURUSD',
    bid: 1.0842,
    ask: 1.0844,
    midpoint: 1.0843,
    spread: 0.0002,
    timestamp: '2026-01-02T03:04:05Z',
  },
  USDJPY: {
    symbol: 'USDJPY',
    bid: 149.62,
    ask: 149.65,
    midpoint: 149.635,
    spread: 0.03,
    timestamp: '2026-01-02T03:04:05Z',
  },
}

function jsonResponse(data: unknown, ok = true) {
  return {
    ok,
    status: ok ? 200 : 500,
    json: async () => data,
  }
}

function renderWatchlist() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <Watchlist />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('Watchlist', () => {
  it('renders loading skeletons while backend availability is checked', () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => undefined)))

    renderWatchlist()

    expect(
      screen.getByLabelText('Loading market watchlist'),
    ).toBeInTheDocument()
  })

  it('renders pair prices and links rows to pair details', async () => {
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
          return Promise.resolve(jsonResponse(pairs))
        }
        if (url.endsWith('/api/v1/market/prices/EURUSD')) {
          return Promise.resolve(jsonResponse(prices.EURUSD))
        }
        if (url.endsWith('/api/v1/market/prices/USDJPY')) {
          return Promise.resolve(jsonResponse(prices.USDJPY))
        }
        return Promise.resolve(jsonResponse({}, false))
      }),
    )

    renderWatchlist()

    const euroDollar = await screen.findByRole('link', {
      name: 'View EUR/USD market',
    })
    expect(euroDollar).toHaveAttribute('href', '/markets/EURUSD')
    expect(screen.getByText('1.08420')).toBeInTheDocument()
    expect(screen.getByText('1.08440')).toBeInTheDocument()
    expect(screen.getByText('0.00020')).toBeInTheDocument()
    expect(euroDollar.querySelector('time')).toHaveAttribute(
      'datetime',
      prices.EURUSD.timestamp,
    )
  })

  it('renders a backend unavailable state when health checking fails', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('offline')))

    renderWatchlist()

    expect(
      await screen.findByRole('heading', { name: 'Backend unavailable' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument()
  })

  it('renders an API failure state when pairs cannot be loaded', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((input: string | URL | Request) => {
        const url = input.toString()
        if (url.endsWith('/api/v1/health')) {
          return Promise.resolve(
            jsonResponse({ status: 'UP', application: 'fxai-backend' }),
          )
        }
        return Promise.resolve(jsonResponse({}, false))
      }),
    )

    renderWatchlist()

    expect(
      await screen.findByRole('heading', { name: 'Market data unavailable' }),
    ).toBeInTheDocument()
  })

  it('renders an empty state when no pairs are available', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((input: string | URL | Request) => {
        const url = input.toString()
        if (url.endsWith('/api/v1/health')) {
          return Promise.resolve(
            jsonResponse({ status: 'UP', application: 'fxai-backend' }),
          )
        }
        return Promise.resolve(jsonResponse([]))
      }),
    )

    renderWatchlist()

    expect(
      await screen.findByRole('heading', { name: 'No currency pairs' }),
    ).toBeInTheDocument()
  })
})

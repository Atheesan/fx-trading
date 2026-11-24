import '@testing-library/jest-dom/vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { createBackendWatchlistRepository } from '../../watchlist/backendWatchlistRepository'
import { defaultWatchlistState, type WatchlistState } from '../../watchlist/watchlist'
vi.mock('../../api/client', async importOriginal => {
  const original = await importOriginal<typeof import('../../api/client')>()
  return { ...original, currentUserId: () => 'alice' }
})
let serverState: WatchlistState
let repository = createBackendWatchlistRepository()
import { WatchlistProvider } from '../../watchlist/WatchlistProvider'
import { Watchlist } from './Watchlist'

const pairs = [
  'EURUSD',
  'GBPUSD',
  'USDJPY',
  'AUDUSD',
  'NZDUSD',
  'USDCAD',
  'USDCHF',
  'EURGBP',
].map((symbol) => ({
  baseCurrency: symbol.slice(0, 3),
  quoteCurrency: symbol.slice(3),
  symbol,
  displayName: `${symbol.slice(0, 3)}/${symbol.slice(3)}`,
}))

function jsonResponse(data: unknown, ok = true) {
  return {
    ok,
    status: ok ? 200 : 500,
    json: async () => data,
  }
}

function successfulFetch(input: string | URL | Request, init?: RequestInit) {
  const url = input.toString()
  if (url.endsWith('/api/v1/auth/csrf')) return Promise.resolve(jsonResponse({ token: 'csrf', headerName: 'X-XSRF-TOKEN' }))
  if (url.endsWith('/api/v1/watchlist')) {
    if (init?.method === 'PUT') serverState = JSON.parse(init.body as string) as WatchlistState
    return Promise.resolve(jsonResponse(serverState))
  }
  if (url.endsWith('/api/v1/health')) {
    return Promise.resolve(
      jsonResponse({ status: 'UP', application: 'fxai-backend' }),
    )
  }
  if (url.endsWith('/api/v1/market/pairs')) {
    return Promise.resolve(jsonResponse(pairs))
  }

  const symbol = url.split('/').at(-1) ?? ''
  return Promise.resolve(jsonResponse({
    symbol,
    bid: 1.0842,
    ask: 1.0844,
    midpoint: 1.0843,
    spread: 0.0002,
    timestamp: '2026-01-02T03:04:05Z',
  }))
}

function renderWatchlist() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <WatchlistProvider repository={repository}>
          <Watchlist />
        </WatchlistProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function renderedSymbols() {
  return screen.getAllByRole('article').map(
    (row) => row.getAttribute('data-symbol'),
  )
}

beforeEach(() => {
  localStorage.clear()
  serverState = structuredClone(defaultWatchlistState)
  repository = createBackendWatchlistRepository()
  vi.stubGlobal('fetch', vi.fn(successfulFetch))
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('Watchlist', () => {
  it('loads the five V1 default pairs from the backend', async () => {
    renderWatchlist()

    await screen.findByRole('button', { name: 'Select EUR/USD' })
    expect(renderedSymbols()).toEqual([
      'EURUSD',
      'GBPUSD',
      'USDJPY',
      'AUDUSD',
      'NZDUSD',
    ])
    expect(screen.getByRole('button', { name: 'Select EUR/USD' }))
      .toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByText('5 pairs')).toBeInTheDocument()
  })

  it('adds an available pair and persists it', async () => {
    renderWatchlist()
    const pairSelect = await screen.findByRole('combobox', {
      name: 'Currency pair to add',
    })
    await waitFor(() => expect(pairSelect).toBeEnabled())

    fireEvent.change(pairSelect, { target: { value: 'USDCHF' } })
    fireEvent.click(screen.getByRole('button', { name: 'Add' }))

    await waitFor(() => {
      expect(renderedSymbols()).toEqual([
        'EURUSD',
        'GBPUSD',
        'USDJPY',
        'AUDUSD',
        'NZDUSD',
        'USDCHF',
      ])
      expect(serverState)
        .toMatchObject({ symbols: expect.arrayContaining(['USDCHF']) })
    })
  })

  it('removes and reorders pairs', async () => {
    renderWatchlist()

    await screen.findByRole('button', { name: 'Select EUR/USD' })
    fireEvent.click(screen.getByRole('button', { name: 'Move GBP/USD up' }))
    expect(renderedSymbols().slice(0, 2)).toEqual(['GBPUSD', 'EURUSD'])

    fireEvent.click(screen.getByRole('button', { name: 'Remove AUD/USD' }))
    expect(renderedSymbols()).not.toContain('AUDUSD')

    await waitFor(() => {
      expect(serverState)
        .toMatchObject({
          symbols: ['GBPUSD', 'EURUSD', 'USDJPY', 'NZDUSD'],
        })
    })
  })

  it('selects a pair and restores the persisted watchlist', async () => {
    serverState = { symbols: ['USDCHF', 'EURGBP'], selectedSymbol: 'EURGBP' }

    renderWatchlist()

    await waitFor(() => {
      expect(renderedSymbols()).toEqual(['USDCHF', 'EURGBP'])
    })
    expect(screen.getByRole('button', { name: 'Select EUR/GBP' }))
      .toHaveAttribute('aria-pressed', 'true')

    fireEvent.click(screen.getByRole('button', { name: 'Select USD/CHF' }))
    expect(screen.getByRole('button', { name: 'Select USD/CHF' }))
      .toHaveAttribute('aria-pressed', 'true')

    await waitFor(() => {
      expect(serverState)
        .toMatchObject({ selectedSymbol: 'USDCHF' })
    })
  })

  it('reports backend persistence failure without overwriting saved data', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('offline')))
    renderWatchlist()

    expect(
      await screen.findByText('Your watchlist could not be loaded. Reload to try again.'),
    ).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Remove GBP/USD' }))
    expect(screen.queryByRole('button', { name: 'Select GBP/USD' }))
      .not.toBeInTheDocument()
  })
})

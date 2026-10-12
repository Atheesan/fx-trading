import '@testing-library/jest-dom/vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { ForexChart } from './ForexChart'

vi.mock('./LightweightCandlestickChart', () => ({
  LightweightCandlestickChart: ({ candles }: { candles: unknown[] }) => (
    <div>Rendered {candles.length} candles</div>
  ),
}))

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  })
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
}

function jsonResponse(data: unknown, ok = true) {
  return {
    ok,
    status: ok ? 200 : 503,
    json: async () => data,
  }
}

describe('ForexChart', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('shows pair, timeframe, loading state, and fetched candles', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse([{
      symbol: 'EURUSD',
      timeframe: '1h',
      timestamp: '2026-01-02T03:00:00Z',
      open: 1.1,
      high: 1.2,
      low: 1,
      close: 1.15,
    }]) as Response)

    render(<ForexChart symbol="EURUSD" />, { wrapper })

    expect(screen.getByLabelText('Loading chart data')).toBeInTheDocument()
    expect(screen.getByText('EUR/USD · 1h')).toBeInTheDocument()
    expect(await screen.findByText('Rendered 1 candles')).toBeInTheDocument()
    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/v1/market/candles/EURUSD?timeframe=1h&limit=500',
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    )

    fireEvent.click(screen.getByRole('button', { name: '5m' }))
    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/market/candles/EURUSD?timeframe=5m&limit=500',
        expect.any(Object),
      )
    })
  })

  it('shows the no-data state', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse([]) as Response)

    render(<ForexChart symbol="EURUSD" />, { wrapper })

    expect(await screen.findByText('No candle data')).toBeInTheDocument()
    expect(screen.getByText('No 1h candles are available for EUR/USD.')).toBeInTheDocument()
  })

  it('shows the API error state', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({}, false) as Response)

    render(<ForexChart symbol="EURUSD" />, { wrapper })

    expect(await screen.findByRole('alert')).toHaveTextContent('Chart unavailable')
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument()
  })
})

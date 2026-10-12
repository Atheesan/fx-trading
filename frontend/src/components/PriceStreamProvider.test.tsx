import '@testing-library/jest-dom/vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import type { PriceUpdateMessage, PriceStreamStatus } from '../api/priceStream'
import { marketQueryKeys } from '../queries/useMarketQueries'
import { PriceStreamProvider } from './PriceStreamProvider'
import { usePriceStreamStatus } from './priceStreamContext'

const streamMock = vi.hoisted(() => ({ subscribe: vi.fn() }))

vi.mock('../api/priceStream', () => ({
  marketPriceStream: streamMock,
}))

function StatusProbe() {
  return <span>{usePriceStreamStatus()}</span>
}

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
})

describe('PriceStreamProvider', () => {
  it('updates the shared price cache and cleans up its subscription', () => {
    const unsubscribe = vi.fn()
    let subscriber: {
      onPriceUpdate: (message: PriceUpdateMessage) => void
      onStatusChange: (status: PriceStreamStatus) => void
    } | undefined
    streamMock.subscribe.mockImplementation((value) => {
      subscriber = value
      return unsubscribe
    })
    const queryClient = new QueryClient()

    const { unmount } = render(
      <QueryClientProvider client={queryClient}>
        <PriceStreamProvider><StatusProbe /></PriceStreamProvider>
      </QueryClientProvider>,
    )

    act(() => {
      subscriber?.onStatusChange('LIVE')
      subscriber?.onPriceUpdate({
        type: 'PRICE_UPDATE',
        symbol: 'EURUSD',
        bid: 1.1,
        ask: 1.2,
        mid: 1.15,
        spread: 0.1,
        timestamp: '2026-01-02T03:04:05Z',
      })
    })

    expect(screen.getByText('LIVE')).toBeInTheDocument()
    expect(queryClient.getQueryData(marketQueryKeys.price('EURUSD'))).toEqual({
      symbol: 'EURUSD',
      bid: 1.1,
      ask: 1.2,
      midpoint: 1.15,
      spread: 0.1,
      timestamp: '2026-01-02T03:04:05Z',
    })

    unmount()
    expect(unsubscribe).toHaveBeenCalledOnce()
  })
})

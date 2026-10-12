import '@testing-library/jest-dom/vitest'
import { act, cleanup, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { Candle } from '../../api/market'
import { LightweightCandlestickChart } from './LightweightCandlestickChart'

const chartMock = vi.hoisted(() => {
  const series = { setData: vi.fn() }
  const chart = {
    addSeries: vi.fn(() => series),
    applyOptions: vi.fn(),
    remove: vi.fn(),
    subscribeCrosshairMove: vi.fn(),
    unsubscribeCrosshairMove: vi.fn(),
    timeScale: vi.fn(() => ({ fitContent: vi.fn() })),
  }
  return { chart, series, createChart: vi.fn(() => chart) }
})

vi.mock('lightweight-charts', () => ({
  CandlestickSeries: {},
  ColorType: { Solid: 'solid' },
  CrosshairMode: { Normal: 0 },
  createChart: chartMock.createChart,
}))

const candle: Candle = {
  symbol: 'EURUSD',
  timeframe: '1h',
  timestamp: '2026-01-02T03:00:00Z',
  open: 1.1,
  high: 1.2,
  low: 1,
  close: 1.15,
  volume: 42,
}

describe('LightweightCandlestickChart', () => {
  let disconnect: ReturnType<typeof vi.fn>

  beforeEach(() => {
    vi.clearAllMocks()
    disconnect = vi.fn()
    vi.stubGlobal('ResizeObserver', class {
      observe = vi.fn()
      disconnect = disconnect
    })
  })

  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('renders candle details and updates them from the crosshair', () => {
    render(<LightweightCandlestickChart candles={[candle]} />)

    expect(screen.getByLabelText('Candle details')).toHaveTextContent('2026-01-02T03:00:00Z')
    expect(chartMock.chart.addSeries).toHaveBeenCalledWith(
      expect.any(Object),
      expect.objectContaining({ upColor: '#14b8a6', downColor: '#f43f5e' }),
    )

    const handler = chartMock.chart.subscribeCrosshairMove.mock.calls[0]?.[0]
    act(() => {
      handler?.({
        time: 1_767_322_800,
        seriesData: new Map([
          [chartMock.series, { time: 1_767_322_800, open: 1.2, high: 1.3, low: 1.1, close: 1.25 }],
        ]),
      })
    })

    expect(screen.getByLabelText('Candle details')).toHaveTextContent('1.25')
  })

  it('unsubscribes, disconnects resizing, and removes the chart on cleanup', () => {
    const { unmount } = render(<LightweightCandlestickChart candles={[candle]} />)
    const handler = chartMock.chart.subscribeCrosshairMove.mock.calls[0]?.[0]

    unmount()

    expect(chartMock.chart.unsubscribeCrosshairMove).toHaveBeenCalledWith(handler)
    expect(disconnect).toHaveBeenCalledOnce()
    expect(chartMock.chart.remove).toHaveBeenCalledOnce()
  })
})

import '@testing-library/jest-dom/vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import type { TechnicalIndicator } from '../../api/market'
import { IndicatorPanel } from './IndicatorPanel'

function indicator(
  overrides: Partial<TechnicalIndicator> & Pick<TechnicalIndicator, 'name' | 'type'>,
): TechnicalIndicator {
  return {
    status: 'AVAILABLE',
    period: 20,
    requiredDataPoints: 20,
    availableDataPoints: 100,
    value: 1.12345,
    signal: null,
    histogram: null,
    points: [],
    ...overrides,
  }
}

afterEach(cleanup)

describe('IndicatorPanel', () => {
  it('displays the five descriptive V1 indicators', () => {
    render(<IndicatorPanel
      timeframe="1h"
      loading={false}
      error={false}
      onRetry={vi.fn()}
      indicators={[
        indicator({ name: 'sma20', type: 'SMA' }),
        indicator({ name: 'sma50', type: 'SMA', period: 50 }),
        indicator({ name: 'ema20', type: 'EMA' }),
        indicator({ name: 'rsi14', type: 'RSI', period: 14, value: 62.4 }),
        indicator({
          name: 'macd',
          type: 'MACD',
          period: null,
          value: 0.0012,
          signal: 0.0009,
          histogram: 0.0003,
        }),
      ]}
    />)

    for (const name of ['SMA20', 'SMA50', 'EMA20', 'RSI14', 'MACD']) {
      expect(screen.getByRole('heading', { name })).toBeInTheDocument()
    }
    expect(screen.getByText('Signal')).toBeInTheDocument()
    expect(screen.getByText('Histogram')).toBeInTheDocument()
    expect(screen.queryByText(/buy|sell|long|short/i)).not.toBeInTheDocument()
  })

  it('reports insufficient historical data explicitly', () => {
    render(<IndicatorPanel
      timeframe="1h"
      loading={false}
      error={false}
      onRetry={vi.fn()}
      indicators={[
        indicator({
          name: 'sma50',
          type: 'SMA',
          period: 50,
          status: 'INSUFFICIENT_DATA',
          availableDataPoints: 30,
          requiredDataPoints: 50,
          value: null,
        }),
      ]}
    />)

    expect(screen.getByText('Insufficient data (30/50)')).toBeInTheDocument()
  })

  it('offers a retry when indicators cannot be loaded', () => {
    const onRetry = vi.fn()
    render(<IndicatorPanel
      timeframe="1h"
      loading={false}
      error
      onRetry={onRetry}
    />)

    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(onRetry).toHaveBeenCalledOnce()
  })
})

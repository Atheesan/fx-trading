import '@testing-library/jest-dom/vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { Alerts } from './Alerts'

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

const alerts = [
  {
    id: 'active-alert',
    symbol: 'EURUSD',
    condition: 'PRICE_ABOVE',
    targetPrice: 1.125,
    status: 'ACTIVE',
    createdAt: '2026-01-02T03:00:00Z',
    triggeredAt: null,
  },
  {
    id: 'triggered-alert',
    symbol: 'USDJPY',
    condition: 'PRICE_BELOW',
    targetPrice: 149.5,
    status: 'TRIGGERED',
    createdAt: '2026-01-02T02:00:00Z',
    triggeredAt: '2026-01-02T03:04:05Z',
  },
]

function jsonResponse(data: unknown, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => data,
  }
}

function renderAlerts() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <Alerts />
    </QueryClientProvider>,
  )
}

beforeEach(() => {
  vi.stubGlobal('fetch', vi.fn((
    input: string | URL | Request,
    init?: RequestInit,
  ) => {
    const url = input.toString()
    if (url.endsWith('/api/v1/auth/csrf')) return Promise.resolve(jsonResponse({ token: 'csrf', headerName: 'X-XSRF-TOKEN' }))
    if (url.endsWith('/api/v1/market/pairs')) {
      return Promise.resolve(jsonResponse(pairs))
    }
    if (url.endsWith('/api/v1/alerts')) {
      if (init?.method === 'POST') {
        return Promise.resolve(jsonResponse(alerts[0], 201))
      }
      return Promise.resolve(jsonResponse(alerts))
    }
    if (url.includes('/api/v1/alerts/') && init?.method === 'DELETE') {
      return Promise.resolve(jsonResponse(null, 204))
    }
    return Promise.resolve(jsonResponse({}, 404))
  }))
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('Alerts', () => {
  it('displays active and triggered alerts separately', async () => {
    renderAlerts()

    expect(await screen.findByRole('heading', { name: 'Active alerts' }, { timeout: 5000 }))
      .toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Triggered alerts' }))
      .toBeInTheDocument()
    expect(screen.getByText('1.125')).toBeInTheDocument()
    expect(screen.getByText('149.50')).toBeInTheDocument()
    expect(screen.getByText(/Triggered/, { selector: 'time' })).toBeInTheDocument()
  })

  it('creates an alert', async () => {
    renderAlerts()
    await screen.findByRole('option', { name: 'USD/JPY' })

    fireEvent.change(screen.getByLabelText('Currency pair'), {
      target: { value: 'USDJPY' },
    })
    fireEvent.change(screen.getByLabelText('Condition'), {
      target: { value: 'PRICE_BELOW' },
    })
    fireEvent.change(screen.getByLabelText('Target price'), {
      target: { value: '149.25' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Create alert' }))

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/alerts',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({
            symbol: 'USDJPY',
            condition: 'PRICE_BELOW',
            targetPrice: 149.25,
          }),
        }),
      )
    })
  })

  it('deletes an alert', async () => {
    renderAlerts()
    const deleteButton = await screen.findByRole('button', {
      name: 'Delete EUR/USD price above alert',
    })

    fireEvent.click(deleteButton)

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/alerts/active-alert',
        expect.objectContaining({ method: 'DELETE' }),
      )
    })
  })
})

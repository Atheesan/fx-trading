import '@testing-library/jest-dom/vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { ConnectionStatus } from './ConnectionStatus'

const statusMocks = vi.hoisted(() => ({ priceStatus: vi.fn() }))

vi.mock('../queries/useHealthQuery', () => ({
  useHealthQuery: () => ({
    isPending: false,
    isSuccess: true,
    data: { status: 'UP' },
  }),
}))

vi.mock('./priceStreamContext', () => ({
  usePriceStreamStatus: statusMocks.priceStatus,
}))

afterEach(cleanup)

describe('ConnectionStatus', () => {
  it.each(['LIVE', 'RECONNECTING', 'DISCONNECTED'] as const)(
    'displays the %s price stream state',
    (status) => {
      statusMocks.priceStatus.mockReturnValue(status)
      render(<ConnectionStatus />)

      expect(screen.getByRole('status')).toHaveTextContent(status)
    },
  )
})

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { PriceStreamClient, type PriceStreamStatus } from './priceStream'

class FakeWebSocket {
  onopen: ((event: Event) => void) | null = null
  onmessage: ((event: MessageEvent) => void) | null = null
  onerror: ((event: Event) => void) | null = null
  onclose: ((event: CloseEvent) => void) | null = null
  close = vi.fn()

  open() {
    this.onopen?.(new Event('open'))
  }

  message(data: string) {
    this.onmessage?.(new MessageEvent('message', { data }))
  }

  closed() {
    this.onclose?.(new CloseEvent('close'))
  }
}

describe('PriceStreamClient', () => {
  const sockets: FakeWebSocket[] = []
  const socketFactory = vi.fn(() => {
    const socket = new FakeWebSocket()
    sockets.push(socket)
    return socket as unknown as WebSocket
  })

  beforeEach(() => {
    sockets.length = 0
    socketFactory.mockClear()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('shares one connection, delivers updates, and cleans up after the last subscriber', () => {
    const client = new PriceStreamClient('ws://localhost/ws/market', socketFactory)
    const firstStatuses: PriceStreamStatus[] = []
    const secondStatuses: PriceStreamStatus[] = []
    const firstUpdate = vi.fn()
    const secondUpdate = vi.fn()

    const unsubscribeFirst = client.subscribe({
      onPriceUpdate: firstUpdate,
      onStatusChange: (status) => firstStatuses.push(status),
    })
    const unsubscribeSecond = client.subscribe({
      onPriceUpdate: secondUpdate,
      onStatusChange: (status) => secondStatuses.push(status),
    })

    expect(socketFactory).toHaveBeenCalledOnce()
    sockets[0]?.open()
    sockets[0]?.message(JSON.stringify({
      type: 'PRICE_UPDATE',
      symbol: 'EURUSD',
      bid: 1.1,
      ask: 1.2,
      mid: 1.15,
      spread: 0.1,
      timestamp: '2026-01-02T03:04:05Z',
    }))

    expect(firstStatuses).toContain('LIVE')
    expect(secondStatuses).toContain('LIVE')
    expect(firstUpdate).toHaveBeenCalledOnce()
    expect(secondUpdate).toHaveBeenCalledOnce()

    unsubscribeFirst()
    expect(sockets[0]?.close).not.toHaveBeenCalled()
    unsubscribeSecond()
    expect(sockets[0]?.close).toHaveBeenCalledOnce()
  })

  it('reconnects with exponential delays and stops after the bounded attempt count', () => {
    const client = new PriceStreamClient('ws://localhost/ws/market', socketFactory)
    const statuses: PriceStreamStatus[] = []
    client.subscribe({ onPriceUpdate: vi.fn(), onStatusChange: (status) => statuses.push(status) })

    const delays = [1_000, 2_000, 4_000, 8_000, 16_000]
    delays.forEach((delay, index) => {
      sockets[index]?.closed()
      vi.advanceTimersByTime(delay - 1)
      expect(socketFactory).toHaveBeenCalledTimes(index + 1)
      vi.advanceTimersByTime(1)
      expect(socketFactory).toHaveBeenCalledTimes(index + 2)
    })
    sockets[5]?.closed()

    expect(statuses.at(-1)).toBe('DISCONNECTED')
    vi.advanceTimersByTime(30_000)
    expect(socketFactory).toHaveBeenCalledTimes(6)
  })
})

import { API_BASE_URL } from './client'

export type PriceStreamStatus = 'LIVE' | 'RECONNECTING' | 'DISCONNECTED'

export interface PriceUpdateMessage {
  type: 'PRICE_UPDATE'
  symbol: string
  bid: number
  ask: number
  mid: number
  spread: number
  timestamp: string
}

interface PriceStreamSubscriber {
  onPriceUpdate: (message: PriceUpdateMessage) => void
  onStatusChange: (status: PriceStreamStatus) => void
}

type SocketFactory = (url: string) => WebSocket

const INITIAL_RECONNECT_DELAY = 1_000
const MAX_RECONNECT_DELAY = 30_000
const MAX_RECONNECT_ATTEMPTS = 5

export function marketPriceWebSocketUrl() {
  const url = new URL('/ws/market', API_BASE_URL)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  return url.toString()
}

function parsePriceUpdate(data: unknown): PriceUpdateMessage | null {
  if (typeof data !== 'string') return null

  try {
    const message: unknown = JSON.parse(data)
    if (
      typeof message !== 'object' ||
      message === null ||
      !('type' in message) ||
      message.type !== 'PRICE_UPDATE' ||
      !('symbol' in message) ||
      typeof message.symbol !== 'string' ||
      !('bid' in message) ||
      typeof message.bid !== 'number' ||
      !('ask' in message) ||
      typeof message.ask !== 'number' ||
      !('mid' in message) ||
      typeof message.mid !== 'number' ||
      !('spread' in message) ||
      typeof message.spread !== 'number' ||
      !('timestamp' in message) ||
      typeof message.timestamp !== 'string'
    ) {
      return null
    }
    return message as PriceUpdateMessage
  } catch {
    return null
  }
}

export class PriceStreamClient {
  private readonly subscribers = new Set<PriceStreamSubscriber>()
  private readonly url: string
  private readonly socketFactory: SocketFactory
  private socket: WebSocket | null = null
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private reconnectAttempts = 0
  private status: PriceStreamStatus = 'DISCONNECTED'

  constructor(
    url = marketPriceWebSocketUrl(),
    socketFactory: SocketFactory = (socketUrl) => new WebSocket(socketUrl),
  ) {
    this.url = url
    this.socketFactory = socketFactory
  }

  subscribe(subscriber: PriceStreamSubscriber) {
    this.subscribers.add(subscriber)
    subscriber.onStatusChange(this.status)
    if (this.subscribers.size === 1) this.connect()

    return () => {
      this.subscribers.delete(subscriber)
      if (this.subscribers.size === 0) this.disconnect()
    }
  }

  private connect() {
    if (this.socket || this.subscribers.size === 0) return

    this.setStatus('RECONNECTING')
    let socket: WebSocket
    try {
      socket = this.socketFactory(this.url)
    } catch {
      this.scheduleReconnect()
      return
    }
    this.socket = socket

    socket.onopen = () => {
      if (this.socket !== socket) return
      this.reconnectAttempts = 0
      this.setStatus('LIVE')
    }
    socket.onmessage = (event) => {
      if (this.socket !== socket) return
      const message = parsePriceUpdate(event.data)
      if (message) {
        this.subscribers.forEach((subscriber) => subscriber.onPriceUpdate(message))
      }
    }
    socket.onerror = () => {
      if (this.socket === socket) socket.close()
    }
    socket.onclose = () => {
      if (this.socket !== socket) return
      this.socket = null
      this.scheduleReconnect()
    }
  }

  private scheduleReconnect() {
    if (this.subscribers.size === 0) {
      this.setStatus('DISCONNECTED')
      return
    }
    if (this.reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
      this.setStatus('DISCONNECTED')
      return
    }

    const delay = Math.min(
      INITIAL_RECONNECT_DELAY * 2 ** this.reconnectAttempts,
      MAX_RECONNECT_DELAY,
    )
    this.reconnectAttempts += 1
    this.setStatus('RECONNECTING')
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      this.connect()
    }, delay)
  }

  private disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    const socket = this.socket
    this.socket = null
    if (socket) {
      socket.onopen = null
      socket.onmessage = null
      socket.onerror = null
      socket.onclose = null
      socket.close()
    }
    this.reconnectAttempts = 0
    this.setStatus('DISCONNECTED')
  }

  private setStatus(status: PriceStreamStatus) {
    this.status = status
    this.subscribers.forEach((subscriber) => subscriber.onStatusChange(status))
  }
}

export const marketPriceStream = new PriceStreamClient()

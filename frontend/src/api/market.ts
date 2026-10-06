import { apiClient } from './client'

export interface CurrencyPair {
  baseCurrency: string
  quoteCurrency: string
  symbol: string
  displayName: string
}

export interface PriceQuote {
  symbol: string
  bid: number
  ask: number
  midpoint: number
  spread: number
  timestamp: string
}

export function getCurrencyPairs(signal?: AbortSignal) {
  return apiClient.get<CurrencyPair[]>('/api/v1/market/pairs', signal)
}

export function getPrice(symbol: string, signal?: AbortSignal) {
  return apiClient.get<PriceQuote>(
    `/api/v1/market/prices/${encodeURIComponent(symbol)}`,
    signal,
  )
}

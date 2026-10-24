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

export const candleTimeframes = ['1m', '5m', '15m', '1h', '1d'] as const

export type CandleTimeframe = (typeof candleTimeframes)[number]

export interface Candle {
  symbol: string
  timeframe: CandleTimeframe
  timestamp: string
  open: number
  high: number
  low: number
  close: number
  volume?: number
}

export const technicalIndicatorNames = [
  'sma20',
  'sma50',
  'ema20',
  'rsi14',
  'macd',
] as const

export type TechnicalIndicatorName = (typeof technicalIndicatorNames)[number]
export type TechnicalIndicatorStatus = 'AVAILABLE' | 'INSUFFICIENT_DATA'

export interface TechnicalIndicatorPoint {
  timestamp: string
  value: number
  signal: number | null
  histogram: number | null
}

export interface TechnicalIndicator {
  name: TechnicalIndicatorName
  type: 'SMA' | 'EMA' | 'RSI' | 'MACD'
  status: TechnicalIndicatorStatus
  period: number | null
  requiredDataPoints: number
  availableDataPoints: number
  value: number | null
  signal: number | null
  histogram: number | null
  points: TechnicalIndicatorPoint[]
}

export interface MarketIndicators {
  symbol: string
  timeframe: CandleTimeframe
  indicators: TechnicalIndicator[]
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

export function getCandles(
  symbol: string,
  timeframe: CandleTimeframe,
  limit: number,
  signal?: AbortSignal,
) {
  const parameters = new URLSearchParams({
    timeframe,
    limit: limit.toString(),
  })

  return apiClient.get<Candle[]>(
    `/api/v1/market/candles/${encodeURIComponent(symbol)}?${parameters}`,
    signal,
  )
}

export function getIndicators(
  symbol: string,
  timeframe: CandleTimeframe,
  indicators: readonly TechnicalIndicatorName[],
  signal?: AbortSignal,
) {
  const parameters = new URLSearchParams({
    timeframe,
    indicators: indicators.join(','),
  })

  return apiClient.get<MarketIndicators>(
    `/api/v1/market/indicators/${encodeURIComponent(symbol)}?${parameters}`,
    signal,
  )
}

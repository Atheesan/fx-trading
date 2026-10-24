import { useQueries, useQuery } from '@tanstack/react-query'

import {
  getCandles,
  getCurrencyPairs,
  getIndicators,
  getPrice,
  type CandleTimeframe,
  type TechnicalIndicatorName,
} from '../api/market'

export const marketQueryKeys = {
  pairs: ['market', 'pairs'] as const,
  price: (symbol: string) => ['market', 'prices', symbol] as const,
  candles: (symbol: string, timeframe: CandleTimeframe, limit: number) =>
    ['market', 'candles', symbol, timeframe, limit] as const,
  indicators: (
    symbol: string,
    timeframe: CandleTimeframe,
    indicators: readonly TechnicalIndicatorName[],
  ) => ['market', 'indicators', symbol, timeframe, ...indicators] as const,
}

export function useCandlesQuery(
  symbol: string,
  timeframe: CandleTimeframe,
  limit = 500,
) {
  return useQuery({
    queryKey: marketQueryKeys.candles(symbol, timeframe, limit),
    queryFn: ({ signal }) => getCandles(symbol, timeframe, limit, signal),
    enabled: symbol.length > 0,
    staleTime: 30_000,
  })
}

export function useIndicatorsQuery(
  symbol: string,
  timeframe: CandleTimeframe,
  indicators: readonly TechnicalIndicatorName[],
) {
  return useQuery({
    queryKey: marketQueryKeys.indicators(symbol, timeframe, indicators),
    queryFn: ({ signal }) => getIndicators(symbol, timeframe, indicators, signal),
    enabled: symbol.length > 0 && indicators.length > 0,
    staleTime: 30_000,
  })
}

export function useCurrencyPairsQuery(enabled = true) {
  return useQuery({
    queryKey: marketQueryKeys.pairs,
    queryFn: ({ signal }) => getCurrencyPairs(signal),
    enabled,
    staleTime: 5 * 60_000,
  })
}

export function usePriceQuery(symbol: string, enabled = true) {
  return useQuery({
    queryKey: marketQueryKeys.price(symbol),
    queryFn: ({ signal }) => getPrice(symbol, signal),
    enabled: enabled && symbol.length > 0,
  })
}

export function usePriceQueries(symbols: string[], enabled = true) {
  return useQueries({
    queries: symbols.map((symbol) => ({
      queryKey: marketQueryKeys.price(symbol),
      queryFn: ({ signal }: { signal: AbortSignal }) => getPrice(symbol, signal),
      enabled,
    })),
  })
}

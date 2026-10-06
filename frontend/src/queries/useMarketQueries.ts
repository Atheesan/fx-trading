import { useQueries, useQuery } from '@tanstack/react-query'

import { getCurrencyPairs, getPrice } from '../api/market'

export const marketQueryKeys = {
  pairs: ['market', 'pairs'] as const,
  price: (symbol: string) => ['market', 'prices', symbol] as const,
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
    refetchInterval: 10_000,
  })
}

export function usePriceQueries(symbols: string[], enabled = true) {
  return useQueries({
    queries: symbols.map((symbol) => ({
      queryKey: marketQueryKeys.price(symbol),
      queryFn: ({ signal }: { signal: AbortSignal }) => getPrice(symbol, signal),
      enabled,
      refetchInterval: 10_000,
    })),
  })
}

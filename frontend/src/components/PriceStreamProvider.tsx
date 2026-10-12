import { useQueryClient } from '@tanstack/react-query'
import { useEffect, useState, type ReactNode } from 'react'

import type { PriceQuote } from '../api/market'
import {
  marketPriceStream,
  type PriceStreamStatus,
} from '../api/priceStream'
import { marketQueryKeys } from '../queries/useMarketQueries'
import { PriceStreamContext } from './priceStreamContext'

export function PriceStreamProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [status, setStatus] = useState<PriceStreamStatus>('DISCONNECTED')

  useEffect(() => marketPriceStream.subscribe({
    onStatusChange: setStatus,
    onPriceUpdate: (message) => {
      const quote: PriceQuote = {
        symbol: message.symbol,
        bid: message.bid,
        ask: message.ask,
        midpoint: message.mid,
        spread: message.spread,
        timestamp: message.timestamp,
      }
      queryClient.setQueryData(marketQueryKeys.price(message.symbol), quote)
    },
  }), [queryClient])

  return (
    <PriceStreamContext.Provider value={status}>
      {children}
    </PriceStreamContext.Provider>
  )
}

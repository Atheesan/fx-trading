import { createContext, useContext } from 'react'

import type { PriceStreamStatus } from '../api/priceStream'

export const PriceStreamContext = createContext<PriceStreamStatus>('DISCONNECTED')

export function usePriceStreamStatus() {
  return useContext(PriceStreamContext)
}

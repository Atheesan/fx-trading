import { apiClient } from './client'

export type AlertCondition = 'PRICE_ABOVE' | 'PRICE_BELOW'
export type AlertStatus = 'ACTIVE' | 'TRIGGERED'

export interface PriceAlert {
  id: string
  symbol: string
  condition: AlertCondition
  targetPrice: number
  status: AlertStatus
  createdAt: string
  triggeredAt: string | null
}

export interface CreatePriceAlert {
  symbol: string
  condition: AlertCondition
  targetPrice: number
}

export function getAlerts(signal?: AbortSignal) {
  return apiClient.get<PriceAlert[]>('/api/v1/alerts', signal)
}

export function createAlert(alert: CreatePriceAlert) {
  return apiClient.post<PriceAlert>('/api/v1/alerts', alert)
}

export function deleteAlert(id: string) {
  return apiClient.delete(`/api/v1/alerts/${encodeURIComponent(id)}`)
}

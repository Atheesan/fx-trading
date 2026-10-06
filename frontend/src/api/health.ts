import { apiClient } from './client'

export interface HealthResponse {
  status: string
  application: string
}

export function getHealth(signal?: AbortSignal) {
  return apiClient.get<HealthResponse>('/api/v1/health', signal)
}

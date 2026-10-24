import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  createAlert,
  deleteAlert,
  getAlerts,
  type CreatePriceAlert,
} from '../api/alerts'

export const alertQueryKeys = {
  all: ['alerts'] as const,
}

export function useAlertsQuery() {
  return useQuery({
    queryKey: alertQueryKeys.all,
    queryFn: ({ signal }) => getAlerts(signal),
    refetchInterval: 5_000,
  })
}

export function useCreateAlertMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (alert: CreatePriceAlert) => createAlert(alert),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: alertQueryKeys.all })
    },
  })
}

export function useDeleteAlertMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => deleteAlert(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: alertQueryKeys.all })
    },
  })
}

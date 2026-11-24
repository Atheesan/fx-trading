import { useEffect, useState, useRef, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { refreshSession, subscribeSession } from '../api/client'

import { AuthContext } from './authContext'
export function AuthProvider({ children }: { children: ReactNode }) {
  const [userId, setUserId] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const previousUser = useRef<string | null>(null)
  const queryClient = useQueryClient()
  useEffect(() => {
    const unsubscribe = subscribeSession(session => {
      if (previousUser.current !== (session?.userId ?? null)) queryClient.clear()
      previousUser.current = session?.userId ?? null
      setUserId(session?.userId ?? null)
    })
    void refreshSession().catch(() => undefined).finally(() => setLoading(false))
    return unsubscribe
  }, [queryClient])
  return <AuthContext.Provider value={{ userId, loading }}>{children}</AuthContext.Provider>
}

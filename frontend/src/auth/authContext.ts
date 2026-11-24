import { createContext, useContext } from 'react'

export const AuthContext = createContext<{ userId: string | null; loading: boolean }>({ userId: null, loading: false })
export function useAuth() { return useContext(AuthContext) }

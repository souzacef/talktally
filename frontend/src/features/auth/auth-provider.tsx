import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useSyncExternalStore,
  type ReactNode,
} from 'react'
import type { QueryClient } from '@tanstack/react-query'
import { authApi, type AuthService } from '@/features/auth/api/auth-api'
import {
  authSession,
  type AuthSession,
  type SessionChangeReason,
} from '@/lib/auth/auth-session'
import { queryClient } from '@/lib/query/query-client'
import type {
  AuthenticationRequest,
  AuthenticationResponse,
  RegistrationRequest,
  UserAccountResponse,
} from '@/types/api'

interface AuthContextValue {
  token: string | null
  user: UserAccountResponse | null
  lastSessionChange: SessionChangeReason | null
  isAuthenticated: boolean
  signIn: (request: AuthenticationRequest) => Promise<AuthenticationResponse>
  register: (request: RegistrationRequest) => Promise<UserAccountResponse>
  signOut: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

interface AuthProviderProps {
  children: ReactNode
  session?: AuthSession
  service?: AuthService
  privateQueryClient?: QueryClient
}

export function AuthProvider({
  children,
  session = authSession,
  service = authApi,
  privateQueryClient = queryClient,
}: AuthProviderProps) {
  const token = useSyncExternalStore(
    session.subscribe,
    session.getToken,
    session.getServerToken,
  )
  const user = useSyncExternalStore(
    session.subscribe,
    session.getUser,
    session.getServerUser,
  )
  const lastSessionChange = useSyncExternalStore(
    session.subscribe,
    session.getLastChangeReason,
    session.getServerChangeReason,
  )

  useEffect(
    () => session.subscribeToReason((reason) => {
      if (reason === 'expired') {
        privateQueryClient.clear()
      }
    }),
    [privateQueryClient, session],
  )

  const signIn = useCallback(async (request: AuthenticationRequest) => {
    const response = await service.signIn(request)
    session.setAuthenticated(response.accessToken, response.user)
    return response
  }, [service, session])

  const register = useCallback(
    (request: RegistrationRequest) => service.register(request),
    [service],
  )

  const signOut = useCallback(() => {
    session.clear()
    privateQueryClient.clear()
  }, [privateQueryClient, session])

  const value = useMemo<AuthContextValue>(() => ({
    token,
    user,
    lastSessionChange,
    isAuthenticated: token !== null,
    signIn,
    register,
    signOut,
  }), [lastSessionChange, register, signIn, signOut, token, user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}

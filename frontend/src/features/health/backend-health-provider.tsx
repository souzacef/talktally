import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { backendHealthApi } from '@/features/health/backend-health-api'

export const SERVICE_STATUS_POLL_INTERVAL_MS = 2_500
export const SERVICE_STATUS_CHECKING_WINDOW_MS = 10_000
export const SERVICE_STATUS_SOFT_RETRY_MS = 150_000
export const SERVICE_STATUS_STARTUP_WINDOW_MS = 240_000

export type BackendHealthPhase =
  | 'checking'
  | 'waking'
  | 'still-waking'
  | 'ready'
  | 'unavailable'

interface BackendHealthContextValue {
  phase: BackendHealthPhase
  ready: boolean
  canRetry: boolean
  retry: () => void
}

const BackendHealthContext = createContext<BackendHealthContextValue | null>(null)

export function BackendHealthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [attempt, setAttempt] = useState(0)
  const [phase, setPhase] = useState<BackendHealthPhase>('checking')
  const queryKey = useMemo(() => ['backend-health', attempt] as const, [attempt])

  const query = useQuery({
    queryKey,
    queryFn: async ({ signal }) => {
      try {
        return await backendHealthApi.isUp(signal)
      } catch {
        return false
      }
    },
    retry: false,
    refetchInterval: (currentQuery) => (
      phase === 'unavailable' || currentQuery.state.data === true
        ? false
        : SERVICE_STATUS_POLL_INTERVAL_MS
    ),
  })

  const ready = query.data === true

  useEffect(() => {
    if (ready) {
      setPhase('ready')
      return
    }

    setPhase('checking')
    const wakingTimer = window.setTimeout(
      () => setPhase('waking'),
      SERVICE_STATUS_CHECKING_WINDOW_MS,
    )
    const softRetryTimer = window.setTimeout(
      () => setPhase('still-waking'),
      SERVICE_STATUS_SOFT_RETRY_MS,
    )
    const unavailableTimer = window.setTimeout(() => {
      setPhase('unavailable')
      void queryClient.cancelQueries({ queryKey, exact: true })
    }, SERVICE_STATUS_STARTUP_WINDOW_MS)

    return () => {
      window.clearTimeout(wakingTimer)
      window.clearTimeout(softRetryTimer)
      window.clearTimeout(unavailableTimer)
    }
  }, [attempt, queryClient, queryKey, ready])

  const retry = useCallback(() => {
    setPhase('checking')
    setAttempt((current) => current + 1)
  }, [])

  const value = useMemo<BackendHealthContextValue>(() => ({
    phase,
    ready,
    canRetry: phase === 'still-waking' || phase === 'unavailable',
    retry,
  }), [phase, ready, retry])

  return (
    <BackendHealthContext.Provider value={value}>
      {children}
    </BackendHealthContext.Provider>
  )
}

export function useBackendHealth(): BackendHealthContextValue {
  const context = useContext(BackendHealthContext)
  if (!context) {
    throw new Error('useBackendHealth must be used within BackendHealthProvider')
  }
  return context
}

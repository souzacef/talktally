import { useEffect, useState } from 'react'
import { CheckCircle2, LoaderCircle } from 'lucide-react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useLocale } from '@/app/providers/locale-provider'
import { Brand } from '@/components/brand/brand'
import { LocaleControl } from '@/components/layout/locale-control'
import { ThemeControl } from '@/components/layout/theme-control'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { authText } from '@/features/auth/auth-messages'
import { backendHealthApi } from '@/features/health/backend-health-api'

export const SERVICE_STATUS_POLL_INTERVAL_MS = 2_500
export const SERVICE_STATUS_STARTUP_WINDOW_MS = 60_000

export function BackendStatusPage() {
  const { locale } = useLocale()
  const text = (key: Parameters<typeof authText>[1]) => authText(locale, key)
  const queryClient = useQueryClient()
  const [attempt, setAttempt] = useState(0)
  const [timedOut, setTimedOut] = useState(false)
  const queryKey = ['backend-health', attempt] as const
  const query = useQuery({
    queryKey,
    queryFn: ({ signal }) => backendHealthApi.isUp(signal),
    retry: false,
    refetchInterval: (currentQuery) => (
      timedOut || currentQuery.state.data === true
        ? false
        : SERVICE_STATUS_POLL_INTERVAL_MS
    ),
  })

  const ready = query.data === true
  const checking = !ready && !timedOut

  useEffect(() => {
    if (ready) return
    const timeout = window.setTimeout(() => {
      setTimedOut(true)
      void queryClient.cancelQueries({ queryKey: ['backend-health', attempt], exact: true })
    }, SERVICE_STATUS_STARTUP_WINDOW_MS)
    return () => window.clearTimeout(timeout)
  }, [attempt, queryClient, ready])

  function retry() {
    setTimedOut(false)
    setAttempt((current) => current + 1)
  }

  return (
    <main className="relative grid min-h-svh place-items-center bg-background px-4 py-14">
      <div className="absolute right-4 top-4 flex items-center gap-2 sm:right-6 sm:top-6"><LocaleControl /><ThemeControl /></div>
      <div className="w-full max-w-sm">
        <div className="mb-8 flex justify-center"><Brand /></div>
        <Card className="shadow-[var(--shadow-lift)]">
          <CardHeader className="text-center"><CardTitle><h1>{text('backendStatusTitle')}</h1></CardTitle></CardHeader>
          <CardContent className="space-y-5 text-center">
            <div className="flex justify-center" aria-hidden="true">
              {checking
                ? <LoaderCircle data-testid="service-checking-icon" className="size-7 animate-spin text-primary" />
                : ready
                  ? <CheckCircle2 data-testid="service-ready-icon" className="size-7 text-income" />
                  : null}
            </div>
            <div className="space-y-2" aria-live="polite">
              <p className="font-medium">
                {ready
                  ? text('backendUp')
                  : timedOut ? text('backendUnavailable') : text('backendChecking')}
              </p>
              {checking && <p className="text-sm leading-relaxed text-muted-foreground">{text('backendWakeHelper')}</p>}
            </div>
            {timedOut && !ready && (
              <Button type="button" variant="outline" onClick={retry}>
                {text('retryBackend')}
              </Button>
            )}
          </CardContent>
        </Card>
      </div>
    </main>
  )
}

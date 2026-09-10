import { AlertCircle, CheckCircle2, LoaderCircle } from 'lucide-react'
import { useLocale } from '@/app/providers/locale-provider'
import { Brand } from '@/components/brand/brand'
import { LocaleControl } from '@/components/layout/locale-control'
import { ThemeControl } from '@/components/layout/theme-control'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { authText } from '@/features/auth/auth-messages'
import { useBackendHealth } from '@/features/health/backend-health-provider'

export function BackendStatusPage() {
  const { locale } = useLocale()
  const text = (key: Parameters<typeof authText>[1]) => authText(locale, key)
  const { canRetry, phase, ready, retry } = useBackendHealth()
  const checking = phase === 'checking' || phase === 'waking' || phase === 'still-waking'

  const statusText = ready
    ? text('backendUp')
    : phase === 'still-waking'
      ? text('backendStillWaking')
      : phase === 'unavailable'
        ? text('backendUnavailable')
        : text('backendChecking')

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
                  : <AlertCircle data-testid="service-unavailable-icon" className="size-7 text-destructive" />}
            </div>
            <div className="space-y-2" aria-live="polite">
              <p className="font-medium">{statusText}</p>
              {checking && <p className="text-sm leading-relaxed text-muted-foreground">{text('backendWakeHelper')}</p>}
            </div>
            {canRetry && !ready && (
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

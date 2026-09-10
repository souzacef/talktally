import { useEffect, useState } from 'react'
import { AlertCircle, CheckCircle2, LoaderCircle, RotateCcw } from 'lucide-react'
import { useLocale } from '@/app/providers/locale-provider'
import { authText } from '@/features/auth/auth-messages'
import { useBackendHealth, type BackendHealthPhase } from '@/features/health/backend-health-provider'
import { cn } from '@/lib/utils'

const READY_LABEL_DURATION_MS = 4_000

function indicatorTextKey(phase: BackendHealthPhase) {
  switch (phase) {
    case 'checking': return 'backendIndicatorChecking' as const
    case 'waking': return 'backendIndicatorWaking' as const
    case 'still-waking': return 'backendIndicatorStillWaking' as const
    case 'ready': return 'backendIndicatorReady' as const
    case 'unavailable': return 'backendIndicatorUnavailable' as const
  }
}

export function BackendStatusIndicator({ className }: { className?: string }) {
  const { locale } = useLocale()
  const text = (key: Parameters<typeof authText>[1]) => authText(locale, key)
  const { canRetry, phase, retry } = useBackendHealth()
  const [readyLabelVisible, setReadyLabelVisible] = useState(true)

  useEffect(() => {
    if (phase !== 'ready') {
      setReadyLabelVisible(true)
      return
    }
    const timer = window.setTimeout(() => setReadyLabelVisible(false), READY_LABEL_DURATION_MS)
    return () => window.clearTimeout(timer)
  }, [phase])

  const label = text(indicatorTextKey(phase))
  const showLabel = phase !== 'ready' || readyLabelVisible
  const active = phase === 'checking' || phase === 'waking' || phase === 'still-waking'

  return (
    <div className={cn(
      'inline-flex h-9 items-center rounded-full border bg-card/90 text-xs font-medium text-muted-foreground shadow-sm backdrop-blur-sm',
      className,
    )}>
      <a
        href="/backend-status"
        target="_blank"
        rel="noopener noreferrer"
        className={cn(
          'inline-flex h-full items-center gap-1.5 rounded-full px-2.5 transition-colors hover:text-foreground',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
          phase === 'ready' && 'text-income',
          phase === 'unavailable' && 'text-destructive',
        )}
        title={text('backendStatusDetails')}
        aria-label={`${label}. ${text('backendStatusDetails')}`}
      >
        {active && <LoaderCircle className="size-3.5 animate-spin" aria-hidden="true" />}
        {phase === 'ready' && <CheckCircle2 className="size-3.5" aria-hidden="true" />}
        {phase === 'unavailable' && <AlertCircle className="size-3.5" aria-hidden="true" />}
        {showLabel && <span aria-live="polite">{label}</span>}
      </a>
      {canRetry && (
        <button
          type="button"
          onClick={retry}
          className="mr-1 grid size-7 place-items-center rounded-full text-muted-foreground transition-colors hover:bg-accent hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          title={text('retryBackend')}
          aria-label={text('retryBackend')}
        >
          <RotateCcw className="size-3.5" aria-hidden="true" />
        </button>
      )}
    </div>
  )
}

import { LoaderCircle, Mic, Square } from 'lucide-react'
import { useLocale } from '@/app/providers/locale-provider'
import { Button } from '@/components/ui/button'
import { assistantText } from '@/features/assistant/assistant-messages'
import type { VoiceWorkflowState } from '@/features/assistant/hooks/use-voice-assistant'
import { cn } from '@/lib/utils'

const stateContent: Record<VoiceWorkflowState, { key: Parameters<typeof assistantText>[1]; icon: typeof Mic }> = {
  idle: { key: 'voiceStart', icon: Mic },
  recording: { key: 'voiceStop', icon: Square },
  processing: { key: 'voiceProcessing', icon: LoaderCircle },
  completed: { key: 'voiceRecordAnother', icon: Mic },
  'needs-clarification': { key: 'voiceTryAgain', icon: Mic },
  'speech-unavailable': { key: 'voiceTryAgain', icon: Mic },
  error: { key: 'voiceTryAgain', icon: Mic },
}

interface VoiceOrbProps {
  state: VoiceWorkflowState
  onClick: () => void
  compact?: boolean
}

export function VoiceOrb({ state, onClick, compact = false }: VoiceOrbProps) {
  const { locale } = useLocale()
  const { key, icon: Icon } = stateContent[state]
  const label = assistantText(locale, key)
  const interactive = state === 'idle' || state === 'recording' || state === 'completed'
    || state === 'needs-clarification' || state === 'speech-unavailable' || state === 'error'

  return (
    <span className={cn('relative inline-grid place-items-center', compact ? 'size-12' : 'size-28')}>
      {state === 'recording' && (
        <>
          <span className="voice-pulse absolute inset-0 rounded-full bg-primary/25" aria-hidden="true" />
          <span className="voice-pulse absolute inset-2 rounded-full bg-voice/35 [animation-delay:600ms]" aria-hidden="true" />
        </>
      )}
      <Button
        type="button"
        size="icon"
        disabled={!interactive}
        aria-label={label}
        title={label}
        onClick={onClick}
        className={cn(
          'relative z-10 rounded-full border-0 shadow-[var(--shadow-voice)] transition-transform hover:scale-[1.03]',
          compact ? 'size-12' : 'size-20',
          'bg-primary text-primary-foreground',
        )}
      >
        <Icon className={cn(compact ? 'size-5' : 'size-7', state === 'processing' && 'animate-spin')} aria-hidden="true" />
      </Button>
    </span>
  )
}

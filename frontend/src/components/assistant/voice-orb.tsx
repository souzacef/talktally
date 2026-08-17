import { Check, CircleAlert, LoaderCircle, Mic, Square, VolumeX } from 'lucide-react'
import { Button } from '@/components/ui/button'
import type { VoiceWorkflowState } from '@/features/assistant/hooks/use-voice-assistant'
import { cn } from '@/lib/utils'

const stateContent: Record<VoiceWorkflowState, { label: string; icon: typeof Mic }> = {
  idle: { label: 'Start microphone recording', icon: Mic },
  recording: { label: 'Stop microphone recording', icon: Square },
  processing: { label: 'Processing voice command', icon: LoaderCircle },
  completed: { label: 'Voice command completed', icon: Check },
  'needs-clarification': { label: 'Voice command needs clarification', icon: CircleAlert },
  'speech-unavailable': { label: 'Voice reply unavailable', icon: VolumeX },
  error: { label: 'Voice command failed', icon: CircleAlert },
}

interface VoiceOrbProps {
  state: VoiceWorkflowState
  onClick: () => void
  compact?: boolean
}

export function VoiceOrb({ state, onClick, compact = false }: VoiceOrbProps) {
  const { label, icon: Icon } = stateContent[state]
  const interactive = state === 'idle' || state === 'recording' || state === 'completed'
    || state === 'needs-clarification' || state === 'speech-unavailable' || state === 'error'
  const tone = state === 'completed'
    ? 'bg-income text-white'
    : state === 'needs-clarification'
      ? 'bg-warning text-white'
      : state === 'speech-unavailable'
        ? 'bg-protected text-white'
        : 'bg-primary text-primary-foreground'

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
          tone,
        )}
      >
        <Icon className={cn(compact ? 'size-5' : 'size-7', state === 'processing' && 'animate-spin')} aria-hidden="true" />
      </Button>
    </span>
  )
}

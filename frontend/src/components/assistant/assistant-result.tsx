import { Check, CircleAlert, Volume2, VolumeX } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { AssistantStatus, SpeechStatus } from '@/types/api'

export function AssistantStatusChip({ status, label }: { status: AssistantStatus; label?: string }) {
  const completed = status === 'COMPLETED'
  const Icon = completed ? Check : CircleAlert
  return (
    <span className={cn(
      'inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-[0.68rem] font-semibold tracking-wide uppercase',
      completed ? 'bg-income-soft text-income' : 'bg-warning-soft text-warning',
    )}>
      <Icon className="size-3" aria-hidden="true" />
      {label ?? (completed ? 'Completed' : 'Needs clarification')}
    </span>
  )
}

interface SpeechResultProps {
  speechStatus: SpeechStatus
  audioUrl: string | null
  unavailableLabel?: string
  voiceReplyLabel?: string
  unsupportedLabel?: string
  onManualPlay?: () => void
}

export function SpeechResult({
  speechStatus,
  audioUrl,
  unavailableLabel = 'Voice reply unavailable — result still succeeded.',
  voiceReplyLabel = 'Voice reply',
  unsupportedLabel = 'Audio playback is not supported.',
  onManualPlay,
}: SpeechResultProps) {
  if (speechStatus === 'UNAVAILABLE') {
    return (
      <div className="flex items-start gap-2 rounded-xl bg-protected-soft p-3 text-sm text-protected" role="status">
        <VolumeX className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
        <span>{unavailableLabel}</span>
      </div>
    )
  }
  if (!audioUrl) return null
  return (
    <div className="rounded-xl bg-voice-soft p-3">
      <p className="mb-2 flex items-center gap-2 text-xs font-semibold text-foreground"><Volume2 className="size-4 text-voice" aria-hidden="true" /> {voiceReplyLabel}</p>
      <audio className="h-9 w-full" controls src={audioUrl} onPlay={onManualPlay}>{unsupportedLabel}</audio>
    </div>
  )
}

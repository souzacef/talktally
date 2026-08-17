import { Check, CircleAlert, Volume2, VolumeX } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { AssistantStatus, SpeechStatus } from '@/types/api'

export function AssistantStatusChip({ status }: { status: AssistantStatus }) {
  const completed = status === 'COMPLETED'
  const Icon = completed ? Check : CircleAlert
  return (
    <span className={cn(
      'inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-[0.68rem] font-semibold tracking-wide uppercase',
      completed ? 'bg-income-soft text-income' : 'bg-warning-soft text-warning',
    )}>
      <Icon className="size-3" aria-hidden="true" />
      {completed ? 'Completed' : 'Needs clarification'}
    </span>
  )
}

export function SpeechResult({ speechStatus, audioUrl }: { speechStatus: SpeechStatus; audioUrl: string | null }) {
  if (speechStatus === 'UNAVAILABLE') {
    return (
      <div className="flex items-start gap-2 rounded-xl bg-protected-soft p-3 text-sm text-protected" role="status">
        <VolumeX className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
        <span>Voice reply unavailable — result still succeeded.</span>
      </div>
    )
  }
  if (!audioUrl) return null
  return (
    <div className="rounded-xl bg-voice-soft p-3">
      <p className="mb-2 flex items-center gap-2 text-xs font-semibold text-foreground"><Volume2 className="size-4 text-voice" aria-hidden="true" /> Voice reply</p>
      <audio className="h-9 w-full" controls src={audioUrl}>Audio playback is not supported.</audio>
    </div>
  )
}

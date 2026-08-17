import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Bot, Send, Sparkles } from 'lucide-react'
import { useMutation } from '@tanstack/react-query'
import { AssistantStatusChip, SpeechResult } from '@/components/assistant/assistant-result'
import { VoiceOrb } from '@/components/assistant/voice-orb'
import { StatePanel } from '@/components/feedback/state-panel'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { assistantApi } from '@/features/assistant/api/assistant-api'
import { useVoiceAssistant } from '@/features/assistant/hooks/use-voice-assistant'
import { ApiError } from '@/lib/api/api-client'
import { createAudioObjectUrl } from '@/lib/audio/base64-audio'
import { cn } from '@/lib/utils'
import type { AssistantStatus, VoiceAssistantResponse } from '@/types/api'

interface ConversationEntry {
  id: number
  role: 'user' | 'assistant'
  content: string
  status?: AssistantStatus
}

export function AssistantPage() {
  const [message, setMessage] = useState('')
  const [conversation, setConversation] = useState<ConversationEntry[]>([])
  const [audioUrl, setAudioUrl] = useState<string | null>(null)
  const nextMessageId = useRef(0)

  function entry(role: ConversationEntry['role'], content: string, status?: AssistantStatus): ConversationEntry {
    nextMessageId.current += 1
    return { id: nextMessageId.current, role, content, status }
  }

  function appendVoiceResult(result: VoiceAssistantResponse) {
    setConversation((current) => [
      ...current,
      entry('user', result.transcript),
      entry('assistant', result.message, result.status),
    ])
  }

  const voice = useVoiceAssistant(appendVoiceResult)
  const textMutation = useMutation({
    mutationFn: (text: string) => assistantApi.sendMessage(text),
  })

  useEffect(() => {
    const audio = voice.result?.audio
    if (!audio) {
      setAudioUrl(null)
      return
    }
    const objectAudio = createAudioObjectUrl(audio.contentType, audio.base64)
    setAudioUrl(objectAudio.url)
    return objectAudio.revoke
  }, [voice.result])

  function sendText(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const submitted = message
    if (!submitted.trim()) return
    setConversation((current) => [...current, entry('user', submitted)])
    setMessage('')
    textMutation.mutate(submitted, {
      onSuccess: (result) => {
        setConversation((current) => [...current, entry('assistant', result.message, result.status)])
      },
    })
  }

  function toggleVoice() {
    if (voice.isRecording) voice.stopRecording()
    else void voice.startRecording()
  }

  const textError = textMutation.error instanceof ApiError
    ? textMutation.error.message
    : textMutation.error ? 'Assistant request failed' : null

  return (
    <section className="space-y-6 pb-24 lg:pb-0">
      <header>
        <p className="mb-1 text-xs font-bold tracking-[0.14em] text-voice uppercase">Voice intelligence</p>
        <h1 className="font-heading text-3xl font-semibold tracking-[-0.045em] sm:text-4xl">Assistant</h1>
        <p className="mt-2 text-muted-foreground">Capture and understand your money through the secured TalkTally assistant.</p>
      </header>

      <div className="grid min-w-0 items-start gap-4 lg:grid-cols-[minmax(0,1fr)_20rem]">
        <Card className="min-w-0 lg:min-h-[38rem]">
          <CardHeader className="border-b">
            <CardTitle className="flex items-center gap-2 text-xl"><Bot className="size-5 text-primary" aria-hidden="true" /> Conversation</CardTitle>
            <CardDescription>Messages are sent to the real Spring assistant API.</CardDescription>
          </CardHeader>
          <CardContent className="flex min-h-[28rem] flex-col gap-4">
            <div className="flex-1 space-y-4" aria-live="polite">
              {conversation.length === 0 && (
                <StatePanel title="Ready when you are" description="Type a message or use the microphone. No sample conversation is loaded." />
              )}
              {conversation.map((item) => (
                <article key={item.id} className={cn('flex', item.role === 'user' ? 'justify-end' : 'justify-start')}>
                  <div className={cn(
                    'max-w-[88%] rounded-2xl px-4 py-3 text-sm leading-relaxed sm:max-w-[75%]',
                    item.role === 'user'
                      ? 'rounded-br-md bg-primary text-primary-foreground shadow-sm'
                      : 'rounded-bl-md border bg-card shadow-[var(--shadow-soft)]',
                  )}>
                    <p>{item.content}</p>
                    {item.status && <div className="mt-3"><AssistantStatusChip status={item.status} /></div>}
                  </div>
                </article>
              ))}
              {textMutation.isPending && (
                <div className="flex justify-start"><div className="flex items-center gap-2 rounded-2xl rounded-bl-md border bg-card px-4 py-3 text-sm text-muted-foreground"><Sparkles className="size-4 animate-pulse text-primary" aria-hidden="true" /> Thinking…</div></div>
              )}
            </div>
              {voice.error && <p className="text-sm text-expense lg:hidden" role="alert">{voice.error}</p>}
              {voice.result && (
                <div className="space-y-3 lg:hidden">
                  <AssistantStatusChip status={voice.result.status} />
                  <SpeechResult speechStatus={voice.result.speechStatus} audioUrl={audioUrl} />
                </div>
              )}
            {textError && <Alert variant="destructive"><AlertDescription>{textError}</AlertDescription></Alert>}
            <form className="hidden items-end gap-2 border-t pt-4 lg:flex" onSubmit={sendText}>
              <div className="min-w-0 flex-1"><Label htmlFor="assistant-message" className="sr-only">Message</Label><Textarea id="assistant-message" className="min-h-20 resize-none" maxLength={4_000} required value={message} onChange={(event) => setMessage(event.target.value)} placeholder="Tell TalkTally what happened…" /></div>
              <Button type="submit" size="icon-lg" disabled={textMutation.isPending || !message.trim()} aria-label="Send message"><Send aria-hidden="true" /></Button>
            </form>
          </CardContent>
        </Card>

        <Card className="sticky top-24 hidden lg:flex">
          <CardHeader className="text-center"><CardTitle className="text-xl">Speak to TalkTally</CardTitle><CardDescription>Mono PCM16 WAV, sent securely</CardDescription></CardHeader>
          <CardContent className="flex flex-col items-center text-center">
            <VoiceOrb state={voice.state} onClick={toggleVoice} />
            <p className="mt-4 font-heading font-semibold">
              {voice.isRecording ? 'Listening…' : voice.isProcessing ? 'Understanding…' : 'Tap to speak'}
            </p>
            <p className="mt-2 text-xs leading-relaxed text-muted-foreground">The recording stops automatically before the backend's 8 MiB limit.</p>
            {voice.error && <p className="mt-4 text-sm text-expense" role="alert">{voice.error}</p>}
            {voice.result && (
              <div className="mt-5 w-full space-y-3 text-left">
                <AssistantStatusChip status={voice.result.status} />
                <SpeechResult speechStatus={voice.result.speechStatus} audioUrl={audioUrl} />
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <div className="fixed inset-x-3 bottom-[calc(4.8rem+env(safe-area-inset-bottom))] z-30 mx-auto max-w-xl rounded-2xl border bg-card/96 p-2 shadow-[var(--shadow-lift)] backdrop-blur-md lg:hidden">
        <form className="flex items-end gap-2" onSubmit={sendText}>
          <div className="min-w-0 flex-1"><Label htmlFor="assistant-message-mobile" className="sr-only">Message</Label><Textarea id="assistant-message-mobile" className="min-h-12 max-h-28 resize-none py-3" maxLength={4_000} required value={message} onChange={(event) => setMessage(event.target.value)} placeholder="What happened?" /></div>
          <VoiceOrb state={voice.state} onClick={toggleVoice} compact />
          <Button type="submit" size="icon" disabled={textMutation.isPending || !message.trim()} aria-label="Send message"><Send aria-hidden="true" /></Button>
        </form>
      </div>
    </section>
  )
}

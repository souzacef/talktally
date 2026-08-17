import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Mic, Send, Square } from 'lucide-react'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { assistantApi } from '@/features/assistant/api/assistant-api'
import { createAudioObjectUrl } from '@/lib/audio/base64-audio'
import { WavMicrophoneRecorder } from '@/lib/audio/wav-recorder'
import { ApiError } from '@/lib/api/api-client'

export function AssistantPage() {
  const [message, setMessage] = useState('')
  const [recording, setRecording] = useState(false)
  const [recordedWav, setRecordedWav] = useState<Blob | null>(null)
  const [recordingError, setRecordingError] = useState<string | null>(null)
  const [audioUrl, setAudioUrl] = useState<string | null>(null)
  const recorderRef = useRef<WavMicrophoneRecorder | null>(null)
  const durationTimer = useRef<number | null>(null)

  const textMutation = useMutation({
    mutationFn: (text: string) => assistantApi.sendMessage(text),
  })
  const voiceMutation = useMutation({
    mutationFn: (wav: Blob) => assistantApi.sendVoice(wav),
  })

  useEffect(() => {
    const audio = voiceMutation.data?.audio
    if (!audio) {
      setAudioUrl(null)
      return
    }
    const objectAudio = createAudioObjectUrl(audio.contentType, audio.base64)
    setAudioUrl(objectAudio.url)
    return objectAudio.revoke
  }, [voiceMutation.data])

  useEffect(() => () => {
    if (durationTimer.current !== null) window.clearTimeout(durationTimer.current)
    void recorderRef.current?.cancel()
  }, [])

  function sendText(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!message.trim()) return
    textMutation.mutate(message, {
      onSuccess: () => setMessage(''),
    })
  }

  async function finishRecording(recorder: WavMicrophoneRecorder) {
    if (recorderRef.current !== recorder) return
    if (durationTimer.current !== null) window.clearTimeout(durationTimer.current)
    durationTimer.current = null
    recorderRef.current = null
    setRecording(false)
    try {
      setRecordedWav(await recorder.stop())
    } catch (error) {
      setRecordingError(error instanceof Error ? error.message : 'Recording could not be completed')
    }
  }

  async function startRecording() {
    setRecordingError(null)
    setRecordedWav(null)
    const recorder = new WavMicrophoneRecorder()
    try {
      await recorder.start()
      recorderRef.current = recorder
      setRecording(true)
      durationTimer.current = window.setTimeout(
        () => void finishRecording(recorder),
        Math.floor(recorder.maximumDurationMs * 0.9),
      )
    } catch (error) {
      setRecordingError(error instanceof Error ? error.message : 'Microphone access failed')
    }
  }

  const textError = textMutation.error instanceof ApiError
    ? textMutation.error.message
    : textMutation.error ? 'Assistant request failed' : null
  const voiceError = voiceMutation.error instanceof ApiError
    ? voiceMutation.error.message
    : voiceMutation.error ? 'Voice request failed' : null

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-3xl font-semibold tracking-tight">Assistant</h1>
        <p className="text-muted-foreground">Text and WAV commands are processed by the secured Spring API.</p>
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader><CardTitle>Text command</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            <form className="space-y-3" onSubmit={sendText}>
              <Label htmlFor="assistant-message">Message</Label>
              <Textarea
                id="assistant-message"
                maxLength={4_000}
                required
                value={message}
                onChange={(event) => setMessage(event.target.value)}
                placeholder="Record lunch for R$ 35 today"
              />
              <Button type="submit" disabled={textMutation.isPending || !message.trim()}>
                <Send aria-hidden="true" />
                {textMutation.isPending ? 'Sending…' : 'Send'}
              </Button>
            </form>
            {textError && <Alert variant="destructive"><AlertDescription>{textError}</AlertDescription></Alert>}
            {textMutation.data && (
              <Alert aria-live="polite">
                <AlertTitle>{textMutation.data.status}</AlertTitle>
                <AlertDescription>{textMutation.data.message}</AlertDescription>
              </Alert>
            )}
          </CardContent>
        </Card>
        <Card>
          <CardHeader><CardTitle>Voice command</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            <p className="text-sm text-muted-foreground">
              The browser records mono PCM and encodes an audio/wav file before upload.
            </p>
            {!recording ? (
              <Button type="button" variant="outline" onClick={() => void startRecording()}>
                <Mic aria-hidden="true" /> Start microphone
              </Button>
            ) : (
              <Button
                type="button"
                variant="destructive"
                aria-label="Stop microphone recording"
                onClick={() => {
                  const recorder = recorderRef.current
                  if (recorder) void finishRecording(recorder)
                }}
              >
                <Square aria-hidden="true" /> Stop recording
              </Button>
            )}
            {recordedWav && (
              <div className="space-y-2">
                <p className="text-sm">WAV ready ({Math.ceil(recordedWav.size / 1024)} KiB)</p>
                <Button
                  type="button"
                  disabled={voiceMutation.isPending}
                  onClick={() => voiceMutation.mutate(recordedWav)}
                >
                  {voiceMutation.isPending ? 'Sending voice…' : 'Send recording'}
                </Button>
              </div>
            )}
            {(recordingError || voiceError) && (
              <Alert variant="destructive" aria-live="polite">
                <AlertDescription>{recordingError ?? voiceError}</AlertDescription>
              </Alert>
            )}
            {voiceMutation.data && (
              <div className="space-y-3" aria-live="polite">
                <Alert>
                  <AlertTitle>{voiceMutation.data.status}</AlertTitle>
                  <AlertDescription>
                    Transcript: {voiceMutation.data.transcript}<br />
                    {voiceMutation.data.message}
                  </AlertDescription>
                </Alert>
                {voiceMutation.data.speechStatus === 'UNAVAILABLE' && (
                  <Alert>
                    <AlertTitle>Speech unavailable</AlertTitle>
                    <AlertDescription>
                      The command result above succeeded, but an audio reply could not be generated.
                    </AlertDescription>
                  </Alert>
                )}
                {audioUrl && <audio controls src={audioUrl}>Audio playback is not supported.</audio>}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </section>
  )
}

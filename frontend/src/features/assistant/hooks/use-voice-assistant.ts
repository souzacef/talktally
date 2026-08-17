import { useEffect, useRef, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { assistantApi } from '@/features/assistant/api/assistant-api'
import { ApiError } from '@/lib/api/api-client'
import { WavMicrophoneRecorder } from '@/lib/audio/wav-recorder'
import type { VoiceAssistantResponse } from '@/types/api'

export type VoiceWorkflowState =
  | 'idle'
  | 'recording'
  | 'processing'
  | 'completed'
  | 'needs-clarification'
  | 'speech-unavailable'
  | 'error'

export function useVoiceAssistant(onResult?: (result: VoiceAssistantResponse) => void) {
  const [recording, setRecording] = useState(false)
  const [recordingError, setRecordingError] = useState<string | null>(null)
  const recorderRef = useRef<WavMicrophoneRecorder | null>(null)
  const durationTimer = useRef<number | null>(null)
  const onResultRef = useRef(onResult)
  onResultRef.current = onResult

  const mutation = useMutation({
    mutationFn: (wav: Blob) => assistantApi.sendVoice(wav),
    onSuccess: (result) => onResultRef.current?.(result),
  })

  function clearDurationTimer() {
    if (durationTimer.current !== null) window.clearTimeout(durationTimer.current)
    durationTimer.current = null
  }

  async function finishRecording(recorder: WavMicrophoneRecorder) {
    if (recorderRef.current !== recorder) return
    clearDurationTimer()
    recorderRef.current = null
    setRecording(false)
    try {
      const wav = await recorder.stop()
      await mutation.mutateAsync(wav)
    } catch (error) {
      setRecordingError(error instanceof ApiError || error instanceof Error
        ? error.message
        : 'Voice request failed')
    }
  }

  async function startRecording() {
    setRecordingError(null)
    mutation.reset()
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
      await recorder.cancel()
      setRecordingError(error instanceof Error ? error.message : 'Microphone access failed')
    }
  }

  function stopRecording() {
    const recorder = recorderRef.current
    if (recorder) void finishRecording(recorder)
  }

  useEffect(() => () => {
    clearDurationTimer()
    void recorderRef.current?.cancel()
  }, [])

  let state: VoiceWorkflowState = 'idle'
  if (recording) state = 'recording'
  else if (mutation.isPending) state = 'processing'
  else if (recordingError || mutation.error) state = 'error'
  else if (mutation.data?.speechStatus === 'UNAVAILABLE') state = 'speech-unavailable'
  else if (mutation.data?.status === 'NEEDS_CLARIFICATION') state = 'needs-clarification'
  else if (mutation.data?.status === 'COMPLETED') state = 'completed'

  return {
    result: mutation.data,
    error: recordingError ?? (mutation.error instanceof ApiError ? mutation.error.message : null),
    state,
    isRecording: recording,
    isProcessing: mutation.isPending,
    startRecording,
    stopRecording,
  }
}

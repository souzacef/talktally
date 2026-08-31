import { useEffect, useRef, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { assistantApi } from '@/features/assistant/api/assistant-api'
import { WavMicrophoneRecorder } from '@/lib/audio/wav-recorder'
import type { VoiceAssistantResponse } from '@/types/api'

export const MINIMUM_MANUAL_RECORDING_MS = 750
export const INITIAL_NO_SPEECH_TIMEOUT_MS = 5_000
export const END_OF_SPEECH_SILENCE_MS = 1_400

interface VoiceRecorder {
  readonly maximumDurationMs: number
  start(): Promise<void>
  stop(): Promise<Blob>
  cancel(): Promise<void>
}

interface VoiceCaptureOptions {
  noSpeechMessage?: string
  tooShortMessage?: string
  requestErrorMessage?: (error: unknown) => string
  recorderFactory?: (onSpeechActivity: () => void) => VoiceRecorder
}

export type VoiceWorkflowState =
  | 'idle'
  | 'recording'
  | 'processing'
  | 'completed'
  | 'needs-clarification'
  | 'speech-unavailable'
  | 'error'

export function useVoiceAssistant(
  onResult?: (result: VoiceAssistantResponse) => void,
  options: VoiceCaptureOptions = {},
) {
  const [recording, setRecording] = useState(false)
  const [recordingError, setRecordingError] = useState<string | null>(null)
  const recorderRef = useRef<VoiceRecorder | null>(null)
  const durationTimer = useRef<number | null>(null)
  const noSpeechTimer = useRef<number | null>(null)
  const silenceTimer = useRef<number | null>(null)
  const recordingStartedAt = useRef(0)
  const speechDetected = useRef(false)
  const onResultRef = useRef(onResult)
  onResultRef.current = onResult

  const mutation = useMutation({
    mutationFn: (wav: Blob) => assistantApi.sendVoice(wav),
    onSuccess: (result) => onResultRef.current?.(result),
  })

  function clearCaptureTimers() {
    if (durationTimer.current !== null) window.clearTimeout(durationTimer.current)
    if (noSpeechTimer.current !== null) window.clearTimeout(noSpeechTimer.current)
    if (silenceTimer.current !== null) window.clearTimeout(silenceTimer.current)
    durationTimer.current = null
    noSpeechTimer.current = null
    silenceTimer.current = null
  }

  async function finishRecording(recorder: VoiceRecorder) {
    if (recorderRef.current !== recorder) return
    recorderRef.current = null
    clearCaptureTimers()
    setRecording(false)
    let wav: Blob
    try {
      wav = await recorder.stop()
    } catch (error) {
      setRecordingError(error instanceof Error ? error.message : 'Voice request failed')
      return
    }
    try {
      await mutation.mutateAsync(wav)
    } catch {
      // The mutation retains request failures for localized presentation.
    }
  }

  async function discardRecording(recorder: VoiceRecorder, message: string) {
    if (recorderRef.current !== recorder) return
    recorderRef.current = null
    clearCaptureTimers()
    setRecording(false)
    setRecordingError(message)
    await recorder.cancel()
  }

  function registerSpeechActivity(recorder: VoiceRecorder) {
    if (recorderRef.current !== recorder) return
    speechDetected.current = true
    if (noSpeechTimer.current !== null) window.clearTimeout(noSpeechTimer.current)
    noSpeechTimer.current = null
    if (silenceTimer.current !== null) window.clearTimeout(silenceTimer.current)
    silenceTimer.current = window.setTimeout(
      () => void finishRecording(recorder),
      END_OF_SPEECH_SILENCE_MS,
    )
  }

  async function startRecording() {
    setRecordingError(null)
    mutation.reset()
    let recorder: VoiceRecorder
    recorder = options.recorderFactory?.(() => registerSpeechActivity(recorder))
      ?? new WavMicrophoneRecorder(() => registerSpeechActivity(recorder))
    recorderRef.current = recorder
    speechDetected.current = false
    try {
      await recorder.start()
      if (recorderRef.current !== recorder) {
        await recorder.cancel()
        return
      }
      recordingStartedAt.current = Date.now()
      setRecording(true)
      if (!speechDetected.current) {
        noSpeechTimer.current = window.setTimeout(
          () => void discardRecording(
            recorder,
            options.noSpeechMessage ?? 'No speech was detected. Try again.',
          ),
          INITIAL_NO_SPEECH_TIMEOUT_MS,
        )
      }
      durationTimer.current = window.setTimeout(
        () => void finishRecording(recorder),
        Math.floor(recorder.maximumDurationMs * 0.9),
      )
    } catch (error) {
      if (recorderRef.current === recorder) recorderRef.current = null
      clearCaptureTimers()
      await recorder.cancel()
      setRecordingError(error instanceof Error ? error.message : 'Microphone access failed')
    }
  }

  function stopRecording() {
    const recorder = recorderRef.current
    if (!recorder) return
    if (Date.now() - recordingStartedAt.current < MINIMUM_MANUAL_RECORDING_MS) {
      void discardRecording(
        recorder,
        options.tooShortMessage ?? 'Recording was too short. Try again.',
      )
    } else if (!speechDetected.current) {
      void discardRecording(
        recorder,
        options.noSpeechMessage ?? 'No speech was detected. Try again.',
      )
    } else {
      void finishRecording(recorder)
    }
  }

  useEffect(() => () => {
    clearCaptureTimers()
    const recorder = recorderRef.current
    recorderRef.current = null
    void recorder?.cancel()
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
    error: recordingError ?? (
      mutation.error
        ? options.requestErrorMessage?.(mutation.error) ?? 'Voice request failed'
        : null
    ),
    state,
    isRecording: recording,
    isProcessing: mutation.isPending,
    startRecording,
    stopRecording,
  }
}

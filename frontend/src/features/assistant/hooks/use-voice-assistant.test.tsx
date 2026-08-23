import type { ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  END_OF_SPEECH_SILENCE_MS,
  INITIAL_NO_SPEECH_TIMEOUT_MS,
  MINIMUM_MANUAL_RECORDING_MS,
  useVoiceAssistant,
} from '@/features/assistant/hooks/use-voice-assistant'

const mocks = vi.hoisted(() => ({ sendVoice: vi.fn() }))

vi.mock('@/features/assistant/api/assistant-api', () => ({
  assistantApi: { sendVoice: mocks.sendVoice },
}))

class TestRecorder {
  maximumDurationMs = 10_000
  onActivity = vi.fn()
  readonly start = vi.fn(async () => undefined)
  readonly stop = vi.fn(async () => new Blob(['wav'], { type: 'audio/wav' }))
  readonly cancel = vi.fn(async () => undefined)

  signalActivity = () => this.onActivity()
}

function renderVoice(recorder: TestRecorder, onResult = vi.fn()) {
  const queryClient = new QueryClient({
    defaultOptions: { mutations: { retry: false } },
  })
  return renderHook(() => useVoiceAssistant(onResult, {
    recorderFactory: (onActivity) => {
      recorder.onActivity = vi.fn(onActivity)
      return recorder
    },
    noSpeechMessage: 'localized no speech',
    tooShortMessage: 'localized too short',
  }), {
    wrapper: ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    ),
  })
}

async function start(result: ReturnType<typeof renderVoice>['result']) {
  await act(async () => result.current.startRecording())
}

describe('useVoiceAssistant capture lifecycle', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    mocks.sendVoice.mockResolvedValue({
      transcript: 'Spoken request',
      message: 'Assistant reply',
      status: 'COMPLETED',
      speechStatus: 'UNAVAILABLE',
      audio: null,
    })
  })

  afterEach(() => vi.useRealTimers())

  it('waits for the full initial window before reporting no speech and never calls the API', async () => {
    const recorder = new TestRecorder()
    const { result } = renderVoice(recorder)
    await start(result)

    await act(async () => vi.advanceTimersByTimeAsync(INITIAL_NO_SPEECH_TIMEOUT_MS - 1))
    expect(result.current.isRecording).toBe(true)
    expect(recorder.cancel).not.toHaveBeenCalled()

    await act(async () => vi.advanceTimersByTimeAsync(1))
    expect(result.current.isRecording).toBe(false)
    expect(result.current.error).toBe('localized no speech')
    expect(recorder.cancel).toHaveBeenCalledTimes(1)
    expect(mocks.sendVoice).not.toHaveBeenCalled()
  })

  it('discards a manual stop below the minimum duration with localized feedback', async () => {
    const recorder = new TestRecorder()
    const { result } = renderVoice(recorder)
    await start(result)
    await act(async () => vi.advanceTimersByTimeAsync(MINIMUM_MANUAL_RECORDING_MS - 1))

    act(() => result.current.stopRecording())
    await act(async () => Promise.resolve())

    expect(result.current.error).toBe('localized too short')
    expect(recorder.cancel).toHaveBeenCalledTimes(1)
    expect(recorder.stop).not.toHaveBeenCalled()
    expect(mocks.sendVoice).not.toHaveBeenCalled()
  })

  it('keeps recording through a short natural pause then auto-submits after sustained silence', async () => {
    const recorder = new TestRecorder()
    const onResult = vi.fn()
    const { result } = renderVoice(recorder, onResult)
    await start(result)

    act(() => recorder.signalActivity())
    await act(async () => vi.advanceTimersByTimeAsync(END_OF_SPEECH_SILENCE_MS - 1))
    expect(result.current.isRecording).toBe(true)
    expect(recorder.stop).not.toHaveBeenCalled()

    await act(async () => vi.advanceTimersByTimeAsync(1))
    expect(recorder.stop).toHaveBeenCalledTimes(1)
    expect(mocks.sendVoice).toHaveBeenCalledTimes(1)
    expect(onResult).toHaveBeenCalledTimes(1)
  })

  it('resets the silence window whenever meaningful activity resumes', async () => {
    const recorder = new TestRecorder()
    const { result } = renderVoice(recorder)
    await start(result)

    act(() => recorder.signalActivity())
    await act(async () => vi.advanceTimersByTimeAsync(1_000))
    act(() => recorder.signalActivity())
    await act(async () => vi.advanceTimersByTimeAsync(1_000))

    expect(result.current.isRecording).toBe(true)
    expect(recorder.stop).not.toHaveBeenCalled()
  })

  it('submits a valid manual stop after speech exactly once', async () => {
    const recorder = new TestRecorder()
    const { result } = renderVoice(recorder)
    await start(result)
    act(() => recorder.signalActivity())
    await act(async () => vi.advanceTimersByTimeAsync(MINIMUM_MANUAL_RECORDING_MS))

    act(() => result.current.stopRecording())
    await act(async () => Promise.resolve())

    expect(recorder.stop).toHaveBeenCalledTimes(1)
    expect(mocks.sendVoice).toHaveBeenCalledTimes(1)
  })

  it('clears a completed result only when the user starts another recording', async () => {
    const recorder = new TestRecorder()
    const { result } = renderVoice(recorder)
    await start(result)
    act(() => recorder.signalActivity())
    await act(async () => vi.advanceTimersByTimeAsync(END_OF_SPEECH_SILENCE_MS))

    expect(result.current.result?.message).toBe('Assistant reply')
    expect(result.current.state).toBe('speech-unavailable')

    await start(result)

    expect(result.current.result).toBeUndefined()
    expect(result.current.state).toBe('recording')
  })

  it('cannot double-submit when manual stop races automatic silence completion', async () => {
    const recorder = new TestRecorder()
    const { result } = renderVoice(recorder)
    await start(result)
    act(() => recorder.signalActivity())

    await act(async () => vi.advanceTimersByTimeAsync(END_OF_SPEECH_SILENCE_MS))
    act(() => result.current.stopRecording())
    await act(async () => Promise.resolve())

    expect(recorder.stop).toHaveBeenCalledTimes(1)
    expect(mocks.sendVoice).toHaveBeenCalledTimes(1)
  })

  it('retains the maximum-duration safety completion', async () => {
    const recorder = new TestRecorder()
    recorder.maximumDurationMs = 1_000
    const { result } = renderVoice(recorder)
    await start(result)
    act(() => recorder.signalActivity())

    await act(async () => vi.advanceTimersByTimeAsync(900))

    expect(recorder.stop).toHaveBeenCalledTimes(1)
    expect(mocks.sendVoice).toHaveBeenCalledTimes(1)
  })

  it('cancels and releases an active recorder when unmounted', async () => {
    const recorder = new TestRecorder()
    const view = renderVoice(recorder)
    await start(view.result)

    view.unmount()
    await act(async () => Promise.resolve())

    expect(recorder.cancel).toHaveBeenCalledTimes(1)
    expect(mocks.sendVoice).not.toHaveBeenCalled()
  })

  it('surfaces microphone/browser startup failures without submitting', async () => {
    const recorder = new TestRecorder()
    recorder.start.mockRejectedValueOnce(new Error('Microphone denied'))
    const { result } = renderVoice(recorder)

    await start(result)

    expect(result.current.error).toBe('Microphone denied')
    expect(recorder.cancel).toHaveBeenCalledTimes(1)
    expect(mocks.sendVoice).not.toHaveBeenCalled()
  })
})

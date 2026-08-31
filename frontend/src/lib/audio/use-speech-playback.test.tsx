import { act, renderHook, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import {
  useSpeechPlayback,
  type SpeechPlaybackOptions,
} from '@/lib/audio/use-speech-playback'

function createAudioContextHarness(initialState: AudioContextState = 'suspended') {
  let state = initialState
  const sources: Array<{
    start: ReturnType<typeof vi.fn>
    stop: ReturnType<typeof vi.fn>
    disconnect: ReturnType<typeof vi.fn>
  }> = []
  const resume = vi.fn(() => {
    state = 'running'
    return Promise.resolve()
  })
  const close = vi.fn(() => {
    state = 'closed'
    return Promise.resolve()
  })
  const decodeAudioData = vi.fn(async () => ({}) as AudioBuffer)
  const createBufferSource = vi.fn(() => {
    const source = {
      buffer: null,
      connect: vi.fn(),
      addEventListener: vi.fn(),
      start: vi.fn(),
      stop: vi.fn(),
      disconnect: vi.fn(),
    }
    sources.push(source)
    return source as unknown as AudioBufferSourceNode
  })
  const context = {
    get state() {
      return state
    },
    destination: {},
    resume,
    close,
    decodeAudioData,
    createBufferSource,
  } as unknown as AudioContext

  return {
    context,
    resume,
    close,
    decodeAudioData,
    createBufferSource,
    sources,
  }
}

function renderPlayback(
  options: SpeechPlaybackOptions,
  initialUrl: string | null = null,
) {
  return renderHook(
    ({ audioUrl }) => useSpeechPlayback(audioUrl, options),
    { initialProps: { audioUrl: initialUrl } },
  )
}

describe('useSpeechPlayback', () => {
  it('creates and resumes its context synchronously when primed', () => {
    const events: string[] = []
    const harness = createAudioContextHarness()
    harness.resume.mockImplementation(() => {
      events.push('resume')
      return Promise.resolve()
    })
    const contextFactory = vi.fn(() => {
      events.push('create')
      return harness.context
    })
    const { result } = renderPlayback({ contextFactory })

    act(() => {
      result.current.prime()
      events.push('after-prime')
    })

    expect(events).toEqual(['create', 'resume', 'after-prime'])
  })

  it('plays each new audio URL once and ignores rerenders with the same URL', async () => {
    const harness = createAudioContextHarness()
    const loadAudio = vi.fn(async (_url: string, _signal: AbortSignal) => new ArrayBuffer(8))
    const options = {
      contextFactory: () => harness.context,
      loadAudio,
    }
    const { result, rerender } = renderPlayback(options)

    act(() => result.current.prime())
    rerender({ audioUrl: 'blob:first' })
    await waitFor(() => expect(harness.sources[0]?.start).toHaveBeenCalledTimes(1))

    rerender({ audioUrl: 'blob:first' })
    expect(loadAudio).toHaveBeenCalledTimes(1)

    rerender({ audioUrl: 'blob:second' })
    await waitFor(() => expect(harness.sources[1]?.start).toHaveBeenCalledTimes(1))

    expect(loadAudio.mock.calls.map(([url]) => url)).toEqual(['blob:first', 'blob:second'])
    expect(harness.sources[0]?.stop).toHaveBeenCalledTimes(1)
    expect(harness.sources[0]?.disconnect).toHaveBeenCalledTimes(1)
  })

  it('fails silently when automatic playback cannot decode audio', async () => {
    const harness = createAudioContextHarness()
    harness.decodeAudioData.mockRejectedValueOnce(new DOMException('Decode failed', 'EncodingError'))
    const loadAudio = vi.fn(async () => new ArrayBuffer(8))
    const { result, rerender } = renderPlayback({
      contextFactory: () => harness.context,
      loadAudio,
    })

    act(() => result.current.prime())
    expect(() => rerender({ audioUrl: 'blob:blocked' })).not.toThrow()
    await waitFor(() => expect(harness.decodeAudioData).toHaveBeenCalledTimes(1))

    expect(harness.createBufferSource).not.toHaveBeenCalled()
  })

  it('leaves playback to native controls when Web Audio is unavailable', () => {
    const loadAudio = vi.fn(async () => new ArrayBuffer(8))
    const { result, rerender } = renderPlayback({
      contextFactory: () => null,
      loadAudio,
    })

    act(() => result.current.prime())
    rerender({ audioUrl: 'blob:fallback' })

    expect(loadAudio).not.toHaveBeenCalled()
  })

  it('stops active playback and closes the context on unmount', async () => {
    const harness = createAudioContextHarness()
    const { result, rerender, unmount } = renderPlayback({
      contextFactory: () => harness.context,
      loadAudio: async () => new ArrayBuffer(8),
    })

    act(() => result.current.prime())
    rerender({ audioUrl: 'blob:active' })
    await waitFor(() => expect(harness.sources[0]?.start).toHaveBeenCalledTimes(1))
    unmount()

    expect(harness.sources[0]?.stop).toHaveBeenCalledTimes(1)
    expect(harness.sources[0]?.disconnect).toHaveBeenCalledTimes(1)
    expect(harness.close).toHaveBeenCalledTimes(1)
  })
})

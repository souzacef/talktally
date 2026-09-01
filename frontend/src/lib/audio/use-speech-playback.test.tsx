import { act, renderHook } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import {
  useSpeechPlayback,
  type SpeechPlaybackAudio,
  type SpeechPlaybackOptions,
} from '@/lib/audio/use-speech-playback'

function createAudioHarness(
  playImplementation?: () => Promise<void>,
) {
  const playedSources: string[] = []

  let audio!: SpeechPlaybackAudio

  const play = vi.fn(() => {
    playedSources.push(audio.src)
    return playImplementation?.() ?? Promise.resolve()
  })
  const pause = vi.fn()
  const load = vi.fn()
  const removeAttribute = vi.fn((name: string) => {
    if (name === 'src') audio.src = ''
  })

  audio = {
    src: '',
    currentTime: 0,
    play,
    pause,
    load,
    removeAttribute,
  }

  const audioFactory = vi.fn(() => audio)

  return {
    audio,
    audioFactory,
    play,
    pause,
    load,
    removeAttribute,
    playedSources,
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
  it('creates and primes one reusable native audio element synchronously', () => {
    const events: string[] = []
    const harness = createAudioHarness()

    harness.audioFactory.mockImplementation(() => {
      events.push('create')
      return harness.audio
    })
    harness.play.mockImplementation(() => {
      events.push('play')
      harness.playedSources.push(harness.audio.src)
      return Promise.resolve()
    })

    const { result } = renderPlayback({
      audioFactory: harness.audioFactory,
    })

    act(() => {
      result.current.prime()
      events.push('after-prime')
    })

    expect(events).toEqual(['create', 'play', 'after-prime'])
    expect(harness.audioFactory).toHaveBeenCalledTimes(1)
    expect(harness.playedSources[0]).toMatch(/^data:audio\/wav;base64,/)
  })

  it('plays each distinct generated URL once and reuses the same audio element', () => {
    const harness = createAudioHarness()
    const { result, rerender } = renderPlayback({
      audioFactory: harness.audioFactory,
    })

    act(() => result.current.prime())

    rerender({ audioUrl: 'blob:first' })
    expect(harness.play).toHaveBeenCalledTimes(2)

    rerender({ audioUrl: 'blob:first' })
    expect(harness.play).toHaveBeenCalledTimes(2)

    rerender({ audioUrl: 'blob:second' })
    expect(harness.play).toHaveBeenCalledTimes(3)

    expect(harness.playedSources).toEqual([
      expect.stringMatching(/^data:audio\/wav;base64,/),
      'blob:first',
      'blob:second',
    ])
    expect(harness.audioFactory).toHaveBeenCalledTimes(1)
  })

  it('does not let delayed primer completion reset a real reply', async () => {
    let resolvePrimer!: () => void

    const primerPromise = new Promise<void>((resolve) => {
      resolvePrimer = resolve
    })

    let playCall = 0
    const harness = createAudioHarness(() => {
      playCall += 1
      return playCall === 1 ? primerPromise : Promise.resolve()
    })

    const { result, rerender } = renderPlayback({
      audioFactory: harness.audioFactory,
    })

    act(() => result.current.prime())

    rerender({ audioUrl: 'blob:reply' })

    expect(harness.audio.src).toBe('blob:reply')
    expect(harness.pause).toHaveBeenCalledTimes(2)
    expect(harness.load).toHaveBeenCalledTimes(2)

    await act(async () => {
      resolvePrimer()
      await primerPromise
    })

    expect(harness.audio.src).toBe('blob:reply')
    expect(harness.pause).toHaveBeenCalledTimes(2)
    expect(harness.load).toHaveBeenCalledTimes(2)
  })

  it('swallows native autoplay rejection', async () => {
    let playCall = 0
    const harness = createAudioHarness(() => {
      playCall += 1
      if (playCall === 2) {
        return Promise.reject(new DOMException('Blocked', 'NotAllowedError'))
      }
      return Promise.resolve()
    })

    const { result, rerender } = renderPlayback({
      audioFactory: harness.audioFactory,
    })

    act(() => result.current.prime())

    expect(() => {
      rerender({ audioUrl: 'blob:blocked' })
    }).not.toThrow()

    await act(async () => {
      await Promise.resolve()
    })

    expect(harness.play).toHaveBeenCalledTimes(2)
    expect(harness.audio.src).toBe('blob:blocked')
  })

  it('leaves playback to visible native controls when automatic audio is unavailable', () => {
    const audioFactory = vi.fn(() => null)
    const { result, rerender } = renderPlayback({ audioFactory })

    act(() => result.current.prime())

    expect(() => {
      rerender({ audioUrl: 'blob:fallback' })
    }).not.toThrow()

    expect(audioFactory).toHaveBeenCalledTimes(1)
  })

  it('stops hidden playback without affecting exactly-once bookkeeping', () => {
    const harness = createAudioHarness()
    const { result, rerender } = renderPlayback({
      audioFactory: harness.audioFactory,
    })

    act(() => result.current.prime())
    rerender({ audioUrl: 'blob:reply' })

    const pauseCallsBeforeStop = harness.pause.mock.calls.length
    const loadCallsBeforeStop = harness.load.mock.calls.length

    act(() => result.current.stop())

    expect(harness.pause).toHaveBeenCalledTimes(pauseCallsBeforeStop + 1)
    expect(harness.load).toHaveBeenCalledTimes(loadCallsBeforeStop + 1)
    expect(harness.audio.src).toBe('')

    rerender({ audioUrl: 'blob:reply' })

    expect(harness.play).toHaveBeenCalledTimes(2)
  })

  it('resets the reusable hidden element on unmount', () => {
    const harness = createAudioHarness()
    const { result, rerender, unmount } = renderPlayback({
      audioFactory: harness.audioFactory,
    })

    act(() => result.current.prime())
    rerender({ audioUrl: 'blob:active' })

    const pauseCallsBeforeUnmount = harness.pause.mock.calls.length
    const loadCallsBeforeUnmount = harness.load.mock.calls.length

    unmount()

    expect(harness.pause).toHaveBeenCalledTimes(pauseCallsBeforeUnmount + 1)
    expect(harness.load).toHaveBeenCalledTimes(loadCallsBeforeUnmount + 1)
    expect(harness.audio.src).toBe('')
  })
})

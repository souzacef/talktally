import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  MAX_AUDIO_BYTES,
  SPEECH_ACTIVITY_RMS_THRESHOLD,
  WavMicrophoneRecorder,
  rootMeanSquare,
} from '@/lib/audio/wav-recorder'

interface FakeProcessor {
  onaudioprocess: ((event: { inputBuffer: { getChannelData: () => Float32Array } }) => void) | null
  connect: ReturnType<typeof vi.fn>
  disconnect: ReturnType<typeof vi.fn>
}

let processor: FakeProcessor
let stopTrack: ReturnType<typeof vi.fn>
let closeContext: ReturnType<typeof vi.fn>
let originalMediaDevices: PropertyDescriptor | undefined

describe('WavMicrophoneRecorder activity detection', () => {
  beforeEach(() => {
    processor = { onaudioprocess: null, connect: vi.fn(), disconnect: vi.fn() }
    stopTrack = vi.fn()
    closeContext = vi.fn(async () => undefined)
    originalMediaDevices = Object.getOwnPropertyDescriptor(navigator, 'mediaDevices')
    Object.defineProperty(navigator, 'mediaDevices', {
      configurable: true,
      value: { getUserMedia: vi.fn(async () => ({ getTracks: () => [{ stop: stopTrack }] })) },
    })
    vi.stubGlobal('AudioContext', class {
      sampleRate = 48_000
      state = 'running'
      destination = {}
      createMediaStreamSource() {
        return { connect: vi.fn(), disconnect: vi.fn() }
      }
      createScriptProcessor() {
        return processor
      }
      close = closeContext
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    if (originalMediaDevices) {
      Object.defineProperty(navigator, 'mediaDevices', originalMediaDevices)
    } else {
      Reflect.deleteProperty(navigator, 'mediaDevices')
    }
  })

  it('distinguishes low room noise from meaningful normalized-sample activity', async () => {
    const onActivity = vi.fn()
    const recorder = new WavMicrophoneRecorder(onActivity)
    await recorder.start()

    processor.onaudioprocess?.({
      inputBuffer: { getChannelData: () => new Float32Array([0.001, -0.001]) },
    })
    expect(onActivity).not.toHaveBeenCalled()
    processor.onaudioprocess?.({
      inputBuffer: { getChannelData: () => new Float32Array([0.03, -0.03]) },
    })

    expect(rootMeanSquare(new Float32Array([0.03, -0.03]))).toBeGreaterThan(
      SPEECH_ACTIVITY_RMS_THRESHOLD,
    )
    expect(onActivity).toHaveBeenCalledTimes(1)
    const wav = await recorder.stop()
    expect(wav.type).toBe('audio/wav')
    expect(stopTrack).toHaveBeenCalledTimes(1)
    expect(closeContext).toHaveBeenCalledTimes(1)
  })

  it('derives a finite maximum duration from the unchanged 8 MiB upload limit', async () => {
    const recorder = new WavMicrophoneRecorder()
    await recorder.start()

    const estimatedBytes = 44 + Math.floor(recorder.maximumDurationMs / 1_000 * 48_000) * 2
    expect(MAX_AUDIO_BYTES).toBe(8 * 1024 * 1024)
    expect(estimatedBytes).toBeLessThanOrEqual(MAX_AUDIO_BYTES)
    await recorder.cancel()
  })

  it('still rejects an empty recording after releasing resources', async () => {
    const recorder = new WavMicrophoneRecorder()
    await recorder.start()

    await expect(recorder.stop()).rejects.toThrow('Recording is empty')
    expect(stopTrack).toHaveBeenCalledTimes(1)
    expect(closeContext).toHaveBeenCalledTimes(1)
  })
})

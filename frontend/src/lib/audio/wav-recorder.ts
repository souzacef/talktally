import { encodePcm16Wav } from '@/lib/audio/wav'

export const MAX_AUDIO_BYTES = 8 * 1024 * 1024
export const SPEECH_ACTIVITY_RMS_THRESHOLD = 0.015

export function rootMeanSquare(samples: Float32Array): number {
  if (samples.length === 0) return 0
  let sumOfSquares = 0
  for (const sample of samples) sumOfSquares += sample * sample
  return Math.sqrt(sumOfSquares / samples.length)
}

export class WavMicrophoneRecorder {
  private readonly onSpeechActivity?: () => void
  private context: AudioContext | null = null
  private stream: MediaStream | null = null
  private source: MediaStreamAudioSourceNode | null = null
  private processor: ScriptProcessorNode | null = null
  private chunks: Float32Array[] = []

  constructor(onSpeechActivity?: () => void) {
    this.onSpeechActivity = onSpeechActivity
  }

  async start(): Promise<void> {
    if (!navigator.mediaDevices?.getUserMedia) {
      throw new Error('Microphone recording is not supported by this browser')
    }
    if (this.context) {
      throw new Error('Recording is already active')
    }

    try {
      this.stream = await navigator.mediaDevices.getUserMedia({
        audio: { channelCount: 1 },
      })
      this.context = new AudioContext()
      this.source = this.context.createMediaStreamSource(this.stream)
      this.processor = this.context.createScriptProcessor(4096, 1, 1)
      this.chunks = []
      this.processor.onaudioprocess = (event) => {
        const samples = new Float32Array(event.inputBuffer.getChannelData(0))
        this.chunks.push(samples)
        if (rootMeanSquare(samples) >= SPEECH_ACTIVITY_RMS_THRESHOLD) {
          this.onSpeechActivity?.()
        }
      }
      this.source.connect(this.processor)
      this.processor.connect(this.context.destination)
    } catch (error) {
      await this.cleanup()
      throw error
    }
  }

  get maximumDurationMs(): number {
    const sampleRate = this.context?.sampleRate ?? 48_000
    return Math.floor(((MAX_AUDIO_BYTES - 44) / (sampleRate * 2)) * 1_000)
  }

  async stop(): Promise<Blob> {
    const context = this.context
    const chunks = this.chunks
    if (!context) {
      throw new Error('Recording is not active')
    }
    const sampleRate = context.sampleRate
    await this.cleanup()
    if (chunks.every((chunk) => chunk.length === 0)) {
      throw new Error('Recording is empty')
    }
    const wav = encodePcm16Wav(chunks, sampleRate)
    if (wav.byteLength > MAX_AUDIO_BYTES) {
      throw new Error('Recording exceeds the 8 MiB upload limit')
    }
    return new Blob([wav], { type: 'audio/wav' })
  }

  async cancel(): Promise<void> {
    this.chunks = []
    await this.cleanup()
  }

  private async cleanup(): Promise<void> {
    this.processor?.disconnect()
    this.source?.disconnect()
    this.stream?.getTracks().forEach((track) => track.stop())
    const context = this.context
    this.processor = null
    this.source = null
    this.stream = null
    this.context = null
    if (context && context.state !== 'closed') {
      await context.close()
    }
  }
}

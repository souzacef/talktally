import { useCallback, useEffect, useRef } from 'react'

type AudioContextFactory = () => AudioContext | null
type AudioLoader = (url: string, signal: AbortSignal) => Promise<ArrayBuffer>

export interface SpeechPlaybackOptions {
  contextFactory?: AudioContextFactory
  loadAudio?: AudioLoader
}

function createBrowserAudioContext(): AudioContext | null {
  const AudioContextConstructor = globalThis.AudioContext
    ?? (globalThis as typeof globalThis & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
  return AudioContextConstructor ? new AudioContextConstructor() : null
}

async function loadAudioFromUrl(url: string, signal: AbortSignal): Promise<ArrayBuffer> {
  const response = await fetch(url, { signal })
  if (!response.ok) throw new Error('Unable to load speech audio')
  return response.arrayBuffer()
}

class SpeechPlaybackSession {
  private context: AudioContext | null = null
  private source: AudioBufferSourceNode | null = null
  private pendingRequest: AbortController | null = null
  private attemptedUrl: string | null = null
  private active = true
  private readonly contextFactory: AudioContextFactory
  private readonly loadAudio: AudioLoader

  constructor(
    contextFactory: AudioContextFactory,
    loadAudio: AudioLoader,
  ) {
    this.contextFactory = contextFactory
    this.loadAudio = loadAudio
  }

  activate() {
    this.active = true
  }

  prime() {
    if (!this.active) return
    try {
      if (!this.context || this.context.state === 'closed') {
        this.context = this.contextFactory()
      }
      if (this.context && this.context.state !== 'running' && this.context.state !== 'closed') {
        void this.context.resume().catch(() => undefined)
      }
    }
    catch {
      // Autoplay remains best-effort; the native audio controls are the fallback.
    }
  }

  playOnce(url: string) {
    if (!this.active || this.attemptedUrl === url) return
    this.attemptedUrl = url
    this.cancelCurrentPlayback()

    const context = this.context
    if (!context || context.state === 'closed') return

    const request = new AbortController()
    this.pendingRequest = request
    void this.play(context, url, request)
      .catch(() => undefined)
      .finally(() => {
        if (this.pendingRequest === request) this.pendingRequest = null
      })
  }

  dispose() {
    this.active = false
    this.attemptedUrl = null
    this.cancelCurrentPlayback()
    const context = this.context
    this.context = null
    if (context && context.state !== 'closed') {
      void context.close().catch(() => undefined)
    }
  }

  private async play(context: AudioContext, url: string, request: AbortController) {
    if (context.state !== 'running') {
      await context.resume()
      if (this.contextState(context) !== 'running') return
    }
    if (!this.active || request.signal.aborted || this.pendingRequest !== request) return

    const encodedAudio = await this.loadAudio(url, request.signal)
    const audioBuffer = await context.decodeAudioData(encodedAudio)
    if (!this.active || request.signal.aborted || this.pendingRequest !== request) return

    const source = context.createBufferSource()
    try {
      source.buffer = audioBuffer
      source.connect(context.destination)
      source.addEventListener('ended', () => {
        if (this.source !== source) return
        this.source = null
        source.disconnect()
      }, { once: true })
      this.pendingRequest = null
      this.source = source
      source.start()
    }
    catch {
      if (this.pendingRequest === request) this.pendingRequest = null
      if (this.source === source) this.source = null
      source.disconnect()
    }
  }

  private cancelCurrentPlayback() {
    this.pendingRequest?.abort()
    this.pendingRequest = null
    const source = this.source
    this.source = null
    if (!source) return
    try {
      source.stop()
    }
    catch {
      // The source may already have ended.
    }
    source.disconnect()
  }

  private contextState(context: AudioContext): AudioContextState {
    return context.state
  }
}

export function useSpeechPlayback(
  audioUrl: string | null,
  options: SpeechPlaybackOptions = {},
) {
  const sessionRef = useRef<SpeechPlaybackSession | null>(null)
  if (!sessionRef.current) {
    sessionRef.current = new SpeechPlaybackSession(
      options.contextFactory ?? createBrowserAudioContext,
      options.loadAudio ?? loadAudioFromUrl,
    )
  }
  const session = sessionRef.current

  useEffect(() => {
    session.activate()
    return () => session.dispose()
  }, [session])

  useEffect(() => {
    if (audioUrl) session.playOnce(audioUrl)
  }, [audioUrl, session])

  const prime = useCallback(() => session.prime(), [session])
  return { prime }
}

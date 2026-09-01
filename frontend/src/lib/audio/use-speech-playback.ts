import { useCallback, useEffect, useRef } from 'react'

const SILENT_PRIMER_WAV = 'data:audio/wav;base64,UklGRiYAAABXQVZFZm10IBAAAAABAAEAQB8AAIA+AAACABAAZGF0YQIAAAAAAA=='

export interface SpeechPlaybackAudio {
  src: string
  currentTime: number
  play: () => Promise<void>
  pause: () => void
  load: () => void
  removeAttribute: (name: string) => void
}

type AudioFactory = () => SpeechPlaybackAudio | null

export interface SpeechPlaybackOptions {
  audioFactory?: AudioFactory
}

function createBrowserAudio(): SpeechPlaybackAudio | null {
  if (typeof Audio === 'undefined') return null
  const audio = new Audio()
  audio.preload = 'auto'
  return audio
}

class SpeechPlaybackSession {
  private audio: SpeechPlaybackAudio | null = null
  private attemptedUrl: string | null = null
  private active = true
  private readonly audioFactory: AudioFactory

  constructor(audioFactory: AudioFactory) {
    this.audioFactory = audioFactory
  }

  activate() {
    this.active = true
  }

  prime() {
    if (!this.active) return
    const audio = this.ensureAudio()
    if (!audio) return

    this.reset(audio)
    audio.src = SILENT_PRIMER_WAV
    this.rewind(audio)
    this.attemptPlay(audio)
  }

  playOnce(url: string) {
    if (!this.active || this.attemptedUrl === url) return
    this.attemptedUrl = url

    const audio = this.audio
    if (!audio) return
    this.reset(audio)
    audio.src = url
    this.rewind(audio)
    this.attemptPlay(audio)
  }

  stop() {
    if (this.audio) this.reset(this.audio)
  }

  dispose() {
    this.active = false
    this.attemptedUrl = null
    this.stop()
    this.audio = null
  }

  private ensureAudio(): SpeechPlaybackAudio | null {
    if (this.audio) return this.audio
    try {
      this.audio = this.audioFactory()
    }
    catch {
      this.audio = null
    }
    return this.audio
  }

  private attemptPlay(audio: SpeechPlaybackAudio) {
    try {
      void audio.play().catch(() => undefined)
    }
    catch {
      // Autoplay remains best-effort; the visible native controls are the fallback.
    }
  }

  private reset(audio: SpeechPlaybackAudio) {
    try {
      audio.pause()
    }
    catch {
      // The element may not have started.
    }
    try {
      audio.removeAttribute('src')
      audio.load()
    }
    catch {
      // Reset failure must not affect the successful Assistant result.
    }
  }

  private rewind(audio: SpeechPlaybackAudio) {
    try {
      audio.currentTime = 0
    }
    catch {
      // Some browsers reject seeking before media metadata is available.
    }
  }
}

export function useSpeechPlayback(
  audioUrl: string | null,
  options: SpeechPlaybackOptions = {},
) {
  const sessionRef = useRef<SpeechPlaybackSession | null>(null)
  if (!sessionRef.current) {
    sessionRef.current = new SpeechPlaybackSession(
      options.audioFactory ?? createBrowserAudio,
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
  const stop = useCallback(() => session.stop(), [session])
  return { prime, stop }
}

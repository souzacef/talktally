export const ACCESS_TOKEN_KEY = 'talktally.accessToken'

export type SessionChangeReason = 'signed-in' | 'signed-out' | 'expired'
type SessionListener = () => void
type ReasonListener = (reason: SessionChangeReason) => void

export class AuthSession {
  private readonly listeners = new Set<SessionListener>()
  private readonly reasonListeners = new Set<ReasonListener>()
  private readonly storage: Storage

  constructor(storage: Storage) {
    this.storage = storage
  }

  getToken = (): string | null => this.storage.getItem(ACCESS_TOKEN_KEY)

  getServerToken = (): null => null

  setToken(accessToken: string): void {
    if (!accessToken.trim()) {
      throw new Error('access token must not be blank')
    }
    this.storage.setItem(ACCESS_TOKEN_KEY, accessToken)
    this.notify('signed-in')
  }

  clear(reason: Exclude<SessionChangeReason, 'signed-in'> = 'signed-out'): void {
    this.storage.removeItem(ACCESS_TOKEN_KEY)
    this.notify(reason)
  }

  expire = (): void => this.clear('expired')

  subscribe = (listener: SessionListener): (() => void) => {
    this.listeners.add(listener)
    return () => this.listeners.delete(listener)
  }

  subscribeToReason(listener: ReasonListener): () => void {
    this.reasonListeners.add(listener)
    return () => this.reasonListeners.delete(listener)
  }

  private notify(reason: SessionChangeReason): void {
    this.listeners.forEach((listener) => listener())
    this.reasonListeners.forEach((listener) => listener(reason))
  }
}

export const authSession = new AuthSession(window.sessionStorage)

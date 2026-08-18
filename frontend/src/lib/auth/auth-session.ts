import type { UserAccountResponse } from '@/types/api'

export const ACCESS_TOKEN_KEY = 'talktally.accessToken'
export const AUTHENTICATED_USER_KEY = 'talktally.authenticatedUser'

export type SessionChangeReason = 'signed-in' | 'signed-out' | 'expired'
type SessionListener = () => void
type ReasonListener = (reason: SessionChangeReason) => void

function isUserAccountResponse(value: unknown): value is UserAccountResponse {
  if (typeof value !== 'object' || value === null) return false
  const user = value as Partial<UserAccountResponse>
  return typeof user.userId === 'string'
    && typeof user.email === 'string'
    && typeof user.displayName === 'string'
    && user.defaultCurrency === 'BRL'
}

function readStoredUser(storage: Storage): UserAccountResponse | null {
  const serialized = storage.getItem(AUTHENTICATED_USER_KEY)
  if (!serialized) return null
  try {
    const value: unknown = JSON.parse(serialized)
    return isUserAccountResponse(value) ? value : null
  } catch {
    return null
  }
}

export class AuthSession {
  private readonly listeners = new Set<SessionListener>()
  private readonly reasonListeners = new Set<ReasonListener>()
  private readonly storage: Storage
  private lastChangeReason: SessionChangeReason | null = null
  private currentUser: UserAccountResponse | null

  constructor(storage: Storage) {
    this.storage = storage
    this.currentUser = readStoredUser(storage)
  }

  getToken = (): string | null => this.storage.getItem(ACCESS_TOKEN_KEY)

  getUser = (): UserAccountResponse | null => this.currentUser

  getLastChangeReason = (): SessionChangeReason | null => this.lastChangeReason

  getServerToken = (): null => null

  getServerUser = (): null => null

  getServerChangeReason = (): null => null

  setAuthenticated(accessToken: string, user: UserAccountResponse): void {
    if (!accessToken.trim()) {
      throw new Error('access token must not be blank')
    }
    const serializedUser = JSON.stringify(user)
    try {
      this.storage.setItem(AUTHENTICATED_USER_KEY, serializedUser)
      this.storage.setItem(ACCESS_TOKEN_KEY, accessToken)
    } catch (error) {
      this.storage.removeItem(ACCESS_TOKEN_KEY)
      this.storage.removeItem(AUTHENTICATED_USER_KEY)
      this.currentUser = null
      throw error
    }
    this.currentUser = user
    this.notify('signed-in')
  }

  clear(reason: Exclude<SessionChangeReason, 'signed-in'> = 'signed-out'): void {
    this.storage.removeItem(ACCESS_TOKEN_KEY)
    this.storage.removeItem(AUTHENTICATED_USER_KEY)
    this.currentUser = null
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
    this.lastChangeReason = reason
    this.listeners.forEach((listener) => listener())
    this.reasonListeners.forEach((listener) => listener(reason))
  }
}

export const authSession = new AuthSession(window.sessionStorage)

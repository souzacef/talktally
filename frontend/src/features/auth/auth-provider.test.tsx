import { QueryClient } from '@tanstack/react-query'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { AuthProvider, useAuth } from '@/features/auth/auth-provider'
import type { AuthService } from '@/features/auth/api/auth-api'
import { assistantConversationStorageKey } from '@/features/assistant/assistant-conversation-storage'
import {
  ACCESS_TOKEN_KEY,
  AUTHENTICATED_USER_KEY,
  AuthSession,
} from '@/lib/auth/auth-session'

const response = {
  accessToken: 'signed-token',
  tokenType: 'Bearer',
  expiresIn: 3600,
  expiresAt: '2026-08-17T12:00:00Z',
  user: {
    userId: '10000000-0000-0000-0000-000000000001',
    email: 'user@example.com',
    displayName: 'User',
    defaultCurrency: 'BRL' as const,
  },
}

function service(): AuthService {
  return {
    signIn: vi.fn().mockResolvedValue(response),
    register: vi.fn().mockResolvedValue(response.user),
  }
}

function Probe() {
  const auth = useAuth()
  return (
    <div>
      <span data-testid="token">{auth.token ?? 'none'}</span>
      <span data-testid="display-name">{auth.user?.displayName ?? 'no-user'}</span>
      <span data-testid="email">{auth.user?.email ?? 'no-email'}</span>
      <span data-testid="session-reason">{auth.lastSessionChange ?? 'none'}</span>
      <button onClick={() => void auth.signIn({ email: 'user@example.com', password: 'secret1234' })}>login</button>
      <button onClick={auth.signOut}>logout</button>
    </div>
  )
}

function renderProvider(session: AuthSession, privateQueryClient = new QueryClient()) {
  return render(
    <AuthProvider
      session={session}
      service={service()}
      privateQueryClient={privateQueryClient}
    >
      <Probe />
    </AuthProvider>,
  )
}

describe('AuthProvider', () => {
  it('stores and exposes the token and authenticated user after sign-in', async () => {
    const session = new AuthSession(window.sessionStorage)
    renderProvider(session)

    await userEvent.click(screen.getByRole('button', { name: 'login' }))

    await waitFor(() => expect(screen.getByTestId('token')).toHaveTextContent('signed-token'))
    expect(screen.getByTestId('display-name')).toHaveTextContent(response.user.displayName)
    expect(screen.getByTestId('email')).toHaveTextContent(response.user.email)
    expect(window.sessionStorage.getItem(ACCESS_TOKEN_KEY)).toBe('signed-token')
    expect(JSON.parse(window.sessionStorage.getItem(AUTHENTICATED_USER_KEY) ?? 'null')).toEqual(response.user)
  })

  it('clears the token and user together on sign-out', async () => {
    const session = new AuthSession(window.sessionStorage)
    session.setAuthenticated('existing-token', response.user)
    const conversationKey = assistantConversationStorageKey(response.user.userId)
    const otherConversationKey = assistantConversationStorageKey('another-user')
    window.sessionStorage.setItem(conversationKey, '[{"role":"user","content":"private"}]')
    window.sessionStorage.setItem(otherConversationKey, '[{"role":"user","content":"other"}]')
    renderProvider(session)

    await userEvent.click(screen.getByRole('button', { name: 'logout' }))

    expect(screen.getByTestId('token')).toHaveTextContent('none')
    expect(screen.getByTestId('display-name')).toHaveTextContent('no-user')
    expect(window.sessionStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull()
    expect(window.sessionStorage.getItem(AUTHENTICATED_USER_KEY)).toBeNull()
    expect(window.sessionStorage.getItem(conversationKey)).toBeNull()
    expect(window.sessionStorage.getItem(otherConversationKey)).not.toBeNull()
  })

  it('restores the token and user through a recreated session after reload', () => {
    new AuthSession(window.sessionStorage).setAuthenticated('existing-token', response.user)

    renderProvider(new AuthSession(window.sessionStorage))

    expect(screen.getByTestId('token')).toHaveTextContent('existing-token')
    expect(screen.getByTestId('display-name')).toHaveTextContent(response.user.displayName)
    expect(screen.getByTestId('email')).toHaveTextContent(response.user.email)
  })

  it('clears token, user, and private query data when the session expires', () => {
    const session = new AuthSession(window.sessionStorage)
    const privateQueryClient = new QueryClient()
    session.setAuthenticated('expired-token', response.user)
    const conversationKey = assistantConversationStorageKey(response.user.userId)
    window.sessionStorage.setItem(conversationKey, '[{"role":"user","content":"private"}]')
    privateQueryClient.setQueryData(['private'], 'private data')
    renderProvider(session, privateQueryClient)

    act(() => session.expire())

    expect(screen.getByTestId('token')).toHaveTextContent('none')
    expect(screen.getByTestId('display-name')).toHaveTextContent('no-user')
    expect(screen.getByTestId('session-reason')).toHaveTextContent('expired')
    expect(session.getUser()).toBeNull()
    expect(privateQueryClient.getQueryData(['private'])).toBeUndefined()
    expect(window.sessionStorage.getItem(conversationKey)).toBeNull()
  })
})

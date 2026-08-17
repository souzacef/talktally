import { QueryClient } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { AuthProvider, useAuth } from '@/features/auth/auth-provider'
import type { AuthService } from '@/features/auth/api/auth-api'
import { ACCESS_TOKEN_KEY, AuthSession } from '@/lib/auth/auth-session'

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
      <span>{auth.token ?? 'none'}</span>
      <button onClick={() => void auth.signIn({ email: 'user@example.com', password: 'secret1234' })}>login</button>
      <button onClick={auth.signOut}>logout</button>
    </div>
  )
}

function renderProvider(session: AuthSession) {
  return render(
    <AuthProvider
      session={session}
      service={service()}
      privateQueryClient={new QueryClient()}
    >
      <Probe />
    </AuthProvider>,
  )
}

describe('AuthProvider', () => {
  it('stores a successful login token in sessionStorage', async () => {
    const session = new AuthSession(window.sessionStorage)
    renderProvider(session)
    await userEvent.click(screen.getByRole('button', { name: 'login' }))
    await waitFor(() => expect(screen.getByText('signed-token')).toBeInTheDocument())
    expect(window.sessionStorage.getItem(ACCESS_TOKEN_KEY)).toBe('signed-token')
  })

  it('clears the token on logout', async () => {
    const session = new AuthSession(window.sessionStorage)
    session.setToken('existing-token')
    renderProvider(session)
    await userEvent.click(screen.getByRole('button', { name: 'logout' }))
    expect(window.sessionStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull()
  })

  it('initializes from an existing session after a reload', () => {
    window.sessionStorage.setItem(ACCESS_TOKEN_KEY, 'existing-token')
    renderProvider(new AuthSession(window.sessionStorage))
    expect(screen.getByText('existing-token')).toBeInTheDocument()
  })
})

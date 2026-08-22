import { QueryClient } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { LocaleProvider } from '@/app/providers/locale-provider'
import { ThemeProvider } from '@/app/providers/theme-provider'
import { ProtectedRoute } from '@/app/router/protected-route'
import { AppShell } from '@/components/layout/app-shell'
import type { AuthService } from '@/features/auth/api/auth-api'
import { AuthProvider } from '@/features/auth/auth-provider'
import { AuthSession } from '@/lib/auth/auth-session'
import { LoginPage } from '@/pages/login-page'
import type { AuthenticationResponse, UserAccountResponse } from '@/types/api'

const userA = {
  userId: '10000000-0000-0000-0000-000000000001',
  email: 'user-a@example.com',
  displayName: 'User A',
  defaultCurrency: 'BRL',
} satisfies UserAccountResponse

const userB = {
  userId: '20000000-0000-0000-0000-000000000002',
  email: 'user-b@example.com',
  displayName: 'User B',
  defaultCurrency: 'BRL',
} satisfies UserAccountResponse

function authServiceFor(user: UserAccountResponse): AuthService {
  const response = {
    accessToken: `token-for-${user.userId}`,
    tokenType: 'Bearer',
    expiresIn: 3600,
    expiresAt: '2026-08-18T15:00:00Z',
    user,
  } satisfies AuthenticationResponse

  return {
    signIn: vi.fn().mockResolvedValue(response),
    register: vi.fn(),
  }
}

function renderAuthFlow(
  session: AuthSession,
  service: AuthService,
  initialPath: string,
) {
  return render(
    <LocaleProvider>
      <ThemeProvider>
        <AuthProvider
          session={session}
          service={service}
          privateQueryClient={new QueryClient()}
        >
          <MemoryRouter initialEntries={[initialPath]}>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route element={<ProtectedRoute />}>
                <Route element={<AppShell />}>
                  <Route path="/dashboard" element={<h1>Dashboard destination</h1>} />
                  <Route path="/transactions" element={<h1>Transactions destination</h1>} />
                </Route>
              </Route>
            </Routes>
          </MemoryRouter>
        </AuthProvider>
      </ThemeProvider>
    </LocaleProvider>,
  )
}

async function signInAsUserB() {
  const browserUser = userEvent.setup()
  await browserUser.type(screen.getByLabelText('Email'), userB.email)
  await browserUser.type(screen.getByLabelText('Password'), 'secret1234')
  await browserUser.click(screen.getByRole('button', { name: 'Sign in' }))
}

describe('authentication navigation', () => {
  it('sends the next user to the dashboard after an explicit sign-out', async () => {
    const session = new AuthSession(window.sessionStorage)
    session.setAuthenticated('token-for-user-a', userA)
    renderAuthFlow(session, authServiceFor(userB), '/transactions')

    expect(screen.getByRole('heading', { name: 'Transactions destination' })).toBeInTheDocument()
    expect(screen.getByText(userA.email)).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Sign out' }))
    expect(await screen.findByText('Welcome to TalkTally!')).toBeInTheDocument()
    expect(screen.getByText('Built by Carlos Eduardo Freire de Souza')).toBeInTheDocument()

    await signInAsUserB()

    expect(await screen.findByRole('heading', { name: 'Dashboard destination' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Transactions destination' })).not.toBeInTheDocument()
    expect(session.getUser()).toEqual(userB)
  })

  it('returns a freshly unauthenticated deep link to its requested page after sign-in', async () => {
    const session = new AuthSession(window.sessionStorage)
    renderAuthFlow(session, authServiceFor(userB), '/transactions?kind=EXPENSE')

    expect(await screen.findByText('Welcome to TalkTally!')).toBeInTheDocument()
    await signInAsUserB()

    expect(await screen.findByRole('heading', { name: 'Transactions destination' })).toBeInTheDocument()
  })
})

import { QueryClient } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { AppShell } from '@/components/layout/app-shell'
import { ThemeProvider } from '@/app/providers/theme-provider'
import { AuthProvider } from '@/features/auth/auth-provider'
import {
  ACCESS_TOKEN_KEY,
  AUTHENTICATED_USER_KEY,
  AuthSession,
} from '@/lib/auth/auth-session'

const activeUser = {
  userId: '10000000-0000-0000-0000-000000000001',
  email: 'active@example.com',
  displayName: 'Active User',
  defaultCurrency: 'BRL' as const,
}

describe('AppShell', () => {
  it('shows the active identity in desktop and mobile account surfaces and signs out', async () => {
    const session = new AuthSession(window.sessionStorage)
    const privateQueryClient = new QueryClient()
    session.setAuthenticated('shell-token', activeUser)

    render(
      <ThemeProvider>
        <AuthProvider session={session} privateQueryClient={privateQueryClient}>
          <MemoryRouter initialEntries={['/dashboard']}>
            <Routes>
              <Route element={<AppShell />}>
                <Route path="/dashboard" element={<p>Dashboard content</p>} />
              </Route>
              <Route path="/login" element={<p>Signed out</p>} />
            </Routes>
          </MemoryRouter>
        </AuthProvider>
      </ThemeProvider>,
    )

    expect(screen.getByRole('navigation', { name: 'Desktop primary navigation' })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Mobile primary navigation' })).toBeInTheDocument()
    expect(screen.getByText(activeUser.displayName)).toBeInTheDocument()
    expect(screen.getByText(activeUser.email)).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Open account settings' }))
    const accountSheet = screen.getByRole('dialog', { name: activeUser.displayName })
    expect(within(accountSheet).getByText(activeUser.email)).toBeInTheDocument()

    await userEvent.click(within(accountSheet).getByRole('button', { name: 'Sign out' }))
    expect(window.sessionStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull()
    expect(window.sessionStorage.getItem(AUTHENTICATED_USER_KEY)).toBeNull()
    expect(await screen.findByText('Signed out')).toBeInTheDocument()
  })
})

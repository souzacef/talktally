import { QueryClient } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { AppShell } from '@/components/layout/app-shell'
import { ThemeProvider } from '@/app/providers/theme-provider'
import { AuthProvider } from '@/features/auth/auth-provider'
import { AuthSession, ACCESS_TOKEN_KEY } from '@/lib/auth/auth-session'

describe('AppShell', () => {
  it('renders desktop and mobile navigation and signs out through the real session', async () => {
    const session = new AuthSession(window.sessionStorage)
    const privateQueryClient = new QueryClient()
    session.setToken('shell-token')

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
    expect(screen.getAllByRole('link', { name: 'Home' })).toHaveLength(2)
    expect(screen.getAllByRole('link', { name: 'Assistant' })).toHaveLength(2)

    await userEvent.click(screen.getByRole('button', { name: 'Sign out' }))
    expect(window.sessionStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull()
    expect(await screen.findByText('Signed out')).toBeInTheDocument()
  })
})

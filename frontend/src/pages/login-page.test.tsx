import { QueryClient } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { LOCALE_KEY, LocaleProvider, type AppLocale } from '@/app/providers/locale-provider'
import { ThemeProvider } from '@/app/providers/theme-provider'
import { AuthProvider } from '@/features/auth/auth-provider'
import type { AuthService } from '@/features/auth/api/auth-api'
import { AuthSession } from '@/lib/auth/auth-session'
import { LoginPage } from '@/pages/login-page'

function renderLogin(locale: AppLocale = 'en-US') {
  window.localStorage.setItem(LOCALE_KEY, locale)
  const service: AuthService = {
    signIn: vi.fn(),
    register: vi.fn(),
  }

  render(
    <LocaleProvider>
      <ThemeProvider>
        <AuthProvider
          session={new AuthSession(window.sessionStorage)}
          service={service}
          privateQueryClient={new QueryClient()}
        >
          <MemoryRouter initialEntries={['/login']}>
            <LoginPage />
          </MemoryRouter>
        </AuthProvider>
      </ThemeProvider>
    </LocaleProvider>,
  )
}

describe('LoginPage password visibility', () => {
  it('toggles the current password without adding registration length validation', async () => {
    renderLogin()
    const password = screen.getByLabelText('Password')

    expect(password).toHaveAttribute('type', 'password')
    expect(password).not.toHaveAttribute('minlength')
    expect(password).not.toHaveAttribute('maxlength')

    await userEvent.click(screen.getByRole('button', { name: 'Show password' }))

    expect(password).toHaveAttribute('type', 'text')
    expect(screen.getByRole('button', { name: 'Hide password' })).toBeInTheDocument()
  })

  it('uses localized accessibility labels in pt-BR', async () => {
    renderLogin('pt-BR')
    const password = screen.getByLabelText('Senha')

    await userEvent.click(screen.getByRole('button', { name: 'Mostrar senha' }))

    expect(password).toHaveAttribute('type', 'text')
    expect(screen.getByRole('button', { name: 'Ocultar senha' })).toBeInTheDocument()
  })
})

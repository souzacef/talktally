import { QueryClient } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { LOCALE_KEY, LocaleProvider, type AppLocale } from '@/app/providers/locale-provider'
import { ThemeProvider } from '@/app/providers/theme-provider'
import { AuthProvider } from '@/features/auth/auth-provider'
import type { AuthService } from '@/features/auth/api/auth-api'
import { ApiError } from '@/lib/api/api-client'
import { AuthSession } from '@/lib/auth/auth-session'
import { RegisterPage } from '@/pages/register-page'

function renderRegistration(service: AuthService, locale: AppLocale = 'en-US') {
  window.localStorage.setItem(LOCALE_KEY, locale)
  render(
    <LocaleProvider>
      <ThemeProvider>
        <AuthProvider
          session={new AuthSession(window.sessionStorage)}
          service={service}
          privateQueryClient={new QueryClient()}
        >
          <MemoryRouter initialEntries={['/register']}>
            <Routes>
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/login" element={<p>Login destination</p>} />
            </Routes>
          </MemoryRouter>
        </AuthProvider>
      </ThemeProvider>
    </LocaleProvider>,
  )
}

async function fillRegistration() {
  await userEvent.type(screen.getByLabelText('Name'), 'Carlos')
  await userEvent.type(screen.getByLabelText('Email'), 'carlos@example.com')
  await userEvent.type(screen.getByLabelText('Password'), 'securepass123')
  await userEvent.type(screen.getByLabelText('Confirm password'), 'securepass123')
  await userEvent.click(screen.getByRole('button', { name: 'Create an account' }))
}

describe('RegisterPage', () => {
  it('navigates to login after the backend creates an account', async () => {
    const register = vi.fn().mockResolvedValue({
      userId: 'id', email: 'carlos@example.com', displayName: 'Carlos', defaultCurrency: 'BRL',
    })
    renderRegistration({ signIn: vi.fn(), register })
    await fillRegistration()
    expect(await screen.findByText('Login destination')).toBeInTheDocument()
    expect(register).toHaveBeenCalledWith({
      displayName: 'Carlos', email: 'carlos@example.com', password: 'securepass123',
    })
  })

  it('shows a safe backend validation error', async () => {
    renderRegistration({
      signIn: vi.fn(),
      register: vi.fn().mockRejectedValue(new ApiError(
        409,
        'EMAIL_ALREADY_REGISTERED',
        'email is already registered',
      )),
    })
    await fillRegistration()
    expect(await screen.findByText('email is already registered')).toBeInTheDocument()
  })

  it('renders registration guidance in pt-BR without changing password rules', () => {
    renderRegistration({ signIn: vi.fn(), register: vi.fn() }, 'pt-BR')

    expect(screen.getByText('Crie sua conta')).toBeInTheDocument()
    expect(screen.getByLabelText('Nome')).toBeInTheDocument()
    expect(screen.getByLabelText('Senha')).toHaveAttribute('minlength', '10')
    expect(screen.getByLabelText('Confirmar senha')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Mostrar senha' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Mostrar confirmação da senha' })).toBeInTheDocument()
    expect(screen.getByText('10–128 caracteres, com pelo menos uma letra e um número.')).toBeInTheDocument()
    expect(screen.getByText('Criado por Carlos Eduardo Freire de Souza')).toBeInTheDocument()
  })

  it('toggles password visibility accessibly for password and confirmation', async () => {
    renderRegistration({ signIn: vi.fn(), register: vi.fn() })
    const password = screen.getByLabelText('Password')
    const confirmation = screen.getByLabelText('Confirm password')

    expect(password).toHaveAttribute('type', 'password')
    expect(confirmation).toHaveAttribute('type', 'password')
    await userEvent.click(screen.getByRole('button', { name: 'Show password' }))
    await userEvent.click(screen.getByRole('button', { name: 'Show confirm password' }))

    expect(password).toHaveAttribute('type', 'text')
    expect(confirmation).toHaveAttribute('type', 'text')
    expect(screen.getByRole('button', { name: 'Hide password' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Hide confirm password' })).toBeInTheDocument()
  })

  it('blocks registration when passwords differ and never sends confirmation', async () => {
    const register = vi.fn()
    renderRegistration({ signIn: vi.fn(), register })
    await userEvent.type(screen.getByLabelText('Name'), 'Carlos')
    await userEvent.type(screen.getByLabelText('Email'), 'carlos@example.com')
    await userEvent.type(screen.getByLabelText('Password'), 'securepass123')
    await userEvent.type(screen.getByLabelText('Confirm password'), 'different123')

    await userEvent.click(screen.getByRole('button', { name: 'Create an account' }))

    expect(screen.getByText('Passwords do not match.')).toBeInTheDocument()
    expect(register).not.toHaveBeenCalled()
  })

  it('localizes the password-mismatch validation in pt-BR', async () => {
    const register = vi.fn()
    renderRegistration({ signIn: vi.fn(), register }, 'pt-BR')
    await userEvent.type(screen.getByLabelText('Nome'), 'Carlos')
    await userEvent.type(screen.getByLabelText('E-mail'), 'carlos@example.com')
    await userEvent.type(screen.getByLabelText('Senha'), 'securepass123')
    await userEvent.type(screen.getByLabelText('Confirmar senha'), 'different123')

    await userEvent.click(screen.getByRole('button', { name: 'Criar uma conta' }))

    expect(screen.getByText('As senhas não coincidem.')).toBeInTheDocument()
    expect(register).not.toHaveBeenCalled()
  })
})

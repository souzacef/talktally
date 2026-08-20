import { QueryClient } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { LocaleProvider } from '@/app/providers/locale-provider'
import { ThemeProvider } from '@/app/providers/theme-provider'
import { AuthProvider } from '@/features/auth/auth-provider'
import type { AuthService } from '@/features/auth/api/auth-api'
import { ApiError } from '@/lib/api/api-client'
import { AuthSession } from '@/lib/auth/auth-session'
import { RegisterPage } from '@/pages/register-page'

function renderRegistration(service: AuthService) {
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
  await userEvent.type(screen.getByLabelText('Display name'), 'Carlos')
  await userEvent.type(screen.getByLabelText('Email'), 'carlos@example.com')
  await userEvent.type(screen.getByLabelText('Password'), 'securepass123')
  await userEvent.click(screen.getByRole('button', { name: 'Create account' }))
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
})

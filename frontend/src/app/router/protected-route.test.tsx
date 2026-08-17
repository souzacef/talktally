import { QueryClient } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { ProtectedRoute } from '@/app/router/protected-route'
import { AuthProvider } from '@/features/auth/auth-provider'
import type { AuthService } from '@/features/auth/api/auth-api'
import { AuthSession } from '@/lib/auth/auth-session'

const unusedService: AuthService = {
  signIn: vi.fn(),
  register: vi.fn(),
}

describe('ProtectedRoute', () => {
  it('redirects an unauthenticated user to login', () => {
    render(
      <AuthProvider
        session={new AuthSession(window.sessionStorage)}
        service={unusedService}
        privateQueryClient={new QueryClient()}
      >
        <MemoryRouter initialEntries={['/private']}>
          <Routes>
            <Route path="/login" element={<p>Login route</p>} />
            <Route element={<ProtectedRoute />}>
              <Route path="/private" element={<p>Private route</p>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </AuthProvider>,
    )
    expect(screen.getByText('Login route')).toBeInTheDocument()
    expect(screen.queryByText('Private route')).not.toBeInTheDocument()
  })
})

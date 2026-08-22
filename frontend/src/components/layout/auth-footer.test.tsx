import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { LOCALE_KEY, LocaleProvider, type AppLocale } from '@/app/providers/locale-provider'
import { AuthFooter } from '@/components/layout/auth-footer'
import { buildBackendHealthUrl, environment } from '@/lib/env/env'

function renderFooter(locale: AppLocale) {
  window.localStorage.setItem(LOCALE_KEY, locale)
  render(<LocaleProvider><AuthFooter /></LocaleProvider>)
}

function expectSafeExternalLink(link: HTMLElement, href: string) {
  expect(link).toHaveAttribute('href', href)
  expect(link).toHaveAttribute('target', '_blank')
  expect(link).toHaveAttribute('rel', 'noopener noreferrer')
}

describe('AuthFooter', () => {
  it('renders English attribution with safe GitHub and configured backend-health links', () => {
    renderFooter('en-US')

    expect(screen.getByText('Built by Carlos Eduardo Freire de Souza')).toBeInTheDocument()
    expectSafeExternalLink(
      screen.getByRole('link', { name: 'GitHub' }),
      'https://github.com/souzacef',
    )

    const healthUrl = `${environment.apiBaseUrl}/actuator/health`
    expect(healthUrl).toMatch(/\/actuator\/health$/)
    expectSafeExternalLink(
      screen.getByRole('link', { name: 'Backend health' }),
      healthUrl,
    )
  })

  it('renders Portuguese attribution and backend-health copy', () => {
    renderFooter('pt-BR')

    expect(screen.getByText('Criado por Carlos Eduardo Freire de Souza')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'GitHub' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Status do backend' })).toBeInTheDocument()
  })

  it('constructs a single-slash health path from an API base with a trailing slash', () => {
    expect(buildBackendHealthUrl('https://api.example.com/')).toBe(
      'https://api.example.com/actuator/health',
    )
  })
})

import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { LOCALE_KEY, LocaleProvider, type AppLocale } from '@/app/providers/locale-provider'
import { AuthFooter } from '@/components/layout/auth-footer'

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
  it('renders English attribution with a safe GitHub link only', () => {
    renderFooter('en-US')

    expect(screen.getByText('Built by Carlos Eduardo Freire de Souza')).toBeInTheDocument()
    expectSafeExternalLink(
      screen.getByRole('link', { name: 'GitHub' }),
      'https://github.com/souzacef',
    )
    expect(screen.queryByRole('link', { name: 'Service status' })).not.toBeInTheDocument()
  })

  it('renders Portuguese attribution without a redundant service-status link', () => {
    renderFooter('pt-BR')

    expect(screen.getByText('Criado por Carlos Eduardo Freire de Souza')).toBeInTheDocument()
    const githubLink = screen.getByRole('link', { name: 'GitHub' })
    const footer = screen.getByRole('contentinfo')

    expect(footer).toHaveClass('sm:-mx-8')
    expect(footer).not.toHaveClass('whitespace-nowrap')
    expect(githubLink.parentElement).toHaveClass('whitespace-nowrap')
    expect(screen.queryByRole('link', { name: 'Status do serviço' })).not.toBeInTheDocument()
  })
})

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { LOCALE_KEY, LocaleProvider } from '@/app/providers/locale-provider'
import { ThemeProvider } from '@/app/providers/theme-provider'
import { BackendStatusPage } from '@/pages/backend-status-page'

const mocks = vi.hoisted(() => ({ isUp: vi.fn() }))

vi.mock('@/features/health/backend-health-api', () => ({
  backendHealthApi: { isUp: mocks.isUp },
}))

function renderStatus(locale: 'en-US' | 'pt-BR' = 'en-US') {
  window.localStorage.setItem(LOCALE_KEY, locale)
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={client}>
      <LocaleProvider><ThemeProvider><BackendStatusPage /></ThemeProvider></LocaleProvider>
    </QueryClientProvider>,
  )
}

describe('BackendStatusPage', () => {
  beforeEach(() => {
    mocks.isUp.mockResolvedValue(true)
  })

  it('shows checking then the friendly up state without backend details', async () => {
    let resolve: ((value: boolean) => void) | undefined
    mocks.isUp.mockImplementation(() => new Promise<boolean>((done) => { resolve = done }))
    renderStatus()

    expect(screen.getByText('Checking backend status…')).toBeInTheDocument()
    await act(async () => resolve?.(true))
    expect(await screen.findByText('Backend is up.')).toBeInTheDocument()
    expect(document.body.textContent).not.toContain('components')
  })

  it('shows a localized unavailable state and retry action', async () => {
    mocks.isUp.mockRejectedValue(new Error('internal health detail'))
    renderStatus('pt-BR')

    expect(await screen.findByText('O backend está indisponível ou ainda está iniciando.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Tentar novamente' })).toBeInTheDocument()
    expect(document.body.textContent).not.toContain('internal health detail')
  })
})

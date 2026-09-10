import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { LOCALE_KEY, LocaleProvider } from '@/app/providers/locale-provider'
import { BackendStatusIndicator } from '@/features/health/backend-status-indicator'

const mocks = vi.hoisted(() => ({
  phase: 'checking',
  ready: false,
  canRetry: false,
  retry: vi.fn(),
}))

vi.mock('@/features/health/backend-health-provider', () => ({
  useBackendHealth: () => mocks,
}))

function renderIndicator(locale: 'en-US' | 'pt-BR' = 'en-US') {
  window.localStorage.setItem(LOCALE_KEY, locale)
  render(<LocaleProvider><BackendStatusIndicator /></LocaleProvider>)
}

describe('BackendStatusIndicator', () => {
  beforeEach(() => {
    mocks.phase = 'checking'
    mocks.ready = false
    mocks.canRetry = false
    mocks.retry.mockReset()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('links the compact checking state to detailed service diagnostics', () => {
    renderIndicator()

    expect(screen.getByText('Checking service…')).toBeInTheDocument()
    const details = screen.getByRole('link', { name: 'Checking service…. Open service details' })
    expect(details).toHaveAttribute('href', '/backend-status')
    expect(details).toHaveAttribute('target', '_blank')
    expect(details).toHaveAttribute('rel', 'noopener noreferrer')
  })

  it('offers Retry during the soft still-starting state without replacing the details link', () => {
    mocks.phase = 'still-waking'
    mocks.canRetry = true
    renderIndicator('pt-BR')

    expect(screen.getByText('Ainda iniciando…')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ainda iniciando…. Abrir detalhes do serviço' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(mocks.retry).toHaveBeenCalledTimes(1)
  })

  it('briefly announces Ready and then collapses to the status icon', async () => {
    vi.useFakeTimers()
    mocks.phase = 'ready'
    mocks.ready = true
    renderIndicator()

    expect(screen.getByText('Ready')).toBeInTheDocument()
    await act(async () => vi.advanceTimersByTimeAsync(4_000))
    expect(screen.queryByText('Ready')).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ready. Open service details' })).toBeInTheDocument()
  })
})

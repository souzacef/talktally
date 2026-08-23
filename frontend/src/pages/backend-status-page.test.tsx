import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { LOCALE_KEY, LocaleProvider } from '@/app/providers/locale-provider'
import { ThemeProvider } from '@/app/providers/theme-provider'
import {
  BackendStatusPage,
  SERVICE_STATUS_POLL_INTERVAL_MS,
  SERVICE_STATUS_STARTUP_WINDOW_MS,
} from '@/pages/backend-status-page'

const mocks = vi.hoisted(() => ({ isUp: vi.fn() }))

vi.mock('@/features/health/backend-health-api', () => ({
  backendHealthApi: { isUp: mocks.isUp },
}))

function renderStatus(locale: 'en-US' | 'pt-BR' = 'en-US') {
  window.localStorage.setItem(LOCALE_KEY, locale)
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: Number.POSITIVE_INFINITY } },
  })
  return render(
    <QueryClientProvider client={client}>
      <LocaleProvider><ThemeProvider><BackendStatusPage /></ThemeProvider></LocaleProvider>
    </QueryClientProvider>,
  )
}

async function flush() {
  await act(async () => {
    await Promise.resolve()
    await vi.advanceTimersByTimeAsync(0)
  })
}

describe('BackendStatusPage', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    mocks.isUp.mockReset()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('shows an animated readiness state then stops polling once TalkTally is ready', async () => {
    mocks.isUp.mockResolvedValueOnce(false).mockResolvedValueOnce(true)
    renderStatus()

    expect(screen.getByRole('heading', { name: 'TalkTally status' })).toBeInTheDocument()
    expect(screen.getByText('Getting TalkTally ready...')).toBeInTheDocument()
    expect(screen.getByText(/Free hosting may take up to a minute/)).toBeInTheDocument()
    expect(screen.getByTestId('service-checking-icon')).toHaveClass('animate-spin')
    await flush()
    await flush()

    await act(async () => vi.advanceTimersByTimeAsync(SERVICE_STATUS_POLL_INTERVAL_MS * 2))
    await flush()
    expect(screen.getByText('TalkTally is ready.')).toBeInTheDocument()
    expect(screen.getByTestId('service-ready-icon')).toBeInTheDocument()
    expect(mocks.isUp).toHaveBeenCalledTimes(2)

    await act(async () => vi.advanceTimersByTimeAsync(10_000))
    expect(mocks.isUp).toHaveBeenCalledTimes(2)
  })

  it('keeps polling through failures, times out near one minute, and Retry restarts', async () => {
    mocks.isUp.mockRejectedValue(new Error('internal health detail'))
    renderStatus()
    await flush()

    await act(async () => vi.advanceTimersByTimeAsync(30_000))
    expect(screen.getByText('Getting TalkTally ready...')).toBeInTheDocument()
    expect(screen.queryByText('TalkTally is taking longer than expected.')).not.toBeInTheDocument()
    expect(mocks.isUp.mock.calls.length).toBeGreaterThan(1)

    await act(async () => vi.advanceTimersByTimeAsync(
      SERVICE_STATUS_STARTUP_WINDOW_MS - 30_000,
    ))
    expect(screen.getByText('TalkTally is taking longer than expected.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument()
    expect(document.body.textContent).not.toContain('internal health detail')

    const callsBeforeRetry = mocks.isUp.mock.calls.length
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    await flush()
    expect(screen.getByText('Getting TalkTally ready...')).toBeInTheDocument()
    expect(mocks.isUp.mock.calls.length).toBeGreaterThan(callsBeforeRetry)
  })

  it('renders the requested pt-BR readiness wording', async () => {
    mocks.isUp.mockResolvedValue(true)
    renderStatus('pt-BR')

    expect(screen.getByRole('heading', { name: 'Status do TalkTally' })).toBeInTheDocument()
    expect(screen.getByText('Preparando o TalkTally...')).toBeInTheDocument()
    expect(screen.getByText(/A hospedagem gratuita pode levar até um minuto/)).toBeInTheDocument()
    await flush()
    expect(screen.getByText('O TalkTally está pronto.')).toBeInTheDocument()
  })

  it('cleans polling timers and aborts query work on unmount', async () => {
    mocks.isUp.mockResolvedValue(false)
    const view = renderStatus()
    await flush()
    expect(mocks.isUp).toHaveBeenCalledTimes(1)

    view.unmount()
    await act(async () => vi.advanceTimersByTimeAsync(SERVICE_STATUS_STARTUP_WINDOW_MS))
    expect(mocks.isUp).toHaveBeenCalledTimes(1)
  })
})

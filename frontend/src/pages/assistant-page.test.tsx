import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { LocaleProvider } from '@/app/providers/locale-provider'
import { AssistantPage } from '@/pages/assistant-page'

const mocks = vi.hoisted(() => ({
  history: vi.fn(),
  sendMessage: vi.fn(),
  clearHistory: vi.fn(),
}))

vi.mock('@/features/assistant/api/assistant-api', () => ({
  assistantApi: {
    history: mocks.history,
    sendMessage: mocks.sendMessage,
    clearHistory: mocks.clearHistory,
    sendVoice: vi.fn(),
  },
}))

vi.mock('@/features/assistant/hooks/use-voice-assistant', () => ({
  useVoiceAssistant: () => ({
    result: undefined,
    error: null,
    state: 'idle',
    isRecording: false,
    isProcessing: false,
    startRecording: vi.fn(),
    stopRecording: vi.fn(),
  }),
}))

const savedHistory = [
  {
    id: 1,
    role: 'USER' as const,
    content: 'I paid for dinner.',
    source: 'ASSISTANT_TEXT' as const,
    status: null,
    createdAt: '2026-08-20T20:00:00Z',
  },
  {
    id: 2,
    role: 'ASSISTANT' as const,
    content: 'Who owes you for it?',
    source: null,
    status: 'NEEDS_CLARIFICATION' as const,
    createdAt: '2026-08-20T20:00:01Z',
  },
]

function renderAssistant(locale: 'en-US' | 'pt-BR' = 'en-US') {
  window.localStorage.setItem('talktally.locale', locale)
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })
  render(
    <QueryClientProvider client={client}>
      <LocaleProvider>
        <AssistantPage />
      </LocaleProvider>
    </QueryClientProvider>,
  )
  return userEvent.setup()
}

describe('AssistantPage conversation history', () => {
  beforeEach(() => {
    window.localStorage.clear()
    mocks.history.mockReset()
    mocks.sendMessage.mockReset()
    mocks.clearHistory.mockReset()
  })

  it('restores the saved server conversation with assistant status', async () => {
    mocks.history.mockResolvedValue(savedHistory)

    renderAssistant()

    expect(await screen.findByText('I paid for dinner.')).toBeInTheDocument()
    expect(screen.getByText('Who owes you for it?')).toBeInTheDocument()
    expect(screen.getByText('Needs clarification')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Clear conversation' })).toBeEnabled()
  })

  it('refreshes from server history after a successful text turn', async () => {
    mocks.history
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([
        {
          id: 3,
          role: 'USER',
          content: 'Rose owes me.',
          source: 'ASSISTANT_TEXT',
          status: null,
          createdAt: '2026-08-20T20:05:00Z',
        },
        {
          id: 4,
          role: 'ASSISTANT',
          content: 'Got it. I recorded the reimbursement.',
          source: null,
          status: 'COMPLETED',
          createdAt: '2026-08-20T20:05:01Z',
        },
      ])
    mocks.sendMessage.mockResolvedValue({
      message: 'Got it. I recorded the reimbursement.',
      status: 'COMPLETED',
    })
    const user = renderAssistant()

    expect(await screen.findByText('Ready when you are')).toBeInTheDocument()
    const messageBoxes = screen.getAllByLabelText('Message')
    await user.type(messageBoxes[0], 'Rose owes me.')
    await user.click(screen.getAllByRole('button', { name: 'Send message' })[0])

    expect(await screen.findByText('Got it. I recorded the reimbursement.')).toBeInTheDocument()
    expect(mocks.sendMessage).toHaveBeenCalledWith('Rose owes me.')
    expect(mocks.history).toHaveBeenCalledTimes(2)
  })

  it('clears saved context without leaving stale bubbles in pt-BR', async () => {
    mocks.history.mockResolvedValue(savedHistory)
    mocks.clearHistory.mockResolvedValue(undefined)
    const user = renderAssistant('pt-BR')

    expect(await screen.findByText('Who owes you for it?')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Limpar conversa' }))
    expect(screen.getByText('Limpar esta conversa e começar de novo?')).toBeInTheDocument()

    const clearButtons = screen.getAllByRole('button', { name: 'Limpar conversa' })
    await user.click(clearButtons[clearButtons.length - 1])

    await waitFor(() => expect(mocks.clearHistory).toHaveBeenCalledTimes(1))
    expect(await screen.findByText('Pronto quando você estiver')).toBeInTheDocument()
    expect(screen.queryByText('Who owes you for it?')).not.toBeInTheDocument()
  })
})

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { LocaleProvider } from '@/app/providers/locale-provider'
import {
  AssistantConversationStorage,
  assistantConversationStorageKey,
} from '@/features/assistant/assistant-conversation-storage'
import type { AuthService } from '@/features/auth/api/auth-api'
import { AuthProvider } from '@/features/auth/auth-provider'
import { AuthSession } from '@/lib/auth/auth-session'
import { AssistantPage } from '@/pages/assistant-page'
import type { UserAccountResponse, VoiceAssistantResponse } from '@/types/api'

const mocks = vi.hoisted(() => ({
  sendMessage: vi.fn(),
  voiceResultHandler: undefined as ((result: VoiceAssistantResponse) => void) | undefined,
}))

vi.mock('@/features/assistant/api/assistant-api', () => ({
  assistantApi: {
    sendMessage: mocks.sendMessage,
  },
}))

vi.mock('@/features/assistant/hooks/use-voice-assistant', () => ({
  useVoiceAssistant: (onResult?: (result: VoiceAssistantResponse) => void) => {
    mocks.voiceResultHandler = onResult
    return {
      result: null,
      error: null,
      state: 'idle',
      isRecording: false,
      isProcessing: false,
      startRecording: vi.fn(),
      stopRecording: vi.fn(),
    }
  },
}))

const userA: UserAccountResponse = {
  userId: '10000000-0000-0000-0000-000000000001',
  email: 'a@example.com',
  displayName: 'User A',
  defaultCurrency: 'BRL',
}

const userB: UserAccountResponse = {
  ...userA,
  userId: '20000000-0000-0000-0000-000000000002',
  email: 'b@example.com',
  displayName: 'User B',
}

const authService: AuthService = {
  signIn: vi.fn(),
  register: vi.fn(),
}

function renderAssistant(
  user: UserAccountResponse = userA,
  conversationStorage = new AssistantConversationStorage(window.sessionStorage),
) {
  const session = new AuthSession(window.sessionStorage)
  session.setAuthenticated('test-token', user)
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <LocaleProvider>
        <AuthProvider session={session} service={authService} privateQueryClient={queryClient}>
          <AssistantPage conversationStorage={conversationStorage} />
        </AuthProvider>
      </LocaleProvider>
    </QueryClientProvider>,
  )
}

async function sendText(message: string) {
  const textboxes = screen.getAllByLabelText('Message')
  await userEvent.type(textboxes[0]!, message)
  await userEvent.click(screen.getAllByRole('button', { name: 'Send message' })[0]!)
}

describe('AssistantPage session conversation', () => {
  beforeEach(() => {
    window.localStorage.setItem('talktally.locale', 'en-US')
  })

  it('restores ordered text messages and status after route unmount and remount', async () => {
    mocks.sendMessage.mockResolvedValueOnce({
      message: 'The transaction was recorded.',
      status: 'COMPLETED',
    })
    const firstRender = renderAssistant()

    await sendText('Record lunch')
    await screen.findByText('The transaction was recorded.')
    await waitFor(() => expect(JSON.parse(
      window.sessionStorage.getItem(assistantConversationStorageKey(userA.userId)) ?? '[]',
    )).toHaveLength(2))
    firstRender.unmount()

    renderAssistant()

    const entries = screen.getAllByRole('article')
    expect(entries).toHaveLength(2)
    expect(entries[0]).toHaveTextContent('Record lunch')
    expect(entries[1]).toHaveTextContent('The transaction was recorded.')
    expect(entries[1]).toHaveTextContent('Completed')
  })

  it('restores history after recreating the page and storage adapter like a reload', () => {
    new AssistantConversationStorage(window.sessionStorage).write(userA.userId, [
      { role: 'user', content: 'How much did I spend?' },
      { role: 'assistant', content: 'You spent R$ 20.00.', status: 'COMPLETED' },
    ])

    renderAssistant(userA, new AssistantConversationStorage(window.sessionStorage))

    const entries = screen.getAllByRole('article')
    expect(entries[0]).toHaveTextContent('How much did I spend?')
    expect(entries[1]).toHaveTextContent('You spent R$ 20.00.')
    expect(entries[1]).toHaveTextContent('Completed')
  })

  it('persists voice transcript, reply, and status without persisting audio payloads', async () => {
    renderAssistant()
    const voiceResult: VoiceAssistantResponse = {
      transcript: 'Record coffee',
      message: 'How much did the coffee cost?',
      status: 'NEEDS_CLARIFICATION',
      speechStatus: 'GENERATED',
      audio: {
        contentType: 'audio/wav',
        base64: 'PRIVATE_AUDIO_BYTES',
      },
    }

    act(() => mocks.voiceResultHandler?.(voiceResult))

    expect(await screen.findByText('Record coffee')).toBeInTheDocument()
    expect(screen.getByText('How much did the coffee cost?')).toBeInTheDocument()
    expect(screen.getByText('Needs clarification')).toBeInTheDocument()
    await waitFor(() => {
      const serialized = window.sessionStorage.getItem(
        assistantConversationStorageKey(userA.userId),
      ) ?? ''
      expect(JSON.parse(serialized)).toEqual([
        { role: 'user', content: 'Record coffee' },
        {
          role: 'assistant',
          content: 'How much did the coffee cost?',
          status: 'NEEDS_CLARIFICATION',
        },
      ])
      expect(serialized).not.toContain('PRIVATE_AUDIO_BYTES')
      expect(serialized).not.toContain('audio/wav')
    })
  })

  it('does not expose another user conversation', () => {
    const storage = new AssistantConversationStorage(window.sessionStorage)
    storage.write(userA.userId, [{ role: 'user', content: 'Private message A' }])
    storage.write(userB.userId, [{ role: 'user', content: 'Private message B' }])

    renderAssistant(userB, storage)

    expect(screen.getByText('Private message B')).toBeInTheDocument()
    expect(screen.queryByText('Private message A')).not.toBeInTheDocument()
  })

  it('falls back to an empty transcript when stored JSON is malformed', () => {
    window.sessionStorage.setItem(assistantConversationStorageKey(userA.userId), '{broken')

    expect(() => renderAssistant()).not.toThrow()
    expect(screen.getByText('Ready when you are')).toBeInTheDocument()
  })

  it('keeps the in-memory conversation usable when persistence writes fail', async () => {
    const unavailableStorage: Storage = {
      length: 0,
      clear: vi.fn(),
      getItem: vi.fn(() => null),
      key: vi.fn(() => null),
      removeItem: vi.fn(),
      setItem: vi.fn(() => { throw new DOMException('Storage unavailable') }),
    }
    mocks.sendMessage.mockResolvedValueOnce({
      message: 'The reply remains visible.',
      status: 'COMPLETED',
    })

    renderAssistant(userA, new AssistantConversationStorage(unavailableStorage))
    await sendText('Keep working')

    expect(await screen.findByText('Keep working')).toBeInTheDocument()
    expect(await screen.findByText('The reply remains visible.')).toBeInTheDocument()
  })
})

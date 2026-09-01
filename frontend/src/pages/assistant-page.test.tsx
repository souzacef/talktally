import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { LocaleProvider } from '@/app/providers/locale-provider'
import {
  AssistantConversationStorage,
  assistantConversationStorageKey,
} from '@/features/assistant/assistant-conversation-storage'
import type { AuthService } from '@/features/auth/api/auth-api'
import { AuthProvider } from '@/features/auth/auth-provider'
import { ApiError } from '@/lib/api/api-client'
import { AuthSession } from '@/lib/auth/auth-session'
import { AssistantPage } from '@/pages/assistant-page'
import type { UserAccountResponse, VoiceAssistantResponse } from '@/types/api'

const mocks = vi.hoisted(() => ({
  sendMessage: vi.fn(),
  voiceResultHandler: undefined as ((result: VoiceAssistantResponse) => void) | undefined,
  voiceStartRecording: vi.fn(),
  speechPlaybackPrime: vi.fn(),
  speechPlaybackStop: vi.fn(),
  voiceResult: null as VoiceAssistantResponse | null,
  voiceError: null as Error | null,
}))

vi.mock('@/features/assistant/api/assistant-api', () => ({
  assistantApi: {
    sendMessage: mocks.sendMessage,
  },
}))

vi.mock('@/features/assistant/hooks/use-voice-assistant', () => ({
  useVoiceAssistant: (
    onResult?: (result: VoiceAssistantResponse) => void,
    options: { requestErrorMessage?: (error: unknown) => string } = {},
  ) => {
    mocks.voiceResultHandler = onResult
    return {
      result: mocks.voiceResult,
      error: mocks.voiceError
        ? options.requestErrorMessage?.(mocks.voiceError) ?? null
        : null,
      state: 'idle',
      isRecording: false,
      isProcessing: false,
      startRecording: mocks.voiceStartRecording,
      stopRecording: vi.fn(),
    }
  },
}))

vi.mock('@/lib/audio/use-speech-playback', () => ({
  useSpeechPlayback: () => ({
    prime: mocks.speechPlaybackPrime,
    stop: mocks.speechPlaybackStop,
  }),
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
    mocks.sendMessage.mockReset()
    mocks.voiceResultHandler = undefined
    mocks.voiceStartRecording.mockReset()
    mocks.speechPlaybackPrime.mockReset()
    mocks.speechPlaybackStop.mockReset()
    mocks.voiceResult = null
    mocks.voiceError = null
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

  it('uses finance-oriented desktop and compact placeholders in English', () => {
    renderAssistant()

    expect(screen.getByPlaceholderText('Ask about your finances or record a transaction…')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('Ask or record a transaction…')).toBeInTheDocument()
  })

  it('primes speech playback synchronously before starting Assistant recording', async () => {
    const order: string[] = []
    mocks.speechPlaybackPrime.mockImplementation(() => order.push('prime'))
    mocks.voiceStartRecording.mockImplementation(() => {
      order.push('start-recording')
      return Promise.resolve()
    })
    renderAssistant()

    await userEvent.click(
      screen.getAllByRole('button', { name: 'Start microphone recording' })[0]!,
    )

    expect(order).toEqual(['prime', 'start-recording'])
  })

  it('keeps both responsive voice players passive and stops hidden autoplay on manual play', async () => {
    Object.defineProperty(URL, 'createObjectURL', {
      configurable: true,
      value: vi.fn(() => 'blob:assistant-voice-reply'),
    })
    Object.defineProperty(URL, 'revokeObjectURL', {
      configurable: true,
      value: vi.fn(),
    })

    mocks.voiceResult = {
      transcript: 'Voice question',
      message: 'Voice answer',
      status: 'COMPLETED',
      speechStatus: 'GENERATED',
      audio: {
        contentType: 'audio/wav',
        base64: 'QUJD',
      },
    }

    renderAssistant()

    await waitFor(() => {
      expect(document.querySelectorAll('audio')).toHaveLength(2)
    })

    const players = Array.from(document.querySelectorAll('audio'))

    for (const player of players) {
      expect(player).not.toHaveAttribute('autoplay')
    }

    fireEvent.play(players[0]!)
    expect(mocks.speechPlaybackStop).toHaveBeenCalledTimes(1)

    fireEvent.play(players[1]!)
    expect(mocks.speechPlaybackStop).toHaveBeenCalledTimes(2)
  })

  it('uses finance-oriented desktop and compact placeholders in pt-BR', () => {
    window.localStorage.setItem('talktally.locale', 'pt-BR')
    renderAssistant()

    expect(screen.getByPlaceholderText('Pergunte sobre suas finanças ou registre uma transação…')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('Pergunte ou registre uma transação…')).toBeInTheDocument()
  })

  it('shows voice status once in the response while preserving playback feedback', () => {
    const storage = new AssistantConversationStorage(window.sessionStorage)
    storage.write(userA.userId, [
      { role: 'user', content: 'Voice question' },
      { role: 'assistant', content: 'Voice answer', status: 'COMPLETED' },
    ])
    mocks.voiceResult = {
      transcript: 'Voice question',
      message: 'Voice answer',
      status: 'COMPLETED',
      speechStatus: 'UNAVAILABLE',
      audio: null,
    }

    renderAssistant(userA, storage)

    expect(screen.getAllByText('Completed')).toHaveLength(1)
    expect(screen.getAllByText('Voice reply unavailable. The result still succeeded.').length).toBeGreaterThan(0)
  })

  it('uses semantic Assistant formatting without applying it to user messages', () => {
    const storage = new AssistantConversationStorage(window.sessionStorage)
    storage.write(userA.userId, [
      { role: 'user', content: '* Show **literal user text**' },
      {
        role: 'assistant',
        content: 'You spent **R$ 416.75**.\n* **Food and dining**: R$ 100.00',
        status: 'COMPLETED',
      },
    ])

    renderAssistant(userA, storage)

    const entries = screen.getAllByRole('article')
    expect(entries[0]).toHaveTextContent('* Show **literal user text**')
    expect(entries[0]?.querySelector('strong')).toBeNull()
    expect(entries[0]?.querySelector('ul')).toBeNull()
    expect(entries[1]?.querySelector('strong')).toHaveTextContent('R$ 416.75')
    expect(entries[1]?.querySelector('ul')).not.toBeNull()
    expect(entries[1]?.querySelector('li strong')).toHaveTextContent('Food and dining')
    expect(entries[1]).not.toHaveTextContent('**')
    expect(entries[1]?.querySelector('p ul')).toBeNull()
  })

  it.each([
    [
      429,
      'RATE_LIMITED',
      'too many requests',
      'You’re sending requests a little too quickly. Wait a moment and try again.',
    ],
    [
      503,
      'ASSISTANT_UNAVAILABLE',
      'assistant is temporarily unavailable',
      'The assistant is temporarily unavailable. Please try again shortly.',
    ],
  ] as const)(
    'uses localized product copy for text Assistant %s %s failures',
    async (status, code, backendMessage, expected) => {
      mocks.sendMessage.mockRejectedValueOnce(new ApiError(status, code, backendMessage))
      renderAssistant()

      await sendText('Test failure handling')

      expect(await screen.findByText(expected)).toBeInTheDocument()
      expect(screen.queryByText(backendMessage)).not.toBeInTheDocument()
    },
  )

  it('uses the shared localized presentation for voice request failures', () => {
    mocks.voiceError = new ApiError(
      503,
      'SPEECH_RECOGNITION_UNAVAILABLE',
      'speech recognition is temporarily unavailable',
    )

    renderAssistant()

    expect(screen.getAllByText('Voice recognition is temporarily unavailable. Try again shortly or type your message instead.').length).toBeGreaterThan(0)
    expect(screen.queryByText('speech recognition is temporarily unavailable')).not.toBeInTheDocument()
  })
})

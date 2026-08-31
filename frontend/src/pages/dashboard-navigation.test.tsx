import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DashboardPage } from '@/pages/dashboard-page'
import { AssistantPage } from '@/pages/assistant-page'
import { LOCALE_KEY, LocaleProvider } from '@/app/providers/locale-provider'
import { AuthProvider } from '@/features/auth/auth-provider'
import { AuthSession } from '@/lib/auth/auth-session'
import {
  assistantConversationStorage,
  assistantConversationStorageKey,
} from '@/features/assistant/assistant-conversation-storage'
import type { TransactionResponse, VoiceAssistantResponse } from '@/types/api'

const mocks = vi.hoisted(() => ({
  summary: vi.fn(),
  categoryBreakdown: vi.fn(),
  monthlyCashFlow: vi.fn(),
  transactionList: vi.fn(),
  categoryList: vi.fn(),
  peopleList: vi.fn(),
  voiceResultHandler: undefined as ((result: VoiceAssistantResponse) => void) | undefined,
}))

vi.mock('@/features/dashboard/hooks/use-dashboard', () => ({
  useFinancialSummary: mocks.summary,
  useCategoryBreakdown: mocks.categoryBreakdown,
  useMonthlyCashFlow: mocks.monthlyCashFlow,
}))
vi.mock('@/features/transactions/api/transaction-api', () => ({
  transactionApi: { list: mocks.transactionList },
}))
vi.mock('@/features/categories/api/category-api', () => ({
  categoryApi: { list: mocks.categoryList },
}))
vi.mock('@/features/reimbursements/api/people-api', () => ({
  peopleApi: { list: mocks.peopleList },
}))
vi.mock('@/features/assistant/hooks/use-voice-assistant', async () => {
  const { useState } = await import('react')
  return {
    useVoiceAssistant: (onResult?: (result: VoiceAssistantResponse) => void) => {
      const [result, setResult] = useState<VoiceAssistantResponse | null>(null)
      mocks.voiceResultHandler = (nextResult) => {
        onResult?.(nextResult)
        setResult(nextResult)
      }
      return {
        result,
        error: null,
        state: 'idle',
        isRecording: false,
        isProcessing: false,
        startRecording: vi.fn(),
        stopRecording: vi.fn(),
      }
    },
  }
})

const recentTransaction: TransactionResponse = {
  id: 'recent-transaction-id',
  kind: 'EXPENSE',
  description: 'Recent coffee',
  amount: '7.89',
  currency: 'BRL',
  categoryId: 'food-category-id',
  eventDate: '2026-08-19',
  firstOccurrenceDate: '2026-08-19',
  source: 'MANUAL',
  installmentCount: 1,
  managedByReimbursement: false,
  createdAt: '2026-08-22T17:35:00Z',
  updatedAt: '2026-08-22T17:35:00Z',
  occurrences: [
    { sequenceNumber: 1, effectiveDate: '2026-08-19', amount: '7.89', currency: 'BRL' },
  ],
}

function renderDashboard(displayName = 'Carlos Eduardo Freire de Souza') {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const session = new AuthSession(window.sessionStorage)
  session.setAuthenticated('test-token', {
    userId: 'user-id',
    email: 'carlos@example.com',
    displayName,
    defaultCurrency: 'BRL',
  })
  const view = render(
    <QueryClientProvider client={client}>
      <LocaleProvider>
        <AuthProvider session={session} privateQueryClient={client}>
          <MemoryRouter initialEntries={['/dashboard']}>
            <Routes>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/transactions" element={<p>All transactions destination</p>} />
              <Route path="/transactions/:transactionId" element={<p>Transaction detail destination</p>} />
              <Route path="/assistant" element={<AssistantPage />} />
            </Routes>
          </MemoryRouter>
        </AuthProvider>
      </LocaleProvider>
    </QueryClientProvider>,
  )
  return { user: userEvent.setup(), ...view }
}

describe('Dashboard Recent Activity navigation', () => {
  beforeEach(() => {
    Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: vi.fn() })
    Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: vi.fn() })
    mocks.voiceResultHandler = undefined
    mocks.summary.mockReturnValue({
      isPending: false,
      error: null,
      data: {
        period: {
          earnedIncome: '0',
          expenses: '7.89',
          reimbursementsReceived: '0',
          netCashFlow: '-7.89',
        },
        owedToMe: { outstanding: '0', openClaims: 0 },
      },
    })
    mocks.categoryBreakdown.mockReturnValue({
      isPending: false,
      error: null,
      data: { categories: [] },
    })
    mocks.monthlyCashFlow.mockReturnValue({
      isPending: false,
      error: null,
      data: { buckets: [] },
    })
    mocks.transactionList.mockResolvedValue({
      items: [recentTransaction],
      page: 0,
      size: 5,
      totalElements: 1,
      totalPages: 1,
    })
    mocks.categoryList.mockResolvedValue([
      { id: 'food-category-id', code: 'FOOD_DINING', displayName: 'Food and dining', allowedKind: 'EXPENSE', builtIn: true },
    ])
    mocks.peopleList.mockResolvedValue([])
  })

  it('navigates View all to the transaction ledger', async () => {
    const { user } = renderDashboard()

    await user.click(await screen.findByRole('link', { name: 'View all' }))
    expect(screen.getByText('All transactions destination')).toBeInTheDocument()
  })

  it('greets the authenticated user by first name', () => {
    renderDashboard()

    expect(screen.getByRole('heading', { name: 'Hello, Carlos!' })).toBeInTheDocument()
  })

  it('shows the friendly category and navigates a recent transaction to detail', async () => {
    const { user } = renderDashboard()

    expect(await screen.findByText(/Food and dining/)).toBeInTheDocument()
    expect(document.body.textContent).not.toContain('food-category-id')
    await user.click(screen.getByRole('link', { name: 'View transaction Recent coffee' }))
    expect(screen.getByText('Transaction detail destination')).toBeInTheDocument()
  })

  it('renders dashboard copy, category labels, dates, and BRL amounts in pt-BR', async () => {
    window.localStorage.setItem(LOCALE_KEY, 'pt-BR')
    renderDashboard()

    expect(screen.getByRole('heading', { name: 'Olá, Carlos!' })).toBeInTheDocument()
    expect(screen.getByText('Atividade recente')).toBeInTheDocument()
    expect(await screen.findByText(/Alimentação · 19\/08\/2026/)).toBeInTheDocument()
    expect(screen.getAllByText(/R\$\s*7,89/).length).toBeGreaterThan(0)
    expect(screen.getByRole('link', { name: 'Ver transação Recent coffee' })).toBeInTheDocument()
  })

  it.each([
    ['en-US', '1 occurrence', '2 occurrences'],
    ['pt-BR', '1 ocorrência', '2 ocorrências'],
  ] as const)(
    'pluralizes category occurrence counts in %s',
    (locale, singular, plural) => {
      window.localStorage.setItem(LOCALE_KEY, locale)
      mocks.categoryBreakdown.mockReturnValue({
        isPending: false,
        error: null,
        data: {
          categories: [
            {
              categoryId: 'food-category-id',
              code: 'FOOD_DINING',
              displayName: 'Food and dining',
              total: '7.89',
              percentage: '33.33',
              occurrenceCount: 1,
              transactionCount: 1,
            },
            {
              categoryId: 'groceries-category-id',
              code: 'GROCERIES',
              displayName: 'Groceries',
              total: '15.78',
              percentage: '66.67',
              occurrenceCount: 2,
              transactionCount: 2,
            },
          ],
        },
      })

      renderDashboard()

      expect(screen.getByText(singular)).toBeInTheDocument()
      expect(screen.getByText(plural)).toBeInTheDocument()
    },
  )

  it.each([
    ['en-US', 0, 'Open claims', 'People'],
    ['en-US', 1, 'Open claim', 'Person'],
    ['en-US', 2, 'Open claims', 'People'],
    ['pt-BR', 0, 'Cobranças abertas', 'Pessoas'],
    ['pt-BR', 1, 'Cobrança aberta', 'Pessoa'],
    ['pt-BR', 2, 'Cobranças abertas', 'Pessoas'],
  ] as const)(
    'pluralizes dashboard count labels in %s for %i',
    async (locale, count, openClaimsLabel, peopleLabel) => {
      window.localStorage.setItem(LOCALE_KEY, locale)
      mocks.summary.mockReturnValue({
        isPending: false,
        error: null,
        data: {
          period: {
            earnedIncome: '0',
            expenses: '7.89',
            reimbursementsReceived: '0',
            netCashFlow: '-7.89',
          },
          owedToMe: { outstanding: '0', openClaims: count },
        },
      })
      mocks.peopleList.mockResolvedValue(Array.from({ length: count }, (_, index) => ({
        id: `person-${index}`,
        displayName: `Person ${index}`,
      })))

      renderDashboard()

      const openClaims = screen.getByText(openClaimsLabel)
      expect(openClaims.previousElementSibling).toHaveTextContent(String(count))
      await waitFor(() => {
        const people = screen.getAllByText(peopleLabel).find(
          (element) => element.previousElementSibling?.textContent === String(count),
        )
        expect(people).toBeDefined()
      })
    },
  )

  it('appends one Home voice exchange to existing Assistant history without audio', async () => {
    const audioBase64 = 'QUJD'
    assistantConversationStorage.write('user-id', [
      { role: 'assistant', content: 'Existing history', status: 'COMPLETED' },
    ])
    const { user } = renderDashboard()
    const result: VoiceAssistantResponse = {
      transcript: 'How much did I spend?',
      message: 'You spent R$ 42.00.',
      status: 'COMPLETED',
      speechStatus: 'GENERATED',
      audio: { contentType: 'audio/wav', base64: audioBase64 },
    }

    act(() => mocks.voiceResultHandler?.(result))

    await waitFor(() => {
      const serialized = window.sessionStorage.getItem(
        assistantConversationStorageKey('user-id'),
      ) ?? ''
      expect(JSON.parse(serialized)).toEqual([
        { role: 'assistant', content: 'Existing history', status: 'COMPLETED' },
        { role: 'user', content: 'How much did I spend?' },
        { role: 'assistant', content: 'You spent R$ 42.00.', status: 'COMPLETED' },
      ])
      expect(serialized).not.toContain(audioBase64)
      expect(serialized).not.toContain('blob:')
    })

    await user.click(screen.getByRole('link', { name: 'Type instead' }))
    const entries = screen.getAllByRole('article')
    expect(entries).toHaveLength(3)
    expect(entries[0]).toHaveTextContent('Existing history')
    expect(entries[1]).toHaveTextContent('How much did I spend?')
    expect(entries[2]).toHaveTextContent('You spent R$ 42.00.')
  })

  it('formats the Home response while preserving the transcript', () => {
    renderDashboard()
    const result: VoiceAssistantResponse = {
      transcript: 'How much did I spend?',
      message: 'You spent **R$ 42.00**.\n* **Food and dining**: R$ 42.00',
      status: 'COMPLETED',
      speechStatus: 'UNAVAILABLE',
      audio: null,
    }

    act(() => mocks.voiceResultHandler?.(result))

    expect(screen.getByText('R$ 42.00').tagName).toBe('STRONG')
    const categoryName = screen.getByText('Food and dining')
    expect(categoryName.closest('ul')).not.toBeNull()
    expect(categoryName.closest('li')?.querySelector('strong')).toHaveTextContent('Food and dining')
    expect(screen.getByText('Heard: How much did I spend?')).toBeInTheDocument()
    expect(screen.getByText('Voice reply unavailable. The result still succeeded.')).toBeInTheDocument()
    expect(document.body).not.toHaveTextContent('**')
    expect(document.querySelector('audio')).toBeNull()
  })

  it('plays generated Home audio and revokes object URLs on replacement and unmount', () => {
    vi.mocked(URL.createObjectURL)
      .mockReturnValueOnce('blob:first-voice-reply')
      .mockReturnValueOnce('blob:second-voice-reply')
    const { unmount } = renderDashboard()

    act(() => mocks.voiceResultHandler?.({
      transcript: 'First question',
      message: 'First answer',
      status: 'COMPLETED',
      speechStatus: 'GENERATED',
      audio: { contentType: 'audio/wav', base64: 'QUJD' },
    }))

    expect(screen.getByText('Voice reply')).toBeInTheDocument()
    expect(document.querySelector('audio')).toHaveAttribute('src', 'blob:first-voice-reply')
    expect(screen.getByText('Heard: First question')).toBeInTheDocument()

    act(() => mocks.voiceResultHandler?.({
      transcript: 'Second question',
      message: 'Second answer',
      status: 'COMPLETED',
      speechStatus: 'GENERATED',
      audio: { contentType: 'audio/wav', base64: 'REVG' },
    }))

    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:first-voice-reply')
    expect(document.querySelector('audio')).toHaveAttribute('src', 'blob:second-voice-reply')

    unmount()
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:second-voice-reply')
  })
})

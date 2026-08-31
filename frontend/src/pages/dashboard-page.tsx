import { lazy, Suspense, useCallback, useEffect, useState } from 'react'
import { ArrowDownLeft, ArrowRight, ArrowUpRight, HandCoins, RotateCcw, Sparkles } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useLocale } from '@/app/providers/locale-provider'
import { AssistantMessageContent } from '@/components/assistant/assistant-message-content'
import { SpeechResult } from '@/components/assistant/assistant-result'
import { VoiceOrb } from '@/components/assistant/voice-orb'
import { StatCard } from '@/components/dashboard/stat-card'
import { StatePanel } from '@/components/feedback/state-panel'
import { KindIcon } from '@/components/finance/financial-visuals'
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { categoryLabelForCode, categoryLabelForId } from '@/features/categories/category-presentation'
import { useCategories } from '@/features/categories/hooks/use-categories'
import { useAuth } from '@/features/auth/auth-provider'
import { peopleApi } from '@/features/reimbursements/api/people-api'
import { transactionApi } from '@/features/transactions/api/transaction-api'
import { useVoiceAssistant } from '@/features/assistant/hooks/use-voice-assistant'
import {
  AssistantConversationStorage,
  assistantConversationStorage,
} from '@/features/assistant/assistant-conversation-storage'
import { assistantText } from '@/features/assistant/assistant-messages'
import {
  useCategoryBreakdown,
  useFinancialSummary,
  useMonthlyCashFlow,
} from '@/features/dashboard/hooks/use-dashboard'
import { currentMonthRange, trailingSixMonthRange } from '@/lib/dates/reporting-periods'
import { createAudioObjectUrl } from '@/lib/audio/base64-audio'
import { financialAmountStyle } from '@/lib/money/financial-display'
import { queryKeys } from '@/lib/query/query-client'
import type { VoiceAssistantResponse } from '@/types/api'

const CashFlowChart = lazy(() => import('@/components/dashboard/cash-flow-chart').then((module) => ({
  default: module.CashFlowChart,
})))

const recentParams = { page: 0, size: 5 } as const

function firstName(displayName: string | null | undefined): string | null {
  return displayName?.trim().split(/\s+/)[0] || null
}

interface DashboardPageProps {
  conversationStorage?: AssistantConversationStorage
}

export function DashboardPage({
  conversationStorage = assistantConversationStorage,
}: DashboardPageProps = {}) {
  const { user } = useAuth()
  const { locale, t, plural, formatDate, formatMoney } = useLocale()
  const period = currentMonthRange()
  const history = trailingSixMonthRange()
  const summary = useFinancialSummary(period.from, period.to)
  const breakdown = useCategoryBreakdown(period.from, period.to, 'EXPENSE')
  const cashFlow = useMonthlyCashFlow(history.from, history.to)
  const transactions = useQuery({
    queryKey: queryKeys.transactions.list(recentParams),
    queryFn: ({ signal }) => transactionApi.list(recentParams, signal),
  })
  const categories = useCategories()
  const people = useQuery({
    queryKey: queryKeys.people.all,
    queryFn: ({ signal }) => peopleApi.list(signal),
  })
  const appendVoiceResult = useCallback((result: VoiceAssistantResponse) => {
    if (!user) return
    conversationStorage.append(user.userId, [
      { role: 'user', content: result.transcript },
      { role: 'assistant', content: result.message, status: result.status },
    ])
  }, [conversationStorage, user])
  const voice = useVoiceAssistant(appendVoiceResult, {
    noSpeechMessage: assistantText(locale, 'noSpeechDetected'),
    tooShortMessage: assistantText(locale, 'recordingTooShort'),
  })
  const [audioUrl, setAudioUrl] = useState<string | null>(null)
  const userFirstName = firstName(user?.displayName)

  useEffect(() => {
    const audio = voice.result?.audio
    if (!audio) {
      setAudioUrl(null)
      return
    }
    const objectAudio = createAudioObjectUrl(audio.contentType, audio.base64)
    setAudioUrl(objectAudio.url)
    return objectAudio.revoke
  }, [voice.result])

  function toggleVoice() {
    if (voice.isRecording) voice.stopRecording()
    else void voice.startRecording()
  }

  return (
    <section className="space-y-7">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-heading text-3xl font-semibold tracking-[-0.045em] sm:text-4xl">
            {userFirstName ? t('dashboard.greetingNamed', { name: userFirstName }) : t('dashboard.greetingGeneric')}
          </h1>
          <p className="mt-2 text-muted-foreground">{t('dashboard.story')}</p>
        </div>
        <p className="rounded-full border bg-card px-3 py-1.5 text-xs font-medium text-muted-foreground shadow-xs">
          {formatDate(period.from)} – {formatDate(period.to)}
        </p>
      </header>

      <div className="grid min-w-0 gap-4 lg:grid-cols-3">
        <Card className="relative min-h-80 overflow-hidden border-primary/15 bg-[linear-gradient(145deg,var(--card),var(--accent))] lg:row-span-2">
          <div className="pointer-events-none absolute -right-16 -top-16 size-52 rounded-full bg-primary/10 blur-2xl" aria-hidden="true" />
          <CardContent className="relative flex h-full flex-col items-center justify-center py-7 text-center">
            <span className="mb-4 rounded-full border border-primary/15 bg-card/75 px-3 py-1 text-xs font-semibold text-primary">{t('dashboard.voiceFirstCapture')}</span>
            <VoiceOrb state={voice.state} onClick={toggleVoice} />
            <h2 className="mt-5 font-heading text-2xl font-semibold tracking-[-0.035em]">{t('dashboard.whatHappened')}</h2>
            <p className="mt-2 max-w-xs text-sm leading-relaxed text-muted-foreground">
              {t('dashboard.voiceDescription')}
            </p>
            {voice.error && <p className="mt-3 text-sm text-expense" role="alert">{voice.error}</p>}
            {voice.result && (
              <div className="mt-4 w-full rounded-2xl border bg-card/80 p-3 text-left text-sm" aria-live="polite">
                <AssistantMessageContent content={voice.result.message} />
                <p className="mt-1 text-xs text-muted-foreground">{t('dashboard.heard', { transcript: voice.result.transcript })}</p>
                <div className="mt-2">
                  <SpeechResult
                    speechStatus={voice.result.speechStatus}
                    audioUrl={audioUrl}
                    unavailableLabel={t('dashboard.voiceReplyUnavailable')}
                    voiceReplyLabel={assistantText(locale, 'voiceReply')}
                    unsupportedLabel={assistantText(locale, 'audioUnsupported')}
                  />
                </div>
              </div>
            )}
            <Link to="/assistant" className="mt-5 text-sm font-semibold text-primary underline-offset-4 hover:underline">{t('dashboard.typeInstead')}</Link>
          </CardContent>
        </Card>

        {summary.isPending && Array.from({ length: 4 }, (_, index) => (
          <div key={index} className="min-h-40 animate-pulse rounded-2xl border bg-card shadow-[var(--shadow-soft)]" aria-hidden="true" />
        ))}
        {summary.error && (
          <StatePanel className="lg:col-span-2" tone="error" title={t('dashboard.summaryUnavailable')} description={t('dashboard.summaryUnavailableDescription')} />
        )}
        {summary.data && (
          <>
            <StatCard title={t('dashboard.earnedIncome')} value={formatMoney(summary.data.period.earnedIncome)} note={t('dashboard.earnedIncomeNote')} icon={ArrowDownLeft} tone="income" />
            <StatCard title={t('dashboard.expenses')} value={formatMoney(summary.data.period.expenses)} note={t('dashboard.expensesNote')} icon={ArrowUpRight} tone="expense" />
            <StatCard title={t('dashboard.reimbursementsReceived')} value={formatMoney(summary.data.period.reimbursementsReceived)} note={t('dashboard.reimbursementsNote')} icon={RotateCcw} tone="reimbursement" />
            <StatCard title={t('dashboard.netCashFlow')} value={formatMoney(summary.data.period.netCashFlow)} note={t('dashboard.netCashFlowNote')} icon={Sparkles} tone="primary" />
          </>
        )}
      </div>

      <div className="grid min-w-0 gap-4 lg:grid-cols-3">
        <Card className="min-w-0 lg:col-span-2">
          <CardHeader>
            <CardTitle className="text-xl">{t('dashboard.monthlyCashFlow')}</CardTitle>
            <CardDescription>{t('dashboard.monthlyCashFlowDescription')}</CardDescription>
          </CardHeader>
          <CardContent>
            {cashFlow.isPending && <div className="h-64 animate-pulse rounded-2xl bg-muted" aria-label={t('dashboard.loadingMonthlyCashFlow')} />}
            {cashFlow.error && <StatePanel tone="error" title={t('dashboard.cashFlowUnavailable')} description={t('dashboard.cashFlowUnavailableDescription')} />}
            {cashFlow.data?.buckets.length === 0 && <StatePanel title={t('dashboard.noCashFlow')} description={t('dashboard.noCashFlowDescription')} />}
            {cashFlow.data && cashFlow.data.buckets.length > 0 && (
              <Suspense fallback={<div className="h-64 animate-pulse rounded-2xl bg-muted" aria-label={t('dashboard.loadingCashFlowChart')} />}>
                <CashFlowChart buckets={cashFlow.data.buckets} />
              </Suspense>
            )}
          </CardContent>
        </Card>

        <Card className="border-primary bg-primary text-primary-foreground shadow-[var(--shadow-lift)]">
          <CardContent className="flex h-full min-h-72 flex-col justify-between">
            <div className="flex items-center justify-between">
              <span className="rounded-full bg-white/12 px-3 py-1 text-xs font-semibold">{t('dashboard.owedToMe')}</span>
              <HandCoins className="size-6 opacity-80" aria-hidden="true" />
            </div>
            <div className="py-7">
              <p className="text-sm opacity-75">{t('dashboard.totalOutstanding')}</p>
              <p className="tabular-nums mt-2 font-heading text-4xl font-semibold tracking-[-0.05em]">
                {summary.data ? formatMoney(summary.data.owedToMe.outstanding) : '—'}
              </p>
            </div>
            <div className="grid grid-cols-2 gap-3 border-t border-white/15 pt-4">
              <div><p className="font-heading text-xl font-semibold">{summary.data?.owedToMe.openClaims ?? '—'}</p><p className="text-xs opacity-70">{summary.data ? plural('dashboard.openClaims', summary.data.owedToMe.openClaims) : t('dashboard.openClaims')}</p></div>
              <div><p className="font-heading text-xl font-semibold">{people.data?.length ?? '—'}</p><p className="text-xs opacity-70">{people.data ? plural('dashboard.people', people.data.length) : t('dashboard.people')}</p></div>
            </div>
            <Link to="/owed" className="mt-5 text-sm font-semibold underline decoration-white/40 underline-offset-4">{t('dashboard.viewReimbursements')}</Link>
          </CardContent>
        </Card>
      </div>

      <div className="grid min-w-0 gap-4 lg:grid-cols-3">
        <Card>
          <CardHeader><CardTitle className="text-xl">{t('dashboard.spendingByCategory')}</CardTitle><CardDescription>{t('dashboard.thisMonth')}</CardDescription></CardHeader>
          <CardContent>
            {breakdown.isPending && <div className="h-36 animate-pulse rounded-2xl bg-muted" />}
            {breakdown.error && <StatePanel tone="error" title={t('dashboard.categoriesUnavailable')} description={t('dashboard.categoriesUnavailableDescription')} />}
            {breakdown.data?.categories.length === 0 && <StatePanel title={t('dashboard.noExpenses')} description={t('dashboard.noExpensesDescription')} />}
            <ul className="space-y-4">
              {breakdown.data?.categories.map((category) => (
                <li key={category.categoryId} className="flex items-center justify-between gap-4">
                  <div className="min-w-0"><p className="truncate font-semibold">{categoryLabelForCode(category.code, category.displayName, locale)}</p><p className="text-xs text-muted-foreground">{plural('dashboard.occurrences', category.occurrenceCount)}</p></div>
                  <span className="tabular-nums font-heading font-semibold text-expense">{formatMoney(category.total)}</span>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="text-xl">{t('dashboard.recentActivity')}</CardTitle>
            <CardDescription>{t('dashboard.recentActivityDescription')}</CardDescription>
            <CardAction>
              <Link to="/transactions" className="inline-flex items-center gap-1 text-sm font-semibold text-primary hover:underline">
                {t('dashboard.viewAll')} <ArrowRight className="size-4" aria-hidden="true" />
              </Link>
            </CardAction>
          </CardHeader>
          <CardContent>
            {transactions.isPending && <div className="h-36 animate-pulse rounded-2xl bg-muted" />}
            {transactions.error && <StatePanel tone="error" title={t('dashboard.activityUnavailable')} description={t('dashboard.activityUnavailableDescription')} />}
            {transactions.data?.items.length === 0 && <StatePanel title={t('dashboard.noTransactions')} description={t('dashboard.noTransactionsDescription')} />}
            <ul className="divide-y">
              {transactions.data?.items.map((transaction) => {
                const amount = financialAmountStyle(transaction.kind)
                const categoryName = categories.isPending
                  ? t('dashboard.loadingCategory')
                  : categoryLabelForId(categories.data, transaction.categoryId, locale)
                return (
                  <li key={transaction.id} className="py-1 first:pt-0 last:pb-0">
                    <Link
                      to={`/transactions/${transaction.id}`}
                      className="flex items-center gap-3 rounded-xl px-1 py-2 outline-none transition-colors hover:bg-muted/45 focus-visible:ring-3 focus-visible:ring-ring/30"
                      aria-label={t('dashboard.viewTransaction', { description: transaction.description })}
                    >
                      <KindIcon kind={transaction.kind} />
                      <div className="min-w-0 flex-1">
                        <p className="truncate font-semibold">{transaction.description}</p>
                        <p className="truncate text-xs text-muted-foreground">{categoryName} · {formatDate(transaction.eventDate)}</p>
                      </div>
                      <span className={`tabular-nums font-heading font-semibold ${amount.className}`}>{amount.prefix}{formatMoney(transaction.amount)}</span>
                    </Link>
                  </li>
                )
              })}
            </ul>
          </CardContent>
        </Card>
      </div>
    </section>
  )
}

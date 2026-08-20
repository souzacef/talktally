import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import {
  LOCALE_KEY,
  LocaleProvider,
  useLocale,
} from '@/app/providers/locale-provider'
import { LocaleControl } from '@/components/layout/locale-control'

function Probe() {
  const { locale, formatDate, formatMoney, formatMonthYear } = useLocale()
  return (
    <div>
      <span>{locale}</span>
      <span>{formatDate('2026-08-20')}</span>
      <span>{formatMoney(1234.56)}</span>
      <span>{formatMonthYear('2026-08-20')}</span>
    </div>
  )
}

describe('LocaleProvider', () => {
  it('persists pt-BR and formats BRL and dates with the selected locale', async () => {
    window.localStorage.setItem(LOCALE_KEY, 'en-US')
    render(
      <LocaleProvider>
        <LocaleControl />
        <Probe />
      </LocaleProvider>,
    )

    expect(screen.getByText('08/20/2026')).toBeInTheDocument()
    expect(screen.getByText(/R\$1,234\.56/)).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Português' }))

    expect(window.localStorage.getItem(LOCALE_KEY)).toBe('pt-BR')
    expect(document.documentElement.lang).toBe('pt-BR')
    expect(screen.getByText('20/08/2026')).toBeInTheDocument()
    expect(screen.getByText(/R\$\s*1\.234,56/)).toBeInTheDocument()
    expect(screen.getByText(/agosto de 2026/i)).toBeInTheDocument()
  })
})

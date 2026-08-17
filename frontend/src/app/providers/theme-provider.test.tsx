import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ThemeControl } from '@/components/layout/theme-control'
import { THEME_KEY, ThemeProvider, useTheme } from '@/app/providers/theme-provider'

function mockSystemDark(matches: boolean) {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockReturnValue({
      matches,
      media: '(prefers-color-scheme: dark)',
      onchange: null,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      addListener: vi.fn(),
      removeListener: vi.fn(),
      dispatchEvent: vi.fn(),
    }),
  })
}

function ResolvedTheme() {
  return <span>{useTheme().resolvedTheme}</span>
}

describe('ThemeProvider', () => {
  it('follows system dark and persists an explicit light override', async () => {
    mockSystemDark(true)
    render(<ThemeProvider><ThemeControl /><ResolvedTheme /></ThemeProvider>)
    await waitFor(() => expect(document.documentElement).toHaveClass('dark'))
    expect(screen.getByText('dark')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Use light theme' }))
    expect(window.localStorage.getItem(THEME_KEY)).toBe('light')
    expect(document.documentElement).not.toHaveClass('dark')
    expect(screen.getByText('light')).toBeInTheDocument()
  })

  it('restores a stored dark preference independently of a light system', async () => {
    mockSystemDark(false)
    window.localStorage.setItem(THEME_KEY, 'dark')
    render(<ThemeProvider><ResolvedTheme /></ThemeProvider>)
    await waitFor(() => expect(document.documentElement).toHaveClass('dark'))
    expect(screen.getByText('dark')).toBeInTheDocument()
  })
})

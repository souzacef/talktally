import { describe, expect, it } from 'vitest'
import { dashboardGreeting } from '@/features/auth/display-name'

describe('dashboardGreeting', () => {
  it('uses only the first name from a multi-word display name', () => {
    expect(dashboardGreeting('Carlos Eduardo Freire de Souza')).toBe('Hello, Carlos!')
  })

  it('uses a single-word display name', () => {
    expect(dashboardGreeting('Carlos')).toBe('Hello, Carlos!')
  })

  it('trims surrounding whitespace and collapses whitespace between names', () => {
    expect(dashboardGreeting('  Carlos   Eduardo  ')).toBe('Hello, Carlos!')
  })

  it.each([undefined, null, '', '   '])(
    'falls back gracefully when the display name is %s',
    (displayName) => {
      expect(dashboardGreeting(displayName)).toBe('Hello!')
    },
  )
})

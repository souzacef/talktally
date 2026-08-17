import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'

export const THEME_KEY = 'talktally.theme'
export type ThemePreference = 'system' | 'light' | 'dark'
type ResolvedTheme = Exclude<ThemePreference, 'system'>

interface ThemeContextValue {
  theme: ThemePreference
  resolvedTheme: ResolvedTheme
  setTheme: (theme: ThemePreference) => void
}

const ThemeContext = createContext<ThemeContextValue | null>(null)

function isTheme(value: string | null): value is ThemePreference {
  return value === 'system' || value === 'light' || value === 'dark'
}

function systemTheme(): ResolvedTheme {
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<ThemePreference>(() => {
    const stored = window.localStorage.getItem(THEME_KEY)
    return isTheme(stored) ? stored : 'system'
  })
  const [system, setSystem] = useState<ResolvedTheme>(systemTheme)
  const resolvedTheme = theme === 'system' ? system : theme

  useEffect(() => {
    const media = window.matchMedia?.('(prefers-color-scheme: dark)')
    if (!media) return
    const update = () => setSystem(media.matches ? 'dark' : 'light')
    media.addEventListener('change', update)
    return () => media.removeEventListener('change', update)
  }, [])

  useEffect(() => {
    document.documentElement.classList.toggle('dark', resolvedTheme === 'dark')
    document.documentElement.style.colorScheme = resolvedTheme
  }, [resolvedTheme])

  const value = useMemo<ThemeContextValue>(() => ({
    theme,
    resolvedTheme,
    setTheme: (nextTheme) => {
      window.localStorage.setItem(THEME_KEY, nextTheme)
      setThemeState(nextTheme)
    },
  }), [resolvedTheme, theme])

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme(): ThemeContextValue {
  const context = useContext(ThemeContext)
  if (!context) throw new Error('useTheme must be used within ThemeProvider')
  return context
}

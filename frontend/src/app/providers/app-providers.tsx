import type { ReactNode } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from '@/features/auth/auth-provider'
import { BackendHealthProvider } from '@/features/health/backend-health-provider'
import { queryClient } from '@/lib/query/query-client'
import { LocaleProvider } from '@/app/providers/locale-provider'
import { ThemeProvider } from '@/app/providers/theme-provider'

export function AppProviders({ children }: { children: ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      <LocaleProvider>
        <ThemeProvider>
          <BackendHealthProvider>
            <AuthProvider>{children}</AuthProvider>
          </BackendHealthProvider>
        </ThemeProvider>
      </LocaleProvider>
    </QueryClientProvider>
  )
}

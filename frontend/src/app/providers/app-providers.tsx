import type { ReactNode } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from '@/features/auth/auth-provider'
import { queryClient } from '@/lib/query/query-client'
import { ThemeProvider } from '@/app/providers/theme-provider'

export function AppProviders({ children }: { children: ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <AuthProvider>{children}</AuthProvider>
      </ThemeProvider>
    </QueryClientProvider>
  )
}

import { useQuery } from '@tanstack/react-query'
import { useLocale } from '@/app/providers/locale-provider'
import { Brand } from '@/components/brand/brand'
import { LocaleControl } from '@/components/layout/locale-control'
import { ThemeControl } from '@/components/layout/theme-control'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { authText } from '@/features/auth/auth-messages'
import { backendHealthApi } from '@/features/health/backend-health-api'

export function BackendStatusPage() {
  const { locale } = useLocale()
  const text = (key: Parameters<typeof authText>[1]) => authText(locale, key)
  const query = useQuery({
    queryKey: ['backend-health'],
    queryFn: ({ signal }) => backendHealthApi.isUp(signal),
    retry: false,
  })

  const message = query.isPending
    ? text('backendChecking')
    : query.data ? text('backendUp') : text('backendUnavailable')

  return (
    <main className="relative grid min-h-svh place-items-center bg-background px-4 py-14">
      <div className="absolute right-4 top-4 flex items-center gap-2 sm:right-6 sm:top-6"><LocaleControl /><ThemeControl /></div>
      <div className="w-full max-w-sm">
        <div className="mb-8 flex justify-center"><Brand /></div>
        <Card className="shadow-[var(--shadow-lift)]">
          <CardHeader className="text-center"><CardTitle>{text('backendHealth')}</CardTitle></CardHeader>
          <CardContent className="space-y-5 text-center">
            <p aria-live="polite">{message}</p>
            {!query.isPending && !query.data && (
              <Button type="button" variant="outline" onClick={() => void query.refetch()}>
                {text('retryBackend')}
              </Button>
            )}
          </CardContent>
        </Card>
      </div>
    </main>
  )
}

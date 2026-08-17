import { useState, type FormEvent } from 'react'
import { ArrowRight } from 'lucide-react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { Brand, Tagline } from '@/components/brand/brand'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ThemeControl } from '@/components/layout/theme-control'
import { useAuth } from '@/features/auth/auth-provider'
import { ApiError } from '@/lib/api/api-client'

interface LoginLocationState {
  from?: string
  registrationComplete?: boolean
}

function safeDestination(value: string | undefined): string {
  return value?.startsWith('/') && !value.startsWith('//') ? value : '/dashboard'
}

export function LoginPage() {
  const { isAuthenticated, signIn } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const state = (location.state ?? {}) as LoginLocationState
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setSubmitting] = useState(false)

  if (isAuthenticated) return <Navigate to="/dashboard" replace />

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await signIn({ email, password })
      navigate(safeDestination(state.from), { replace: true })
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to sign in')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="relative grid min-h-svh place-items-center overflow-hidden bg-background px-4 py-14">
      <div className="pointer-events-none absolute -left-24 top-1/4 size-72 rounded-full bg-accent/55 blur-3xl" aria-hidden="true" />
      <div className="absolute right-4 top-4 sm:right-6 sm:top-6"><ThemeControl /></div>
      <div className="relative w-full max-w-sm">
        <div className="mb-8 flex flex-col items-center text-center">
          <Brand />
          <Tagline className="mt-3" />
        </div>
        <Card className="shadow-[var(--shadow-lift)]">
          <CardHeader className="text-center">
            <CardTitle className="font-heading text-2xl font-semibold tracking-[-0.035em]">Welcome back</CardTitle>
            <CardDescription>Pick up where your money left off.</CardDescription>
          </CardHeader>
          <CardContent>
            {state.registrationComplete && (
              <Alert className="mb-5 border-income/20 bg-income-soft text-income">
                <AlertDescription className="text-current">Registration complete. You can sign in now.</AlertDescription>
              </Alert>
            )}
            {error && (
              <Alert variant="destructive" className="mb-5" aria-live="polite">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
            <form className="space-y-4" onSubmit={submit}>
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  name="email"
                  type="email"
                  autoComplete="email"
                  required
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  placeholder="you@example.com"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="password">Password</Label>
                <Input
                  id="password"
                  name="password"
                  type="password"
                  autoComplete="current-password"
                  required
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                />
              </div>
              <Button className="mt-2 w-full" size="lg" type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Signing in…' : 'Sign in'}
                {!isSubmitting && <ArrowRight aria-hidden="true" />}
              </Button>
            </form>
            <p className="mt-6 text-center text-sm text-muted-foreground">
              New to TalkTally? <Link className="font-semibold text-primary underline-offset-4 hover:underline" to="/register">Create an account</Link>
            </p>
          </CardContent>
        </Card>
      </div>
    </main>
  )
}

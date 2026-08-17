import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
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
    <main className="grid min-h-svh place-items-center bg-muted/30 p-4">
      <div className="absolute right-4 top-4"><ThemeControl /></div>
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Sign in to TalkTally</CardTitle>
          <CardDescription>Use your TalkTally account to continue.</CardDescription>
        </CardHeader>
        <CardContent>
          {state.registrationComplete && (
            <Alert className="mb-4">
              <AlertDescription>Registration complete. You can sign in now.</AlertDescription>
            </Alert>
          )}
          {error && (
            <Alert variant="destructive" className="mb-4" aria-live="polite">
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
            <Button className="w-full" type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Signing in…' : 'Sign in'}
            </Button>
          </form>
          <p className="mt-4 text-center text-sm text-muted-foreground">
            New to TalkTally? <Link className="underline" to="/register">Create an account</Link>
          </p>
        </CardContent>
      </Card>
    </main>
  )
}

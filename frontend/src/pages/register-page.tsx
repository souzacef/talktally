import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ThemeControl } from '@/components/layout/theme-control'
import { useAuth } from '@/features/auth/auth-provider'
import { ApiError } from '@/lib/api/api-client'

function validatePassword(password: string): string | null {
  if (password.length < 10 || password.length > 128 || !/\p{L}/u.test(password) || !/\d/.test(password)) {
    return 'Password must contain 10 to 128 characters, including a letter and a digit.'
  }
  return null
}

export function RegisterPage() {
  const { isAuthenticated, register } = useAuth()
  const navigate = useNavigate()
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setSubmitting] = useState(false)

  if (isAuthenticated) return <Navigate to="/dashboard" replace />

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const passwordError = validatePassword(password)
    if (passwordError) {
      setError(passwordError)
      return
    }
    setError(null)
    setSubmitting(true)
    try {
      await register({ displayName, email, password })
      navigate('/login', { replace: true, state: { registrationComplete: true } })
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to register')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="grid min-h-svh place-items-center bg-muted/30 p-4">
      <div className="absolute right-4 top-4"><ThemeControl /></div>
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Create your account</CardTitle>
          <CardDescription>TalkTally currently supports BRL accounts.</CardDescription>
        </CardHeader>
        <CardContent>
          {error && (
            <Alert variant="destructive" className="mb-4" aria-live="polite">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
          <form className="space-y-4" onSubmit={submit}>
            <div className="space-y-2">
              <Label htmlFor="display-name">Display name</Label>
              <Input
                id="display-name"
                autoComplete="name"
                required
                maxLength={120}
                value={displayName}
                onChange={(event) => setDisplayName(event.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                required
                maxLength={320}
                value={email}
                onChange={(event) => setEmail(event.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                autoComplete="new-password"
                required
                minLength={10}
                maxLength={128}
                aria-describedby="password-help"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
              <p id="password-help" className="text-xs text-muted-foreground">
                10–128 characters with at least one letter and one digit.
              </p>
            </div>
            <Button className="w-full" type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Creating account…' : 'Create account'}
            </Button>
          </form>
          <p className="mt-4 text-center text-sm text-muted-foreground">
            Already registered? <Link className="underline" to="/login">Sign in</Link>
          </p>
        </CardContent>
      </Card>
    </main>
  )
}

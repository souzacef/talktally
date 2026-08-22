import { useState, type FormEvent } from 'react'
import { ArrowRight } from 'lucide-react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useLocale } from '@/app/providers/locale-provider'
import { Brand, Tagline } from '@/components/brand/brand'
import { AuthFooter } from '@/components/layout/auth-footer'
import { LocaleControl } from '@/components/layout/locale-control'
import { ThemeControl } from '@/components/layout/theme-control'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { authText } from '@/features/auth/auth-messages'
import { useAuth } from '@/features/auth/auth-provider'
import { PasswordField } from '@/features/auth/components/password-field'
import { ApiError } from '@/lib/api/api-client'

function validatePassword(password: string, message: string): string | null {
  if (password.length < 10 || password.length > 128 || !/\p{L}/u.test(password) || !/\d/.test(password)) {
    return message
  }
  return null
}

export function RegisterPage() {
  const { locale } = useLocale()
  const text = (key: Parameters<typeof authText>[1]) => authText(locale, key)
  const { isAuthenticated, register } = useAuth()
  const navigate = useNavigate()
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setSubmitting] = useState(false)

  if (isAuthenticated) return <Navigate to="/dashboard" replace />

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const passwordError = validatePassword(password, text('passwordInvalid'))
    if (passwordError) {
      setError(passwordError)
      return
    }
    if (password !== confirmPassword) {
      setError(text('passwordsDoNotMatch'))
      return
    }
    setError(null)
    setSubmitting(true)
    try {
      await register({ displayName, email, password })
      navigate('/login', { replace: true, state: { registrationComplete: true } })
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : text('unableToRegister'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="relative grid min-h-svh place-items-center overflow-hidden bg-background px-4 py-14">
      <div className="pointer-events-none absolute -right-24 top-1/3 size-72 rounded-full bg-accent/55 blur-3xl" aria-hidden="true" />
      <div className="absolute right-4 top-4 flex items-center gap-2 sm:right-6 sm:top-6"><LocaleControl /><ThemeControl /></div>
      <div className="relative w-full max-w-sm">
        <div className="mb-8 flex flex-col items-center text-center">
          <Brand />
          <Tagline className="mt-3" />
        </div>
        <Card className="shadow-[var(--shadow-lift)]">
          <CardHeader className="text-center">
            <CardTitle className="font-heading text-2xl font-semibold tracking-[-0.035em]">{text('createYourAccount')}</CardTitle>
            <CardDescription>{text('registerSubtitle')}</CardDescription>
          </CardHeader>
          <CardContent>
            {error && (
              <Alert variant="destructive" className="mb-5" aria-live="polite">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
            <form className="space-y-4" onSubmit={submit}>
              <div className="space-y-2">
                <Label htmlFor="display-name">{text('displayName')}</Label>
                <Input id="display-name" autoComplete="name" required maxLength={120} value={displayName} onChange={(event) => setDisplayName(event.target.value)} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">{text('email')}</Label>
                <Input id="email" type="email" autoComplete="email" required maxLength={320} value={email} onChange={(event) => setEmail(event.target.value)} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="password">{text('password')}</Label>
                <PasswordField id="password" autoComplete="new-password" value={password} onChange={setPassword} describedBy="password-help" showLabel={text('showPassword')} hideLabel={text('hidePassword')} />
                <p id="password-help" className="text-xs leading-relaxed text-muted-foreground">{text('passwordHelp')}</p>
              </div>
              <div className="space-y-2">
                <Label htmlFor="confirm-password">{text('confirmPassword')}</Label>
                <PasswordField id="confirm-password" autoComplete="new-password" value={confirmPassword} onChange={setConfirmPassword} showLabel={text('showConfirmPassword')} hideLabel={text('hideConfirmPassword')} />
              </div>
              <Button className="mt-2 w-full" size="lg" type="submit" disabled={isSubmitting}>
                {isSubmitting ? text('creatingAccount') : text('createAccount')}
                {!isSubmitting && <ArrowRight aria-hidden="true" />}
              </Button>
            </form>
            <p className="mt-6 text-center text-sm text-muted-foreground">
              {text('alreadyRegistered')} <Link className="font-semibold text-primary underline-offset-4 hover:underline" to="/login">{text('signIn')}</Link>
            </p>
          </CardContent>
        </Card>
        <AuthFooter />
      </div>
    </main>
  )
}

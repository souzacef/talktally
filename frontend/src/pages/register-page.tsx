import { useState, type FormEvent } from 'react'
import { ArrowRight, Check, Circle, X } from 'lucide-react'
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

const EMAIL_LOCAL_CHARACTERS = /^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+$/i
const EMAIL_DOMAIN_LABEL = /^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$/i

function isDisplayNameValid(value: string): boolean {
  const normalized = value.trim()
  return normalized.length >= 1 && normalized.length <= 120
}

function isEmailValid(value: string): boolean {
  const normalized = value.trim().toLowerCase()
  if (!normalized || normalized.length > 320 || /\s/.test(normalized)) return false
  const parts = normalized.split('@')
  if (parts.length !== 2) return false
  const [localPart = '', domain = ''] = parts
  if (!localPart || localPart.length > 64 || !EMAIL_LOCAL_CHARACTERS.test(localPart)) return false
  if (localPart.startsWith('.') || localPart.endsWith('.') || localPart.includes('..')) return false
  const labels = domain.split('.')
  return labels.length >= 2 && labels.every((label) => EMAIL_DOMAIN_LABEL.test(label))
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
  const [submitAttempted, setSubmitAttempted] = useState(false)

  const passwordChecks = {
    length: password.length >= 10 && password.length <= 128,
    letter: /\p{L}/u.test(password),
    number: /\p{Nd}/u.test(password),
  }
  const passwordValid = Object.values(passwordChecks).every(Boolean)
  const displayNameValid = isDisplayNameValid(displayName)
  const emailValid = isEmailValid(email)
  const confirmationStarted = confirmPassword.length > 0
  const passwordsMatch = confirmationStarted && password === confirmPassword
  const formValid = displayNameValid && emailValid && passwordValid && passwordsMatch

  if (isAuthenticated) return <Navigate to="/dashboard" replace />

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitAttempted(true)
    if (!formValid) {
      setError(text('registrationInvalid'))
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
            <form className="space-y-4" noValidate onSubmit={submit}>
              <div className="space-y-2">
                <Label htmlFor="display-name">{text('displayName')}</Label>
                <Input id="display-name" autoComplete="name" required maxLength={120} aria-invalid={(submitAttempted || displayName.length > 0) && !displayNameValid} aria-describedby={!displayNameValid && submitAttempted ? 'display-name-error' : undefined} value={displayName} onChange={(event) => setDisplayName(event.target.value)} />
                {!displayNameValid && submitAttempted && <p id="display-name-error" className="text-xs text-destructive">{text('nameInvalid')}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">{text('email')}</Label>
                <Input id="email" type="email" autoComplete="email" required maxLength={320} aria-invalid={(submitAttempted || email.length > 0) && !emailValid} aria-describedby={!emailValid && (submitAttempted || email.length > 0) ? 'email-error' : undefined} value={email} onChange={(event) => setEmail(event.target.value)} />
                {!emailValid && (submitAttempted || email.length > 0) && <p id="email-error" className="text-xs text-destructive">{text('emailInvalid')}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="password">{text('password')}</Label>
                <PasswordField id="password" autoComplete="new-password" value={password} onChange={setPassword} describedBy="password-requirements" invalid={password.length > 0 && !passwordValid} showLabel={text('showPassword')} hideLabel={text('hidePassword')} />
                <div id="password-requirements" className="space-y-1.5 text-xs" aria-label={text('passwordRequirements')}>
                  <p className="font-medium text-foreground">{text('passwordRequirements')}</p>
                  <ul className="space-y-1">
                    {([
                      ['length', 'passwordLengthRequirement'],
                      ['letter', 'passwordLetterRequirement'],
                      ['number', 'passwordNumberRequirement'],
                    ] as const).map(([rule, key]) => {
                      const state = password.length === 0 ? 'neutral' : passwordChecks[rule] ? 'met' : 'unmet'
                      const Icon = state === 'met' ? Check : state === 'unmet' ? X : Circle
                      return (
                        <li key={rule} data-state={state} className={state === 'met' ? 'flex items-center gap-2 text-income' : state === 'unmet' ? 'flex items-center gap-2 text-destructive' : 'flex items-center gap-2 text-muted-foreground'}>
                          <Icon className="size-3.5 shrink-0" aria-hidden="true" />
                          <span>
                            {state !== 'neutral' && <span className="sr-only">{text(state === 'met' ? 'requirementMet' : 'requirementNotMet')}: </span>}
                            {text(key)}
                          </span>
                        </li>
                      )
                    })}
                  </ul>
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="confirm-password">{text('confirmPassword')}</Label>
                <PasswordField id="confirm-password" autoComplete="new-password" value={confirmPassword} onChange={setConfirmPassword} describedBy={confirmationStarted ? 'password-confirmation-feedback' : undefined} invalid={confirmationStarted && !passwordsMatch} showLabel={text('showConfirmPassword')} hideLabel={text('hideConfirmPassword')} />
                {confirmationStarted && (
                  <p id="password-confirmation-feedback" className={passwordsMatch ? 'flex items-center gap-2 text-xs text-income' : 'flex items-center gap-2 text-xs text-destructive'} aria-live="polite">
                    {passwordsMatch ? <Check className="size-3.5" aria-hidden="true" /> : <X className="size-3.5" aria-hidden="true" />}
                    {text(passwordsMatch ? 'passwordsMatch' : 'passwordsDoNotMatch')}
                  </p>
                )}
              </div>
              <Button className="mt-2 w-full" size="lg" type="submit" disabled={!formValid || isSubmitting}>
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

import { useLocale } from '@/app/providers/locale-provider'
import { authText } from '@/features/auth/auth-messages'

const GITHUB_PROFILE_URL = 'https://github.com/souzacef'
const linkClassName = 'font-medium text-foreground/80 underline-offset-4 transition-colors hover:text-primary hover:underline focus-visible:rounded-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'

export function AuthFooter() {
  const { locale } = useLocale()
  const text = (key: Parameters<typeof authText>[1]) => authText(locale, key)

  return (
    <footer className="mt-6 px-2 text-center text-xs leading-relaxed text-muted-foreground sm:-mx-8">
      <p>
        {text('builtBy')}
        <span className="inline-block whitespace-nowrap">
          <span aria-hidden="true"> · </span>
          <a
            className={linkClassName}
            href={GITHUB_PROFILE_URL}
            target="_blank"
            rel="noopener noreferrer"
          >
            {text('github')}
          </a>
        </span>
        <span className="inline-block whitespace-nowrap">
          <span aria-hidden="true"> · </span>
          <a
            className={linkClassName}
            href="/backend-status"
            target="_blank"
            rel="noopener noreferrer"
          >
            {text('backendHealth')}
          </a>
        </span>
      </p>
    </footer>
  )
}

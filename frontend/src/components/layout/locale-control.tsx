import { Languages } from 'lucide-react'
import { useLocale, type AppLocale } from '@/app/providers/locale-provider'
import { cn } from '@/lib/utils'

const options: Array<{
  value: AppLocale
  labelKey: 'locale.english' | 'locale.portuguese'
  shortLabel: string
}> = [
  { value: 'en-US', labelKey: 'locale.english', shortLabel: 'EN' },
  { value: 'pt-BR', labelKey: 'locale.portuguese', shortLabel: 'PT-BR' },
]

export function LocaleControl({ showLabels = false, className }: { showLabels?: boolean; className?: string }) {
  const { locale, setLocale, t } = useLocale()

  return (
    <div
      className={cn('inline-flex rounded-full border bg-muted/60 p-1 shadow-xs', className)}
      role="group"
      aria-label={t('locale.preference')}
    >
      {options.map(({ value, labelKey, shortLabel }) => {
        const label = t(labelKey)
        return (
          <button
            key={value}
            type="button"
            aria-pressed={locale === value}
            aria-label={label}
            title={label}
            onClick={() => setLocale(value)}
            className={cn(
              'inline-flex h-8 items-center justify-center gap-1.5 rounded-full px-2.5 text-xs font-semibold text-muted-foreground transition-all',
              'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
              locale === value && 'bg-card text-foreground shadow-sm',
            )}
          >
            <Languages className="size-3.5" aria-hidden="true" />
            <span>{showLabels ? label : shortLabel}</span>
          </button>
        )
      })}
    </div>
  )
}

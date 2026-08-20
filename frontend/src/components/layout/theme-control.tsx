import { Moon, Monitor, Sun } from 'lucide-react'
import { useLocale } from '@/app/providers/locale-provider'
import { useTheme, type ThemePreference } from '@/app/providers/theme-provider'
import { cn } from '@/lib/utils'

const options: Array<{
  value: ThemePreference
  labelKey: 'theme.system' | 'theme.light' | 'theme.dark'
  icon: typeof Sun
}> = [
  { value: 'system', labelKey: 'theme.system', icon: Monitor },
  { value: 'light', labelKey: 'theme.light', icon: Sun },
  { value: 'dark', labelKey: 'theme.dark', icon: Moon },
]

export function ThemeControl({ showLabels = false, className }: { showLabels?: boolean; className?: string }) {
  const { theme, setTheme } = useTheme()
  const { t } = useLocale()

  return (
    <div
      className={cn('inline-flex rounded-full border bg-muted/60 p-1 shadow-xs', className)}
      role="group"
      aria-label={t('theme.preference')}
    >
      {options.map(({ value, labelKey, icon: Icon }) => {
        const label = t(labelKey)
        return (
          <button
            key={value}
            type="button"
            aria-pressed={theme === value}
            aria-label={t('theme.use', { theme: label.toLowerCase() })}
            title={t('theme.title', { theme: label })}
            onClick={() => setTheme(value)}
            className={cn(
              'inline-flex h-8 items-center justify-center rounded-full font-semibold text-muted-foreground transition-all',
              'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
              showLabels ? 'min-w-0 flex-1 gap-1 px-1.5 text-[0.7rem]' : 'gap-1.5 px-2.5 text-xs',
              theme === value && 'bg-card text-foreground shadow-sm',
            )}
          >
            <Icon className="size-3.5 shrink-0" aria-hidden="true" />
            {showLabels && <span className="whitespace-nowrap">{label}</span>}
          </button>
        )
      })}
    </div>
  )
}

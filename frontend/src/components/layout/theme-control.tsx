import { Moon, Monitor, Sun } from 'lucide-react'
import { useTheme, type ThemePreference } from '@/app/providers/theme-provider'
import { cn } from '@/lib/utils'

const options: Array<{
  value: ThemePreference
  label: string
  icon: typeof Sun
}> = [
  { value: 'system', label: 'System', icon: Monitor },
  { value: 'light', label: 'Light', icon: Sun },
  { value: 'dark', label: 'Dark', icon: Moon },
]

export function ThemeControl({ showLabels = false, className }: { showLabels?: boolean; className?: string }) {
  const { theme, setTheme } = useTheme()
  return (
    <div
      className={cn('inline-flex rounded-full border bg-muted/60 p-1 shadow-xs', className)}
      role="group"
      aria-label="Theme preference"
    >
      {options.map(({ value, label, icon: Icon }) => (
        <button
          key={value}
          type="button"
          aria-pressed={theme === value}
          aria-label={`Use ${label.toLowerCase()} theme`}
          title={`${label} theme`}
          onClick={() => setTheme(value)}
          className={cn(
            'inline-flex h-8 items-center justify-center gap-1.5 rounded-full px-2.5 text-xs font-semibold text-muted-foreground transition-all',
            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
            theme === value && 'bg-card text-foreground shadow-sm',
          )}
        >
          <Icon className="size-3.5" aria-hidden="true" />
          {showLabels && <span>{label}</span>}
        </button>
      ))}
    </div>
  )
}

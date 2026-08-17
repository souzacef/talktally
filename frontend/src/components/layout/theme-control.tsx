import { Moon, Monitor, Sun } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useTheme, type ThemePreference } from '@/app/providers/theme-provider'

const options: Array<{
  value: ThemePreference
  label: string
  icon: typeof Sun
}> = [
  { value: 'system', label: 'System', icon: Monitor },
  { value: 'light', label: 'Light', icon: Sun },
  { value: 'dark', label: 'Dark', icon: Moon },
]

export function ThemeControl() {
  const { theme, setTheme } = useTheme()
  return (
    <div className="flex gap-1" aria-label="Theme preference">
      {options.map(({ value, label, icon: Icon }) => (
        <Button
          key={value}
          type="button"
          variant={theme === value ? 'secondary' : 'ghost'}
          size="sm"
          aria-pressed={theme === value}
          aria-label={`Use ${label.toLowerCase()} theme`}
          onClick={() => setTheme(value)}
        >
          <Icon aria-hidden="true" />
          <span className="hidden sm:inline">{label}</span>
        </Button>
      ))}
    </div>
  )
}

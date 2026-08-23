import { useState } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { Input } from '@/components/ui/input'

interface PasswordFieldProps {
  id: string
  value: string
  onChange: (value: string) => void
  autoComplete: 'new-password' | 'current-password'
  showLabel: string
  hideLabel: string
  describedBy?: string
  invalid?: boolean
}

export function PasswordField({
  id,
  value,
  onChange,
  autoComplete,
  showLabel,
  hideLabel,
  describedBy,
  invalid = false,
}: PasswordFieldProps) {
  const [visible, setVisible] = useState(false)
  const Icon = visible ? EyeOff : Eye

  return (
    <div className="relative">
      <Input
        id={id}
        className="pr-12"
        type={visible ? 'text' : 'password'}
        autoComplete={autoComplete}
        required
        minLength={10}
        maxLength={128}
        aria-describedby={describedBy}
        aria-invalid={invalid}
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
      <button
        type="button"
        className="absolute right-1 top-1/2 grid size-9 -translate-y-1/2 place-items-center rounded-lg text-muted-foreground hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        aria-label={visible ? hideLabel : showLabel}
        onClick={() => setVisible((current) => !current)}
      >
        <Icon className="size-4" aria-hidden="true" />
      </button>
    </div>
  )
}

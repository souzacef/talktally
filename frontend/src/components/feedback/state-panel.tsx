import { CircleAlert, Inbox } from 'lucide-react'
import { cn } from '@/lib/utils'

interface StatePanelProps {
  title: string
  description: string
  tone?: 'empty' | 'error'
  className?: string
}

export function StatePanel({ title, description, tone = 'empty', className }: StatePanelProps) {
  const Icon = tone === 'error' ? CircleAlert : Inbox
  return (
    <div className={cn('grid min-h-36 place-items-center rounded-2xl border border-dashed bg-muted/25 p-6 text-center', className)}>
      <div className="space-y-2">
        <Icon className={cn('mx-auto size-6', tone === 'error' ? 'text-expense' : 'text-muted-foreground')} aria-hidden="true" />
        <p className="font-heading font-semibold">{title}</p>
        <p className="mx-auto max-w-sm text-sm text-muted-foreground">{description}</p>
      </div>
    </div>
  )
}

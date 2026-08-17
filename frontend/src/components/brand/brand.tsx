import { cn } from '@/lib/utils'

interface BrandProps {
  className?: string
  compact?: boolean
}

export function BrandGlyph({ className }: { className?: string }) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        'relative grid size-10 shrink-0 place-items-center rounded-[0.85rem] bg-primary text-primary-foreground shadow-[var(--shadow-soft)]',
        className,
      )}
    >
      <svg viewBox="0 0 40 40" className="size-8" fill="none">
        <path d="M11 16v8M16.5 12.5v15M22 15v10M27.5 10.5v19" stroke="currentColor" strokeWidth="2.8" strokeLinecap="round" />
        <path d="M8.5 21h22" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" opacity=".58" />
      </svg>
      <span className="absolute right-1.5 top-1.5 size-1.5 rounded-full bg-voice ring-2 ring-primary" />
    </span>
  )
}

export function Wordmark({ className }: { className?: string }) {
  return (
    <span className={cn('font-heading text-xl font-semibold tracking-[-0.035em]', className)}>
      Talk<span className="text-primary">Tally</span>
    </span>
  )
}

export function Tagline({ className }: { className?: string }) {
  return <span className={cn('text-sm text-muted-foreground', className)}>Speak it. Track it. Understand it.</span>
}

export function Brand({ className, compact = false }: BrandProps) {
  return (
    <span className={cn('inline-flex items-center gap-3', className)}>
      <BrandGlyph />
      {!compact && <Wordmark />}
    </span>
  )
}

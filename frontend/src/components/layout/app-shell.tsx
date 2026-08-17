import { useState } from 'react'
import { Bot, HandCoins, House, LogOut, ReceiptText, Settings, X } from 'lucide-react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { Brand, Tagline, Wordmark } from '@/components/brand/brand'
import { Button } from '@/components/ui/button'
import { ThemeControl } from '@/components/layout/theme-control'
import { useAuth } from '@/features/auth/auth-provider'
import { cn } from '@/lib/utils'

const navigation = [
  { to: '/dashboard', label: 'Home', icon: House },
  { to: '/transactions', label: 'Transactions', icon: ReceiptText },
  { to: '/owed', label: 'Owed to Me', icon: HandCoins },
  { to: '/assistant', label: 'Assistant', icon: Bot },
]

export function AppShell() {
  const { signOut } = useAuth()
  const navigate = useNavigate()
  const [accountOpen, setAccountOpen] = useState(false)

  function leaveSession() {
    signOut()
    navigate('/login', { replace: true })
  }

  return (
    <div className="min-h-svh bg-background text-foreground">
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 flex-col border-r border-sidebar-border bg-sidebar text-sidebar-foreground lg:flex">
        <div className="px-6 pb-8 pt-7">
          <Brand />
          <Tagline className="mt-3 block text-xs" />
        </div>
        <nav className="flex-1 space-y-1 px-3" aria-label="Desktop primary navigation">
          {navigation.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) => cn(
                'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition-colors',
                'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sidebar-ring',
                isActive
                  ? 'bg-sidebar-accent text-sidebar-accent-foreground'
                  : 'text-sidebar-foreground/70 hover:bg-sidebar-accent/60 hover:text-sidebar-foreground',
              )}
            >
              <Icon aria-hidden="true" className="size-4.5" />
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="space-y-5 border-t border-sidebar-border p-4">
          <div>
            <p className="mb-2 px-1 text-[0.68rem] font-bold tracking-[0.12em] text-muted-foreground uppercase">Appearance</p>
            <ThemeControl showLabels className="w-full justify-between" />
          </div>
          <div className="rounded-2xl border border-sidebar-border bg-card/60 p-3">
            <div className="mb-3 flex items-center gap-2 text-sm font-semibold">
              <span className="grid size-8 place-items-center rounded-full bg-accent text-primary"><Settings className="size-4" aria-hidden="true" /></span>
              <span>Account</span>
            </div>
            <Button variant="ghost" className="w-full justify-start text-muted-foreground" onClick={leaveSession}>
              <LogOut aria-hidden="true" /> Sign out
            </Button>
          </div>
        </div>
      </aside>

      <div className="min-w-0 lg:pl-64">
        <header className="sticky top-0 z-20 border-b bg-background/92 backdrop-blur-md">
          <div className="mx-auto flex h-16 max-w-6xl items-center gap-3 px-4 sm:px-6 lg:px-8">
            <NavLink to="/dashboard" className="flex items-center gap-2 lg:hidden" aria-label="TalkTally home">
              <Brand compact />
              <Wordmark className="text-lg" />
            </NavLink>
            <div className="hidden lg:block">
              <p className="text-xs font-semibold tracking-[0.12em] text-muted-foreground uppercase">Financial workspace</p>
            </div>
            <span className="ml-auto inline-flex items-center gap-2 text-xs text-muted-foreground">
              <span className="size-2 rounded-full bg-voice shadow-[0_0_0_4px_var(--voice-soft)]" aria-hidden="true" />
              <span className="hidden sm:inline">Secure session</span>
            </span>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="lg:hidden"
              aria-label="Open account settings"
              aria-expanded={accountOpen}
              onClick={() => setAccountOpen(true)}
            >
              <Settings aria-hidden="true" />
            </Button>
          </div>
        </header>

        <main className="mx-auto min-w-0 max-w-6xl px-4 pb-[calc(7rem+env(safe-area-inset-bottom))] pt-6 sm:px-6 sm:pt-8 lg:px-8 lg:pb-12">
          <Outlet />
        </main>
      </div>

      <nav
        className="fixed inset-x-0 bottom-0 z-40 border-t bg-card/95 px-2 pb-[max(0.5rem,env(safe-area-inset-bottom))] pt-2 shadow-[0_-12px_30px_-22px_oklch(0_0_0/0.35)] backdrop-blur-md lg:hidden"
        aria-label="Mobile primary navigation"
      >
        <div className="mx-auto grid max-w-lg grid-cols-4 gap-1">
          {navigation.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) => cn(
                'group flex min-w-0 flex-col items-center gap-1 rounded-xl px-1 py-1.5 text-[0.65rem] font-semibold text-muted-foreground',
                'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
                isActive && 'text-primary',
              )}
            >
              {({ isActive }) => (
                <>
                  <span className={cn('grid size-8 place-items-center rounded-xl', isActive && 'bg-accent')}>
                    <Icon className="size-4.5" aria-hidden="true" />
                  </span>
                  <span className="truncate">{label}</span>
                </>
              )}
            </NavLink>
          ))}
        </div>
      </nav>

      {accountOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button className="absolute inset-0 bg-foreground/20 backdrop-blur-sm" aria-label="Close account settings" onClick={() => setAccountOpen(false)} />
          <section
            role="dialog"
            aria-modal="true"
            aria-labelledby="account-sheet-title"
            className="absolute inset-x-3 bottom-[calc(5.5rem+env(safe-area-inset-bottom))] rounded-3xl border bg-card p-5 shadow-[var(--shadow-lift)]"
          >
            <div className="mb-5 flex items-center justify-between">
              <div>
                <h2 id="account-sheet-title" className="font-heading text-lg font-semibold">Account</h2>
                <p className="text-sm text-muted-foreground">Appearance and session</p>
              </div>
              <Button type="button" variant="ghost" size="icon" aria-label="Close account settings" onClick={() => setAccountOpen(false)}>
                <X aria-hidden="true" />
              </Button>
            </div>
            <p className="mb-2 text-xs font-bold tracking-[0.12em] text-muted-foreground uppercase">Appearance</p>
            <ThemeControl showLabels className="mb-5 w-full justify-between" />
            <Button variant="outline" className="w-full justify-center" onClick={leaveSession}>
              <LogOut aria-hidden="true" /> Sign out
            </Button>
          </section>
        </div>
      )}
    </div>
  )
}

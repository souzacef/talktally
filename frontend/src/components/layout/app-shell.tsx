import { Bot, HandCoins, House, LogOut, ReceiptText } from 'lucide-react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
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

  return (
    <div className="min-h-svh bg-background text-foreground">
      <header className="border-b bg-card">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-3 px-4 py-3">
          <NavLink to="/dashboard" className="mr-auto text-lg font-semibold">
            TalkTally
          </NavLink>
          <ThemeControl />
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              signOut()
              navigate('/login', { replace: true })
            }}
          >
            <LogOut aria-hidden="true" />
            Sign out
          </Button>
        </div>
        <nav className="mx-auto flex max-w-6xl gap-1 overflow-x-auto px-4 pb-3" aria-label="Primary">
          {navigation.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) => cn(
                'inline-flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium',
                'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
                isActive ? 'bg-primary text-primary-foreground' : 'hover:bg-muted',
              )}
            >
              <Icon aria-hidden="true" className="size-4" />
              {label}
            </NavLink>
          ))}
        </nav>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}

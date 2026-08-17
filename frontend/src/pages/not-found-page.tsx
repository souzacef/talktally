import { Link } from 'react-router-dom'
import { buttonVariants } from '@/components/ui/button'
import { useAuth } from '@/features/auth/auth-provider'

export function NotFoundPage() {
  const { isAuthenticated } = useAuth()
  return (
    <main className="grid min-h-svh place-items-center p-6 text-center">
      <div className="space-y-4">
        <p className="text-sm font-medium text-muted-foreground">404</p>
        <h1 className="text-3xl font-semibold">Page not found</h1>
        <Link className={buttonVariants()} to={isAuthenticated ? '/dashboard' : '/login'}>Back to TalkTally</Link>
      </div>
    </main>
  )
}

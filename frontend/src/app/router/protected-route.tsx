import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/features/auth/auth-provider'

export function ProtectedRoute() {
  const { isAuthenticated, lastSessionChange } = useAuth()
  const location = useLocation()
  if (!isAuthenticated) {
    return (
      <Navigate
        to="/login"
        replace
        state={lastSessionChange === 'signed-out'
          ? undefined
          : { from: `${location.pathname}${location.search}` }}
      />
    )
  }
  return <Outlet />
}

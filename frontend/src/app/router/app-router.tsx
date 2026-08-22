import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from '@/components/layout/app-shell'
import { ProtectedRoute } from '@/app/router/protected-route'
import { useAuth } from '@/features/auth/auth-provider'
import { AssistantPage } from '@/pages/assistant-page'
import { BackendStatusPage } from '@/pages/backend-status-page'
import { DashboardPage } from '@/pages/dashboard-page'
import { LoginPage } from '@/pages/login-page'
import { NotFoundPage } from '@/pages/not-found-page'
import { OwedPage } from '@/pages/owed-page'
import { RegisterPage } from '@/pages/register-page'
import { TransactionsPage } from '@/pages/transactions-page'
import { TransactionDetailPage } from '@/pages/transaction-detail-page'

function RootRedirect() {
  const { isAuthenticated } = useAuth()
  return <Navigate to={isAuthenticated ? '/dashboard' : '/login'} replace />
}

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<RootRedirect />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/backend-status" element={<BackendStatusPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppShell />}>
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/transactions" element={<TransactionsPage />} />
            <Route path="/transactions/:transactionId" element={<TransactionDetailPage />} />
            <Route path="/owed" element={<OwedPage />} />
            <Route path="/assistant" element={<AssistantPage />} />
          </Route>
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  )
}

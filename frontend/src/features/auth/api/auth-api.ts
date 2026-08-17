import { apiClient } from '@/lib/api/api-client'
import type {
  AuthenticationRequest,
  AuthenticationResponse,
  RegistrationRequest,
  UserAccountResponse,
} from '@/types/api'

export interface AuthService {
  signIn(request: AuthenticationRequest, signal?: AbortSignal): Promise<AuthenticationResponse>
  register(request: RegistrationRequest, signal?: AbortSignal): Promise<UserAccountResponse>
}

export const authApi: AuthService = {
  signIn: (request, signal) =>
    apiClient.post<AuthenticationResponse>('/api/v1/auth/sessions', request, signal),
  register: (request, signal) =>
    apiClient.post<UserAccountResponse>('/api/v1/auth/registrations', request, signal),
}

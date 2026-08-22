export type UUID = string
export type IsoDate = string
export type IsoInstant = string

// Spring serializes BigDecimal values as JSON numbers. Inputs remain decimal
// strings until serialization, and values are converted to Number only for display.
export type DecimalValue = number | string
export type DecimalInput = string

export type TransactionKind = 'EXPENSE' | 'INCOME' | 'REIMBURSEMENT_RECEIPT'
export type UserManagedTransactionKind = Exclude<TransactionKind, 'REIMBURSEMENT_RECEIPT'>
export type CategoryAllowedKind = TransactionKind | 'ANY'
export type TransactionSource = 'MANUAL' | 'ASSISTANT_TEXT' | 'VOICE'
export type ReimbursementStatus = 'PENDING' | 'PARTIALLY_PAID' | 'PAID'
export type AssistantStatus = 'COMPLETED' | 'NEEDS_CLARIFICATION'
export type SpeechStatus = 'GENERATED' | 'UNAVAILABLE'

export interface ApiErrorPayload {
  code?: string
  message?: string
  type?: string
  title?: string
  detail?: string
  path?: string
  errors?: Record<string, unknown>
}

export interface UserAccountResponse {
  userId: UUID
  email: string
  displayName: string
  defaultCurrency: 'BRL'
}

export interface AuthenticationRequest {
  email: string
  password: string
}

export interface RegistrationRequest extends AuthenticationRequest {
  displayName: string
}

export interface AuthenticationResponse {
  accessToken: string
  tokenType: 'Bearer' | string
  expiresIn: number
  expiresAt: IsoInstant
  user: UserAccountResponse
}

export interface TransactionRequest {
  kind: UserManagedTransactionKind
  description: string
  amount: DecimalInput
  categoryId: UUID
  eventDate: IsoDate
  firstOccurrenceDate?: IsoDate | null
  installmentCount: number
}

export interface Category {
  id: UUID
  code: string
  displayName: string
  allowedKind: CategoryAllowedKind
  builtIn: boolean
}

export interface TransactionOccurrenceResponse {
  sequenceNumber: number
  effectiveDate: IsoDate
  amount: DecimalValue
  currency: 'BRL'
}

export interface TransactionResponse {
  id: UUID
  kind: TransactionKind
  description: string
  amount: DecimalValue
  currency: 'BRL'
  categoryId: UUID
  eventDate: IsoDate
  firstOccurrenceDate: IsoDate
  source: TransactionSource
  installmentCount: number
  managedByReimbursement: boolean
  createdAt: IsoInstant
  updatedAt: IsoInstant
  occurrences: TransactionOccurrenceResponse[]
}

export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface TransactionListParams {
  kind?: TransactionKind
  categoryId?: UUID
  from?: IsoDate
  to?: IsoDate
  search?: string
  page?: number
  size?: number
}

export interface PersonRequest {
  displayName: string
}

export interface PersonResponse {
  id: UUID
  displayName: string
}

export interface PersonReimbursementSummaryResponse {
  personId: UUID
  displayName: string
  totalOriginal: DecimalValue
  totalReimbursed: DecimalValue
  totalOutstanding: DecimalValue
  currency: 'BRL'
  openClaimCount: number
}

export interface CreateReimbursementRequest {
  description: string
  amount: DecimalInput
  categoryId: UUID
  eventDate: IsoDate
  firstOccurrenceDate?: IsoDate | null
  installmentCount: number
  personId: UUID
  amountOwed?: DecimalInput | null
  note?: string | null
}

export interface ReimbursementPaymentRequest {
  amount: DecimalInput
  receivedDate: IsoDate
  note?: string | null
}

export interface ReimbursementPaymentResponse {
  id: UUID
  amount: DecimalValue
  currency: 'BRL'
  receivedDate: IsoDate
  receiptTransactionId: UUID
  note: string | null
}

export interface ReimbursementSourceExpenseResponse {
  transactionId: UUID
  description: string
  amount: DecimalValue
  currency: 'BRL'
  categoryId: UUID
  eventDate: IsoDate
  firstOccurrenceDate: IsoDate
  installmentCount: number
}

export interface ReimbursementClaimResponse {
  id: UUID
  expenseTransactionId: UUID
  sourceExpense: ReimbursementSourceExpenseResponse
  personId: UUID
  personDisplayName: string
  originalAmount: DecimalValue
  amountReimbursed: DecimalValue
  remainingAmount: DecimalValue
  currency: 'BRL'
  status: ReimbursementStatus
  note: string | null
  payments: ReimbursementPaymentResponse[]
}

export interface CreateReimbursementResponse {
  expense: TransactionResponse
  claim: ReimbursementClaimResponse
}

export interface RecordReimbursementPaymentResponse {
  paymentId: UUID
  receiptTransactionId: UUID
  claim: ReimbursementClaimResponse
}

export interface ReimbursementListParams {
  personId?: UUID
  status?: ReimbursementStatus
  page?: number
  size?: number
}

export interface FinancialSummaryResponse {
  from: IsoDate
  to: IsoDate
  currency: 'BRL'
  period: {
    earnedIncome: DecimalValue
    expenses: DecimalValue
    reimbursementsReceived: DecimalValue
    netCashFlow: DecimalValue
    occurrenceCount: number
    transactionCount: number
  }
  owedToMe: {
    outstanding: DecimalValue
    openClaims: number
  }
}

export interface CategoryBreakdownResponse {
  from: IsoDate
  to: IsoDate
  kind: TransactionKind
  currency: 'BRL'
  total: DecimalValue
  categories: Array<{
    categoryId: UUID
    code: string
    displayName: string
    total: DecimalValue
    percentage: DecimalValue
    occurrenceCount: number
    transactionCount: number
  }>
}

export interface MonthlyCashFlowResponse {
  from: IsoDate
  to: IsoDate
  currency: 'BRL'
  buckets: Array<{
    year: number
    month: number
    earnedIncome: DecimalValue
    expenses: DecimalValue
    reimbursementsReceived: DecimalValue
    netCashFlow: DecimalValue
  }>
}

export interface AssistantMessageRequest {
  message: string
}

export interface AssistantMessageResponse {
  message: string
  status: AssistantStatus
}

export interface VoiceAssistantResponse {
  transcript: string
  message: string
  status: AssistantStatus
  speechStatus: SpeechStatus
  audio: {
    contentType: string
    base64: string
  } | null
}

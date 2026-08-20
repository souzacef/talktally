import type { AppLocale } from '@/app/providers/locale-provider'
import type { Category, UserManagedTransactionKind } from '@/types/api'

export const UNKNOWN_CATEGORY_LABEL = 'Unknown category'

const UNKNOWN_CATEGORY_LABELS: Record<AppLocale, string> = {
  'en-US': UNKNOWN_CATEGORY_LABEL,
  'pt-BR': 'Categoria desconhecida',
}

const BUILT_IN_LABELS: Record<string, Record<AppLocale, string>> = {
  SALARY: { 'en-US': 'Salary', 'pt-BR': 'Salário' },
  FREELANCE: { 'en-US': 'Freelance', 'pt-BR': 'Freelance' },
  FOOD_DINING: { 'en-US': 'Food and dining', 'pt-BR': 'Alimentação' },
  GROCERIES: { 'en-US': 'Groceries', 'pt-BR': 'Supermercado' },
  HOUSING: { 'en-US': 'Housing', 'pt-BR': 'Moradia' },
  UTILITIES: { 'en-US': 'Utilities', 'pt-BR': 'Contas e serviços' },
  TRANSPORT: { 'en-US': 'Transport', 'pt-BR': 'Transporte' },
  HEALTH: { 'en-US': 'Health', 'pt-BR': 'Saúde' },
  EDUCATION: { 'en-US': 'Education', 'pt-BR': 'Educação' },
  ENTERTAINMENT: { 'en-US': 'Entertainment', 'pt-BR': 'Entretenimento' },
  SHOPPING: { 'en-US': 'Shopping', 'pt-BR': 'Compras' },
  TRAVEL: { 'en-US': 'Travel', 'pt-BR': 'Viagens' },
  TAXES_FEES: { 'en-US': 'Taxes and fees', 'pt-BR': 'Impostos e taxas' },
  REIMBURSEMENT: { 'en-US': 'Reimbursement', 'pt-BR': 'Reembolso' },
  OTHER: { 'en-US': 'Other', 'pt-BR': 'Outros' },
}

export function categoryLabelForCode(code: string, displayName: string, locale: AppLocale = 'en-US'): string {
  return BUILT_IN_LABELS[code]?.[locale] ?? displayName
}

export function categoryLabel(category: Category, locale: AppLocale = 'en-US'): string {
  // Only known built-in codes are translated. Custom categories deliberately
  // preserve their owner-defined displayName regardless of the active locale.
  return category.builtIn
    ? categoryLabelForCode(category.code, category.displayName, locale)
    : category.displayName
}

export function categoryLabelForId(
  categories: readonly Category[] | undefined,
  categoryId: string,
  locale: AppLocale = 'en-US',
): string {
  const category = categories?.find((candidate) => candidate.id === categoryId)
  return category ? categoryLabel(category, locale) : UNKNOWN_CATEGORY_LABELS[locale]
}

export function categorySupportsKind(
  category: Category,
  kind: UserManagedTransactionKind,
): boolean {
  return category.allowedKind === kind || category.allowedKind === 'ANY'
}

export function ordinaryCategoriesForKind(
  categories: readonly Category[],
  kind: UserManagedTransactionKind,
): Category[] {
  return categories.filter((category) => categorySupportsKind(category, kind))
}

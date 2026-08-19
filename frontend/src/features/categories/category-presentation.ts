import type { Category, UserManagedTransactionKind } from '@/types/api'

export const UNKNOWN_CATEGORY_LABEL = 'Unknown category'

export function categoryLabel(category: Category): string {
  // Built-in localization can later branch on category.code. Custom categories
  // deliberately continue to use their owner-defined displayName.
  return category.displayName
}

export function categoryLabelForId(
  categories: readonly Category[] | undefined,
  categoryId: string,
): string {
  const category = categories?.find((candidate) => candidate.id === categoryId)
  return category ? categoryLabel(category) : UNKNOWN_CATEGORY_LABEL
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

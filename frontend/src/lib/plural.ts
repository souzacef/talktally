export interface PluralForms {
  readonly one: string
  readonly other: string
}

export function formatPluralMessage(
  locale: string,
  count: number,
  forms: PluralForms,
): string {
  const pluralCategory = new Intl.PluralRules(locale).select(count)
  // Product count labels use the plural form for zero, including in pt-BR.
  const form = count !== 0 && pluralCategory === 'one' ? 'one' : 'other'
  return forms[form].replace('{count}', String(count))
}

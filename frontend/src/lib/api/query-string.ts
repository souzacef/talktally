export function withQuery(
  path: string,
  values: object,
): string {
  const params = new URLSearchParams()
  Object.entries(values as Record<string, string | number | undefined>).forEach(([key, value]) => {
    if (value !== undefined && value !== '') {
      params.set(key, String(value))
    }
  })
  const query = params.toString()
  return query ? `${path}?${query}` : path
}

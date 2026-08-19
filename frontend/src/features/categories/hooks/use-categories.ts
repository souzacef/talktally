import { useQuery } from '@tanstack/react-query'
import { categoryApi } from '@/features/categories/api/category-api'
import { queryKeys } from '@/lib/query/query-client'

export function useCategories() {
  return useQuery({
    queryKey: queryKeys.categories.all,
    queryFn: ({ signal }) => categoryApi.list(signal),
  })
}

import { useQuery } from '@tanstack/react-query'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { peopleApi } from '@/features/reimbursements/api/people-api'
import { reimbursementApi } from '@/features/reimbursements/api/reimbursement-api'
import { formatBrl } from '@/lib/money/format-brl'
import { queryKeys } from '@/lib/query/query-client'

const openClaims = { page: 0, size: 20 } as const

export function OwedPage() {
  const people = useQuery({
    queryKey: queryKeys.people.all,
    queryFn: ({ signal }) => peopleApi.list(signal),
  })
  const claims = useQuery({
    queryKey: queryKeys.reimbursements.list(openClaims),
    queryFn: ({ signal }) => reimbursementApi.list(openClaims, signal),
  })

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-3xl font-semibold tracking-tight">Owed to Me</h1>
        <p className="text-muted-foreground">Reimbursement claims remain separate from earned income.</p>
      </div>
      <div className="grid gap-4 lg:grid-cols-3">
        <Card>
          <CardHeader><CardTitle>People</CardTitle></CardHeader>
          <CardContent>
            {people.isPending && <p>Loading people…</p>}
            {people.error && <p>People could not be loaded.</p>}
            {people.data?.length === 0 && <p>No people added yet.</p>}
            <ul className="space-y-2">
              {people.data?.map((person) => <li key={person.id}>{person.displayName}</li>)}
            </ul>
          </CardContent>
        </Card>
        <Card className="lg:col-span-2">
          <CardHeader><CardTitle>Claims</CardTitle></CardHeader>
          <CardContent>
            {claims.isPending && <p>Loading claims…</p>}
            {claims.error && <p>Claims could not be loaded.</p>}
            {claims.data?.items.length === 0 && <p>No reimbursement claims yet.</p>}
            <ul className="divide-y">
              {claims.data?.items.map((claim) => (
                <li key={claim.id} className="flex justify-between gap-4 py-3">
                  <div>
                    <p className="font-medium">{claim.personDisplayName}</p>
                    <p className="text-sm text-muted-foreground">{claim.status}</p>
                  </div>
                  <span>{formatBrl(claim.remainingAmount)} outstanding</span>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
      </div>
    </section>
  )
}

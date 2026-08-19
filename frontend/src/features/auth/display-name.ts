export function dashboardGreeting(displayName: string | null | undefined): string {
  const firstName = displayName?.trim().split(/\s+/)[0]
  return firstName ? `Hello, ${firstName}!` : 'Hello!'
}

import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { AssistantStatusChip, SpeechResult } from '@/components/assistant/assistant-result'

describe('assistant result presentation', () => {
  it('treats unavailable speech as a successful result rather than a destructive error', () => {
    render(<SpeechResult speechStatus="UNAVAILABLE" audioUrl={null} />)
    const result = screen.getByRole('status')
    expect(result).toHaveTextContent('Voice reply unavailable — result still succeeded.')
    expect(result).toHaveClass('text-protected')
    expect(result).not.toHaveClass('text-destructive')
  })

  it('renders only backend-supported assistant statuses', () => {
    const { rerender } = render(<AssistantStatusChip status="COMPLETED" />)
    expect(screen.getByText('Completed')).toBeInTheDocument()
    rerender(<AssistantStatusChip status="NEEDS_CLARIFICATION" />)
    expect(screen.getByText('Needs clarification')).toBeInTheDocument()
  })
})

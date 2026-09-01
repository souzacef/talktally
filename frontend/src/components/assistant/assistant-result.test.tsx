import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
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

  it('preserves localized native controls as the manual playback fallback', () => {
    const { rerender } = render(<SpeechResult speechStatus="GENERATED" audioUrl="blob:first" />)

    expect(screen.getByText('Voice reply')).toBeInTheDocument()
    expect(document.querySelector('audio')).toHaveAttribute('controls')
    expect(document.querySelector('audio')).toHaveAttribute('src', 'blob:first')

    rerender(<SpeechResult speechStatus="GENERATED" audioUrl="blob:first" voiceReplyLabel="Localized reply" />)
    expect(screen.getByText('Localized reply')).toBeInTheDocument()

    rerender(<SpeechResult speechStatus="GENERATED" audioUrl="blob:second" />)
    expect(document.querySelector('audio')).toHaveAttribute('src', 'blob:second')
  })

  it('notifies the page when manual native playback starts', () => {
    const onManualPlay = vi.fn()
    render(
      <SpeechResult
        speechStatus="GENERATED"
        audioUrl="blob:manual"
        onManualPlay={onManualPlay}
      />,
    )

    const audio = document.querySelector('audio')
    expect(audio).not.toBeNull()

    fireEvent.play(audio!)

    expect(onManualPlay).toHaveBeenCalledTimes(1)
  })

  it('does not render controls for unavailable speech or a null audio URL', () => {
    const { rerender } = render(<SpeechResult speechStatus="UNAVAILABLE" audioUrl="blob:unused" />)

    expect(screen.getByRole('status')).toBeInTheDocument()

    rerender(<SpeechResult speechStatus="GENERATED" audioUrl={null} />)
    expect(document.querySelector('audio')).toBeNull()
  })
})

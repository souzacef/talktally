import { render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AssistantStatusChip, SpeechResult } from '@/components/assistant/assistant-result'

describe('assistant result presentation', () => {
  const play = vi.fn()

  beforeEach(() => {
    play.mockReset()
    play.mockResolvedValue(undefined)
    Object.defineProperty(HTMLMediaElement.prototype, 'play', {
      configurable: true,
      value: play,
    })
  })

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

  it('attempts playback once per new audio URL while preserving native controls', async () => {
    const { rerender } = render(<SpeechResult speechStatus="GENERATED" audioUrl="blob:first" />)

    await waitFor(() => expect(play).toHaveBeenCalledTimes(1))
    expect(screen.getByText('Voice reply')).toBeInTheDocument()
    expect(document.querySelector('audio')).toHaveAttribute('controls')
    expect(document.querySelector('audio')).toHaveAttribute('src', 'blob:first')

    rerender(<SpeechResult speechStatus="GENERATED" audioUrl="blob:first" voiceReplyLabel="Localized reply" />)
    expect(play).toHaveBeenCalledTimes(1)
    expect(screen.getByText('Localized reply')).toBeInTheDocument()

    rerender(<SpeechResult speechStatus="GENERATED" audioUrl="blob:second" />)
    await waitFor(() => expect(play).toHaveBeenCalledTimes(2))
    expect(document.querySelector('audio')).toHaveAttribute('src', 'blob:second')
  })

  it('handles blocked autoplay without failing or removing manual controls', async () => {
    play.mockRejectedValueOnce(new DOMException('Autoplay blocked', 'NotAllowedError'))

    expect(() => render(
      <SpeechResult speechStatus="GENERATED" audioUrl="blob:blocked" />,
    )).not.toThrow()

    await waitFor(() => expect(play).toHaveBeenCalledTimes(1))
    expect(document.querySelector('audio')).toHaveAttribute('controls')
    expect(screen.getByText('Voice reply')).toBeInTheDocument()
  })

  it('does not attempt playback for unavailable speech or a null audio URL', () => {
    const { rerender } = render(<SpeechResult speechStatus="UNAVAILABLE" audioUrl="blob:unused" />)

    expect(play).not.toHaveBeenCalled()
    expect(screen.getByRole('status')).toBeInTheDocument()

    rerender(<SpeechResult speechStatus="GENERATED" audioUrl={null} />)
    expect(play).not.toHaveBeenCalled()
    expect(document.querySelector('audio')).toBeNull()
  })
})

import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { LocaleProvider } from '@/app/providers/locale-provider'
import { VoiceOrb } from '@/components/assistant/voice-orb'
import type { VoiceWorkflowState } from '@/features/assistant/hooks/use-voice-assistant'

function renderOrb(state: VoiceWorkflowState, onClick = vi.fn(), compact = false) {
  render(<LocaleProvider><VoiceOrb state={state} onClick={onClick} compact={compact} /></LocaleProvider>)
  return onClick
}

describe('VoiceOrb', () => {
  it('uses Stop while recording and a disabled spinner while processing', () => {
    const recording = renderOrb('recording')
    expect(screen.getByRole('button', { name: 'Stop microphone recording' }).querySelector('.lucide-square')).not.toBeNull()
    expect(screen.getByRole('button', { name: 'Stop microphone recording' })).toBeEnabled()
    expect(recording).not.toHaveBeenCalled()
  })

  it('shows the processing spinner as non-actionable', () => {
    renderOrb('processing')
    const button = screen.getByRole('button', { name: 'Processing voice command' })
    expect(button).toBeDisabled()
    expect(button.querySelector('.lucide-loader-circle')).not.toBeNull()
    expect(button.querySelector('.animate-spin')).not.toBeNull()
  })

  it.each([
    ['completed', 'Record another message'],
    ['needs-clarification', 'Try recording again'],
    ['speech-unavailable', 'Try recording again'],
    ['error', 'Try recording again'],
  ] as const)('returns terminal %s state to an actionable microphone', async (state, label) => {
    const onClick = renderOrb(state, vi.fn(), true)
    const button = screen.getByRole('button', { name: label })

    expect(button).toBeEnabled()
    expect(button.querySelector('.lucide-mic')).not.toBeNull()
    await userEvent.click(button)
    expect(onClick).toHaveBeenCalledTimes(1)
  })
})

import { describe, expect, it } from 'vitest'
import { assistantErrorText } from '@/features/assistant/assistant-messages'
import { ApiError } from '@/lib/api/api-client'

describe('assistantErrorText', () => {
  it.each([
    ['en-US', 429, 'RATE_LIMITED', 'You’re sending requests a little too quickly. Wait a moment and try again.'],
    ['pt-BR', 429, 'RATE_LIMITED', 'Você está enviando solicitações rápido demais. Aguarde um momento e tente novamente.'],
    ['en-US', 503, 'ASSISTANT_UNAVAILABLE', 'The assistant is temporarily unavailable. Please try again shortly.'],
    ['pt-BR', 503, 'ASSISTANT_UNAVAILABLE', 'O assistente está temporariamente indisponível. Tente novamente em instantes.'],
    ['en-US', 503, 'SPEECH_RECOGNITION_UNAVAILABLE', 'Voice recognition is temporarily unavailable. Try again shortly or type your message instead.'],
    ['pt-BR', 503, 'SPEECH_RECOGNITION_UNAVAILABLE', 'O reconhecimento de voz está temporariamente indisponível. Tente novamente em instantes ou digite sua mensagem.'],
    ['en-US', 413, 'AUDIO_TOO_LARGE', 'That recording is too large. Try a shorter recording.'],
    ['pt-BR', 413, 'AUDIO_TOO_LARGE', 'Essa gravação é grande demais. Tente uma gravação mais curta.'],
    ['en-US', 400, 'INVALID_AUDIO', 'That recording could not be processed. Try recording again.'],
    ['pt-BR', 400, 'INVALID_AUDIO', 'Não foi possível processar essa gravação. Tente gravar novamente.'],
    ['en-US', 400, 'INVALID_TRANSCRIPT', 'We couldn’t understand that voice command. Try recording again or type your message.'],
    ['pt-BR', 400, 'INVALID_TRANSCRIPT', 'Não conseguimos entender esse comando de voz. Tente gravar novamente ou digite sua mensagem.'],
  ] as const)(
    'maps %s %s to localized product copy',
    (locale, status, code, expected) => {
      const backendMessage = 'private backend/provider wording'
      const result = assistantErrorText(
        locale,
        new ApiError(status, code, backendMessage),
      )

      expect(result).toBe(expected)
      expect(result).not.toContain(backendMessage)
    },
  )

  it.each([
    ['en-US', 'Assistant request failed'],
    ['pt-BR', 'Falha na solicitação ao assistente'],
  ] as const)('uses the localized generic fallback for unknown ApiError codes in %s', (
    locale,
    expected,
  ) => {
    const result = assistantErrorText(
      locale,
      new ApiError(500, 'UNKNOWN_ASSISTANT_FAILURE', 'arbitrary backend detail'),
    )

    expect(result).toBe(expected)
    expect(result).not.toContain('arbitrary backend detail')
  })

  it('uses the generic fallback for non-ApiError request failures', () => {
    expect(assistantErrorText('en-US', new Error('private network detail')))
      .toBe('Assistant request failed')
  })
})

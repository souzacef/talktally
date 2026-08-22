import { describe, expect, it } from 'vitest'
import {
  AssistantConversationStorage,
  MAX_ASSISTANT_CONVERSATION_ENTRIES,
  assistantConversationStorageKey,
} from '@/features/assistant/assistant-conversation-storage'

describe('AssistantConversationStorage', () => {
  it('isolates conversation history by authenticated user', () => {
    const storage = new AssistantConversationStorage(window.sessionStorage)

    storage.write('user-a', [{ role: 'user', content: 'Message from A' }])
    storage.write('user-b', [{ role: 'assistant', content: 'Reply to B', status: 'COMPLETED' }])

    expect(storage.read('user-a')).toEqual([{ role: 'user', content: 'Message from A' }])
    expect(storage.read('user-b')).toEqual([
      { role: 'assistant', content: 'Reply to B', status: 'COMPLETED' },
    ])
  })

  it('treats malformed or invalid stored data as an empty conversation', () => {
    const storage = new AssistantConversationStorage(window.sessionStorage)
    const malformedKey = assistantConversationStorageKey('malformed-user')
    const invalidKey = assistantConversationStorageKey('invalid-user')
    window.sessionStorage.setItem(malformedKey, '{not-json')
    window.sessionStorage.setItem(invalidKey, JSON.stringify([
      { role: 'system', content: 'Untrusted role' },
    ]))

    expect(storage.read('malformed-user')).toEqual([])
    expect(storage.read('invalid-user')).toEqual([])
    expect(window.sessionStorage.getItem(malformedKey)).toBeNull()
    expect(window.sessionStorage.getItem(invalidKey)).toBeNull()
  })

  it('keeps only the most recent bounded conversation entries', () => {
    const storage = new AssistantConversationStorage(window.sessionStorage)
    const entries = Array.from(
      { length: MAX_ASSISTANT_CONVERSATION_ENTRIES + 5 },
      (_, index) => ({ role: 'user' as const, content: `Message ${index}` }),
    )

    storage.write('bounded-user', entries)

    const restored = storage.read('bounded-user')
    expect(restored).toHaveLength(MAX_ASSISTANT_CONVERSATION_ENTRIES)
    expect(restored[0]?.content).toBe('Message 5')
    expect(restored.at(-1)?.content).toBe(`Message ${MAX_ASSISTANT_CONVERSATION_ENTRIES + 4}`)
  })
})

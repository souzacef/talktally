import type { AssistantStatus } from '@/types/api'

const STORAGE_KEY_PREFIX = 'talktally.assistantConversation'
const MAX_ENTRY_CONTENT_LENGTH = 20_000

// Bounds visible session history and prevents unbounded sessionStorage growth.
export const MAX_ASSISTANT_CONVERSATION_ENTRIES = 50

export interface StoredAssistantConversationEntry {
  role: 'user' | 'assistant'
  content: string
  status?: AssistantStatus
}

function isAssistantStatus(value: unknown): value is AssistantStatus {
  return value === 'COMPLETED' || value === 'NEEDS_CLARIFICATION'
}

function isConversationEntry(value: unknown): value is StoredAssistantConversationEntry {
  if (typeof value !== 'object' || value === null) return false
  const entry = value as Partial<StoredAssistantConversationEntry>
  return (entry.role === 'user' || entry.role === 'assistant')
    && typeof entry.content === 'string'
    && entry.content.length <= MAX_ENTRY_CONTENT_LENGTH
    && (entry.status === undefined || isAssistantStatus(entry.status))
}

function storedEntry(entry: StoredAssistantConversationEntry): StoredAssistantConversationEntry {
  return entry.status === undefined
    ? { role: entry.role, content: entry.content }
    : { role: entry.role, content: entry.content, status: entry.status }
}

export function assistantConversationStorageKey(userId: string): string {
  return `${STORAGE_KEY_PREFIX}.${encodeURIComponent(userId)}`
}

export function boundAssistantConversation(
  entries: readonly StoredAssistantConversationEntry[],
): StoredAssistantConversationEntry[] {
  return entries.slice(-MAX_ASSISTANT_CONVERSATION_ENTRIES).map(storedEntry)
}

export class AssistantConversationStorage {
  private readonly storage: Storage

  constructor(storage: Storage) {
    this.storage = storage
  }

  read(userId: string): StoredAssistantConversationEntry[] {
    if (!userId.trim()) return []
    try {
      const serialized = this.storage.getItem(assistantConversationStorageKey(userId))
      if (!serialized) return []
      const parsed: unknown = JSON.parse(serialized)
      if (!Array.isArray(parsed) || !parsed.every(isConversationEntry)) {
        this.clear(userId)
        return []
      }
      return boundAssistantConversation(parsed)
    } catch {
      this.clear(userId)
      return []
    }
  }

  write(userId: string, entries: readonly StoredAssistantConversationEntry[]): void {
    if (!userId.trim()) return
    try {
      this.storage.setItem(
        assistantConversationStorageKey(userId),
        JSON.stringify(boundAssistantConversation(entries)),
      )
    } catch {
      // Persistence is best-effort; the in-memory Assistant remains usable.
    }
  }

  append(userId: string, entries: readonly StoredAssistantConversationEntry[]): void {
    if (entries.length === 0) return
    this.write(userId, [...this.read(userId), ...entries])
  }

  clear(userId: string): void {
    if (!userId.trim()) return
    try {
      this.storage.removeItem(assistantConversationStorageKey(userId))
    } catch {
      // Storage may be unavailable; session cleanup remains best-effort.
    }
  }
}

export const assistantConversationStorage = new AssistantConversationStorage(window.sessionStorage)

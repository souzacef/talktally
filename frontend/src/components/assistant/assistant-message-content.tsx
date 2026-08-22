import { Fragment, type ReactNode } from 'react'

const BOLD_SEGMENT = /\*\*(.+?)\*\*/g

export function AssistantMessageContent({ content }: { content: string }) {
  const parts: ReactNode[] = []
  let cursor = 0

  for (const match of content.matchAll(BOLD_SEGMENT)) {
    const index = match.index
    if (index > cursor) parts.push(content.slice(cursor, index))
    parts.push(<strong className="font-semibold" key={index}>{match[1]}</strong>)
    cursor = index + match[0].length
  }

  if (cursor === 0) return <>{content}</>
  if (cursor < content.length) parts.push(content.slice(cursor))
  return <>{parts.map((part, index) => <Fragment key={index}>{part}</Fragment>)}</>
}

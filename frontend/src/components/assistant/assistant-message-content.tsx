import { Fragment, type ReactNode } from 'react'

const BOLD_SEGMENT = /\*\*(.+?)\*\*/g
const LIST_ITEM = /^[*-] (.*)$/

type ContentBlock =
  | { type: 'paragraph', lines: string[] }
  | { type: 'list', items: string[] }

function inlineContent(content: string, keyPrefix: string): ReactNode {
  const parts: ReactNode[] = []
  let cursor = 0

  for (const match of content.matchAll(BOLD_SEGMENT)) {
    const index = match.index
    if (index > cursor) parts.push(content.slice(cursor, index))
    parts.push(<strong className="font-semibold" key={`${keyPrefix}-${index}`}>{match[1]}</strong>)
    cursor = index + match[0].length
  }

  if (parts.length === 0) return content
  if (cursor < content.length) parts.push(content.slice(cursor))
  return parts
}

function contentBlocks(content: string): ContentBlock[] {
  const blocks: ContentBlock[] = []
  let active: ContentBlock | undefined

  for (const line of content.split(/\r?\n/)) {
    if (line.trim() === '') {
      active = undefined
      continue
    }

    const listItem = line.match(LIST_ITEM)
    if (listItem) {
      if (active?.type === 'list') active.items.push(listItem[1] ?? '')
      else {
        active = { type: 'list', items: [listItem[1] ?? ''] }
        blocks.push(active)
      }
    }
    else if (active?.type === 'paragraph') {
      active.lines.push(line)
    }
    else {
      active = { type: 'paragraph', lines: [line] }
      blocks.push(active)
    }
  }

  return blocks
}

export function AssistantMessageContent({ content }: { content: string }) {
  return (
    <div className="space-y-2">
      {contentBlocks(content).map((block, blockIndex) => (
        block.type === 'list'
          ? (
              <ul className="list-disc space-y-1 pl-5" key={blockIndex}>
                {block.items.map((item, itemIndex) => (
                  <li key={itemIndex}>{inlineContent(item, `${blockIndex}-${itemIndex}`)}</li>
                ))}
              </ul>
            )
          : (
              <p key={blockIndex}>
                {block.lines.map((line, lineIndex) => (
                  <Fragment key={lineIndex}>
                    {lineIndex > 0 && <br />}
                    {inlineContent(line, `${blockIndex}-${lineIndex}`)}
                  </Fragment>
                ))}
              </p>
            )
      ))}
    </div>
  )
}

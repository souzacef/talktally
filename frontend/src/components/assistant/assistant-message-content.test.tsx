import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { AssistantMessageContent } from '@/components/assistant/assistant-message-content'

describe('AssistantMessageContent', () => {
  it('renders plain text normally', () => {
    render(<AssistantMessageContent content="Plain financial text" />)

    expect(screen.getByText('Plain financial text')).toBeInTheDocument()
  })

  it('renders multiple paired double-asterisk spans as safe emphasis', () => {
    const { container } = render(
      <AssistantMessageContent content="You spent **R$ 416.75** on **Food and dining**." />,
    )

    expect(screen.getByText('R$ 416.75').tagName).toBe('STRONG')
    expect(screen.getByText('Food and dining').tagName).toBe('STRONG')
    expect(container).not.toHaveTextContent('**')
  })

  it('renders consecutive asterisk and hyphen items as semantic lists with inline bold', () => {
    const { container } = render(<AssistantMessageContent content={[
      'Here is the breakdown:',
      '* **Food and dining**: R$ 130.00',
      '- **Groceries**: R$ 125.50',
      'The remaining categories were lower.',
    ].join('\n')} />)

    const list = screen.getByRole('list')
    const items = within(list).getAllByRole('listitem')
    expect(items).toHaveLength(2)
    expect(items[0]?.querySelector('strong')).toHaveTextContent('Food and dining')
    expect(items[1]?.querySelector('strong')).toHaveTextContent('Groceries')
    expect(container).not.toHaveTextContent('*')
    expect(screen.getByText('The remaining categories were lower.').tagName).toBe('P')
  })

  it('uses blank lines as clean paragraph boundaries', () => {
    const { container } = render(
      <AssistantMessageContent content={'First paragraph.\n\nSecond paragraph.'} />,
    )

    expect(container.querySelectorAll('p')).toHaveLength(2)
  })

  it.each(['**unfinished', 'hello **world'])('leaves malformed bold readable: %s', (content) => {
    const { container } = render(<AssistantMessageContent content={content} />)

    expect(container).toHaveTextContent(content)
    expect(container.querySelector('strong')).toBeNull()
  })

  it('renders script-like input as inert text rather than HTML', () => {
    const { container } = render(
      <AssistantMessageContent content={'<script>alert("xss")</script> **safe**'} />,
    )

    expect(container.querySelector('script')).toBeNull()
    expect(container).toHaveTextContent('<script>alert("xss")</script> safe')
  })

  it('does not reinterpret ordinary asterisks or hyphens as list markers', () => {
    const content = 'R$ 10 * 2 remains text\nR$ 20 - R$ 5 remains text'
    const { container } = render(<AssistantMessageContent content={content} />)

    expect(container.querySelector('ul')).toBeNull()
    expect(container).toHaveTextContent('R$ 10 * 2 remains text')
    expect(container).toHaveTextContent('R$ 20 - R$ 5 remains text')
  })
})

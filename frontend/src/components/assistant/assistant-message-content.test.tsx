import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { AssistantMessageContent } from '@/components/assistant/assistant-message-content'

describe('AssistantMessageContent', () => {
  it('renders only paired double-asterisk segments as safe emphasis', () => {
    render(<p><AssistantMessageContent content="You spent **R$ 416.75** this month." /></p>)

    expect(screen.getByText('R$ 416.75').tagName).toBe('STRONG')
    expect(screen.getByText(/You spent/)).not.toHaveTextContent('**')
  })

  it.each(['Plain financial text', '**unfinished', 'hello **world'])(
    'leaves harmless plain or malformed text readable: %s',
    (content) => {
      const { container } = render(<AssistantMessageContent content={content} />)
      expect(container).toHaveTextContent(content)
      expect(container.querySelector('strong')).toBeNull()
    },
  )

  it('renders script-like input as text rather than HTML', () => {
    const { container } = render(
      <AssistantMessageContent content={'<script>alert("xss")</script> **safe**'} />,
    )

    expect(container.querySelector('script')).toBeNull()
    expect(container).toHaveTextContent('<script>alert("xss")</script> safe')
  })
})

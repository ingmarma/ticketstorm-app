import * as React from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useChat } from '@/hooks/useChat'
import { ChatBubble } from '@/components/shared/ChatBubble'
import { ChatInput } from '@/components/shared/ChatInput'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { aiApi } from '@/services/api'
import { formatDate, formatGuarani } from '@/utils/format'
import type { ChatMessage, Event } from '@/types/api'

const FALLBACK_SUGGESTIONS = [
  '¿Qué conciertos hay en noviembre?',
  '¿Cuál es el evento más barato?',
  'Quiero ir a ver fútbol',
]

function eventsFromMessage(message: ChatMessage): Event[] {
  const events = message.metadata?.events
  return Array.isArray(events) ? (events as Event[]) : []
}

function ChatEventCard({ event }: { event: Event }) {
  return (
    <Link
      to={`/events/${event.id}`}
      className="block rounded-xl border border-border bg-background p-3 transition-colors hover:bg-secondary/60"
    >
      <div className="flex items-start justify-between gap-2">
        <p className="text-sm font-semibold leading-tight">{event.name}</p>
        <Badge variant="secondary" className="shrink-0 text-[10px]">
          {event.category}
        </Badge>
      </div>
      <p className="mt-1 text-xs text-muted-foreground">
        {event.venue} · {event.city}
      </p>
      <p className="text-xs text-muted-foreground">{formatDate(event.eventDate)}</p>
      <p className="mt-1 text-sm font-bold text-aws-orange">
        {formatGuarani(event.minPrice)}
      </p>
    </Link>
  )
}

export function ChatPanel() {
  const [isOpen, setIsOpen] = React.useState(false)
  const { messages, sendMessage, isLoading } = useChat()
  const messagesEndRef = React.useRef<HTMLDivElement>(null)

  const { data: fetchedSuggestions } = useQuery({
    queryKey: ['chat-suggestions'],
    queryFn: () => aiApi.getSuggestions(),
    enabled: isOpen,
    staleTime: 10 * 60 * 1000,
    retry: 1,
  })

  const suggestions =
    fetchedSuggestions && fetchedSuggestions.length > 0
      ? fetchedSuggestions
      : FALLBACK_SUGGESTIONS

  React.useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const handleSend = (message: string) => {
    sendMessage(message)
  }

  return (
    <div className="fixed bottom-6 right-6 z-50">
      {isOpen && (
        <div className="mb-4 w-80 rounded-2xl border border-border bg-card shadow-2xl animate-slide-up sm:w-96">
          <div className="flex items-center justify-between border-b border-border p-4">
            <div className="flex items-center gap-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-aws-orange">
                <span className="text-sm font-bold text-navy-900">AI</span>
              </div>
              <div>
                <h3 className="font-medium">TicketStorm Assistant</h3>
                <p className="text-xs text-muted-foreground">Powered by AI</p>
              </div>
            </div>
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setIsOpen(false)}
              className="h-8 w-8"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <line x1="18" x2="6" y1="6" y2="18" />
                <line x1="6" x2="18" y1="6" y2="18" />
              </svg>
            </Button>
          </div>

          <div className="h-80 overflow-y-auto p-4 space-y-4">
            {messages.length === 0 && (
              <div className="flex flex-col items-center justify-center h-full text-center">
                <div className="mb-4 rounded-full bg-aws-orange/10 p-4">
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="32"
                    height="32"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    className="text-aws-orange"
                  >
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                  </svg>
                </div>
                <h4 className="font-medium">¿En qué puedo ayudarte?</h4>
                <p className="mt-1 text-sm text-muted-foreground">
                  Pregúntame por eventos, entradas o sedes
                </p>
                <div className="mt-4 space-y-2">
                  {suggestions.map((suggestion) => (
                    <button
                      key={suggestion}
                      type="button"
                      onClick={() => handleSend(suggestion)}
                      className="block w-full rounded-lg border border-border bg-secondary px-3 py-2 text-left text-sm hover:bg-secondary/80 transition-colors"
                    >
                      {suggestion}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {messages.map((message) => {
              const events = eventsFromMessage(message)
              return (
                <div key={message.id} className="space-y-2">
                  <ChatBubble message={message} />
                  {message.role === 'assistant' && events.length > 0 && (
                    <div className="space-y-2 pl-1">
                      {events.map((event) => (
                        <ChatEventCard key={event.id} event={event} />
                      ))}
                    </div>
                  )}
                </div>
              )
            })}

            {isLoading && messages[messages.length - 1]?.role !== 'assistant' && (
              <div className="flex justify-start">
                <div className="rounded-2xl rounded-bl-md bg-secondary px-4 py-3">
                  <div className="flex space-x-1">
                    <div className="h-2 w-2 animate-bounce rounded-full bg-muted-foreground" />
                    <div className="h-2 w-2 animate-bounce rounded-full bg-muted-foreground [animation-delay:0.1s]" />
                    <div className="h-2 w-2 animate-bounce rounded-full bg-muted-foreground [animation-delay:0.2s]" />
                  </div>
                </div>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          <div className="border-t border-border p-4">
            <ChatInput onSend={handleSend} disabled={isLoading} />
          </div>
        </div>
      )}

      <Button
        onClick={() => setIsOpen(!isOpen)}
        className="h-14 w-14 rounded-full shadow-lg shadow-aws-orange/20"
        size="icon"
      >
        {isOpen ? (
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="24"
            height="24"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <line x1="18" x2="6" y1="6" y2="18" />
            <line x1="6" x2="18" y1="6" y2="18" />
          </svg>
        ) : (
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="24"
            height="24"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
        )}
      </Button>
    </div>
  )
}

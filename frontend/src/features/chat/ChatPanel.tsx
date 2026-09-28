import * as React from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useChat } from '@/hooks/useChat'
import { ChatBubble } from '@/components/shared/ChatBubble'
import { ChatInput } from '@/components/shared/ChatInput'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { aiApi } from '@/services/api'
import { useStore } from '@/store/useStore'
import { availabilityColor, formatDate, formatGuarani } from '@/utils/format'
import { cn } from '@/utils/cn'
import type { ChatAction, ChatMessage, Event, Seat, TicketSection } from '@/types/api'

const FALLBACK_SUGGESTIONS = [
  '¿Qué conciertos hay en noviembre?',
  '¿Cuál es el evento más barato?',
  'Quiero ir a ver fútbol',
]

interface ChatPreFill {
  text: string
  id: number
}

function eventsFromMessage(message: ChatMessage): Event[] {
  const events = message.metadata?.events
  return Array.isArray(events) ? (events as Event[]) : []
}

function sectionsFromMessage(message: ChatMessage): TicketSection[] {
  const sections = message.metadata?.sections
  return Array.isArray(sections) ? (sections as TicketSection[]) : []
}

function actionFromMessage(message: ChatMessage): ChatAction | null {
  const action = message.metadata?.action
  if (!action || typeof action !== 'object') return null
  const typed = action as ChatAction
  return typed.type ? typed : null
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

interface SectionListProps {
  event: Event
  sections: TicketSection[]
  highlightSectionId?: string | null
  onBuy: (section: TicketSection, quantity: number) => void
}

function ChatSectionList({ event, sections, highlightSectionId, onBuy }: SectionListProps) {
  return (
    <div className="rounded-xl border border-border bg-background p-3">
      <Link
        to={`/events/${event.id}`}
        className="mb-2 block text-xs font-semibold uppercase tracking-wide text-muted-foreground hover:text-foreground"
      >
        Entradas · {event.name}
      </Link>
      <div className="space-y-3">
        {sections.map((section) => {
          const soldOut = section.availableCapacity <= 0
          const percentage =
            section.totalCapacity > 0
              ? (section.availableCapacity / section.totalCapacity) * 100
              : 0
          const highlighted = highlightSectionId === section.id
          return (
            <div
              key={section.id}
              className={`flex items-start justify-between gap-3 border-t border-border pt-3 first:border-t-0 first:pt-0 ${
                highlighted ? 'rounded-lg bg-aws-orange/5 p-2 -m-2' : ''
              }`}
            >
              <div className="min-w-0">
                <p className="text-sm font-medium">{section.name}</p>
                <p className="text-xs text-muted-foreground">
                  {section.availableCapacity} de {section.totalCapacity} disponibles
                </p>
                <div className="mt-1 h-1.5 w-20 overflow-hidden rounded-full bg-muted">
                  <div
                    className={`h-full ${availabilityColor(percentage)}`}
                    style={{ width: `${percentage}%` }}
                  />
                </div>
              </div>
              <div className="shrink-0 text-right">
                <p className="text-sm font-bold text-aws-orange">
                  {formatGuarani(section.price)}
                </p>
                {soldOut ? (
                  <span className="text-xs text-muted-foreground">Agotado</span>
                ) : (
                  <button
                    type="button"
                    onClick={() => onBuy(section, 1)}
                    className="text-xs font-semibold text-aws-orange hover:underline"
                  >
                    Comprar
                  </button>
                )}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

export function ChatPanel() {
  const [isOpen, setIsOpen] = React.useState(false)
  const [preFill, setPreFill] = React.useState<ChatPreFill | null>(null)
  const { messages, sendMessage, isLoading } = useChat()
  const messagesEndRef = React.useRef<HTMLDivElement>(null)
  const navigate = useNavigate()
  const { addToCart } = useStore((state) => state.cart)

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

  React.useEffect(() => {
    const onOpenChat: EventListener = (ev) => {
      const detail = (ev as CustomEvent<{ message?: string }>).detail
      const message = detail?.message?.trim()
      setIsOpen(true)
      if (message) {
        setPreFill({ text: message, id: Date.now() })
      }
    }
    window.addEventListener('open-chat', onOpenChat)
    return () => window.removeEventListener('open-chat', onOpenChat)
  }, [])

  const handleSend = (message: string) => {
    sendMessage(message)
  }

  const handleBuy = (event: Event, section: TicketSection, quantity: number) => {
    const count = Math.max(1, quantity)
    const seats: Seat[] = Array.from({ length: count }, (_, index) => ({
      id: `${section.id}-${index + 1}`,
      eventId: event.id,
      section: section.name,
      row: '',
      number: String(index + 1),
      price: Number(section.price),
    }))
    addToCart(event, seats)
    setIsOpen(false)
    navigate(`/checkout/${event.id}`)
  }

  const handleViewEvent = (eventId: string) => {
    setIsOpen(false)
    navigate(`/events/${eventId}`)
  }

  return (
    <>
      {isOpen && (
        <div
          className="fixed inset-0 z-[90] bg-black/60 backdrop-blur-sm md:hidden"
          onClick={() => setIsOpen(false)}
          aria-hidden="true"
        />
      )}

      {isOpen && (
        <div className="fixed inset-0 z-[100] flex animate-slide-up flex-col overflow-hidden bg-card shadow-2xl md:inset-auto md:bottom-24 md:right-6 md:h-auto md:max-h-[600px] md:min-h-[24rem] md:w-[400px] md:max-w-[400px] md:rounded-2xl md:border md:border-border">
          <div className="flex shrink-0 items-center justify-between border-b border-border p-4">
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
              className="h-8 w-8 shrink-0"
              aria-label="Cerrar chat"
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

          <div className="flex min-h-0 flex-1 flex-col space-y-4 overflow-y-auto p-4">
            {messages.length === 0 && (
              <div className="flex flex-1 flex-col items-center justify-center text-center">
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
                <div className="mt-4 w-full space-y-2">
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
              const sections = sectionsFromMessage(message)
              const action = actionFromMessage(message)
              const focusEvent =
                (action && events.find((event) => event.id === action.eventId)) ||
                events[0]
              const hasSections = message.role === 'assistant' && sections.length > 0 && !!focusEvent
              const listedEvents = hasSections ? [] : events
              const buySection =
                action?.type === 'BUY'
                  ? sections.find((section) => section.id === action.sectionId)
                  : undefined
              const buyQuantity = action?.type === 'BUY' ? action.quantity ?? 1 : 1

              return (
                <div key={message.id} className="space-y-2">
                  <ChatBubble message={message} />

                  {listedEvents.length > 0 && (
                    <div className="space-y-2 pl-1">
                      {listedEvents.map((event) => (
                        <ChatEventCard key={event.id} event={event} />
                      ))}
                    </div>
                  )}

                  {hasSections && focusEvent && (
                    <ChatSectionList
                      event={focusEvent}
                      sections={sections}
                      highlightSectionId={action?.sectionId}
                      onBuy={(section, quantity) => handleBuy(focusEvent, section, quantity)}
                    />
                  )}

                  {action?.type === 'BUY' && focusEvent && buySection && (
                    <Button
                      className="w-full"
                      disabled={buySection.availableCapacity <= 0}
                      onClick={() => handleBuy(focusEvent, buySection, buyQuantity)}
                    >
                      Comprar {buyQuantity} × {buySection.name} ·{' '}
                      {formatGuarani(Number(buySection.price) * buyQuantity)}
                    </Button>
                  )}

                  {action?.type === 'VIEW_EVENT' && action.eventId && (
                    <Button
                      variant="secondary"
                      className="w-full"
                      onClick={() => handleViewEvent(action.eventId)}
                    >
                      Ver evento
                    </Button>
                  )}

                  {action?.type === 'VIEW_SECTIONS' && action.eventId && (
                    <Button
                      variant="secondary"
                      className="w-full"
                      onClick={() => handleViewEvent(action.eventId)}
                    >
                      Ver todas las entradas
                    </Button>
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

          <div className="shrink-0 border-t border-border p-4">
            <ChatInput prefill={preFill} onSend={handleSend} disabled={isLoading} />
          </div>
        </div>
      )}

      <div className={cn('fixed bottom-6 right-6 z-[110]', isOpen && 'hidden md:block')}>
        <Button
          onClick={() => setIsOpen(!isOpen)}
          className="h-14 w-14 rounded-full shadow-lg shadow-aws-orange/20"
          size="icon"
          aria-label="Abrir asistente de IA"
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
    </>
  )
}

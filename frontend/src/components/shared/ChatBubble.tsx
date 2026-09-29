import { cn } from '@/utils/cn'
import type { ChatMessage } from '@/types/api'

interface ChatBubbleProps {
  message: ChatMessage
  className?: string
}

export function ChatBubble({ message, className }: ChatBubbleProps) {
  const isUser = message.role === 'user'

  return (
    <div
      className={cn(
        'flex w-full animate-fade-in',
        isUser ? 'justify-end' : 'justify-start',
        className
      )}
    >
      <div
        className={cn(
          'max-w-[80%] rounded-2xl px-4 py-3',
          isUser
            ? 'bg-aws-orange text-navy-900 rounded-br-md'
            : 'bg-secondary text-foreground rounded-bl-md'
        )}
      >
        {!isUser && (
          <div className="mb-1 flex items-center gap-2">
            <div className="flex h-5 w-5 items-center justify-center rounded-full bg-aws-orange/20">
              <span className="text-xs font-bold text-aws-orange">AI</span>
            </div>
            <span className="text-xs font-medium text-muted-foreground">Assistant</span>
          </div>
        )}
        <p className="text-sm whitespace-pre-wrap">{message.content}</p>
        <p
          className={cn(
            'mt-1 text-xs',
            isUser ? 'text-navy-900/70' : 'text-muted-foreground'
          )}
        >
          {new Date(message.timestamp).toLocaleTimeString([], {
            hour: '2-digit',
            minute: '2-digit',
          })}
        </p>
      </div>
    </div>
  )
}

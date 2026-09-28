import { useState, useCallback } from 'react'
import { useMutation } from '@tanstack/react-query'
import { aiApi } from '@/services/api'
import type { ChatMessage } from '@/types/api'

const ERROR_MESSAGE =
  'No pude responder en este momento. Intenta de nuevo en unos segundos.'

export function useChat() {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [sessionId, setSessionId] = useState<string | undefined>()

  const chatMutation = useMutation({
    mutationFn: (message: string) => aiApi.chat(message, sessionId),
    onSuccess: (data) => {
      setSessionId(data.sessionId)
      setMessages((prev) => [
        ...prev,
        {
          id: `assistant-${Date.now()}`,
          role: 'assistant',
          content: data.response,
          timestamp: new Date().toISOString(),
          metadata: {
            events: data.events ?? [],
            sections: data.sections ?? [],
            action: data.action ?? null,
          },
        },
      ])
    },
    onError: () => {
      setMessages((prev) => [
        ...prev,
        {
          id: `assistant-${Date.now()}`,
          role: 'assistant',
          content: ERROR_MESSAGE,
          timestamp: new Date().toISOString(),
        },
      ])
    },
  })

  const sendMessage = useCallback(
    (content: string) => {
      const userMessage: ChatMessage = {
        id: `user-${Date.now()}`,
        role: 'user',
        content,
        timestamp: new Date().toISOString(),
      }

      setMessages((prev) => [...prev, userMessage])
      chatMutation.mutate(content)
    },
    [chatMutation]
  )

  const clearMessages = useCallback(() => {
    setMessages([])
    setSessionId(undefined)
  }, [])

  return {
    messages,
    sendMessage,
    clearMessages,
    isLoading: chatMutation.isPending,
    error: chatMutation.error,
  }
}

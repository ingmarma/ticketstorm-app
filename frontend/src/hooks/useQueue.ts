import { useEffect, useCallback, useRef } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { queueApi } from '@/services/api'
import { useStore } from '@/store/useStore'
import type { QueuePosition } from '@/types/api'

export function useQueue(eventId: string) {
  const queryClient = useQueryClient()
  const queue = useStore(
  (state) => state.queue
)
  const { joinQueue: storeJoinQueue, leaveQueue: storeLeaveQueue, setQueuePosition } = queue
  const wsRef = useRef<WebSocket | null>(null)

  const joinMutation = useMutation({
    mutationFn: () => queueApi.joinQueue(eventId),
    onSuccess: (data: QueuePosition) => {
      storeJoinQueue(eventId)
      setQueuePosition(data)
      queryClient.setQueryData(['queue-position', eventId], data)
    },
  })

  const leaveMutation = useMutation({
    mutationFn: () => queueApi.leaveQueue(eventId),
    onSuccess: () => {
      storeLeaveQueue()
      queryClient.removeQueries({ queryKey: ['queue-position', eventId] })
    },
  })

  const positionQuery = useQuery({
    queryKey: ['queue-position', eventId],
    queryFn: () => queueApi.getPosition(eventId),
    enabled: queue.isWaiting && queue.eventId === eventId,
    refetchInterval: 5000,
  })

  const connectWebSocket = useCallback(() => {
    if (wsRef.current) {
      wsRef.current.close()
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/ws/queue/${eventId}`
    const ws = new WebSocket(wsUrl)

    ws.onmessage = (event) => {
      const data = JSON.parse(event.data) as QueuePosition
      setQueuePosition(data)
      queryClient.setQueryData(['queue-position', eventId], data)

      if (data.status === 'IN_PROGRESS') {
        ws.close()
      }
    }

    ws.onerror = () => {
      console.error('WebSocket error, will retry via polling')
    }

    ws.onclose = () => {
      if (queue.isWaiting && queue.eventId === eventId) {
        setTimeout(connectWebSocket, 3000)
      }
    }

    wsRef.current = ws
  }, [eventId, queue.isWaiting, queue.eventId, setQueuePosition, queryClient])

  useEffect(() => {
    if (queue.isWaiting && queue.eventId === eventId) {
      connectWebSocket()
    }

    return () => {
      if (wsRef.current) {
        wsRef.current.close()
      }
    }
  }, [queue.isWaiting, queue.eventId, eventId, connectWebSocket])

  return {
    position: positionQuery.data ?? queue.position,
    isWaiting: queue.isWaiting && queue.eventId === eventId,
    joinQueue: joinMutation.mutate,
    leaveQueue: leaveMutation.mutate,
    isJoining: joinMutation.isPending,
    isLeaving: leaveMutation.isPending,
    error: joinMutation.error ?? positionQuery.error,
  }
}

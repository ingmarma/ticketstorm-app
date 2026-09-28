import { useQuery } from '@tanstack/react-query'
import { eventsApi } from '@/services/api'
import type { SearchParams } from '@/types/api'

export function useEventSearch(params?: SearchParams) {
  return useQuery({
    queryKey: ['events', params],
    queryFn: () => eventsApi.getEvents(params),
    staleTime: 5 * 60 * 1000,
  })
}

export function useEvent(id: string) {
  return useQuery({
    queryKey: ['event', id],
    queryFn: () => eventsApi.getEvent(id),
    enabled: !!id,
  })
}

export function useEventSeats(eventId: string) {
  return useQuery({
    queryKey: ['event-seats', eventId],
    queryFn: () => eventsApi.getEventSeats(eventId),
    enabled: !!eventId,
  })
}

export function useEventSections(eventId: string) {
  return useQuery({
    queryKey: ['event-sections', eventId],
    queryFn: () => eventsApi.getEventSections(eventId),
    enabled: !!eventId,
  })
}

export function useSemanticSearch(query: string) {
  return useQuery({
    queryKey: ['semantic-search', query],
    queryFn: () => eventsApi.searchEvents(query),
    enabled: query.length >= 3,
    staleTime: 10 * 60 * 1000,
  })
}

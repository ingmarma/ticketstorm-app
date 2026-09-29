import axios from 'axios'
import type {
  Event,
  TicketSection,
  Reservation,
  Payment,
  QueuePosition,
  FraudCheck,
  ChatResponse,
  SearchParams,
  PaginatedResponse,
  AdminMetrics,
  CircuitBreakerState,
} from '@/types/api'

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('auth_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('auth_token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export const eventsApi = {
  getEvents: async (params?: SearchParams): Promise<PaginatedResponse<Event>> => {
    const { data } = await api.get('/events', { params })
    if (Array.isArray(data)) return { data, total: data.length, page: 1, limit: data.length, totalPages: 1 }
    return data
  },

  getEvent: async (id: string): Promise<Event> => {
    const { data } = await api.get(`/events/${id}`)
    return data
  },

  searchEvents: async (query: string, params?: SearchParams): Promise<Event[]> => {
    const { data } = await api.get<Event[]>('/events/search', {
      params: { query, ...params },
    })
    return data
  },

  getEventSeats: async (eventId: string): Promise<TicketSection[]> => {
    const { data } = await api.get(`/events/${eventId}/seats`)
    return data
  },

  getEventSections: async (eventId: string): Promise<TicketSection[]> => {
    const { data } = await api.get(`/events/${eventId}/sections`)
    return data
  },
}

export const queueApi = {
  joinQueue: async (eventId: string): Promise<QueuePosition> => {
    const { data } = await api.post(`/queue/${eventId}/join`)
    return data
  },

  getPosition: async (eventId: string): Promise<QueuePosition> => {
    const { data } = await api.get(`/queue/${eventId}/position`)
    return data
  },

  leaveQueue: async (eventId: string): Promise<void> => {
    await api.delete(`/queue/${eventId}/leave`)
  },
}

export const reservationApi = {
  createReservation: async (eventId: string, seatIds: string[]): Promise<Reservation> => {
    const { data } = await api.post('/reservations', { eventId, seatIds })
    return data
  },

  getReservation: async (id: string): Promise<Reservation> => {
    const { data } = await api.get(`/reservations/${id}`)
    return data
  },

  cancelReservation: async (id: string): Promise<void> => {
    await api.delete(`/reservations/${id}`)
  },
}

export const paymentApi = {
  processPayment: async (
    reservationId: string,
    paymentMethod: string
  ): Promise<Payment> => {
    const { data } = await api.post('/payments', {
      reservationId,
      paymentMethod,
    })
    return data
  },

  getPayment: async (id: string): Promise<Payment> => {
    const { data } = await api.get(`/payments/${id}`)
    return data
  },
}

export const aiApi = {
  search: async (query: string): Promise<Event[]> => {
    const { data } = await api.post('/ai/search', { query })
    return data
  },

  chat: async (message: string, sessionId?: string): Promise<ChatResponse> => {
    const { data } = await api.post('/chat', { message, sessionId })
    return data
  },

  getSuggestions: async (): Promise<string[]> => {
    const { data } = await api.get('/chat/suggestions')
    return data
  },

  fraudCheck: async (userId: string, eventId: string): Promise<FraudCheck> => {
    const { data } = await api.post('/ai/fraud-check', { userId, eventId })
    return data
  },
}

export const adminApi = {
  getMetrics: async (): Promise<AdminMetrics> => {
    const { data } = await api.get('/admin/metrics')
    return data
  },

  getFraudAlerts: async (): Promise<FraudCheck[]> => {
    const { data } = await api.get('/admin/fraud-alerts')
    return data
  },

  getCircuitBreakers: async (): Promise<CircuitBreakerState[]> => {
    const { data } = await api.get('/admin/circuit-breakers')
    return data
  },

  resetCircuitBreaker: async (serviceName: string): Promise<void> => {
    await api.post(`/admin/circuit-breakers/${serviceName}/reset`)
  },
}

export default api

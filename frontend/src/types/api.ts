export interface Event {
  id: string
  name: string
  description: string
  venue: string
  city: string
  eventDate: string
  category: EventCategory
  imageUrl?: string
  minPrice: number
  currency: string
  totalSeats: number
  availableSeats: number
  ticketSections?: TicketSection[]
  createdAt: string
  updatedAt: string
}

export interface TicketSection {
  id: string
  name: string
  description?: string
  price: number
  currency: string
  totalCapacity: number
  availableCapacity: number
  sortOrder: number
}

export type EventCategory = 'CONCERT' | 'SPORTS' | 'THEATER' | 'COMEDY' | 'CONFERENCE' | 'FESTIVAL'

export interface Seat {
  id: string
  eventId: string
  section: string
  row: string
  number: string
  price: number
  status?: string
  type?: string
  wheelchairAccessible?: boolean
}

export interface Reservation {
  id: string
  eventId: string
  userId: string
  seatIds: string[]
  seats: Seat[]
  event: Event
  totalAmount: number
  status: ReservationStatus
  expiresAt: string
  createdAt: string
}

export type ReservationStatus = 'PENDING' | 'CONFIRMED' | 'EXPIRED' | 'CANCELLED' | 'COMPLETED'

export interface Payment {
  id: string
  reservationId: string
  amount: number
  currency: string
  status: PaymentStatus
  method: PaymentMethod
  transactionId?: string
  createdAt: string
}

export type PaymentStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'REFUNDED'
export type PaymentMethod = 'CREDIT_CARD' | 'DEBIT_CARD' | 'PAYPAL' | 'STRIPE'

export interface QueuePosition {
  position: number
  totalInQueue: number
  estimatedWaitMinutes: number
  eventId: string
  userId: string
  joinedAt: string
  status: QueueStatus
}

export type QueueStatus = 'WAITING' | 'IN_PROGRESS' | 'COMPLETED' | 'EXPIRED'

export interface FraudCheck {
  id: string
  userId: string
  eventId: string
  riskScore: number
  flags: FraudFlag[]
  status: FraudStatus
  checkedAt: string
}

export type FraudFlag = 'VELOCITY' | 'GEOLOCATION' | 'DISPOSABLE_EMAIL' | 'SUSPICIOUS_BEHAVIOR' | 'MULTI_ACCOUNT'
export type FraudStatus = 'CLEAN' | 'SUSPICIOUS' | 'BLOCKED'

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  timestamp: string
  metadata?: Record<string, unknown>
}

export interface ChatConversation {
  id: string
  messages: ChatMessage[]
  createdAt: string
  updatedAt: string
}

export interface ChatResponse {
  response: string
  events: Event[]
  sessionId: string
}

export interface AdminMetrics {
  totalEvents: number
  totalReservations: number
  totalRevenue: number
  activeQueues: number
  fraudAlerts: number
  systemHealth: SystemHealth
}

export interface SystemHealth {
  apiGateway: ServiceStatus
  eventService: ServiceStatus
  reservationService: ServiceStatus
  paymentService: ServiceStatus
  queueService: ServiceStatus
}

export type ServiceStatus = 'HEALTHY' | 'DEGRADED' | 'DOWN'

export interface CircuitBreakerState {
  serviceName: string
  state: 'CLOSED' | 'OPEN' | 'HALF_OPEN'
  failureCount: number
  lastFailureTime?: string
  nextAttemptTime?: string
}

export interface SearchParams {
  query?: string
  category?: EventCategory
  city?: string
  dateFrom?: string
  dateTo?: string
  minPrice?: number
  maxPrice?: number
  page?: number
  limit?: number
}

export interface PaginatedResponse<T> {
  data: T[]
  total: number
  page: number
  limit: number
  totalPages: number
}

export interface ApiError {
  message: string
  code: string
  status: number
  details?: Record<string, unknown>
}

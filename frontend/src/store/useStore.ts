import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { Event, Seat, Reservation, QueuePosition } from '@/types/api'

interface UserState {
  userId: string | null
  name: string | null
  email: string | null
  isAuthenticated: boolean
  setUser: (user: { userId: string; name: string; email: string }) => void
  clearUser: () => void
}

interface QueueState {
  eventId: string | null
  position: QueuePosition | null
  isWaiting: boolean
  setQueuePosition: (position: QueuePosition) => void
  joinQueue: (eventId: string) => void
  leaveQueue: () => void
}

interface CartState {
  eventId: string | null
  event: Event | null
  selectedSeats: Seat[]
  reservation: Reservation | null
  addToCart: (event: Event, seats: Seat[]) => void
  setReservation: (reservation: Reservation) => void
  clearCart: () => void
}

interface AppState {
  user: UserState
  queue: QueueState
  cart: CartState
  darkMode: boolean
  toggleDarkMode: () => void
}

export const useStore = create<AppState>()(
  persist(
    (set) => ({
      user: {
        userId: null,
        name: null,
        email: null,
        isAuthenticated: false,
        setUser: (user) =>
          set((state) => ({
            user: { ...state.user, ...user, isAuthenticated: true },
          })),
        clearUser: () =>
          set((state) => ({
            user: {
              userId: null,
              name: null,
              email: null,
              isAuthenticated: false,
              setUser: state.user.setUser,
              clearUser: state.user.clearUser,
            },
          })),
      },
      queue: {
        eventId: null,
        position: null,
        isWaiting: false,
        setQueuePosition: (position) =>
          set((state) => ({
            queue: { ...state.queue, position },
          })),
        joinQueue: (eventId) =>
          set((state) => ({
            queue: { ...state.queue, eventId, isWaiting: true },
          })),
        leaveQueue: () =>
          set((state) => ({
            queue: {
              eventId: null,
              position: null,
              isWaiting: false,
              setQueuePosition: state.queue.setQueuePosition,
              joinQueue: state.queue.joinQueue,
              leaveQueue: state.queue.leaveQueue,
            },
          })),
      },
      cart: {
        eventId: null,
        event: null,
        selectedSeats: [],
        reservation: null,
        addToCart: (event, seats) =>
          set((state) => ({
            cart: { ...state.cart, eventId: event.id, event, selectedSeats: seats },
          })),
        setReservation: (reservation) =>
          set((state) => ({
            cart: { ...state.cart, reservation },
          })),
        clearCart: () =>
          set((state) => ({
            cart: {
              eventId: null,
              event: null,
              selectedSeats: [],
              reservation: null,
              addToCart: state.cart.addToCart,
              setReservation: state.cart.setReservation,
              clearCart: state.cart.clearCart,
            },
          })),
      },
      darkMode: true,
      toggleDarkMode: () =>
        set((state) => ({ darkMode: !state.darkMode })),
    }),
    {
      name: 'ticketstorm-storage',
      partialize: (state) => ({
        user: { userId: state.user.userId, name: state.user.name, email: state.user.email, isAuthenticated: state.user.isAuthenticated },
        darkMode: state.darkMode,
      }),
    }
  )
)

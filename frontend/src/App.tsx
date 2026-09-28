import * as React from 'react'
import { Routes, Route } from 'react-router-dom'
import { Header } from '@/components/layout/Header'
import { ChatPanel } from '@/features/chat/ChatPanel'

const EventListPage = React.lazy(() =>
  import('@/features/events/EventListPage').then((m) => ({ default: m.EventListPage }))
)
const EventDetailPage = React.lazy(() =>
  import('@/features/events/EventDetailPage').then((m) => ({ default: m.EventDetailPage }))
)
const QueuePage = React.lazy(() =>
  import('@/features/queue/QueuePage').then((m) => ({ default: m.QueuePage }))
)
const CheckoutPage = React.lazy(() =>
  import('@/features/checkout/CheckoutPage').then((m) => ({ default: m.CheckoutPage }))
)
const AdminDashboard = React.lazy(() =>
  import('@/features/admin/AdminDashboard').then((m) => ({ default: m.AdminDashboard }))
)

function LoadingFallback() {
  return (
    <div className="flex h-screen items-center justify-center">
      <div className="flex flex-col items-center gap-4">
        <div className="h-12 w-12 animate-spin rounded-full border-4 border-aws-orange border-t-transparent" />
        <p className="text-muted-foreground">Loading...</p>
      </div>
    </div>
  )
}

export function App() {
  return (
    <div className="min-h-screen bg-background text-foreground">
      <Header />
      <React.Suspense fallback={<LoadingFallback />}>
        <Routes>
          <Route path="/" element={<EventListPage />} />
          <Route path="/events/:id" element={<EventDetailPage />} />
          <Route path="/queue/:eventId" element={<QueuePage />} />
          <Route path="/checkout/:reservationId" element={<CheckoutPage />} />
          <Route path="/admin" element={<AdminDashboard />} />
        </Routes>
      </React.Suspense>
      <ChatPanel />
    </div>
  )
}

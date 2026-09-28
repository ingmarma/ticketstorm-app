import * as React from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import confetti from 'canvas-confetti'
import { paymentApi, reservationApi } from '@/services/api'
import { PageContainer } from '@/components/layout/PageContainer'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card'
import { CountdownTimer } from '@/components/shared/CountdownTimer'
import { useStore } from '@/store/useStore'
import toast from 'react-hot-toast'

export function CheckoutPage() {
  const { reservationId } = useParams<{ reservationId: string }>()
  const navigate = useNavigate()
  const { cart, clearCart } = useStore((state) => state)
  const [paymentComplete, setPaymentComplete] = React.useState(false)

  const paymentMutation = useMutation({
    mutationFn: (paymentMethod: string) =>
      paymentApi.processPayment(reservationId!, paymentMethod),
    onSuccess: () => {
      setPaymentComplete(true)
      clearCart()
      confetti({
        particleCount: 100,
        spread: 70,
        origin: { y: 0.6 },
        colors: ['#FF9900', '#0D1B2A', '#22C55E'],
      })
      toast.success('Payment successful!')
    },
    onError: (error) => {
      toast.error('Payment failed. Please try again.')
    },
  })

  const handlePayment = () => {
    paymentMutation.mutate('CREDIT_CARD')
  }

  const handleExpire = () => {
    toast.error('Your reservation has expired')
    navigate(`/events/${cart.eventId}`)
  }

  if (paymentComplete) {
    return (
      <PageContainer>
        <div className="mx-auto max-w-lg text-center">
          <Card>
            <CardContent className="p-8">
              <div className="mb-6 flex justify-center">
                <div className="flex h-20 w-20 items-center justify-center rounded-full bg-success/20">
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="40"
                    height="40"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    className="text-success"
                  >
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                    <polyline points="22 4 12 14.01 9 11.01" />
                  </svg>
                </div>
              </div>
              <h2 className="mb-2 text-2xl font-bold">Payment Successful!</h2>
              <p className="mb-6 text-muted-foreground">
                Your tickets have been confirmed. Check your email for details.
              </p>
              <div className="mb-6 rounded-lg bg-secondary p-4 text-left">
                <p className="text-sm text-muted-foreground">Reservation ID</p>
                <p className="font-mono font-medium">{reservationId}</p>
              </div>
              <Button onClick={() => navigate('/')} className="w-full">
                Browse More Events
              </Button>
            </CardContent>
          </Card>
        </div>
      </PageContainer>
    )
  }

  return (
    <PageContainer>
      <div className="mx-auto max-w-2xl">
        <div className="mb-8 text-center">
          <h1 className="mb-2 text-3xl font-bold">Checkout</h1>
          <p className="text-muted-foreground">
            Complete your purchase within the time limit
          </p>
        </div>

        <div className="grid gap-6 md:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle>Reservation Timer</CardTitle>
            </CardHeader>
            <CardContent>
              <CountdownTimer expiresAt={cart.reservation?.expiresAt || new Date(Date.now() + 600000).toISOString()} onExpire={handleExpire} />
              <p className="mt-4 text-center text-sm text-muted-foreground">
                Your seats are reserved for 10 minutes
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Order Summary</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {cart.event && (
                <div className="rounded-lg bg-secondary p-3">
                  <h3 className="font-medium">{cart.event.name}</h3>
                  <p className="text-sm text-muted-foreground">
                    {cart.event.venue} • {cart.event.city}
                  </p>
                </div>
              )}

              {cart.selectedSeats.length > 0 && (
                <div className="space-y-2">
                  {cart.selectedSeats.map((seat) => (
                    <div key={seat.id} className="flex justify-between text-sm">
                      <span>
                        {seat.section} - {seat.row}{seat.number}
                      </span>
                      <span className="font-medium">${seat.price}</span>
                    </div>
                  ))}
                </div>
              )}

              <div className="border-t border-border pt-4">
                <div className="flex justify-between font-medium">
                  <span>Total</span>
                  <span className="text-aws-orange">
                    ${cart.selectedSeats.reduce((sum, s) => sum + s.price, 0)}
                  </span>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="mt-6">
          <Card>
            <CardContent className="p-6">
              <h3 className="mb-4 font-medium">Payment Method</h3>
              <div className="mb-4 grid grid-cols-3 gap-2">
                <Button variant="secondary" className="justify-center">
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="20"
                    height="20"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    className="mr-2"
                  >
                    <rect width="20" height="14" x="2" y="5" rx="2" />
                    <line x1="2" x2="22" y1="10" y2="10" />
                  </svg>
                  Card
                </Button>
                <Button variant="secondary" className="justify-center" disabled>
                  PayPal
                </Button>
                <Button variant="secondary" className="justify-center" disabled>
                  Apple Pay
                </Button>
              </div>

              <Button
                onClick={handlePayment}
                className="w-full"
                size="lg"
                disabled={paymentMutation.isPending}
              >
                {paymentMutation.isPending ? (
                  <>
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
                      className="mr-2 animate-spin"
                    >
                      <path d="M21 12a9 9 0 1 1-6.219-8.56" />
                    </svg>
                    Processing...
                  </>
                ) : (
                  `Pay $${cart.selectedSeats.reduce((sum, s) => sum + s.price, 0)}`
                )}
              </Button>

              {paymentMutation.isError && (
                <p className="mt-2 text-center text-sm text-danger">
                  Payment failed. Please try again.
                </p>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </PageContainer>
  )
}

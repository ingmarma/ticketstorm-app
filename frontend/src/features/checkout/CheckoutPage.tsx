import * as React from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import confetti from 'canvas-confetti'
import { PageContainer } from '@/components/layout/PageContainer'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card'
import { CountdownTimer } from '@/components/shared/CountdownTimer'
import { useStore } from '@/store/useStore'
import { formatGuarani } from '@/utils/format'
import toast from 'react-hot-toast'

interface OrderDetails {
  eventName: string
  section: string
  quantity: number
  total: number
}

interface Receipt extends OrderDetails {
  code: string
}

const DEFAULT_TOTAL = 300000
const PAYMENT_DELAY_MS = 2000

const PAYMENT_METHODS = [
  {
    id: 'CREDIT_CARD',
    label: 'Card',
    icon: (
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
    ),
  },
  { id: 'PAYPAL', label: 'PayPal' },
  { id: 'APPLE_PAY', label: 'Apple Pay' },
] as const

function generateConfirmationCode(): string {
  const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  let suffix = ''
  for (let i = 0; i < 5; i++) {
    suffix += alphabet.charAt(Math.floor(Math.random() * alphabet.length))
  }
  return `TS-2026-${suffix}`
}

function positiveParam(params: URLSearchParams, key: string): number | null {
  const value = Number(params.get(key))
  return Number.isFinite(value) && value > 0 ? value : null
}

export function CheckoutPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const cart = useStore((state) => state.cart)
  const clearCart = useStore((state) => state.cart.clearCart)
  const [paymentMethod, setPaymentMethod] = React.useState<string>('CREDIT_CARD')
  const [stage, setStage] = React.useState<'idle' | 'processing' | 'success'>('idle')
  const [receipt, setReceipt] = React.useState<Receipt | null>(null)
  const paymentTimerRef = React.useRef<number | null>(null)

  const order = React.useMemo<OrderDetails>(() => {
    const paramTotal = positiveParam(searchParams, 'price')
    const paramQuantity = positiveParam(searchParams, 'quantity')
    const paramSection = searchParams.get('section')
    const paramEvent = searchParams.get('event')
    const seatsTotal = cart.selectedSeats.reduce(
      (sum, seat) => sum + (seat.price || 0),
      0
    )

    return {
      eventName: paramEvent || cart.event?.name || 'Entradas TicketStorm',
      section:
        paramSection || cart.selectedSeats[0]?.section || 'General',
      quantity: paramQuantity || (cart.selectedSeats.length > 0 ? cart.selectedSeats.length : 1),
      total: paramTotal || (seatsTotal > 0 ? seatsTotal : DEFAULT_TOTAL),
    }
  }, [searchParams, cart.event, cart.selectedSeats])

  const fallbackExpiresAt = React.useMemo(
    () => cart.reservation?.expiresAt || new Date(Date.now() + 600000).toISOString(),
    [cart.reservation?.expiresAt]
  )

  React.useEffect(
    () => () => {
      if (paymentTimerRef.current) {
        window.clearTimeout(paymentTimerRef.current)
      }
    },
    []
  )

  const handlePayment = () => {
    if (stage !== 'idle') return
    setStage('processing')
    paymentTimerRef.current = window.setTimeout(() => {
      setReceipt({ ...order, code: generateConfirmationCode() })
      setStage('success')
      clearCart()
      confetti({
        particleCount: 100,
        spread: 70,
        origin: { y: 0.6 },
        colors: ['#FF9900', '#0D1B2A', '#22C55E'],
      })
      toast.success('¡Pago exitoso!')
    }, PAYMENT_DELAY_MS)
  }

  const handleExpire = React.useCallback(() => {
    toast.error('Tu reserva ha expirado')
    navigate(cart.eventId ? `/events/${cart.eventId}` : '/')
  }, [cart.eventId, navigate])

  if (stage === 'processing') {
    return (
      <PageContainer>
        <div className="mx-auto max-w-md text-center">
          <Card>
            <CardContent className="p-10">
              <div className="mb-6 flex justify-center">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="48"
                  height="48"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  className="animate-spin text-aws-orange"
                >
                  <path d="M21 12a9 9 0 1 1-6.219-8.56" />
                </svg>
              </div>
              <h2 className="mb-2 text-xl font-bold">Procesando tu pago…</h2>
              <p className="text-sm text-muted-foreground">
                Estamos confirmando tu compra, esto toma solo un momento
              </p>
            </CardContent>
          </Card>
        </div>
      </PageContainer>
    )
  }

  if (stage === 'success' && receipt) {
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
              <h2 className="mb-2 text-2xl font-bold">¡Pago Exitoso!</h2>
              <p className="mb-6 text-muted-foreground">Tu compra ha sido confirmada</p>

              <div className="mb-4 space-y-3 rounded-lg bg-secondary p-4 text-left">
                <div className="flex justify-between gap-4 text-sm">
                  <span className="text-muted-foreground">Evento</span>
                  <span className="font-medium">{receipt.eventName}</span>
                </div>
                <div className="flex justify-between gap-4 text-sm">
                  <span className="text-muted-foreground">Sección</span>
                  <span className="font-medium">{receipt.section}</span>
                </div>
                <div className="flex justify-between gap-4 text-sm">
                  <span className="text-muted-foreground">Cantidad</span>
                  <span className="font-medium">{receipt.quantity}</span>
                </div>
                <div className="flex justify-between gap-4 border-t border-border pt-3 text-sm">
                  <span className="text-muted-foreground">Total</span>
                  <span className="font-bold text-aws-orange">
                    {formatGuarani(receipt.total)}
                  </span>
                </div>
              </div>

              <div className="mb-6 rounded-lg border border-dashed border-border p-4">
                <p className="text-xs uppercase tracking-wide text-muted-foreground">
                  Código de confirmación
                </p>
                <p className="font-mono text-lg font-bold">{receipt.code}</p>
              </div>

              <div className="space-y-2">
                <Button
                  variant="secondary"
                  className="w-full"
                  onClick={() => toast('La descarga estará disponible en la versión completa')}
                >
                  Descargar Entrada
                </Button>
                <Button className="w-full" onClick={() => navigate('/')}>
                  Volver a Eventos
                </Button>
              </div>
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
              <CountdownTimer expiresAt={fallbackExpiresAt} onExpire={handleExpire} />
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
              <div className="rounded-lg bg-secondary p-3">
                <h3 className="font-medium">{order.eventName}</h3>
                <p className="text-sm text-muted-foreground">
                  {cart.event ? `${cart.event.venue} • ${cart.event.city}` : 'TicketStorm · Paraguay'}
                </p>
              </div>

              {cart.selectedSeats.length > 0 ? (
                <div className="space-y-2">
                  {cart.selectedSeats.map((seat) => (
                    <div key={seat.id} className="flex justify-between text-sm">
                      <span>
                        {seat.section} - {seat.row}
                        {seat.number}
                      </span>
                      <span className="font-medium">{formatGuarani(seat.price)}</span>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="flex justify-between text-sm">
                  <span>
                    {order.section} × {order.quantity}
                  </span>
                  <span className="font-medium">{formatGuarani(order.total)}</span>
                </div>
              )}

              <div className="border-t border-border pt-4">
                <div className="flex justify-between font-medium">
                  <span>Total</span>
                  <span className="text-aws-orange">{formatGuarani(order.total)}</span>
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
                {PAYMENT_METHODS.map((method) => (
                  <Button
                    key={method.id}
                    type="button"
                    variant={paymentMethod === method.id ? 'primary' : 'secondary'}
                    className="justify-center"
                    onClick={() => setPaymentMethod(method.id)}
                  >
                    {'icon' in method ? method.icon : null}
                    {method.label}
                  </Button>
                ))}
              </div>

              <Button onClick={handlePayment} className="w-full" size="lg">
                {`Pagar ${formatGuarani(order.total)}`}
              </Button>

              <p className="mt-2 text-center text-xs text-muted-foreground">
                Pago simulado para la demo · sin cobro real
              </p>
            </CardContent>
          </Card>
        </div>
      </div>
    </PageContainer>
  )
}

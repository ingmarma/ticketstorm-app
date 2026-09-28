import * as React from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useEvent } from '@/hooks/useEventSearch'
import { useQueue } from '@/hooks/useQueue'
import { PageContainer } from '@/components/layout/PageContainer'
import { Button } from '@/components/ui/Button'
import { Badge } from '@/components/ui/Badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card'
import { Skeleton } from '@/components/ui/Skeleton'
import { useStore } from '@/store/useStore'
import { formatGuarani, formatDateTime, availabilityColor, availabilityLabel } from '@/utils/format'
import type { TicketSection } from '@/types/api'

export function EventDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { addToCart } = useStore((state) => state.cart)
  const [selectedSection, setSelectedSection] = React.useState<TicketSection | null>(null)
  const [quantity, setQuantity] = React.useState(1)

  const { data: event, isLoading: isEventLoading } = useEvent(id!)
  const { isWaiting } = useQueue(id!)

  const handleBuy = () => {
    if (!selectedSection || !event) return
    addToCart(event, [])
    const params = new URLSearchParams({
      event: event.name,
      section: selectedSection.name,
      quantity: String(quantity),
      price: String(selectedSection.price * quantity),
    })
    navigate(`/checkout/${event.id}?${params.toString()}`)
  }

  const handleJoinQueue = () => {
    window.dispatchEvent(
      new CustomEvent('open-chat', {
        detail: { message: `Quiero comprar entradas para ${event?.name ?? ''}`.trim() },
      })
    )
  }

  const sortedSections = React.useMemo(() => {
    if (!event?.ticketSections) return []
    return [...event.ticketSections].sort((a, b) => a.sortOrder - b.sortOrder)
  }, [event?.ticketSections])

  if (isEventLoading) {
    return (
      <PageContainer>
        <div className="grid gap-8 lg:grid-cols-3">
          <div className="lg:col-span-2 space-y-4">
            <Skeleton className="h-64 w-full rounded-xl" />
            <Skeleton className="h-8 w-3/4" />
            <Skeleton className="h-4 w-1/2" />
          </div>
          <div className="space-y-4">
            <Skeleton className="h-48 w-full rounded-xl" />
            <Skeleton className="h-12 w-full" />
          </div>
        </div>
      </PageContainer>
    )
  }

  if (!event) {
    return (
      <PageContainer>
        <div className="flex flex-col items-center justify-center py-16">
          <h2 className="text-2xl font-bold">Evento no encontrado</h2>
          <Button onClick={() => navigate('/')} className="mt-4">
            Volver a Eventos
          </Button>
        </div>
      </PageContainer>
    )
  }

  const totalAvailable = sortedSections.reduce((sum, s) => sum + s.availableCapacity, 0)
  const totalCapacity = sortedSections.reduce((sum, s) => sum + s.totalCapacity, 0)
  const availabilityPercentage = totalCapacity > 0 ? (totalAvailable / totalCapacity) * 100 : 0

  return (
    <PageContainer>
      <div className="mb-6">
        <Button variant="ghost" onClick={() => navigate(-1)} className="mb-4">
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
            className="mr-2"
          >
            <path d="m15 18-6-6 6-6" />
          </svg>
          Volver
        </Button>
      </div>

      <div className="grid gap-8 lg:grid-cols-3">
        <div className="lg:col-span-2 space-y-6">
          <div className="relative h-64 overflow-hidden rounded-2xl bg-gradient-to-br from-navy-700 to-navy-900 md:h-80">
            {event.imageUrl ? (
              <img
                src={event.imageUrl}
                alt={event.name}
                className="h-full w-full object-cover"
              />
            ) : (
              <div className="flex h-full items-center justify-center">
                <span className="text-6xl font-bold text-navy-500">
                  {event.name.charAt(0)}
                </span>
              </div>
            )}
            <div className="absolute top-4 left-4">
              <Badge className="bg-navy-900/80 text-white backdrop-blur">
                {event.category}
              </Badge>
            </div>
          </div>

          <div>
            <h1 className="mb-2 text-3xl font-bold">{event.name}</h1>
            <p className="text-lg text-muted-foreground">{event.description}</p>
          </div>

          <div className="flex flex-wrap gap-4 text-sm">
            <div className="flex items-center gap-2 rounded-lg bg-secondary px-4 py-2">
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
              >
                <rect width="18" height="18" x="3" y="4" rx="2" ry="2" />
                <line x1="16" x2="16" y1="2" y2="6" />
                <line x1="8" x2="8" y1="2" y2="6" />
                <line x1="3" x2="21" y1="10" y2="10" />
              </svg>
              {formatDateTime(event.eventDate)}
            </div>
            <div className="flex items-center gap-2 rounded-lg bg-secondary px-4 py-2">
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
              >
                <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z" />
                <circle cx="12" cy="10" r="3" />
              </svg>
              {event.venue}, {event.city}
            </div>
          </div>

          <Card>
            <CardHeader>
              <CardTitle>Entradas Disponibles</CardTitle>
            </CardHeader>
            <CardContent>
              {sortedSections.length > 0 ? (
                <div className="space-y-3">
                  {sortedSections.map((section) => {
                    const sectionPercent = section.totalCapacity > 0
                      ? (section.availableCapacity / section.totalCapacity) * 100
                      : 0
                    const isSelected = selectedSection?.id === section.id
                    const isSoldOut = section.availableCapacity <= 0

                    return (
                      <div
                        key={section.id}
                        onClick={() => !isSoldOut && setSelectedSection(section)}
                        className={`relative flex items-center justify-between rounded-xl border p-4 transition-all ${
                          isSoldOut
                            ? 'cursor-not-allowed opacity-50 border-border bg-muted/30'
                            : isSelected
                            ? 'cursor-pointer border-aws-orange bg-aws-orange/5 ring-1 ring-aws-orange'
                            : 'cursor-pointer border-border hover:border-aws-orange/50 hover:bg-muted/30'
                        }`}
                      >
                        <div className="flex-1">
                          <div className="flex items-center gap-2">
                            <span className="font-semibold">{section.name}</span>
                            {sectionPercent <= 10 && sectionPercent > 0 && (
                              <Badge variant="limited" className="text-xs">¡Últimas!</Badge>
                            )}
                            {isSoldOut && (
                              <Badge variant="sold-out" className="text-xs">Agotado</Badge>
                            )}
                          </div>
                          {section.description && (
                            <p className="mt-1 text-sm text-muted-foreground">{section.description}</p>
                          )}
                          <div className="mt-2 flex items-center gap-3 text-xs text-muted-foreground">
                            <span>{section.availableCapacity} / {section.totalCapacity} disponibles</span>
                            <div className="h-1.5 w-20 overflow-hidden rounded-full bg-muted">
                              <div
                                className={`h-full rounded-full ${availabilityColor(sectionPercent)}`}
                                style={{ width: `${sectionPercent}%` }}
                              />
                            </div>
                          </div>
                        </div>
                        <div className="ml-4 text-right">
                          <div className="text-lg font-bold text-aws-orange">
                            {formatGuarani(section.price)}
                          </div>
                          <div className="text-xs text-muted-foreground">por entrada</div>
                        </div>
                      </div>
                    )
                  })}
                </div>
              ) : (
                <p className="text-center text-muted-foreground">
                  No hay secciones disponibles
                </p>
              )}
            </CardContent>
          </Card>
        </div>

        <div className="space-y-6">
          <Card className="sticky top-24">
            <CardHeader>
              <CardTitle>Resumen</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Disponibilidad</span>
                  <span className="font-medium">
                    {totalAvailable} / {totalCapacity} entradas
                  </span>
                </div>
              </div>

              <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
                <div
                  className={`h-full rounded-full transition-all ${availabilityColor(availabilityPercentage)}`}
                  style={{ width: `${availabilityPercentage}%` }}
                />
              </div>

              <p className="text-xs text-center text-muted-foreground">
                {availabilityLabel(availabilityPercentage)}
              </p>

              {selectedSection && (
                <div className="space-y-2 rounded-lg bg-secondary p-3">
                  <div className="flex justify-between text-sm">
                    <span className="font-medium">Sección</span>
                    <span>{selectedSection.name}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Precio</span>
                    <span className="font-medium">{formatGuarani(selectedSection.price)}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm text-muted-foreground">Cantidad:</span>
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => setQuantity(Math.max(1, quantity - 1))}
                        className="h-7 w-7 rounded border border-border flex items-center justify-center hover:bg-muted"
                      >
                        -
                      </button>
                      <span className="w-8 text-center text-sm font-medium">{quantity}</span>
                      <button
                        onClick={() => setQuantity(Math.min(10, quantity + 1))}
                        className="h-7 w-7 rounded border border-border flex items-center justify-center hover:bg-muted"
                      >
                        +
                      </button>
                    </div>
                  </div>
                  <div className="border-t border-border pt-2">
                    <div className="flex justify-between font-medium">
                      <span>Total</span>
                      <span className="text-aws-orange">
                        {formatGuarani(selectedSection.price * quantity)}
                      </span>
                    </div>
                  </div>
                </div>
              )}

              {totalAvailable <= 0 ? (
                <Button disabled className="w-full">
                  Agotado
                </Button>
              ) : isWaiting ? (
                <Button disabled className="w-full">
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
                  En cola...
                </Button>
              ) : selectedSection ? (
                <Button onClick={handleBuy} className="w-full" size="lg">
                  Comprar Ahora - {formatGuarani(selectedSection.price * quantity)}
                </Button>
              ) : (
                <Button onClick={handleJoinQueue} variant="secondary" className="w-full" size="lg">
                  Comprar con AI Assistant
                </Button>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </PageContainer>
  )
}

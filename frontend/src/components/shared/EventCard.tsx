import * as React from 'react'
import { Link } from 'react-router-dom'
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import type { Event } from '@/types/api'
import { cn } from '@/utils/cn'
import { formatGuarani, formatDate } from '@/utils/format'

interface EventCardProps {
  event: Event
  className?: string
}

const categoryColors: Record<string, string> = {
  CONCERT: 'bg-purple-500/20 text-purple-400',
  SPORTS: 'bg-blue-500/20 text-blue-400',
  THEATER: 'bg-pink-500/20 text-pink-400',
  COMEDY: 'bg-yellow-500/20 text-yellow-400',
  CONFERENCE: 'bg-cyan-500/20 text-cyan-400',
  FESTIVAL: 'bg-orange-500/20 text-orange-400',
}

export function EventCard({ event, className }: EventCardProps) {
  const availabilityPercentage = (event.availableSeats / event.totalSeats) * 100

  const getAvailabilityBadge = () => {
    if (availabilityPercentage <= 0) return <Badge variant="sold-out">Agotado</Badge>
    if (availabilityPercentage <= 10) return <Badge variant="limited">Últimas entradas</Badge>
    if (availabilityPercentage <= 30) return <Badge variant="limited">¡Se agota!</Badge>
    return <Badge variant="available">Disponible</Badge>
  }

  const getMinPrice = () => {
    if (event.ticketSections && event.ticketSections.length > 0) {
      const min = Math.min(...event.ticketSections.map(s => s.price))
      return min
    }
    return event.minPrice
  }

  return (
    <Card className={cn('group overflow-hidden transition-all duration-300 hover:shadow-lg hover:shadow-aws-orange/10', className)}>
      <div className="relative h-48 bg-gradient-to-br from-navy-700 to-navy-900 overflow-hidden">
        {event.imageUrl ? (
          <img
            src={event.imageUrl}
            alt={event.name}
            className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
          />
        ) : (
          <div className="flex h-full items-center justify-center">
            <span className="text-4xl font-bold text-navy-500">
              {event.name.charAt(0)}
            </span>
          </div>
        )}
        <div className="absolute top-3 right-3">
          <Badge className={categoryColors[event.category] || 'bg-secondary text-secondary-foreground'}>
            {event.category}
          </Badge>
        </div>
        <div className="absolute top-3 left-3">
          {getAvailabilityBadge()}
        </div>
      </div>

      <CardHeader className="pb-2">
        <CardTitle className="line-clamp-1 text-lg">{event.name}</CardTitle>
        <p className="text-sm text-muted-foreground">{event.venue}</p>
      </CardHeader>

      <CardContent className="pb-2">
        <div className="flex items-center text-sm text-muted-foreground">
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
            <rect width="18" height="18" x="3" y="4" rx="2" ry="2" />
            <line x1="16" x2="16" y1="2" y2="6" />
            <line x1="8" x2="8" y1="2" y2="6" />
            <line x1="3" x2="21" y1="10" y2="10" />
          </svg>
          {formatDate(event.eventDate)}
        </div>
        <div className="mt-2 flex items-center text-sm text-muted-foreground">
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
            <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z" />
            <circle cx="12" cy="10" r="3" />
          </svg>
          {event.city}
        </div>
      </CardContent>

      <CardFooter className="flex items-center justify-between pt-2">
        <div>
          <span className="text-sm text-muted-foreground">Desde </span>
          <span className="text-2xl font-bold text-aws-orange">
            {formatGuarani(getMinPrice())}
          </span>
        </div>
        <Button asChild size="sm">
          <Link to={`/events/${event.id}`}>Ver Detalles</Link>
        </Button>
      </CardFooter>
    </Card>
  )
}

import * as React from 'react'
import { cn } from '@/utils/cn'
import type { Seat } from '@/types/api'

interface SeatMapProps {
  seats: Seat[]
  selectedSeats: Seat[]
  onSeatSelect: (seat: Seat) => void
  className?: string
}

const sectionColors: Record<string, string> = {
  VIP: 'bg-aws-orange/30 hover:bg-aws-orange/50 border-aws-orange',
  PREMIUM: 'bg-purple-500/30 hover:bg-purple-500/50 border-purple-500',
  STANDARD: 'bg-blue-500/30 hover:bg-blue-500/50 border-blue-500',
  GENERAL_ADMISSION: 'bg-green-500/30 hover:bg-green-500/50 border-green-500',
}

const statusStyles: Record<string, string> = {
  AVAILABLE: 'cursor-pointer',
  RESERVED: 'opacity-50 cursor-not-allowed bg-gray-500/30',
  SOLD: 'opacity-30 cursor-not-allowed bg-gray-700/50',
  HELD: 'opacity-50 cursor-not-allowed bg-yellow-500/30',
}

export function SeatMap({ seats, selectedSeats, onSeatSelect, className }: SeatMapProps) {
  const sections = React.useMemo(() => {
    const grouped = seats.reduce((acc, seat) => {
      const bucket = acc[seat.section]
      if (bucket) {
        bucket.push(seat)
      } else {
        acc[seat.section] = [seat]
      }
      return acc
    }, {} as Record<string, Seat[]>)

    return Object.entries(grouped).sort(([a], [b]) => a.localeCompare(b))
  }, [seats])

  const isSelected = (seat: Seat) =>
    selectedSeats.some((s) => s.id === seat.id)

  return (
    <div className={cn('space-y-6', className)}>
      <div className="flex flex-wrap gap-4 text-sm">
        <div className="flex items-center gap-2">
          <div className="h-4 w-4 rounded bg-aws-orange/30 border border-aws-orange" />
          <span>VIP</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="h-4 w-4 rounded bg-purple-500/30 border border-purple-500" />
          <span>Premium</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="h-4 w-4 rounded bg-blue-500/30 border border-blue-500" />
          <span>Standard</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="h-4 w-4 rounded bg-green-500/30 border border-green-500" />
          <span>General</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="h-4 w-4 rounded bg-gray-500/30 border border-gray-500" />
          <span>Unavailable</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="h-4 w-4 rounded bg-aws-orange" />
          <span>Selected</span>
        </div>
      </div>

      <div className="rounded-xl border border-border bg-card p-6">
        <div className="mb-6 text-center">
          <div className="mx-auto h-12 w-48 rounded-lg bg-gradient-to-b from-aws-orange/20 to-aws-orange/5 border border-aws-orange/30 flex items-center justify-center">
            <span className="text-xs font-medium text-aws-orange">STAGE</span>
          </div>
        </div>

        <div className="space-y-4">
          {sections.map(([sectionName, sectionSeats]) => (
            <div key={sectionName}>
              <h4 className="mb-2 text-sm font-medium text-muted-foreground">
                Section {sectionName}
              </h4>
              <div className="flex flex-wrap gap-2">
                {sectionSeats.map((seat) => (
                  <button
                    key={seat.id}
                    onClick={() => {
                      if (seat.status === 'AVAILABLE') {
                        onSeatSelect(seat)
                      }
                    }}
                    disabled={seat.status !== 'AVAILABLE'}
                    className={cn(
                      'h-10 w-10 rounded-lg border-2 transition-all duration-200 text-xs font-medium',
                      sectionColors[seat.type ?? ''] || sectionColors.STANDARD,
                      statusStyles[seat.status ?? ''],
                      isSelected(seat) && 'bg-aws-orange text-navy-900 border-aws-orange scale-110 ring-2 ring-aws-orange/50',
                      seat.wheelchairAccessible && 'ring-2 ring-blue-400'
                    )}
                    title={`${seat.section} ${seat.row}${seat.number} - $${seat.price}${seat.wheelchairAccessible ? ' (Wheelchair accessible)' : ''}`}
                  >
                    {seat.row}{seat.number}
                  </button>
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

import * as React from 'react'
import { cn } from '@/utils/cn'

interface CountdownTimerProps {
  expiresAt: string
  onExpire?: () => void
  className?: string
}

export function CountdownTimer({ expiresAt, onExpire, className }: CountdownTimerProps) {
  const [timeLeft, setTimeLeft] = React.useState(calculateTimeLeft(expiresAt))

  React.useEffect(() => {
    const timer = setInterval(() => {
      const newTimeLeft = calculateTimeLeft(expiresAt)
      setTimeLeft(newTimeLeft)

      if (newTimeLeft.total <= 0) {
        clearInterval(timer)
        onExpire?.()
      }
    }, 1000)

    return () => clearInterval(timer)
  }, [expiresAt, onExpire])

  const isUrgent = timeLeft.total < 120000

  return (
    <div className={cn('flex flex-col items-center', className)}>
      <p className="mb-2 text-sm text-muted-foreground">Time remaining</p>
      <div className="flex items-center gap-2">
        <TimeUnit value={timeLeft.minutes} label="min" isUrgent={isUrgent} />
        <span className={cn('text-2xl font-bold', isUrgent ? 'text-danger' : 'text-foreground')}>
          :
        </span>
        <TimeUnit value={timeLeft.seconds} label="sec" isUrgent={isUrgent} />
      </div>
      {isUrgent && (
        <p className="mt-2 text-sm font-medium text-danger animate-pulse">
          Hurry! Your reservation expires soon
        </p>
      )}
      <div className="mt-3 h-2 w-48 overflow-hidden rounded-full bg-muted">
        <div
          className={cn(
            'h-full rounded-full transition-all duration-1000',
            isUrgent ? 'bg-danger' : 'bg-aws-orange'
          )}
          style={{ width: `${(timeLeft.total / 600000) * 100}%` }}
        />
      </div>
    </div>
  )
}

function calculateTimeLeft(expiresAt: string) {
  const total = Math.max(0, new Date(expiresAt).getTime() - Date.now())
  const minutes = Math.floor(total / 60000)
  const seconds = Math.floor((total % 60000) / 1000)

  return { total, minutes, seconds }
}

function TimeUnit({
  value,
  label,
  isUrgent,
}: {
  value: number
  label: string
  isUrgent: boolean
}) {
  return (
    <div className="flex flex-col items-center">
      <div
        className={cn(
          'flex h-16 w-16 items-center justify-center rounded-xl border-2 font-mono text-3xl font-bold',
          isUrgent
            ? 'border-danger bg-danger/10 text-danger'
            : 'border-aws-orange bg-aws-orange/10 text-aws-orange'
        )}
      >
        {String(value).padStart(2, '0')}
      </div>
      <span className="mt-1 text-xs text-muted-foreground">{label}</span>
    </div>
  )
}

import { cn } from '@/utils/cn'
import type { QueuePosition } from '@/types/api'

interface QueueIndicatorProps {
  position: QueuePosition | null
  className?: string
}

export function QueueIndicator({ position, className }: QueueIndicatorProps) {
  if (!position) {
    return (
      <div className={cn('flex flex-col items-center justify-center p-8', className)}>
        <div className="h-32 w-32 animate-pulse rounded-full bg-muted flex items-center justify-center">
          <span className="text-4xl font-bold text-muted-foreground">...</span>
        </div>
        <p className="mt-4 text-muted-foreground">Connecting to queue...</p>
      </div>
    )
  }

  const progress = position.totalInQueue > 1
    ? ((position.totalInQueue - position.position) / (position.totalInQueue - 1)) * 100
    : 100

  return (
    <div className={cn('flex flex-col items-center justify-center p-8', className)}>
      <div className="relative">
        <div className="flex h-32 w-32 items-center justify-center rounded-full bg-gradient-to-br from-aws-orange/20 to-aws-orange/5 border-2 border-aws-orange">
          <span className="text-5xl font-bold text-aws-orange">{position.position}</span>
        </div>
        <div className="absolute -bottom-2 left-1/2 -translate-x-1/2 rounded-full bg-navy-900 px-3 py-1">
          <span className="text-xs font-medium text-aws-orange">
            of {position.totalInQueue}
          </span>
        </div>
      </div>

      <div className="mt-8 w-full max-w-xs">
        <div className="mb-2 flex justify-between text-sm">
          <span className="text-muted-foreground">Progress</span>
          <span className="font-medium text-aws-orange">{Math.round(progress)}%</span>
        </div>
        <div className="h-3 w-full overflow-hidden rounded-full bg-muted">
          <div
            className="h-full rounded-full bg-gradient-to-r from-aws-orange to-aws-orange-light transition-all duration-500 ease-out"
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>

      <div className="mt-6 text-center">
        <p className="text-sm text-muted-foreground">Estimated wait time</p>
        <p className="text-2xl font-semibold text-foreground">
          {position.estimatedWaitMinutes < 1
            ? 'Less than a minute'
            : `~${position.estimatedWaitMinutes} min`}
        </p>
      </div>

      {position.status === 'IN_PROGRESS' && (
        <div className="mt-4 animate-pulse rounded-lg bg-success/20 px-4 py-2">
          <p className="text-sm font-medium text-success">
            It's your turn! Redirecting...
          </p>
        </div>
      )}

      {position.status === 'WAITING' && (
        <p className="mt-4 text-sm text-muted-foreground">
          Please keep this page open. You'll be redirected when it's your turn.
        </p>
      )}
    </div>
  )
}

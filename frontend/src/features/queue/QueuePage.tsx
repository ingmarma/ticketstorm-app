import * as React from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQueue } from '@/hooks/useQueue'
import { PageContainer } from '@/components/layout/PageContainer'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card'
import { QueueIndicator } from '@/components/shared/QueueIndicator'
import { useStore } from '@/store/useStore'

export function QueuePage() {
  const { eventId } = useParams<{ eventId: string }>()
  const navigate = useNavigate()
  const { position, isWaiting, leaveQueue } = useQueue(eventId!)
  const { cart } = useStore((state) => state)

  React.useEffect(() => {
    if (position?.status === 'IN_PROGRESS') {
      setTimeout(() => {
        navigate(`/events/${eventId}`)
      }, 2000)
    }
  }, [position?.status, navigate, eventId])

  return (
    <PageContainer>
      <div className="mx-auto max-w-lg">
        <Card>
          <CardHeader className="text-center">
            <CardTitle>Virtual Queue</CardTitle>
            <p className="text-sm text-muted-foreground">
              You're in line for this event. Please wait while we process your request.
            </p>
          </CardHeader>
          <CardContent className="space-y-6">
            <QueueIndicator position={position} />

            <div className="flex gap-4">
              <Button
                variant="secondary"
                onClick={() => navigate(`/events/${eventId}`)}
                className="flex-1"
              >
                View Event
              </Button>
              <Button
                variant="danger"
                onClick={() => {
                  leaveQueue()
                  navigate(`/events/${eventId}`)
                }}
                className="flex-1"
              >
                Leave Queue
              </Button>
            </div>

            <div className="rounded-lg bg-secondary p-4 text-sm">
              <h4 className="mb-2 font-medium">Queue Tips</h4>
              <ul className="space-y-1 text-muted-foreground">
                <li>• Keep this page open in your browser</li>
                <li>• Don't refresh the page</li>
                <li>• You'll be redirected when it's your turn</li>
                <li>• Average wait time is based on current queue speed</li>
              </ul>
            </div>
          </CardContent>
        </Card>
      </div>
    </PageContainer>
  )
}

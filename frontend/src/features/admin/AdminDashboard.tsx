import * as React from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { adminApi } from '@/services/api'
import { PageContainer } from '@/components/layout/PageContainer'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { Badge } from '@/components/ui/Badge'
import { Skeleton } from '@/components/ui/Skeleton'
import type { CircuitBreakerState, FraudCheck } from '@/types/api'
import toast from 'react-hot-toast'

const statusColors: Record<string, string> = {
  HEALTHY: 'bg-success/20 text-success',
  DEGRADED: 'bg-warning/20 text-warning',
  DOWN: 'bg-danger/20 text-danger',
}

const circuitStateColors: Record<string, string> = {
  CLOSED: 'bg-success/20 text-success',
  OPEN: 'bg-danger/20 text-danger',
  HALF_OPEN: 'bg-warning/20 text-warning',
}

export function AdminDashboard() {
  const queryClient = useQueryClient()

  const { data: metrics, isLoading: isMetricsLoading } = useQuery({
    queryKey: ['admin-metrics'],
    queryFn: adminApi.getMetrics,
    refetchInterval: 30000,
  })

  const { data: fraudAlerts, isLoading: isFraudLoading } = useQuery({
    queryKey: ['admin-fraud-alerts'],
    queryFn: adminApi.getFraudAlerts,
    refetchInterval: 10000,
  })

  const { data: circuitBreakers, isLoading: isCircuitLoading } = useQuery({
    queryKey: ['admin-circuit-breakers'],
    queryFn: adminApi.getCircuitBreakers,
    refetchInterval: 5000,
  })

  const resetMutation = useMutation({
    mutationFn: adminApi.resetCircuitBreaker,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-circuit-breakers'] })
      toast.success('Circuit breaker reset successfully')
    },
    onError: () => {
      toast.error('Failed to reset circuit breaker')
    },
  })

  return (
    <PageContainer>
      <div className="mb-8">
        <h1 className="mb-2 text-3xl font-bold">Admin Dashboard</h1>
        <p className="text-muted-foreground">Monitor system health and performance</p>
      </div>

      <div className="mb-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {isMetricsLoading ? (
          Array.from({ length: 4 }).map((_, i) => (
            <Card key={i}>
              <CardContent className="p-6">
                <Skeleton className="h-4 w-24 mb-2" />
                <Skeleton className="h-8 w-16" />
              </CardContent>
            </Card>
          ))
        ) : metrics ? (
          <>
            <Card>
              <CardContent className="p-6">
                <p className="text-sm text-muted-foreground">Total Events</p>
                <p className="text-3xl font-bold">{metrics.totalEvents}</p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="p-6">
                <p className="text-sm text-muted-foreground">Reservations</p>
                <p className="text-3xl font-bold">{metrics.totalReservations}</p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="p-6">
                <p className="text-sm text-muted-foreground">Revenue</p>
                <p className="text-3xl font-bold text-aws-orange">
                  ${metrics.totalRevenue.toLocaleString()}
                </p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="p-6">
                <p className="text-sm text-muted-foreground">Active Queues</p>
                <p className="text-3xl font-bold">{metrics.activeQueues}</p>
              </CardContent>
            </Card>
          </>
        ) : null}
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>System Health</CardTitle>
          </CardHeader>
          <CardContent>
            {isMetricsLoading ? (
              <div className="space-y-3">
                {Array.from({ length: 5 }).map((_, i) => (
                  <Skeleton key={i} className="h-10 w-full" />
                ))}
              </div>
            ) : metrics?.systemHealth ? (
              <div className="space-y-3">
                {Object.entries(metrics.systemHealth).map(([service, status]) => (
                  <div
                    key={service}
                    className="flex items-center justify-between rounded-lg border border-border p-3"
                  >
                    <span className="font-medium capitalize">
                      {service.replace(/([A-Z])/g, ' $1').trim()}
                    </span>
                    <Badge className={statusColors[status]}>
                      {status}
                    </Badge>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-muted-foreground">No health data available</p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Circuit Breakers</CardTitle>
          </CardHeader>
          <CardContent>
            {isCircuitLoading ? (
              <div className="space-y-3">
                {Array.from({ length: 3 }).map((_, i) => (
                  <Skeleton key={i} className="h-16 w-full" />
                ))}
              </div>
            ) : circuitBreakers && circuitBreakers.length > 0 ? (
              <div className="space-y-3">
                {circuitBreakers.map((cb: CircuitBreakerState) => (
                  <div
                    key={cb.serviceName}
                    className="flex items-center justify-between rounded-lg border border-border p-3"
                  >
                    <div>
                      <p className="font-medium">{cb.serviceName}</p>
                      <p className="text-xs text-muted-foreground">
                        Failures: {cb.failureCount}
                      </p>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge className={circuitStateColors[cb.state]}>
                        {cb.state}
                      </Badge>
                      {cb.state !== 'CLOSED' && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => resetMutation.mutate(cb.serviceName)}
                          disabled={resetMutation.isPending}
                        >
                          Reset
                        </Button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-muted-foreground">All circuit breakers healthy</p>
            )}
          </CardContent>
        </Card>
      </div>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle>Recent Fraud Alerts</CardTitle>
        </CardHeader>
        <CardContent>
          {isFraudLoading ? (
            <Skeleton className="h-48 w-full" />
          ) : fraudAlerts && fraudAlerts.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border">
                    <th className="pb-3 text-left font-medium text-muted-foreground">
                      User ID
                    </th>
                    <th className="pb-3 text-left font-medium text-muted-foreground">
                      Event ID
                    </th>
                    <th className="pb-3 text-left font-medium text-muted-foreground">
                      Risk Score
                    </th>
                    <th className="pb-3 text-left font-medium text-muted-foreground">
                      Flags
                    </th>
                    <th className="pb-3 text-left font-medium text-muted-foreground">
                      Status
                    </th>
                    <th className="pb-3 text-left font-medium text-muted-foreground">
                      Time
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {fraudAlerts.slice(0, 10).map((alert: FraudCheck) => (
                    <tr key={alert.id}>
                      <td className="py-3 font-mono text-xs">{alert.userId.slice(0, 8)}...</td>
                      <td className="py-3 font-mono text-xs">{alert.eventId.slice(0, 8)}...</td>
                      <td className="py-3">
                        <span
                          className={`font-medium ${
                            alert.riskScore >= 70
                              ? 'text-danger'
                              : alert.riskScore >= 40
                              ? 'text-warning'
                              : 'text-success'
                          }`}
                        >
                          {alert.riskScore}%
                        </span>
                      </td>
                      <td className="py-3">
                        <div className="flex flex-wrap gap-1">
                          {alert.flags.map((flag) => (
                            <Badge key={flag} variant="secondary" className="text-xs">
                              {flag}
                            </Badge>
                          ))}
                        </div>
                      </td>
                      <td className="py-3">
                        <Badge
                          className={
                            alert.status === 'BLOCKED'
                              ? 'bg-danger/20 text-danger'
                              : alert.status === 'SUSPICIOUS'
                              ? 'bg-warning/20 text-warning'
                              : 'bg-success/20 text-success'
                          }
                        >
                          {alert.status}
                        </Badge>
                      </td>
                      <td className="py-3 text-muted-foreground">
                        {new Date(alert.checkedAt).toLocaleString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="text-center text-muted-foreground py-8">
              No fraud alerts detected
            </p>
          )}
        </CardContent>
      </Card>
    </PageContainer>
  )
}

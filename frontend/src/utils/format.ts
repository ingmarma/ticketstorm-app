export function formatGuarani(amount: number): string {
  return '₲' + amount.toLocaleString('es-PY')
}

export function formatDate(isoDate: string): string {
  try {
    const date = new Date(isoDate)
    if (isNaN(date.getTime())) return 'Fecha por confirmar'
    return date.toLocaleDateString('es-PY', {
      weekday: 'short',
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    })
  } catch {
    return 'Fecha por confirmar'
  }
}

export function formatDateTime(isoDate: string): string {
  try {
    const date = new Date(isoDate)
    if (isNaN(date.getTime())) return 'Fecha por confirmar'
    return date.toLocaleDateString('es-PY', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    }) + ' • ' + date.toLocaleTimeString('es-PY', {
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return 'Fecha por confirmar'
  }
}

export function availabilityColor(percentage: number): string {
  if (percentage <= 10) return 'bg-red-500'
  if (percentage <= 30) return 'bg-yellow-500'
  return 'bg-green-500'
}

export function availabilityLabel(percentage: number): string {
  if (percentage <= 0) return 'Agotado'
  if (percentage <= 10) return '¡Últimas entradas!'
  if (percentage <= 30) return '¡Se agota rápido!'
  if (percentage <= 60) return 'Disponibilidad media'
  return 'Buena disponibilidad'
}

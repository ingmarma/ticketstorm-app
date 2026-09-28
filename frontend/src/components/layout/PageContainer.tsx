import * as React from 'react'
import { cn } from '@/utils/cn'

interface PageContainerProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode
}

export function PageContainer({ children, className, ...props }: PageContainerProps) {
  return (
    <main
      className={cn('container mx-auto px-4 py-8', className)}
      {...props}
    >
      {children}
    </main>
  )
}

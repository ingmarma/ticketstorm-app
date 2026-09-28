import * as React from 'react'
import { useSearchParams } from 'react-router-dom'
import { useEventSearch, useSemanticSearch } from '@/hooks/useEventSearch'
import { EventCard } from '@/components/shared/EventCard'
import { PageContainer } from '@/components/layout/PageContainer'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { Skeleton } from '@/components/ui/Skeleton'
import type { EventCategory, SearchParams } from '@/types/api'

const categories: { value: EventCategory | ''; label: string }[] = [
  { value: '', label: 'All' },
  { value: 'CONCERT', label: 'Concerts' },
  { value: 'SPORTS', label: 'Sports' },
  { value: 'THEATER', label: 'Theater' },
  { value: 'COMEDY', label: 'Comedy' },
  { value: 'CONFERENCE', label: 'Conferences' },
  { value: 'FESTIVAL', label: 'Festivals' },
]

export function EventListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [searchQuery, setSearchQuery] = React.useState(searchParams.get('q') || '')
  const [isSemantic, setIsSemantic] = React.useState(false)

  const filters: SearchParams = {
    query: searchParams.get('q') || undefined,
    category: (searchParams.get('category') as EventCategory) || undefined,
    city: searchParams.get('city') || undefined,
    minPrice: searchParams.get('minPrice') ? Number(searchParams.get('minPrice')) : undefined,
    maxPrice: searchParams.get('maxPrice') ? Number(searchParams.get('maxPrice')) : undefined,
  }

  const { data: regularResults, isLoading: isRegularLoading } = useEventSearch(filters)
  const { data: semanticResults, isLoading: isSemanticLoading } = useSemanticSearch(
    isSemantic ? searchQuery : ''
  )

  const events = isSemantic ? semanticResults : regularResults?.data
  const isLoading = isSemantic ? isSemanticLoading : isRegularLoading

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    if (isSemantic) {
      setIsSemantic(true)
    } else {
      setSearchParams((prev) => {
        if (searchQuery) {
          prev.set('q', searchQuery)
        } else {
          prev.delete('q')
        }
        return prev
      })
    }
  }

  const handleCategoryFilter = (category: EventCategory | '') => {
    setSearchParams((prev) => {
      if (category) {
        prev.set('category', category)
      } else {
        prev.delete('category')
      }
      return prev
    })
  }

  return (
    <PageContainer>
      <section className="mb-12">
        <div className="rounded-2xl bg-gradient-to-r from-navy-900 to-navy-700 p-8 md:p-12">
          <h1 className="mb-4 text-3xl font-bold text-white md:text-4xl">
            Find Your Next <span className="text-aws-orange">Experience</span>
          </h1>
          <p className="mb-6 text-lg text-navy-200">
            Discover concerts, sports, theater, and more. Powered by AI search.
          </p>

          <form onSubmit={handleSearch} className="flex flex-col gap-4 sm:flex-row">
            <div className="flex-1">
              <Input
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder={
                  isSemantic
                    ? 'Describe what you\'re looking for...'
                    : 'Search events, venues, cities...'
                }
                icon={
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="18"
                    height="18"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <circle cx="11" cy="11" r="8" />
                    <path d="m21 21-4.3-4.3" />
                  </svg>
                }
                className="bg-background/10 border-navy-600 text-white placeholder:text-navy-300"
              />
            </div>
            <div className="flex gap-2">
              <Button
                type="button"
                variant={isSemantic ? 'primary' : 'secondary'}
                onClick={() => setIsSemantic(!isSemantic)}
                className="shrink-0"
              >
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
                  <path d="M12 2a4 4 0 0 0-4 4v2H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V10a2 2 0 0 0-2-2h-2V6a4 4 0 0 0-4-4z" />
                </svg>
                AI Search
              </Button>
              <Button type="submit" className="shrink-0">
                Search
              </Button>
            </div>
          </form>
        </div>
      </section>

      <section className="mb-8">
        <div className="flex flex-wrap gap-2">
          {categories.map((cat) => (
            <Button
              key={cat.value}
              variant={
                searchParams.get('category') === cat.value || (!searchParams.get('category') && !cat.value)
                  ? 'primary'
                  : 'secondary'
              }
              size="sm"
              onClick={() => handleCategoryFilter(cat.value)}
            >
              {cat.label}
            </Button>
          ))}
        </div>
      </section>

      <section>
        {isLoading ? (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="rounded-xl border border-border bg-card overflow-hidden">
                <Skeleton className="h-48 w-full" />
                <div className="p-6 space-y-3">
                  <Skeleton className="h-6 w-3/4" />
                  <Skeleton className="h-4 w-1/2" />
                  <Skeleton className="h-4 w-2/3" />
                </div>
              </div>
            ))}
          </div>
        ) : events && events.length > 0 ? (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {events.map((event) => (
              <EventCard key={event.id} event={event} />
            ))}
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-16 text-center">
            <div className="mb-4 rounded-full bg-muted p-6">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="48"
                height="48"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
                strokeLinejoin="round"
                className="text-muted-foreground"
              >
                <circle cx="11" cy="11" r="8" />
                <path d="m21 21-4.3-4.3" />
              </svg>
            </div>
            <h3 className="mb-2 text-xl font-semibold">No events found</h3>
            <p className="text-muted-foreground">
              Try adjusting your search or filters
            </p>
          </div>
        )}
      </section>
    </PageContainer>
  )
}

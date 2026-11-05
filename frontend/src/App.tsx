import { useEffect, useMemo, useState } from 'react'
import { api } from './api/client'
import type { CategoryResponse, PageResponse, ServiceResponse } from './api/types'
import { offers } from './catalog'
import CategoryTiles from './components/CategoryTiles'
import Footer from './components/Footer'
import Header from './components/Header'
import Hero from './components/Hero'
import HowItWorks from './components/HowItWorks'
import ServiceGrid from './components/ServiceGrid'
import './App.css'

const PAGE_SIZE = 9

interface Filters { q: string; location: string; categoryId: number | null }

function servicesUrl(f: Filters, page: number, size = PAGE_SIZE) {
  const p = new URLSearchParams({ page: String(page), size: String(size) })
  if (f.q) p.set('q', f.q)
  if (f.location) p.set('location', f.location)
  if (f.categoryId) p.set('categoryId', String(f.categoryId))
  return `/services?${p}`
}

export default function App() {
  const [categories, setCategories] = useState<CategoryResponse[]>([])
  const [all, setAll] = useState<ServiceResponse[]>([])
  const [filters, setFilters] = useState<Filters>({ q: '', location: '', categoryId: null })
  const [result, setResult] = useState<PageResponse<ServiceResponse> | null>(null)
  const [error, setError] = useState(false)

  // Categories plus one unfiltered list for counts, providers and cities.
  // ponytail: one request of 100 is enough for a school catalogue; switch to a stats endpoint if it grows
  useEffect(() => {
    api<CategoryResponse[]>('/categories').then(setCategories).catch(() => setError(true))
    api<PageResponse<ServiceResponse>>('/services?size=100').then(p => setAll(p.content)).catch(() => setError(true))
  }, [])

  useEffect(() => {
    api<PageResponse<ServiceResponse>>(servicesUrl(filters, 0)).then(setResult).catch(() => setError(true))
  }, [filters])

  const stats = useMemo(() => {
    const counts: Record<number, number> = {}
    for (const s of all) counts[s.category.id] = (counts[s.category.id] ?? 0) + 1
    return {
      counts,
      services: all.length,
      providers: new Set(all.map(s => s.provider.id)).size,
      cities: [...new Set(all.map(s => s.provider.location))].sort(),
    }
  }, [all])

  function loadMore() {
    if (!result) return
    api<PageResponse<ServiceResponse>>(servicesUrl(filters, result.page + 1))
      .then(next => setResult({ ...next, content: [...result.content, ...next.content] }))
      .catch(() => setError(true))
  }

  const services = result?.content ?? []
  const hasMore = result ? result.page + 1 < result.totalPages : false
  const searchText = [filters.q, filters.location].filter(Boolean).join(', ')

  return (
    <>
      <Header />
      <main>
        <Hero stats={stats} latest={[...all].sort((a, b) => b.id - a.id).slice(0, 4)} onSearch={(q, location) => setFilters(f => ({ ...f, q, location }))} />

        <section className="results" id="sluzby">
          <CategoryTiles
            categories={categories}
            counts={stats.counts}
            total={stats.services}
            selected={filters.categoryId}
            onSelect={categoryId => setFilters(f => ({ ...f, categoryId }))}
          />

          <div className="results-head">
            <h2>{result ? `${offers(result.totalElements)}${searchText ? ` pre „${searchText}“` : ''}` : 'Načítavam ponuky…'}</h2>
            {searchText && (
              <button className="link-button" onClick={() => setFilters(f => ({ ...f, q: '', location: '' }))}>
                Zrušiť vyhľadávanie
              </button>
            )}
          </div>

          {error && <p className="notice">Ponuky sa nepodarilo načítať. Skúste obnoviť stránku.</p>}

          {result && services.length === 0 && !error && (
            <div className="empty">
              <h3>Na toto zatiaľ nemáme majstra</h3>
              <p>Skúste iné slovo, mesto alebo kategóriu. Ak takú prácu robíte vy, pridajte svoju službu.</p>
            </div>
          )}

          <ServiceGrid services={services} />
          {hasMore && <button className="more" onClick={loadMore}>Zobraziť ďalšie ponuky</button>}
        </section>

        <HowItWorks />
      </main>
      <Footer />
    </>
  )
}

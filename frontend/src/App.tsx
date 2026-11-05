import { useEffect, useState } from 'react'
import { api } from './api/client'
import type { CategoryResponse, PageResponse, ServiceResponse } from './api/types'
import { offers } from './catalog'
import CategoryTiles from './components/CategoryTiles'
import Footer from './components/Footer'
import Header from './components/Header'
import Hero from './components/Hero'
import ServiceGrid from './components/ServiceGrid'
import './App.css'

const PAGE_SIZE = 8

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
  const [counts, setCounts] = useState<Record<number, number>>({})
  const [total, setTotal] = useState(0)
  const [filters, setFilters] = useState<Filters>({ q: '', location: '', categoryId: null })
  const [result, setResult] = useState<PageResponse<ServiceResponse> | null>(null)
  const [error, setError] = useState(false)

  // Categories and how many services each has (for the tile labels)
  useEffect(() => {
    api<CategoryResponse[]>('/categories').then(async cats => {
      setCategories(cats)
      const pages = await Promise.all(cats.map(c => api<PageResponse<ServiceResponse>>(`/services?categoryId=${c.id}&size=1`)))
      setCounts(Object.fromEntries(cats.map((c, i) => [c.id, pages[i].totalElements])))
      setTotal(pages.reduce((sum, p) => sum + p.totalElements, 0))
    }).catch(() => setError(true))
  }, [])

  // First page of services whenever the filters change
  useEffect(() => {
    api<PageResponse<ServiceResponse>>(servicesUrl(filters, 0)).then(setResult).catch(() => setError(true))
  }, [filters])

  function loadMore() {
    if (!result) return
    api<PageResponse<ServiceResponse>>(servicesUrl(filters, result.page + 1))
      .then(next => setResult({ ...next, content: [...result.content, ...next.content] }))
      .catch(() => setError(true))
  }

  const services = result?.content ?? []
  const hasMore = result ? result.page + 1 < result.totalPages : false

  return (
    <>
      <Header />
      <main>
        <Hero onSearch={(q, location) => setFilters(f => ({ ...f, q, location }))} />
        <CategoryTiles
          categories={categories}
          counts={counts}
          total={total}
          selected={filters.categoryId}
          onSelect={categoryId => setFilters(f => ({ ...f, categoryId }))}
        />

        <section className="results">
          <div className="results-head">
            <h2>{result ? offers(result.totalElements) : 'Načítavam…'}</h2>
            {(filters.q || filters.location) && (
              <button className="chip" onClick={() => setFilters(f => ({ ...f, q: '', location: '' }))}>
                {[filters.q, filters.location].filter(Boolean).join(' · ')} ✕
              </button>
            )}
          </div>

          {error && <p className="notice">Nepodarilo sa načítať služby. Skúste obnoviť stránku.</p>}

          {result && services.length === 0 && !error && (
            <div className="empty">
              <h3>Zatiaľ tu nie je žiadna služba</h3>
              <p>Skúste inú kategóriu alebo mesto. Ak ponúkate služby, môžete byť prvý.</p>
            </div>
          )}

          <ServiceGrid services={services} />
          {hasMore && <button className="more" onClick={loadMore}>Načítať ďalšie</button>}
        </section>
      </main>
      <Footer />
    </>
  )
}

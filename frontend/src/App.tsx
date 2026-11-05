import { useEffect, useState } from 'react'
import { api } from './api/client'
import type { AppInfo, CategoryResponse } from './api/types'

// Starter page: proves the frontend reaches the backend and the database.
export default function App() {
  const [info, setInfo] = useState<AppInfo | null>(null)
  const [categories, setCategories] = useState<CategoryResponse[]>([])
  const [error, setError] = useState(false)

  useEffect(() => {
    api<AppInfo>('/info').then(setInfo).catch(() => setError(true))
    api<CategoryResponse[]>('/categories').then(setCategories).catch(() => setError(true))
  }, [])

  return (
    <main>
      <h1>{info?.name ?? 'Hodinový majster'}</h1>
      {error && <p>Backend nie je dostupný. Beží na porte 8080?</p>}
      <ul>
        {categories.map(c => <li key={c.id}>{c.name}</li>)}
      </ul>
    </main>
  )
}

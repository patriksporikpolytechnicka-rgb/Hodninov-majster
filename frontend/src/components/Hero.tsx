import { useState, type FormEvent } from 'react'
import { MagnifyingGlass, MapPin } from '@phosphor-icons/react'

const popular = ['montáž nábytku', 'kosenie', 'sťahovanie']

export default function Hero({ onSearch }: { onSearch: (q: string, location: string) => void }) {
  const [q, setQ] = useState('')
  const [location, setLocation] = useState('')

  function submit(e: FormEvent) {
    e.preventDefault()
    onSearch(q.trim(), location.trim())
  }

  return (
    <section className="hero">
      <div>
        <h1>Potrebujete s niečím pomôcť? Nájdite majstra vo svojom okolí.</h1>
        <p>Cenu vidíte ešte pred objednávkou. Platíte len majstrovi, nič navyše.</p>
      </div>
      <div>
        <form className="search" onSubmit={submit} role="search">
          <label className="search-field search-what">
            <MagnifyingGlass size={20} weight="light" />
            <input value={q} onChange={e => setQ(e.target.value)} placeholder="Čo treba urobiť?" aria-label="Čo treba urobiť" />
          </label>
          <label className="search-field search-where">
            <MapPin size={20} weight="light" />
            <input value={location} onChange={e => setLocation(e.target.value)} placeholder="Mesto" aria-label="Mesto" />
          </label>
          <button className="search-go">Hľadať</button>
        </form>
        <p className="popular">
          Často hľadané:
          {popular.map(term => (
            <button key={term} type="button" onClick={() => { setQ(term); onSearch(term, location.trim()) }}>
              {term}
            </button>
          ))}
        </p>
      </div>
    </section>
  )
}

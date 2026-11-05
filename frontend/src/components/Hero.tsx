import { useState, type FormEvent } from 'react'
import { MagnifyingGlass, MapPin } from '@phosphor-icons/react'
import type { ServiceResponse } from '../api/types'
import { categoryIcon, categorySlug, formatPrice, unitLabel } from '../catalog'

interface Props {
  stats: { services: number; providers: number; cities: string[] }
  latest: ServiceResponse[]
  onSearch: (q: string, location: string) => void
}

const popular = ['montáž nábytku', 'kosenie', 'sťahovanie', 'Wi-Fi']

export default function Hero({ stats, latest, onSearch }: Props) {
  const [q, setQ] = useState('')
  const [location, setLocation] = useState('')

  function submit(e: FormEvent) {
    e.preventDefault()
    onSearch(q.trim(), location.trim())
  }

  return (
    <section className="hero">
      <div className="hero-main">
        <h1>
          Majster na hodinu,<br />
          <span className="hero-mark">s cenou vopred.</span>
        </h1>
        <p className="hero-lead">
          Montáž, opravy, záhrada, sťahovanie aj pomoc s počítačom.
          Cenu za hodinu vidíte ešte pred objednávkou a platíte priamo majstrovi.
        </p>

        <form className="search" onSubmit={submit} role="search">
          <label className="search-field search-what">
            <MagnifyingGlass size={20} weight="light" />
            <input value={q} onChange={e => setQ(e.target.value)} placeholder="Čo treba urobiť?" aria-label="Čo treba urobiť" />
          </label>
          <label className="search-field search-where">
            <MapPin size={20} weight="light" />
            <input value={location} onChange={e => setLocation(e.target.value)} placeholder="Mesto" aria-label="Mesto" list="cities" />
            <datalist id="cities">{stats.cities.map(c => <option key={c} value={c} />)}</datalist>
          </label>
          <button className="search-go">Hľadať</button>
        </form>

        <div className="hero-foot">
          <p className="popular">
            <span>Ľudia hľadajú:</span>
            {popular.map(term => (
              <button key={term} type="button" onClick={() => { setQ(term); onSearch(term, location.trim()) }}>
                {term}
              </button>
            ))}
          </p>
          {stats.services > 0 && (
            <p className="hero-stats">
              <b>{stats.services}</b> služieb · <b>{stats.providers}</b> majstrov · {stats.cities.join(', ')}
            </p>
          )}
        </div>
      </div>

      {latest.length > 0 && (
        <aside className="latest" aria-label="Pribudlo nedávno">
          <h2>Pribudlo nedávno</h2>
          <ul>
            {latest.map(s => (
              <li key={s.id} className={`cat-${categorySlug(s.category.name)}`}>
                <a href={`/sluzby/${s.id}`}>
                  <span className="latest-icon" dangerouslySetInnerHTML={{ __html: categoryIcon(s.category.name) }} />
                  <span className="latest-name">{s.name}<small>{s.provider.name} · {s.provider.location}</small></span>
                  <span className="latest-price">{formatPrice(s.price)}<small>/{unitLabel(s.priceUnit)}</small></span>
                </a>
              </li>
            ))}
          </ul>
          <a className="latest-all" href="#sluzby">Všetky ponuky →</a>
        </aside>
      )}
    </section>
  )
}

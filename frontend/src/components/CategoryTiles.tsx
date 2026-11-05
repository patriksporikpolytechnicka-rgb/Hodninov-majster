import type { CategoryResponse } from '../api/types'
import { categoryIcon, categorySlug, offers } from '../catalog'

interface Props {
  categories: CategoryResponse[]
  counts: Record<number, number>
  total: number
  selected: number | null
  onSelect: (id: number | null) => void
}

export default function CategoryTiles({ categories, counts, total, selected, onSelect }: Props) {
  return (
    <nav className="tiles" aria-label="Kategórie">
      <button className={`tile tile-all cat-vsetko ${selected === null ? 'is-on' : ''}`} aria-pressed={selected === null} onClick={() => onSelect(null)}>
        <span className="tile-icon" dangerouslySetInnerHTML={{ __html: categoryIcon() }} />
        <span><b>Všetko</b><small>{offers(total)}</small></span>
      </button>
      {categories.map(c => (
        <button
          key={c.id}
          className={`tile cat-${categorySlug(c.name)} ${selected === c.id ? 'is-on' : ''}`}
          aria-pressed={selected === c.id}
          onClick={() => onSelect(c.id)}
        >
          <span className="tile-icon" dangerouslySetInnerHTML={{ __html: categoryIcon(c.name) }} />
          <span><b>{c.name}</b><small>{offers(counts[c.id] ?? 0)}</small></span>
        </button>
      ))}
    </nav>
  )
}

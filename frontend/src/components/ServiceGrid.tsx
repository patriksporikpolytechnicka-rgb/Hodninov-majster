import type { ServiceResponse } from '../api/types'
import { categoryIcon, categorySlug, formatPrice, unitLabel } from '../catalog'

export default function ServiceGrid({ services }: { services: ServiceResponse[] }) {
  return (
    <div className="grid">
      {services.map(s => (
        <a key={s.id} className={`card cat-${categorySlug(s.category.name)}`} href={`/sluzby/${s.id}`}>
          <div className="card-top">
            <span className="card-tag">{s.category.name}</span>
            <span className="card-icon" dangerouslySetInnerHTML={{ __html: categoryIcon(s.category.name) }} />
          </div>
          <div className="card-body">
            <p className="card-price">{formatPrice(s.price)} <small>/ {unitLabel(s.priceUnit)}</small></p>
            <h3>{s.name}</h3>
            <p className="card-meta"><span>{s.provider.name}</span><span>{s.provider.location}</span></p>
          </div>
        </a>
      ))}
    </div>
  )
}

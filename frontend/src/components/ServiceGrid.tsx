import type { ServiceResponse } from '../api/types'
import { categoryIcon, categorySlug, formatPrice, initials, unitLabel } from '../catalog'

export default function ServiceGrid({ services }: { services: ServiceResponse[] }) {
  return (
    <div className="grid">
      {services.map(s => (
        <a key={s.id} className={`card cat-${categorySlug(s.category.name)}`} href={`/sluzby/${s.id}`}>
          <p className="card-cat">
            <span className="card-icon" dangerouslySetInnerHTML={{ __html: categoryIcon(s.category.name) }} />
            {s.category.name}
          </p>
          <h3>{s.name}</h3>
          <p className="card-desc">{s.description}</p>

          <div className="card-foot">
            <span className="avatar" aria-hidden="true">{initials(s.provider.name)}</span>
            <span className="card-who">
              {s.provider.name}
              <small>
                {s.provider.location}
                {!s.provider.availability && <> · <em>teraz nedostupný</em></>}
              </small>
            </span>
            <span className="card-price">
              {formatPrice(s.price)}
              <small>za {unitLabel(s.priceUnit)}</small>
            </span>
          </div>
        </a>
      ))}
    </div>
  )
}

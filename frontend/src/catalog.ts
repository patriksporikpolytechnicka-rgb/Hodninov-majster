import type { PriceUnit } from './api/types'
import vsetko from './assets/icons/vsetko.svg?raw'
import domacnost from './assets/icons/domacnost.svg?raw'
import zahrada from './assets/icons/zahrada.svg?raw'
import itPomoc from './assets/icons/it-pomoc.svg?raw'
import stahovanie from './assets/icons/stahovanie.svg?raw'

// "Sťahovanie" -> "stahovanie", "IT pomoc" -> "it-pomoc"
export function categorySlug(name: string) {
  return name.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim().replace(/\s+/g, '-')
}

// Drop the fixed colour and size from the design files, so icons follow the tile
const clean = (svg: string) =>
  svg.replace(/ color="[^"]*"/, '').replace(/ width="\d+" height="\d+"/, ' width="100%" height="100%" aria-hidden="true"')

const icons: Record<string, string> = Object.fromEntries(
  Object.entries({ vsetko, domacnost, zahrada, 'it-pomoc': itPomoc, stahovanie }).map(([k, svg]) => [k, clean(svg)]),
)

// Inline SVG markup, so the icon takes the tile colour through currentColor
export function categoryIcon(name?: string) {
  return (name && icons[categorySlug(name)]) || icons.vsetko
}

const units: Record<PriceUnit, string> = { HOUR: 'hod', JOB: 'zákazka', SQUARE_METER: 'm²' }

export function formatPrice(price: number) {
  const decimals = Number.isInteger(price) ? 0 : 2
  return price.toLocaleString('sk-SK', { style: 'currency', currency: 'EUR', minimumFractionDigits: decimals })
}

export function unitLabel(unit: PriceUnit) {
  return units[unit]
}

// "Peter Novák" -> "PN"
export function initials(name: string) {
  return name.split(/\s+/).filter(Boolean).slice(0, 2).map(w => w[0].toUpperCase()).join('')
}

// Slovak plural: 1 ponuka, 2–4 ponuky, 0 / 5+ ponúk
export function offers(n: number) {
  return `${n} ${n === 1 ? 'ponuka' : n >= 2 && n <= 4 ? 'ponuky' : 'ponúk'}`
}

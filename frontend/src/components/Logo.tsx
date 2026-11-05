import icon from '../assets/logo.svg'
import './Logo.css'

// Clock icon + "hodinový / majster." with the yellow dot and underline (logo concept A + D)
export default function Logo({ light = false, size = 36 }: { light?: boolean; size?: number }) {
  return (
    <span className={`logo ${light ? 'logo-light' : ''}`} style={{ fontSize: size * 0.47 }}>
      <img src={icon} alt="" width={size} height={size} />
      <span className="logo-word">
        <span>hodinový</span>
        <span>majster<b>.</b></span>
      </span>
      <span className="sr-only">Hodinový majster</span>
    </span>
  )
}

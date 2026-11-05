import { useEffect, useState } from 'react'
import { CaretRight, ClipboardText, Heart, Info, List, MagnifyingGlass, Plus, Wrench, X } from '@phosphor-icons/react'
import logo from '../assets/logo.svg'
import './Header.css'

const menuLinks = [
  { label: 'Nájsť službu', href: '#', icon: MagnifyingGlass },
  { label: 'Moje objednávky', href: '#', icon: ClipboardText },
  { label: 'Uložené', href: '#', icon: Heart },
  { label: 'Ako to funguje', href: '#', icon: Info },
  { label: 'Pre majstrov', href: '#', icon: Wrench },
]

export default function Header() {
  const [menuOpen, setMenuOpen] = useState(false)

  // Close the phone menu with Escape; listen only while it is open
  useEffect(() => {
    if (!menuOpen) return
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setMenuOpen(false)
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [menuOpen])

  return (
    <header className="header">
      <a className="header-logo" href="/">
        <img src={logo} alt="" width={32} height={32} />
        Hodinový majster
      </a>

      <nav className="header-nav" aria-label="Hlavné menu">
        <a href="#">Ako to funguje</a>
        <a href="#">Pre majstrov</a>
      </nav>

      <div className="header-actions">
        <a href="#">Moje objednávky</a>
        <a href="#">Prihlásiť sa</a>
        <a className="button-navy" href="#">
          <Plus weight="bold" /> Pridať službu
        </a>
      </div>

      <button
        className="menu-button"
        aria-label="Otvoriť menu"
        aria-expanded={menuOpen}
        aria-controls="mobile-menu"
        onClick={() => setMenuOpen(true)}
      >
        <List size={24} weight="light" />
      </button>

      {menuOpen && (
        <div className="mobile-menu" id="mobile-menu">
          <div className="mobile-menu-top">
            <span className="header-logo">
              <img src={logo} alt="" width={28} height={28} />
              Hodinový majster
            </span>
            <button className="menu-button" aria-label="Zavrieť menu" onClick={() => setMenuOpen(false)}>
              <X size={24} weight="light" />
            </button>
          </div>

          <ul>
            {menuLinks.map(({ label, href, icon: Icon }) => (
              <li key={label}>
                <a href={href} onClick={() => setMenuOpen(false)}>
                  <Icon size={22} weight="light" className="mobile-menu-icon" />
                  {label}
                  <CaretRight size={18} weight="light" className="mobile-menu-caret" />
                </a>
              </li>
            ))}
          </ul>

          <div className="mobile-menu-bottom">
            <a className="button-yellow" href="#">
              <Plus weight="bold" /> Pridať službu
            </a>
            <a className="button-outline" href="#">Prihlásiť sa</a>
          </div>
        </div>
      )}
    </header>
  )
}

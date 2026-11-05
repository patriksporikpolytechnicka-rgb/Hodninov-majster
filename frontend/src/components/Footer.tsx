import { Wrench } from '@phosphor-icons/react'

export default function Footer() {
  return (
    <>
      <section className="band">
        <Wrench size={40} weight="light" className="band-icon" />
        <div>
          <b>Ponúkate služby?</b>
          <p>Prvých 30 dní zadarmo, potom 10 € mesačne. Z vašich zákaziek si neberieme nič.</p>
        </div>
        <a className="button-yellow band-cta" href="#">Začať ponúkať</a>
      </section>
      <footer className="footer">
        <span>Hodinový majster</span>
        <span>Školský projekt · 2026</span>
      </footer>
    </>
  )
}

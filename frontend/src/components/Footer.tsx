import Logo from './Logo'

export default function Footer() {
  return (
    <>
      <section className="band" id="pre-majstrov">
        <div>
          <h2>Ponúkate služby?</h2>
          <p>Prvých 30 dní zadarmo, potom 10 € mesačne. Z vašich zákaziek si neberieme ani cent.</p>
        </div>
        <a className="button-yellow band-cta" href="#">Pridať moju službu</a>
      </section>

      <footer className="footer">
        <a href="/" aria-label="Hodinový majster, domov"><Logo light size={30} /></a>
        <nav className="footer-links" aria-label="Pätička">
          <a href="#sluzby">Nájsť službu</a>
          <a href="#ako-to-funguje">Ako to funguje</a>
          <a href="#pre-majstrov">Pre majstrov</a>
          <a href="#">Moje objednávky</a>
        </nav>
        <p className="footer-note">Nicolas Kiš a Patrik Šporík · Školský projekt · Košická akadémia · 2026</p>
      </footer>
    </>
  )
}

const steps = [
  { title: 'Vyberte si majstra', text: 'Porovnajte ceny za hodinu, zákazku alebo m² a pozrite si, kto prácu robí a odkiaľ je.' },
  { title: 'Pošlite objednávku', text: 'Zvoľte termín, adresu a napíšte, čo treba urobiť. Cenu vypočítame hneď.' },
  { title: 'Majster ju potvrdí', text: 'Stav objednávky vidíte v „Moje objednávky“. Po práci platíte priamo jemu.' },
]

export default function HowItWorks() {
  return (
    <section className="how" id="ako-to-funguje">
      <h2>Ako to funguje</h2>
      <ol>
        {steps.map((s, i) => (
          <li key={s.title}>
            <span className="how-num">{i + 1}</span>
            <h3>{s.title}</h3>
            <p>{s.text}</p>
          </li>
        ))}
      </ol>
    </section>
  )
}

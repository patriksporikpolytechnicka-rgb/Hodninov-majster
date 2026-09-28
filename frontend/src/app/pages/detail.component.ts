import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService, SessionService, apiError, unitLabel } from '../core/api.service';
import { ServiceResponse } from '../core/api-types';
import { IconComponent } from '../shared/icon.component';

@Component({
  standalone: true, imports: [FormsModule, DecimalPipe, RouterLink, IconComponent],
  template: `
    <section class="container detail-page"><a class="text-link" routerLink="/">← Všetky služby</a>
    @if (loading()) { <div class="loading" role="status"><span class="spinner"></span> Načítavame službu…</div> }
    @if (error()) { <div class="notice error" role="alert">{{ error() }}</div> }
    @if (service(); as s) {
      <div class="detail-grid">
        <div class="detail-copy"><p class="eyebrow">{{ s.category.name }} · {{ s.provider.location }}</p><h1>{{ s.name }}</h1><p class="detail-description">{{ s.description }}</p>
          <div class="detail-facts"><span><app-icon name="pin" /> {{ s.provider.location }}</span><span><app-icon name="clock" /> Cena za {{ unit(s.priceUnit) }}</span></div>
          <div class="provider-panel"><span class="avatar avatar-large">{{ s.provider.name.charAt(0) }}</span><div><p class="eyebrow">VÁŠ POSKYTOVATEĽ</p><h3>{{ s.provider.name }}</h3><p>{{ s.provider.description }}</p><span class="available" [class.unavailable]="!s.provider.availability">{{ s.provider.availability ? 'Prijíma nové objednávky' : 'Momentálne nedostupný' }}</span></div></div>
        </div>
        <div class="panel booking-panel"><p class="eyebrow">DOHODNIME SI POMOC</p><div class="booking-price">{{ s.price | number:'1.2-2' }} € <span>/ {{ unit(s.priceUnit) }}</span></div>
          @if (created()) {
            <div class="success-state" role="status"><span class="success-check"><app-icon name="check" /></span><h3>Objednávka je odoslaná.</h3><p>Číslo #{{ created() }}. Teraz čaká na prijatie poskytovateľom.</p><a class="button full-width" routerLink="/objednavky">Moje objednávky</a></div>
          } @else if (!session.auth()) {
            <p>Prihláste sa a vyberte si termín, ktorý vám vyhovuje.</p><a class="button full-width" routerLink="/prihlasenie">Prihlásiť sa a objednať <app-icon name="arrow" /></a>
          } @else if (session.auth()!.user.role !== 'CUSTOMER') {
            <p>Objednávky vytvára zákaznícky účet. Ako poskytovateľ nájdete prijaté požiadavky v objednávkach.</p><a class="button button-outline full-width" routerLink="/objednavky">Zobraziť objednávky</a>
          } @else if (!s.provider.availability) {
            <p>Tento poskytovateľ momentálne neprijíma objednávky.</p>
          } @else {
            <form #form="ngForm" (ngSubmit)="book()">
              <label>Termín<input type="datetime-local" name="date" [(ngModel)]="date" required [min]="minDate"></label><small>Čas na Slovensku · Europe/Bratislava</small>
              <label>{{ s.priceUnit === 'HOUR' ? 'Počet hodín' : s.priceUnit === 'JOB' ? 'Počet úkonov' : 'Plocha v m²' }}<input type="number" name="quantity" [(ngModel)]="quantity" required [min]="s.priceUnit === 'JOB' ? 1 : 0.01" max="999999.99" [step]="s.priceUnit === 'JOB' ? 1 : 0.25"></label>
              <label>Adresa<input name="address" [(ngModel)]="address" required maxlength="300" autocomplete="street-address" placeholder="Ulica, číslo a mesto"></label>
              <label>Poznámka <span class="optional">(nepovinné)</span><textarea name="note" [(ngModel)]="note" maxlength="2000" rows="3" placeholder="Čo by mal majster vedieť?"></textarea></label>
              <div class="booking-total"><span>Cena za objednaný rozsah</span><strong>{{ s.price * (quantity || 0) | number:'1.2-2' }} €</strong></div>
              <button class="button full-width" [disabled]="form.invalid || busy()" type="submit">{{ busy() ? 'Odosielame…' : 'Objednať službu' }} <app-icon name="arrow" /></button><small>Výslednú cenu overí server. Online platba sa nevykonáva.</small>
            </form>
          }
        </div>
      </div>
    }
    </section>
  `
})
export class DetailComponent implements OnInit {
  private api = inject(ApiService); private route = inject(ActivatedRoute);
  readonly session = inject(SessionService);
  service = signal<ServiceResponse | null>(null); loading = signal(true); busy = signal(false); error = signal(''); created = signal<number | null>(null);
  date = ''; quantity = 1; address = ''; note = ''; unit = unitLabel;
  minDate = new Intl.DateTimeFormat('sv-SE', { timeZone: 'Europe/Bratislava', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date()).replace(' ', 'T');
  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isSafeInteger(id) || id < 1) { this.loading.set(false); this.error.set('Neplatná služba.'); return; }
    this.api.service(id).subscribe({ next: s => { this.service.set(s); this.loading.set(false); }, error: e => { this.error.set(apiError(e)); this.loading.set(false); } });
  }
  book() {
    const service = this.service();
    if (!service || this.busy()) return;
    if (service.priceUnit === 'JOB' && !Number.isInteger(this.quantity)) { this.error.set('Počet úkonov musí byť celé číslo.'); return; }
    this.busy.set(true); this.error.set('');
    this.api.createOrder({ serviceId: service.id, scheduledAt: this.date, quantity: this.quantity, address: this.address, note: this.note || null })
      .subscribe({ next: order => { this.created.set(order.id); this.busy.set(false); }, error: e => { this.error.set(apiError(e)); this.busy.set(false); } });
  }
}

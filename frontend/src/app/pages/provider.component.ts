import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService, SessionService, apiError } from '../core/api.service';
import { ProviderRequest, SubscriptionResponse } from '../core/api-types';
@Component({
  standalone: true, imports: [DatePipe, FormsModule, RouterLink],
  template: `
    <section class="container auth-page">
      <div class="auth-intro"><p class="eyebrow">MAJSTER</p><h1>Váš profil<br><em>a predplatné.</em></h1>
        <p>Prvých 30 dní od vytvorenia profilu zdarma, potom 10 € mesačne. Celá cena zákazky patrí vám.</p>
        @if (subscription(); as s) {
          <div class="panel"><h2>{{ s.status === 'TRIAL' ? '30 dní zdarma' : s.status === 'ACTIVE' ? 'Aktívne predplatné' : 'Predplatné vypršalo' }}</h2>
            <p>Platnosť do {{ s.subscriptionEnd | date:'dd. MM. yyyy HH:mm' }}</p>
            <p>Mesačný poplatok: {{ s.monthlyFee }} €</p>
            @if (!s.canReceiveOrders) {
              <p>Na prijímanie nových zákaziek obnovte predplatné.</p>
              @if (s.demoPayment) { <button class="button" [disabled]="busy()" (click)="pay()">Simulovať platbu 10 € / mesiac</button><p>Školská ukážka: peniaze sa neúčtujú.</p> }
              @else { <p>Online platba ešte nie je dostupná. Predplatné sa automaticky neúčtuje.</p> }
            }
          </div>
        }
      </div>
      @if (session.auth()?.user?.role !== 'PROVIDER') { <div class="panel"><p>Prihláste sa ako poskytovateľ.</p><a routerLink="/prihlasenie">Prihlásiť sa</a></div> }
      @else if (loading()) { <div class="panel" role="status">Načítavame profil…</div> }
      @else {
        <form class="panel auth-form" #form="ngForm" (ngSubmit)="save()">
          <h2>{{ exists() ? 'Upraviť profil' : 'Vytvoriť profil' }}</h2>
          <label>Meno<input name="name" [(ngModel)]="profile.name" required maxlength="150"></label>
          <label>Popis<textarea name="description" [(ngModel)]="profile.description" required maxlength="1000"></textarea></label>
          <label>Mesto<input name="location" [(ngModel)]="profile.location" required maxlength="150"></label>
          <label>Telefón<input name="phone" [(ngModel)]="profile.phone" required maxlength="40"></label>
          <label><input type="checkbox" name="availability" [(ngModel)]="profile.availability"> Som dostupný</label>
          <p>Upravenie profilu neobnovuje bezplatných 30 dní.</p>
          <button class="button" [disabled]="form.invalid || busy()">{{ busy() ? 'Ukladáme…' : 'Uložiť profil' }}</button>
        </form>
      }
      @if (error()) { <div class="notice error" role="alert">{{ error() }}</div> }
      @if (message()) { <div class="notice" role="status">{{ message() }}</div> }
    </section>
  `
})
export class ProviderComponent implements OnInit {
  readonly api = inject(ApiService); readonly session = inject(SessionService);
  subscription = signal<SubscriptionResponse | null>(null);
  exists = signal(false); loading = signal(false); busy = signal(false); error = signal(''); message = signal('');
  profile: ProviderRequest = { name: '', description: '', location: '', phone: '', profileImage: null, availability: true };
  ngOnInit() {
    if (this.session.auth()?.user.role !== 'PROVIDER') return;
    this.loading.set(true);
    this.api.myProfile().subscribe({ next: p => { this.profile = p; this.exists.set(true); this.loading.set(false); this.loadSubscription(); },
      error: e => { this.loading.set(false); if (!(e instanceof HttpErrorResponse && e.status === 404)) this.error.set(apiError(e)); } });
  }
  loadSubscription() { this.api.subscription().subscribe({ next: s => this.subscription.set(s), error: e => this.error.set(apiError(e)) }); }
  save() {
    if (this.busy()) return;
    this.busy.set(true); this.error.set(''); this.message.set('');
    this.api.saveProfile(this.profile).subscribe({ next: p => { this.profile = p; this.exists.set(true); this.busy.set(false); this.message.set('Profil je uložený.'); this.loadSubscription(); },
      error: e => { this.busy.set(false); this.error.set(apiError(e)); } });
  }
  pay() {
    if (this.busy()) return;
    this.busy.set(true); this.error.set(''); this.message.set('');
    this.api.demoSubscriptionPayment().subscribe({ next: s => { this.subscription.set(s); this.busy.set(false); this.message.set('Simulovaná platba bola úspešná.'); },
      error: e => { this.busy.set(false); this.error.set(apiError(e)); } });
  }
}

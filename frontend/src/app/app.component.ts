import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet, Router } from '@angular/router';
import { ApiService, SessionService } from './core/api.service';
@Component({
  selector: 'app-root', standalone: true, imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <a class="skip-link" href="#main">Preskočiť na obsah</a>
    @if (api.info()?.demo) {
      <div class="demo-strip"><span class="live-dot"></span> Školská ukážka <span class="demo-description">· Fiktívne služby a účty. Dáta sa po reštarte vymažú.</span></div>
    } @else if (api.info()?.demoAccounts) {
      <div class="demo-strip"><span class="live-dot"></span> Školská ukážka <span class="demo-description">· Ukážkové účty a služby. Dáta sa ukladajú do databázy.</span></div>
    }
    <header class="site-header"><div class="container header-inner">
      <a class="brand" routerLink="/" aria-label="Hodinový majster – domov"><span class="brand-mark">h<span>m</span></span><span>hodinový<strong>majster.</strong></span></a>
      <nav aria-label="Hlavná navigácia">
        <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}">Nájsť službu</a>
        <a routerLink="/objednavky" routerLinkActive="active">Objednávky</a>
      </nav>
      <div class="header-actions">
        @if (session.auth(); as auth) {
          <span class="account-label" [title]="auth.user.email">{{ auth.user.role === 'PROVIDER' ? 'Poskytovateľ' : 'Môj účet' }}</span>
          <button class="button button-small button-outline" (click)="logout()">Odhlásiť sa</button>
        } @else {
          <a class="login-link" routerLink="/prihlasenie">Prihlásiť sa</a>
          <a class="button button-small" routerLink="/registracia">Vytvoriť účet <span aria-hidden="true">↗</span></a>
        }
      </div>
    </div></header>
    <main id="main"><router-outlet /></main>
    <footer><div class="container footer-inner"><a class="footer-brand" routerLink="/">hodinový majster.</a><p>Šikovné ruky. Viac času pre vás.</p><span>Školský projekt · 2026</span></div></footer>
  `
})
export class AppComponent {
  readonly api = inject(ApiService);
  readonly session = inject(SessionService);
  private router = inject(Router);
  constructor() { this.api.loadInfo(); }
  logout() { this.session.logout(); void this.router.navigateByUrl('/'); }
}


import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService, apiError } from '../core/api.service';
import { IconComponent } from '../shared/icon.component';

@Component({
  standalone: true, imports: [FormsModule, RouterLink, IconComponent],
  template: `
    <section class="container auth-page">
      <div class="auth-intro"><p class="eyebrow">VITAJTE U MAJSTRA</p><h1>{{ register ? 'Každá dobrá spolupráca' : 'Vaše plány.' }}<br><em>{{ register ? 'začína tu.' : 'Na jednom mieste.' }}</em></h1>
        <p>{{ register ? 'Vytvorte si účet a nájdite pomoc alebo ponúknite svoje šikovné ruky.' : 'Prihláste sa a majte svoje objednávky pod kontrolou.' }}</p>
        <a routerLink="/" class="text-link">← Späť na ponuku služieb</a>
        @if (api.info()?.demoAccounts && !register) {
          <div class="demo-accounts"><strong>Vyskúšajte školskú ukážku</strong><p>{{ api.info()?.demo ? 'Údaje sú fiktívne a po reštarte sa vymažú.' : 'Ukážkové účty sú uložené v databáze.' }}</p><div><button class="button button-outline button-small" type="button" (click)="demo('zakaznik@demo.sk')">Som zákazník</button><button class="button button-outline button-small" type="button" (click)="demo('majster@demo.sk')">Som poskytovateľ</button></div><small>Tlačidlo vyplní ukážkové prihlasovacie údaje.</small></div>
        }
      </div>
      <form class="panel auth-form" #form="ngForm" (ngSubmit)="submit()">
        <span class="panel-icon"><app-icon name="user" /></span><h2>{{ register ? 'Vytvoriť účet' : 'Prihlásiť sa' }}</h2>
        <label>E-mail<input type="email" name="email" [(ngModel)]="email" required email maxlength="254" autocomplete="email" placeholder="vas@email.sk"></label>
        <label>Heslo<input type="password" name="password" [(ngModel)]="password" required [minlength]="register ? 10 : 1" maxlength="72" [autocomplete]="register ? 'new-password' : 'current-password'" placeholder="Vaše heslo"></label>
        @if (register) {
          <small>Najmenej 10 znakov, najviac 72 bajtov.</small>
          <label>Chcem<select name="role" [(ngModel)]="role"><option value="CUSTOMER">Objednávať služby</option><option value="PROVIDER">Ponúkať služby</option></select></label>
          @if (role === 'PROVIDER') { <p class="small-muted">Prvých 30 dní od vytvorenia profilu zdarma, potom 10 € mesačne. Celá cena zákazky patrí vám. Zákaznícky účet je zdarma.</p> }
        }
        @if (error()) { <div class="notice error" role="alert">{{ error() }}</div> }
        <button class="button full-width" type="submit" [disabled]="form.invalid || busy()">{{ busy() ? 'Chvíľku strpenia…' : register ? 'Vytvoriť účet' : 'Prihlásiť sa' }} <app-icon name="arrow" /></button>
        <p class="auth-switch">{{ register ? 'Už máte účet?' : 'Ešte nemáte účet?' }} <a [routerLink]="register ? '/prihlasenie' : '/registracia'">{{ register ? 'Prihláste sa' : 'Zaregistrujte sa' }}</a></p>
      </form>
    </section>
  `
})
export class AuthComponent {
  readonly api = inject(ApiService);
  private router = inject(Router);
  readonly register = inject(ActivatedRoute).snapshot.routeConfig?.path === 'registracia';
  email = ''; password = ''; role: 'CUSTOMER' | 'PROVIDER' = 'CUSTOMER';
  busy = signal(false); error = signal('');
  demo(email: string) { this.email = email; this.password = 'DemoHeslo123!'; this.error.set(''); }
  submit() {
    if (this.busy()) return;
    this.busy.set(true); this.error.set('');
    const request = this.register ? this.api.register({ email: this.email, password: this.password, role: this.role }) : this.api.login({ email: this.email, password: this.password });
    request.subscribe({ next: auth => { this.busy.set(false); void this.router.navigateByUrl(auth.user.role === 'PROVIDER' ? '/profil' : '/'); }, error: error => { this.busy.set(false); this.error.set(apiError(error)); } });
  }
}

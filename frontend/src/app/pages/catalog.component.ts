import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService, apiError, unitLabel } from '../core/api.service';
import { CategoryResponse, PageResponse, ServiceResponse } from '../core/api-types';
import { IconComponent } from '../shared/icon.component';

@Component({
  standalone: true, imports: [FormsModule, RouterLink, DecimalPipe, IconComponent],
  template: `
    <section class="hero container">
      <div class="hero-copy">
        <p class="eyebrow"><span class="small-line"></span> POMOC, KTORÁ JE BLÍZKO</p>
        <h1>Na každú prácu<br>sa nájde <em>majster.</em></h1>
        <p class="hero-description">Od novej police po krajšiu záhradu. Nájdite šikovných ľudí vo svojom okolí a nechajte si viac času na to, čo máte radi.</p>
        <a class="button hero-cta" href="#ponuka">Nájsť pomoc <app-icon name="arrow" /></a>
        <div class="hero-benefits"><span><app-icon name="check" /> Prehľadné ceny</span><span><app-icon name="check" /> Jednoduché objednanie</span></div>
      </div>
      <div class="hero-board" aria-label="Od nápadu k hotovej práci">
        <div class="board-head"><span>VÁŠ DOMOV. VAŠE PLÁNY.</span><span class="board-sun" aria-hidden="true">✳</span></div>
        <div class="board-illustration" aria-hidden="true">
          <div class="illustration-grid"></div><div class="house-shape"><div class="house-window"></div><div class="house-door"></div></div>
          <div class="plant"><span></span><span></span><i></i></div>
          <div class="tool-sticker"><app-icon name="tool" /></div>
          <span class="illustration-label">Malá pomoc.<br><strong>Veľký rozdiel.</strong></span>
        </div>
        <div class="board-bottom"><span class="board-check"><app-icon name="check" /></span><span>To-do zoznam?<br><strong>Čoskoro hotovo.</strong></span><span class="board-arrow">↗</span></div>
      </div>
    </section>
    <section class="how-strip"><div class="container how-inner"><span class="how-title">Ako to funguje</span>
      <span><b>01</b> Vyberte službu</span><span><b>02</b> Dohodnite termín</span><span><b>03</b> Nechajte to na majstra</span>
    </div></section>
    <section class="container catalog" id="ponuka">
      <div class="section-heading"><div><p class="eyebrow">ČO POTREBUJETE VYRIEŠIŤ?</p><h2>Dobrá pomoc začína tu.</h2></div><span class="result-count">{{ result()?.totalElements ?? 0 }} dostupných služieb</span></div>
      <form class="search-bar" (ngSubmit)="search()">
        <label class="search-input"><app-icon name="search" /><input aria-label="Hľadať službu" name="q" [(ngModel)]="q" placeholder="S čím potrebujete pomôcť?" maxlength="150"></label>
        <label class="location-input"><app-icon name="pin" /><input aria-label="Mesto" name="location" [(ngModel)]="location" placeholder="Mesto alebo obec" maxlength="150"></label>
        <button class="button" type="submit">Vyhľadať <app-icon name="arrow" /></button>
      </form>
      <div class="category-tabs" aria-label="Kategórie">
        <button [class.selected]="categoryId === undefined" [attr.aria-pressed]="categoryId === undefined" (click)="category(undefined)">Všetky služby</button>
        @for (cat of categories(); track cat.id) {
          <button [class.selected]="categoryId === cat.id" [attr.aria-pressed]="categoryId === cat.id" (click)="category(cat.id)"><app-icon [name]="icon(cat.name)" />{{ cat.name }}</button>
        }
      </div>
      @if (error()) {
        <div class="notice error" role="alert"><strong>Nepodarilo sa načítať ponuku.</strong><p>{{ error() }}</p><button class="button button-small" (click)="search()">Skúsiť znova</button></div>
      } @else if (loading()) {
        <div class="loading" role="status"><span class="spinner"></span> Hľadáme šikovné ruky…</div>
      } @else if (result()?.content?.length) {
        <div class="service-grid">
          @for (service of result()!.content; track service.id) {
            <a class="service-card" [routerLink]="['/sluzby', service.id]">
              <div class="card-art" [attr.data-kind]="icon(service.category.name)">
                <span class="category-tag">{{ service.category.name }}</span><app-icon [name]="icon(service.category.name)" /><span class="card-art-number">0{{ service.id }}</span>
              </div>
              <div class="card-body"><div class="card-location"><app-icon name="pin" /> {{ service.provider.location }} <span class="available" [class.unavailable]="!service.provider.availability">{{ service.provider.availability ? 'Dostupný' : 'Nedostupný' }}</span></div>
                <h3>{{ service.name }}</h3><p class="card-description">{{ service.description }}</p>
                <div class="provider-line"><span class="avatar">{{ initials(service.provider.name) }}</span><span>{{ service.provider.name }}</span></div>
                <div class="card-bottom"><span><strong>{{ service.price | number:'1.2-2' }} €</strong> / {{ unit(service.priceUnit) }}</span><span class="round-arrow"><app-icon name="arrow" /></span></div>
              </div>
            </a>
          }
        </div>
        @if (result()!.totalPages > 1) {
          <div class="pagination"><button class="button button-outline button-small" [disabled]="page === 0" (click)="changePage(-1)">Predošlé</button><span>Strana {{ page + 1 }} z {{ result()!.totalPages }}</span><button class="button button-outline button-small" [disabled]="page + 1 >= result()!.totalPages" (click)="changePage(1)">Ďalšie</button></div>
        }
      } @else {
        <div class="empty-state"><app-icon name="search" /><h3>Zatiaľ sme nič nenašli.</h3><p>Skúste inú službu, mesto alebo zrušte filtre.</p><button class="button button-outline" (click)="reset()">Zobraziť všetky služby</button></div>
      }
    </section>
    <section class="container"><div class="bottom-cta"><div><p class="eyebrow">MÁTE ŠIKOVNÉ RUKY?</p><h2>Vaša práca môže niekomu<br>uľahčiť celý deň.</h2></div><a class="button button-light" routerLink="/registracia">Vytvoriť účet poskytovateľa <app-icon name="arrow" /></a></div></section>
  `
})
export class CatalogComponent implements OnInit {
  private api = inject(ApiService);
  categories = signal<CategoryResponse[]>([]);
  result = signal<PageResponse<ServiceResponse> | null>(null);
  loading = signal(true); error = signal('');
  q = ''; location = ''; categoryId?: number; page = 0;
  private generation = 0;
  unit = unitLabel;
  ngOnInit() { this.loadCategories(); this.load(); }
  loadCategories() { this.api.categories().subscribe({ next: c => this.categories.set(c), error: () => {} }); }
  search() { this.page = 0; this.loadCategories(); this.load(); }
  category(id?: number) { this.categoryId = id; this.search(); }
  reset() { this.q = ''; this.location = ''; this.categoryId = undefined; this.search(); }
  changePage(delta: number) { this.page += delta; this.load(); }
  load() {
    const generation = ++this.generation;
    this.loading.set(true); this.error.set('');
    this.api.services({ q: this.q.trim(), location: this.location.trim(), categoryId: this.categoryId, page: this.page }).subscribe({
      next: result => { if (generation === this.generation) { this.result.set(result); this.loading.set(false); } },
      error: error => { if (generation === this.generation) { this.error.set(apiError(error)); this.loading.set(false); } }
    });
  }
  icon(name: string) { return name.includes('Záhr') ? 'leaf' : name.includes('IT') ? 'tech' : name.includes('Sťah') ? 'box' : 'home'; }
  initials(name: string) { return name.split(' ').map(part => part[0]).slice(0, 2).join(''); }
}

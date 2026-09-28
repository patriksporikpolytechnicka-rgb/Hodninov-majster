import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService, SessionService, apiError } from '../core/api.service';
import { OrderResponse, OrderStatus, PageResponse } from '../core/api-types';
import { IconComponent } from '../shared/icon.component';
@Component({
  standalone: true, imports: [DecimalPipe, RouterLink, IconComponent],
  template: `
    <section class="container orders-page"><p class="eyebrow">VŠETKO NA JEDNOM MIESTE</p><h1>{{ session.auth()?.user?.role === 'PROVIDER' ? 'Vaše zákazky.' : 'Vaše objednávky.' }}</h1><p class="page-intro">Od prvého plánu až po hotovú prácu.</p>
      @if (!session.auth()) { <div class="empty-state"><app-icon name="user" /><h3>Najprv sa prihláste.</h3><p>Objednávky sú dostupné len vo vašom účte.</p><a class="button" routerLink="/prihlasenie">Prihlásiť sa</a></div> }
      @else {
        @if (error()) { <div class="notice error" role="alert">{{ error() }} <button class="button button-outline button-small" (click)="load()">Obnoviť</button></div> }
        @if (loading()) { <div class="loading" role="status"><span class="spinner"></span> Načítavame objednávky…</div> }
        @else if (result()?.content?.length) {
          <div class="order-list">@for (order of result()!.content; track order.id) {
            <article class="order-card"><div class="order-heading"><span class="order-number">OBJEDNÁVKA #{{ order.id }}</span><span class="status-badge" [attr.data-status]="order.status">{{ label(order.status) }}</span></div>
              <div class="order-main"><div><h3>{{ order.serviceName }}</h3><p><app-icon name="clock" /> {{ formatDate(order.scheduledAt) }} · slovenský čas</p><p><app-icon name="pin" /> {{ order.address }}</p></div><strong class="order-price">{{ order.totalPrice | number:'1.2-2' }} €</strong></div>
              @if (order.note) { <p class="order-note">{{ order.note }}</p> }
              @if (actions(order).length) { <div class="order-actions">@for (action of actions(order); track action.status) { <button class="button button-small" [class.button-outline]="action.status === 'CANCELLED' || action.status === 'REJECTED'" [disabled]="busyId() !== null" (click)="update(order, action.status)">{{ busyId() === order.id ? 'Ukladáme…' : action.label }}</button> }</div> }
            </article>
          }</div>
          @if (result()!.totalPages > 1) { <div class="pagination"><button class="button button-outline button-small" [disabled]="page === 0" (click)="changePage(-1)">Predošlé</button><span>{{ page + 1 }} / {{ result()!.totalPages }}</span><button class="button button-outline button-small" [disabled]="page + 1 >= result()!.totalPages" (click)="changePage(1)">Ďalšie</button></div> }
        } @else if (!error()) { <div class="empty-state"><app-icon name="box" /><h3>Zatiaľ tu nie sú objednávky.</h3><p>{{ session.auth()!.user.role === 'PROVIDER' ? 'Nové zákaznícke požiadavky sa zobrazia tu.' : 'Vyberte si službu a prvý krok máte za sebou.' }}</p><a class="button" routerLink="/">Prezrieť služby</a></div> }
      }
    </section>
  `
})
export class OrdersComponent implements OnInit {
  private api = inject(ApiService); readonly session = inject(SessionService);
  result = signal<PageResponse<OrderResponse> | null>(null); error = signal(''); loading = signal(false); busyId = signal<number | null>(null); page = 0;
  ngOnInit() { if (this.session.auth()) this.load(); }
  load() { this.loading.set(true); this.error.set(''); this.api.orders(this.page).subscribe({ next: r => { this.result.set(r); this.loading.set(false); }, error: e => { this.error.set(apiError(e)); this.loading.set(false); } }); }
  changePage(delta: number) { this.page += delta; this.load(); }
  label(status: OrderStatus) { return ({ PENDING: 'Čaká na prijatie', ACCEPTED: 'Prijatá', IN_PROGRESS: 'Prebieha', COMPLETED: 'Dokončená', CANCELLED: 'Zrušená', REJECTED: 'Odmietnutá' })[status]; }
  formatDate(date: string) { const [day, time] = date.split('T'); return day.split('-').reverse().join('. ') + ' o ' + time.slice(0, 5); }
  actions(order: OrderResponse): { status: OrderStatus; label: string }[] {
    const provider = this.session.auth()?.user.role === 'PROVIDER';
    const customer = this.session.auth()?.user.role === 'CUSTOMER';
    if (order.status === 'PENDING') return provider ? [{ status: 'ACCEPTED', label: 'Prijať' }, { status: 'REJECTED', label: 'Odmietnuť' }] : customer ? [{ status: 'CANCELLED', label: 'Zrušiť objednávku' }] : [];
    if (order.status === 'ACCEPTED') return provider ? [{ status: 'IN_PROGRESS', label: 'Začať prácu' }, { status: 'CANCELLED', label: 'Zrušiť' }] : customer ? [{ status: 'CANCELLED', label: 'Zrušiť objednávku' }] : [];
    if (order.status === 'IN_PROGRESS' && provider) return [{ status: 'COMPLETED', label: 'Dokončiť prácu' }];
    return [];
  }
  update(order: OrderResponse, status: OrderStatus) {
    if (this.busyId() !== null) return;
    this.busyId.set(order.id); this.error.set('');
    this.api.status(order.id, status).subscribe({ next: () => { this.busyId.set(null); this.load(); }, error: e => { this.busyId.set(null); this.error.set(apiError(e)); } });
  }
}

import { HttpClient, HttpErrorResponse, HttpInterceptorFn, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { tap } from 'rxjs';
import { AuthResponse, CategoryResponse, LoginRequest, OrderRequest, OrderResponse, OrderStatus, PageResponse, RegisterRequest, ServiceResponse } from './api-types';

@Injectable({ providedIn: 'root' })
export class SessionService {
  readonly auth = signal<AuthResponse | null>(null);
  private expiresAt = 0;
  private timer?: ReturnType<typeof setTimeout>;
  set(value: AuthResponse) {
    if (this.timer) clearTimeout(this.timer);
    this.expiresAt = Date.now() + value.expiresIn * 1000;
    this.auth.set(value);
    this.timer = setTimeout(() => this.logout(), value.expiresIn * 1000);
  }
  token(): string | null {
    if (Date.now() >= this.expiresAt) { this.logout(); return null; }
    return this.auth()?.accessToken ?? null;
  }
  logout() { if (this.timer) clearTimeout(this.timer); this.auth.set(null); this.expiresAt = 0; }
}

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  // Credentials only go to this application's relative API paths.
  const token = request.url.startsWith('/api/') ? inject(SessionService).token() : null;
  return next(token ? request.clone({ setHeaders: { Authorization: 'Bearer ' + token } }) : request);
};

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private session = inject(SessionService);
  readonly info = signal<{ name: string; demo: boolean } | null>(null);
  loadInfo() { this.http.get<{ name: string; demo: boolean }>('/api/info').subscribe({ next: value => this.info.set(value), error: () => this.info.set(null) }); }
  categories() { return this.http.get<CategoryResponse[]>('/api/categories'); }
  services(filters: { q?: string; location?: string; categoryId?: number; page?: number }) {
    let params = new HttpParams().set('size', 6).set('page', filters.page ?? 0);
    for (const key of ['q', 'location', 'categoryId'] as const) {
      const value = filters[key];
      if (value !== undefined && value !== '') params = params.set(key, value);
    }
    return this.http.get<PageResponse<ServiceResponse>>('/api/services', { params });
  }
  service(id: number) { return this.http.get<ServiceResponse>('/api/services/' + id); }
  login(request: LoginRequest) { return this.http.post<AuthResponse>('/api/auth/login', request).pipe(tap(value => this.session.set(value))); }
  register(request: RegisterRequest) { return this.http.post<AuthResponse>('/api/auth/register', request).pipe(tap(value => this.session.set(value))); }
  createOrder(request: OrderRequest) { return this.http.post<OrderResponse>('/api/orders', request); }
  orders(page = 0) { return this.http.get<PageResponse<OrderResponse>>('/api/orders/my', { params: { page, size: 10 } }); }
  status(id: number, status: OrderStatus) { return this.http.patch<OrderResponse>('/api/orders/' + id + '/status', { status }); }
}

export function apiError(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) return 'Niečo sa nepodarilo. Skúste to znova.';
  if (error.status === 0 || error.status >= 500) return 'Backend momentálne neodpovedá. Skontrolujte jeho spustenie a skúste to znova.';
  if (error.status === 401) return 'Prihlásenie zlyhalo alebo vypršalo. Skontrolujte údaje a prihláste sa znova.';
  if (error.status === 403) return 'Na túto akciu nemáte oprávnenie.';
  if (error.status === 404) return 'Požadovaný záznam sa nenašiel.';
  if (error.status === 409) return 'Údaje sú v konflikte. E-mail môže byť obsadený alebo sa zmenil stav služby či objednávky.';
  return 'Skontrolujte vyplnené údaje, termín a množstvo. ' + (error.error?.message === 'JOB quantity must be a whole number' ? 'Počet úkonov musí byť celé číslo.' : '');
}
export function unitLabel(unit: string) { return ({ HOUR: 'hod', JOB: 'úkon', SQUARE_METER: 'm²' } as Record<string, string>)[unit] ?? unit; }


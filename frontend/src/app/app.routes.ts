import { ProviderComponent } from './pages/provider.component';
import { Routes } from '@angular/router';
import { CatalogComponent } from './pages/catalog.component';
import { DetailComponent } from './pages/detail.component';
import { AuthComponent } from './pages/auth.component';
import { OrdersComponent } from './pages/orders.component';

export const routes: Routes = [
  { path: 'profil', component: ProviderComponent, title: 'Profil a predplatné | Hodinový majster' },
  { path: '', component: CatalogComponent, title: 'Nájdite pomoc | Hodinový majster' },
  { path: 'sluzby/:id', component: DetailComponent, title: 'Detail služby | Hodinový majster' },
  { path: 'prihlasenie', component: AuthComponent, title: 'Prihlásenie | Hodinový majster' },
  { path: 'registracia', component: AuthComponent, title: 'Registrácia | Hodinový majster' },
  { path: 'objednavky', component: OrdersComponent, title: 'Objednávky | Hodinový majster' },
  { path: '**', redirectTo: '' }
];


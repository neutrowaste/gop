import { Routes } from '@angular/router';

import { authGuard } from './guards/auth.guard';
import { AdminShellPage } from './pages/admin/admin-shell.page';
import { AdminDashboardPage } from './pages/admin/dashboard/admin-dashboard.page';
import { EmpresaDetailPage } from './pages/admin/empresas/empresa-detail.page';
import { EmpresasListPage } from './pages/admin/empresas/empresas-list.page';
import { LoginPage } from './pages/login/login.page';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'admin/login' },
  { path: 'admin/login', component: LoginPage },
  {
    path: 'admin',
    component: AdminShellPage,
    canActivate: [authGuard],
    children: [
      { path: '', component: AdminDashboardPage },
      { path: 'empresas', component: EmpresasListPage },
      { path: 'empresas/:id', component: EmpresaDetailPage },
    ],
  },
  { path: '**', redirectTo: 'admin/login' },
];

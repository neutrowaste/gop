import { Routes } from '@angular/router';

import {
  adminEmpresaGuard,
  adminPlataformaGuard,
  authGuard,
  funcionarioGuard,
} from './guards/auth.guard';
import { AdminShellPage } from './pages/admin/admin-shell.page';
import { AdminDashboardPage } from './pages/admin/dashboard/admin-dashboard.page';
import { EmpresaDetailPage } from './pages/admin/empresas/empresa-detail.page';
import { EmpresasListPage } from './pages/admin/empresas/empresas-list.page';
import { AppHomePage } from './pages/app/home/app-home.page';
import { AppShellPage } from './pages/app/app-shell.page';
import { AppModuloPlaceholderPage } from './pages/app/modulo/app-modulo-placeholder.page';
import { LoginPage } from './pages/login/login.page';
import { OrgDashboardPage } from './pages/org/dashboard/org-dashboard.page';
import { OrgShellPage } from './pages/org/org-shell.page';
import { OrgUsuarioDetailPage } from './pages/org/usuarios/org-usuario-detail.page';
import { OrgUsuariosListPage } from './pages/org/usuarios/org-usuarios-list.page';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'admin/login' },
  { path: 'login', pathMatch: 'full', redirectTo: 'admin/login' },
  { path: 'admin/login', component: LoginPage },
  {
    path: 'admin',
    component: AdminShellPage,
    canActivate: [authGuard, adminPlataformaGuard],
    children: [
      { path: '', component: AdminDashboardPage },
      { path: 'empresas', component: EmpresasListPage },
      { path: 'empresas/:id', component: EmpresaDetailPage },
    ],
  },
  {
    path: 'org',
    component: OrgShellPage,
    canActivate: [authGuard, adminEmpresaGuard],
    children: [
      { path: '', component: OrgDashboardPage },
      { path: 'usuarios', component: OrgUsuariosListPage },
      { path: 'usuarios/:id', component: OrgUsuarioDetailPage },
    ],
  },
  {
    path: 'app',
    component: AppShellPage,
    canActivate: [authGuard, funcionarioGuard],
    children: [
      { path: '', component: AppHomePage },
      { path: 'modulos/:codigo', component: AppModuloPlaceholderPage },
    ],
  },
  { path: '**', redirectTo: 'admin/login' },
];

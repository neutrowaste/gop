import { Routes } from '@angular/router';

import {
  adminEmpresaGuard,
  adminPlataformaGuard,
  authGuard,
  funcionarioGuard,
  orgEscalaGuard,
  orgMenuGuard,
  orgModuloGuard,
  orgPainelGuard,
  orgServicoGuard,
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
import { OrgEscalaDetailPage } from './pages/org/escalas/org-escala-detail.page';
import { OrgEscalasListPage } from './pages/org/escalas/org-escalas-list.page';
import { OrgEquipesListPage } from './pages/org/equipes/org-equipes-list.page';
import { OrgOcorrenciaDetailPage } from './pages/org/ocorrencias/org-ocorrencia-detail.page';
import { OrgOcorrenciasListPage } from './pages/org/ocorrencias/org-ocorrencias-list.page';
import { OrgShellPage } from './pages/org/org-shell.page';
import { OrgSetorDetailPage } from './pages/org/setores/org-setor-detail.page';
import { OrgSetoresListPage } from './pages/org/setores/org-setores-list.page';
import { OrgTrechoDetailPage } from './pages/org/trechos/org-trecho-detail.page';
import { OrgTrechosListPage } from './pages/org/trechos/org-trechos-list.page';
import { OrgServicoPlaceholderPage } from './pages/org/servicos/org-servico-placeholder.page';
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
    canActivate: [authGuard, orgPainelGuard],
    children: [
      { path: '', component: OrgDashboardPage },
      {
        path: 'usuarios',
        component: OrgUsuariosListPage,
        canActivate: [adminEmpresaGuard],
      },
      {
        path: 'usuarios/:id',
        component: OrgUsuarioDetailPage,
        canActivate: [adminEmpresaGuard],
      },
      {
        path: 'setores',
        component: OrgSetoresListPage,
        canActivate: [orgMenuGuard('SETORES')],
      },
      {
        path: 'setores/:id',
        component: OrgSetorDetailPage,
        canActivate: [orgMenuGuard('SETORES')],
      },
      {
        path: 'trechos',
        component: OrgTrechosListPage,
        canActivate: [orgMenuGuard('TRECHOS')],
      },
      {
        path: 'trechos/:id',
        component: OrgTrechoDetailPage,
        canActivate: [orgMenuGuard('TRECHOS')],
      },
      {
        path: 'equipes',
        component: OrgEquipesListPage,
        canActivate: [orgMenuGuard('COLABORADORES')],
      },
      {
        path: 'mapa',
        canActivate: [orgModuloGuard('MONITORAMENTO')],
        loadComponent: () => import('./pages/org/mapa/org-mapa.page').then((m) => m.OrgMapaPage),
      },
      {
        path: 'escalas',
        component: OrgEscalasListPage,
        canActivate: [orgEscalaGuard],
      },
      {
        path: 'escalas/:id',
        component: OrgEscalaDetailPage,
        canActivate: [orgEscalaGuard],
      },
      {
        path: 'ocorrencias',
        component: OrgOcorrenciasListPage,
        canActivate: [orgMenuGuard('OCORRENCIAS')],
      },
      {
        path: 'ocorrencias/:id',
        component: OrgOcorrenciaDetailPage,
        canActivate: [orgMenuGuard('OCORRENCIAS')],
      },
      {
        path: 'servicos/:codigo/dashboard',
        canActivate: [orgServicoGuard],
        loadComponent: () =>
          import('./pages/org/servicos/org-servico-dashboard.page').then(
            (m) => m.OrgServicoDashboardPage,
          ),
      },
      {
        path: 'servicos/:codigo',
        component: OrgServicoPlaceholderPage,
        canActivate: [orgServicoGuard],
      },
    ],
  },
  {
    path: 'app',
    component: AppShellPage,
    canActivate: [authGuard, funcionarioGuard],
    children: [
      { path: '', component: AppHomePage },
      {
        path: 'modulos/MONITORAMENTO',
        loadComponent: () => import('./pages/org/mapa/org-mapa.page').then((m) => m.OrgMapaPage),
      },
      { path: 'modulos/:codigo', component: AppModuloPlaceholderPage },
    ],
  },
  { path: '**', redirectTo: 'admin/login' },
];

import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { EmpresaModulo } from '../../models/organizacao';
import { AutenticacaoService } from '../../services/autenticacao.service';
import { OrganizacaoApiService } from '../../services/organizacao-api.service';

const ICONES_SERVICO: Record<string, string> = {
  ESCALA: 'ph-calendar-check',
  VARRIACAO_VIAS: 'ph-broom',
  COLETA_PAPELEIRAS: 'ph-recycle',
  MONITORAMENTO: 'ph-map-pin-line',
  AUDITORIA: 'ph-clipboard-text',
};

@Component({
  selector: 'app-org-shell-page',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './org-shell.page.html',
})
export class OrgShellPage implements OnInit, OnDestroy {
  private readonly autenticacao = inject(AutenticacaoService);
  private readonly orgApi = inject(OrganizacaoApiService);
  private readonly router = inject(Router);
  private menuTimer: ReturnType<typeof setTimeout> | null = null;

  readonly usuario = this.autenticacao.usuario;
  readonly ehAdminEmpresa = this.autenticacao.ehAdminEmpresa;
  readonly servicos = signal<EmpresaModulo[]>([]);
  readonly menuServico = signal<{ codigo: string; top: number; left: number } | null>(null);

  pode(codigo: string): boolean {
    return this.autenticacao.temAcessoMenu(codigo);
  }

  ngOnInit(): void {
    this.orgApi.listarModulosContratados().subscribe({
      next: (modulos) => this.servicos.set(modulos),
      error: () => this.servicos.set([]),
    });
  }

  ngOnDestroy(): void {
    this.limparTimerMenu();
  }

  iconeServico(codigo: string): string {
    return ICONES_SERVICO[codigo?.toUpperCase()] ?? 'ph-package';
  }

  rotaServico(codigo: string): string {
    switch (codigo?.toUpperCase()) {
      case 'MONITORAMENTO':
        return '/org/mapa';
      case 'ESCALA':
        return '/org/escalas';
      default:
        return `/org/servicos/${codigo}`;
    }
  }

  rotaDashboardServico(codigo: string): string {
    return `/org/servicos/${codigo}/dashboard`;
  }

  servicoAtivo(codigo: string): boolean {
    const url = this.router.url.toUpperCase();
    const chave = codigo.toUpperCase();
    const rota = this.rotaServico(codigo).toUpperCase();
    return url === rota || url.startsWith(`${rota}/`) || url.includes(`/SERVICOS/${chave}`);
  }

  dashboardServicoAtivo(codigo: string): boolean {
    return this.router.url.toUpperCase().includes(`/SERVICOS/${codigo.toUpperCase()}/DASHBOARD`);
  }

  abrirMenuServico(servico: EmpresaModulo, evento: MouseEvent): void {
    this.limparTimerMenu();
    const ret = (evento.currentTarget as HTMLElement).getBoundingClientRect();
    const alturaMenu = 48;
    this.menuServico.set({
      codigo: servico.codigo,
      top: Math.min(ret.top, window.innerHeight - alturaMenu - 8),
      left: ret.right,
    });
  }

  manterMenuServico(): void {
    this.limparTimerMenu();
  }

  agendarFecharMenuServico(): void {
    this.limparTimerMenu();
    this.menuTimer = setTimeout(() => this.menuServico.set(null), 180);
  }

  sair(): void {
    this.autenticacao.logout();
    void this.router.navigateByUrl('/admin/login');
  }

  private limparTimerMenu(): void {
    if (this.menuTimer) {
      clearTimeout(this.menuTimer);
      this.menuTimer = null;
    }
  }
}

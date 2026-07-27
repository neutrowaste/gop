import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { EmpresaModulo, OrganizacaoUsuario } from '../../../models/organizacao';
import { AutenticacaoService } from '../../../services/autenticacao.service';
import { mensagemHttp } from '../../../services/http-erro';
import { OrganizacaoApiService } from '../../../services/organizacao-api.service';

@Component({
  selector: 'app-org-dashboard-page',
  imports: [RouterLink],
  templateUrl: './org-dashboard.page.html',
})
export class OrgDashboardPage implements OnInit {
  private readonly autenticacao = inject(AutenticacaoService);
  private readonly orgApi = inject(OrganizacaoApiService);

  readonly usuario = this.autenticacao.usuario;
  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly totalUsuarios = signal(0);
  readonly usuariosAtivos = signal(0);
  readonly modulosContratados = signal(0);
  readonly recentes = signal<OrganizacaoUsuario[]>([]);
  readonly modulos = signal<EmpresaModulo[]>([]);

  ngOnInit(): void {
    this.orgApi.listarUsuarios().subscribe({
      next: (usuarios) => {
        this.totalUsuarios.set(usuarios.length);
        this.usuariosAtivos.set(usuarios.filter((u) => u.ativo).length);
        this.recentes.set(usuarios.slice(0, 5));
        this.carregarModulos();
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao carregar visão geral'));
        this.carregando.set(false);
      },
    });
  }

  private carregarModulos(): void {
    this.orgApi.listarModulosContratados().subscribe({
      next: (modulos) => {
        this.modulos.set(modulos);
        this.modulosContratados.set(modulos.length);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao carregar módulos contratados'));
        this.carregando.set(false);
      },
    });
  }
}

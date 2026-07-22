import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Empresa } from '../../../models/empresa';
import { AutenticacaoService } from '../../../services/autenticacao.service';
import { EmpresaAdminApiService } from '../../../services/empresa-admin-api.service';
import { mensagemHttp } from '../../../services/http-erro';

@Component({
  selector: 'app-admin-dashboard-page',
  imports: [RouterLink],
  templateUrl: './admin-dashboard.page.html',
})
export class AdminDashboardPage implements OnInit {
  private readonly autenticacao = inject(AutenticacaoService);
  private readonly empresasApi = inject(EmpresaAdminApiService);

  readonly usuario = this.autenticacao.usuario;
  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly totalEmpresas = signal(0);
  readonly empresasAtivas = signal(0);
  readonly recentes = signal<Empresa[]>([]);

  ngOnInit(): void {
    this.empresasApi.listar().subscribe({
      next: (empresas) => {
        this.totalEmpresas.set(empresas.length);
        this.empresasAtivas.set(empresas.filter((e) => e.ativo).length);
        this.recentes.set(empresas.slice(0, 5));
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao carregar visão geral'));
        this.carregando.set(false);
      },
    });
  }
}

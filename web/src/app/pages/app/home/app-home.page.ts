import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { UsuarioModulo } from '../../../models/organizacao';
import { AutenticacaoService } from '../../../services/autenticacao.service';
import { mensagemHttp } from '../../../services/http-erro';

const DESCRICOES: Record<string, string> = {
  ESCALA: 'Amarração diária de setor, equipe, GPS e supervisor.',
  VARRIACAO_VIAS: 'Operação e acompanhamento de varrição de vias.',
  COLETA_PAPELEIRAS: 'Roteiros e execução de coleta de papeleiras.',
  MONITORAMENTO: 'Mapa operacional e acompanhamento em tempo real.',
  AUDITORIA: 'Evidências, justificativas e informe diário.',
};

@Component({
  selector: 'app-funcionario-home-page',
  imports: [RouterLink],
  templateUrl: './app-home.page.html',
})
export class AppHomePage implements OnInit {
  private readonly autenticacao = inject(AutenticacaoService);

  readonly usuario = this.autenticacao.usuario;
  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly modulos = signal<UsuarioModulo[]>([]);

  ngOnInit(): void {
    this.autenticacao.meusModulos().subscribe({
      next: (modulos) => {
        this.modulos.set(modulos);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao carregar seus módulos'));
        this.carregando.set(false);
      },
    });
  }

  descricao(codigo: string): string {
    return DESCRICOES[codigo] ?? 'Módulo operacional liberado para o seu usuário.';
  }

  rotuloCategoria(categoria: string): string {
    switch (categoria?.toUpperCase()) {
      case 'OPERACIONAL':
        return 'Operacional';
      case 'SUPORTE':
        return 'Suporte';
      case 'TRANSVERSAL':
        return 'Transversal';
      default:
        return categoria;
    }
  }
}

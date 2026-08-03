import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Ocorrencia, TIPO_OCORRENCIA_LABELS } from '../../../models/cadastros-operacionais';
import { CadastrosOperacionaisApiService } from '../../../services/cadastros-operacionais-api.service';
import { mensagemHttp } from '../../../services/http-erro';

@Component({
  selector: 'app-org-ocorrencia-detail-page',
  imports: [ReactiveFormsModule, RouterLink, DatePipe],
  templateUrl: './org-ocorrencia-detail.page.html',
})
export class OrgOcorrenciaDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(CadastrosOperacionaisApiService);

  readonly tipoLabels = TIPO_OCORRENCIA_LABELS;

  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly ok = signal<string | null>(null);
  readonly ocorrencia = signal<Ocorrencia | null>(null);

  readonly form = this.fb.nonNullable.group({
    observacao: [''],
  });

  private id = 0;

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.id) {
      this.erro.set('Ocorrência inválida');
      this.carregando.set(false);
      return;
    }
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.api.buscarOcorrencia(this.id).subscribe({
      next: (ocorrencia) => {
        this.ocorrencia.set(ocorrencia);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao carregar ocorrência'));
        this.carregando.set(false);
      },
    });
  }

  resolver(status: 'RESOLVIDA' | 'JUSTIFICADA'): void {
    this.salvando.set(true);
    this.erro.set(null);
    this.ok.set(null);
    const observacao = this.form.controls.observacao.value.trim() || null;
    this.api.resolverOcorrencia(this.id, { status, observacao }).subscribe({
      next: (ocorrencia) => {
        this.ocorrencia.set(ocorrencia);
        this.salvando.set(false);
        this.ok.set(status === 'JUSTIFICADA' ? 'Ocorrência justificada.' : 'Ocorrência resolvida.');
      },
      error: (erro: unknown) => {
        this.salvando.set(false);
        this.erro.set(mensagemHttp(erro, 'Falha ao tratar ocorrência'));
      },
    });
  }
}

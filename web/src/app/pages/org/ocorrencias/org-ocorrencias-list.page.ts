import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import {
  Ocorrencia,
  Setor,
  StatusOcorrencia,
  TIPO_OCORRENCIA_LABELS,
  TipoOcorrencia,
  rotuloSetor,
} from '../../../models/cadastros-operacionais';
import { CadastrosOperacionaisApiService } from '../../../services/cadastros-operacionais-api.service';
import { hojeIso as hoje } from '../../../services/data-local';
import { mensagemHttp } from '../../../services/http-erro';

@Component({
  selector: 'app-org-ocorrencias-list-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './org-ocorrencias-list.page.html',
})
export class OrgOcorrenciasListPage implements OnInit {
  private readonly api = inject(CadastrosOperacionaisApiService);
  private readonly fb = inject(FormBuilder);

  readonly tipoLabels = TIPO_OCORRENCIA_LABELS;
  readonly rotuloSetor = rotuloSetor;

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly ocorrencias = signal<Ocorrencia[]>([]);

  readonly setores = signal<Setor[]>([]);

  readonly filtro = this.fb.nonNullable.group({
    data: [hoje()],
    setorId: [0],
    status: ['ABERTA' as StatusOcorrencia | ''],
    tipo: ['' as TipoOcorrencia | ''],
  });

  ngOnInit(): void {
    this.api.listarSetores().subscribe({ next: (lista) => this.setores.set(lista.filter((s) => s.ativo)) });

    this.carregar();
    this.filtro.controls.setorId.valueChanges.subscribe(() => this.carregar());
    this.filtro.controls.status.valueChanges.subscribe(() => this.carregar());
    this.filtro.controls.tipo.valueChanges.subscribe(() => this.carregar());
    this.filtro.controls.data.valueChanges.subscribe(() => this.carregar());
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    const raw = this.filtro.getRawValue();
    this.api
      .listarOcorrencias({
        data: raw.data || undefined,
        setorId: raw.setorId || undefined,
        status: raw.status || undefined,
        tipo: raw.tipo || undefined,
      })
      .subscribe({
        next: (lista) => {
          this.ocorrencias.set(lista);
          this.carregando.set(false);
        },
        error: (erro: unknown) => {
          this.erro.set(mensagemHttp(erro, 'Falha ao listar ocorrências'));
          this.carregando.set(false);
        },
      });
  }

  limparFiltros(): void {
    this.filtro.reset({ data: '', setorId: 0, status: '', tipo: '' });
  }
}

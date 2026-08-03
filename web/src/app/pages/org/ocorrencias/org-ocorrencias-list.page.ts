import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import {
  Equipe,
  Ocorrencia,
  Setor,
  StatusOcorrencia,
  TIPO_OCORRENCIA_LABELS,
  TipoOcorrencia,
} from '../../../models/cadastros-operacionais';
import { CadastrosOperacionaisApiService } from '../../../services/cadastros-operacionais-api.service';
import { mensagemHttp } from '../../../services/http-erro';

function hoje(): string {
  return new Date().toISOString().slice(0, 10);
}

@Component({
  selector: 'app-org-ocorrencias-list-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './org-ocorrencias-list.page.html',
})
export class OrgOcorrenciasListPage implements OnInit {
  private readonly api = inject(CadastrosOperacionaisApiService);
  private readonly fb = inject(FormBuilder);

  readonly tipoLabels = TIPO_OCORRENCIA_LABELS;

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly ocorrencias = signal<Ocorrencia[]>([]);

  readonly setores = signal<Setor[]>([]);
  readonly equipes = signal<Equipe[]>([]);

  readonly filtro = this.fb.nonNullable.group({
    data: [hoje()],
    setorId: [0],
    equipeId: [0],
    status: ['ABERTA' as StatusOcorrencia | ''],
    tipo: ['' as TipoOcorrencia | ''],
  });

  readonly equipesDoSetor = computed(() => {
    const setorId = this.filtro.controls.setorId.value;
    return this.equipes().filter((e) => !setorId || e.setorId === setorId);
  });

  ngOnInit(): void {
    this.api.listarSetores().subscribe({ next: (lista) => this.setores.set(lista.filter((s) => s.ativo)) });
    this.api.listarEquipes().subscribe({ next: (lista) => this.equipes.set(lista) });

    this.carregar();
    this.filtro.controls.setorId.valueChanges.subscribe(() => {
      this.filtro.controls.equipeId.setValue(0);
      this.carregar();
    });
    this.filtro.controls.equipeId.valueChanges.subscribe(() => this.carregar());
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
        equipeId: raw.equipeId || undefined,
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
    this.filtro.reset({ data: '', setorId: 0, equipeId: 0, status: '', tipo: '' });
  }
}

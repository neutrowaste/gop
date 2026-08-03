import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import {
  Equipamento,
  Equipe,
  Escala,
  Setor,
  StatusEscala,
  TURNO_LABELS,
  TURNO_OPCOES,
  Turno,
} from '../../../models/cadastros-operacionais';
import { OrganizacaoUsuario } from '../../../models/organizacao';
import { CadastrosOperacionaisApiService } from '../../../services/cadastros-operacionais-api.service';
import { mensagemHttp } from '../../../services/http-erro';
import { OrganizacaoApiService } from '../../../services/organizacao-api.service';

function hoje(): string {
  return new Date().toISOString().slice(0, 10);
}

@Component({
  selector: 'app-org-escalas-list-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './org-escalas-list.page.html',
})
export class OrgEscalasListPage implements OnInit {
  private readonly api = inject(CadastrosOperacionaisApiService);
  private readonly orgApi = inject(OrganizacaoApiService);
  private readonly fb = inject(FormBuilder);

  readonly turnoOpcoes = TURNO_OPCOES;
  readonly turnoLabels = TURNO_LABELS;

  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly ok = signal<string | null>(null);

  readonly escalas = signal<Escala[]>([]);
  readonly setores = signal<Setor[]>([]);
  readonly equipes = signal<Equipe[]>([]);
  readonly equipamentos = signal<Equipamento[]>([]);
  readonly supervisores = signal<OrganizacaoUsuario[]>([]);

  readonly mostrarForm = signal(false);

  readonly filtro = this.fb.nonNullable.group({
    data: [hoje()],
    setorId: [0],
    equipeId: [0],
    status: ['' as StatusEscala | ''],
  });

  readonly form = this.fb.nonNullable.group({
    data: [hoje(), [Validators.required]],
    turno: ['MANHA' as Turno, [Validators.required]],
    setorId: [0, [Validators.required, Validators.min(1)]],
    equipeId: [0, [Validators.required, Validators.min(1)]],
    equipamentoId: [0, [Validators.required, Validators.min(1)]],
    supervisorUsuarioId: [0, [Validators.required, Validators.min(1)]],
  });

  readonly equipesDoSetorForm = computed(() => {
    const setorId = this.form.controls.setorId.value;
    return this.equipes().filter((e) => e.ativo && (!setorId || e.setorId === setorId));
  });

  readonly equipamentosDaEquipeForm = computed(() => {
    const equipeId = this.form.controls.equipeId.value;
    return this.equipamentos().filter((e) => e.ativo && (!equipeId || e.equipeId === equipeId));
  });

  readonly equipesDoSetorFiltro = computed(() => {
    const setorId = this.filtro.controls.setorId.value;
    return this.equipes().filter((e) => !setorId || e.setorId === setorId);
  });

  ngOnInit(): void {
    this.carregarBases();
    this.carregar();

    this.filtro.controls.setorId.valueChanges.subscribe(() => {
      this.filtro.controls.equipeId.setValue(0);
      this.carregar();
    });
    this.filtro.controls.equipeId.valueChanges.subscribe(() => this.carregar());
    this.filtro.controls.status.valueChanges.subscribe(() => this.carregar());
    this.filtro.controls.data.valueChanges.subscribe(() => this.carregar());

    this.form.controls.setorId.valueChanges.subscribe(() => {
      this.form.controls.equipeId.setValue(0);
      this.form.controls.equipamentoId.setValue(0);
    });
    this.form.controls.equipeId.valueChanges.subscribe(() => {
      this.form.controls.equipamentoId.setValue(0);
    });
  }

  carregarBases(): void {
    this.api.listarSetores().subscribe({ next: (lista) => this.setores.set(lista.filter((s) => s.ativo)) });
    this.api.listarEquipes().subscribe({ next: (lista) => this.equipes.set(lista) });
    this.api.listarEquipamentos().subscribe({ next: (lista) => this.equipamentos.set(lista) });
    this.orgApi.listarUsuarios().subscribe({
      next: (lista) => this.supervisores.set(lista.filter((u) => u.ativo)),
    });
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    const raw = this.filtro.getRawValue();
    this.api
      .listarEscalas({
        data: raw.data || undefined,
        setorId: raw.setorId || undefined,
        equipeId: raw.equipeId || undefined,
        status: raw.status || undefined,
      })
      .subscribe({
        next: (lista) => {
          this.escalas.set(lista);
          this.carregando.set(false);
        },
        error: (erro: unknown) => {
          this.erro.set(mensagemHttp(erro, 'Falha ao listar escalas'));
          this.carregando.set(false);
        },
      });
  }

  limparFiltros(): void {
    this.filtro.reset({ data: '', setorId: 0, equipeId: 0, status: '' });
  }

  abrirForm(): void {
    this.form.reset({
      data: this.filtro.controls.data.value || hoje(),
      turno: 'MANHA',
      setorId: this.setores()[0]?.id ?? 0,
      equipeId: 0,
      equipamentoId: 0,
      supervisorUsuarioId: this.supervisores()[0]?.id ?? 0,
    });
    this.mostrarForm.set(true);
  }

  fecharForm(): void {
    this.mostrarForm.set(false);
  }

  criar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.salvando.set(true);
    this.erro.set(null);
    this.ok.set(null);
    const raw = this.form.getRawValue();
    this.api
      .criarEscala({
        data: raw.data,
        turno: raw.turno,
        setorId: Number(raw.setorId),
        equipeId: Number(raw.equipeId),
        equipamentoId: Number(raw.equipamentoId),
        supervisorUsuarioId: Number(raw.supervisorUsuarioId),
        ativo: true,
      })
      .subscribe({
        next: () => {
          this.salvando.set(false);
          this.fecharForm();
          this.ok.set('Escala criada em rascunho.');
          this.carregar();
        },
        error: (erro: unknown) => {
          this.salvando.set(false);
          this.erro.set(mensagemHttp(erro, 'Falha ao criar escala'));
        },
      });
  }
}

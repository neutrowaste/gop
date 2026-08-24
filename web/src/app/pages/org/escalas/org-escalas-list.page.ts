import { Component, OnInit, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import {
  Colaborador,
  DIA_SEMANA_OPCOES,
  DiaSemana,
  Escala,
  Setor,
  TIPO_ESCALA_LABELS,
  TURNO_LABELS,
  TURNO_OPCOES,
  TipoEscala,
  Trecho,
  Turno,
  rotuloSetor,
} from '../../../models/cadastros-operacionais';
import { OrganizacaoUsuario } from '../../../models/organizacao';
import { CadastrosOperacionaisApiService } from '../../../services/cadastros-operacionais-api.service';
import { hojeIso as hoje } from '../../../services/data-local';
import { mensagemHttp } from '../../../services/http-erro';
import { OrganizacaoApiService } from '../../../services/organizacao-api.service';

function minLengthArray(min: number) {
  return (control: AbstractControl) => {
    const valor = control.value as unknown;
    return Array.isArray(valor) && valor.length >= min ? null : { minLengthArray: true };
  };
}

export type VisaoEscala = 'dia' | 'frequencias';

@Component({
  selector: 'app-org-escalas-list-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './org-escalas-list.page.html',
})
export class OrgEscalasListPage implements OnInit {
  private readonly api = inject(CadastrosOperacionaisApiService);
  private readonly orgApi = inject(OrganizacaoApiService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  readonly turnoOpcoes = TURNO_OPCOES;
  readonly turnoLabels = TURNO_LABELS;
  readonly tipoLabels = TIPO_ESCALA_LABELS;
  readonly diaSemanaOpcoes = DIA_SEMANA_OPCOES;
  readonly rotuloSetor = rotuloSetor;

  readonly visao = signal<VisaoEscala>('dia');
  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly ok = signal<string | null>(null);

  readonly escalas = signal<Escala[]>([]);
  readonly setores = signal<Setor[]>([]);
  readonly colaboradores = signal<Colaborador[]>([]);
  readonly supervisores = signal<OrganizacaoUsuario[]>([]);
  readonly trechosDoSetor = signal<Trecho[]>([]);

  readonly mostrarForm = signal(false);

  readonly filtro = this.fb.nonNullable.group({
    data: [hoje()],
    setorId: [0],
    tipo: ['' as TipoEscala | ''],
  });

  readonly form = this.fb.nonNullable.group({
    tipo: ['AVULSA' as TipoEscala, [Validators.required]],
    data: [hoje(), [Validators.required]],
    diasSemana: this.fb.nonNullable.control<DiaSemana[]>([]),
    turno: ['MANHA' as Turno, [Validators.required]],
    setorId: [0, [Validators.required, Validators.min(1)]],
    colaboradorIds: this.fb.nonNullable.control<number[]>([], minLengthArray(1)),
    trechoIds: this.fb.nonNullable.control<number[]>([]),
    supervisorUsuarioId: [0, [Validators.required, Validators.min(1)]],
  });

  ngOnInit(): void {
    this.carregarBases();
    this.carregar();

    this.filtro.controls.setorId.valueChanges.subscribe(() => this.carregar());
    this.filtro.controls.tipo.valueChanges.subscribe(() => this.carregar());
    this.filtro.controls.data.valueChanges.subscribe(() => {
      if (this.visao() === 'dia') {
        this.carregar();
      }
    });
    this.form.controls.tipo.valueChanges.subscribe((tipo) => this.aplicarValidadoresTipo(tipo));
    this.form.controls.setorId.valueChanges.subscribe((setorId) => this.carregarTrechosDoSetor(Number(setorId)));
  }

  mostrarDia(): void {
    this.visao.set('dia');
    this.carregar();
  }

  mostrarFrequencias(): void {
    this.visao.set('frequencias');
    this.carregar();
  }

  carregarBases(): void {
    this.api.listarSetores().subscribe({ next: (lista) => this.setores.set(lista.filter((s) => s.ativo)) });
    this.api.listarColaboradores().subscribe({
      next: (lista) => this.colaboradores.set(lista.filter((c) => c.ativo)),
    });
    this.orgApi.listarUsuarios().subscribe({
      next: (lista) => this.supervisores.set(lista.filter((u) => u.ativo)),
    });
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    const raw = this.filtro.getRawValue();
    const frequencias = this.visao() === 'frequencias';
    this.api
      .listarEscalas({
        data: frequencias ? undefined : raw.data || undefined,
        setorId: raw.setorId || undefined,
        tipo: frequencias ? 'FREQUENCIA' : raw.tipo || undefined,
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
    this.filtro.reset({ data: this.visao() === 'dia' ? hoje() : '', setorId: 0, tipo: '' });
    this.carregar();
  }

  abrirForm(): void {
    const tipoInicial: TipoEscala = this.visao() === 'frequencias' ? 'FREQUENCIA' : 'AVULSA';
    this.form.reset({
      tipo: tipoInicial,
      data: this.filtro.controls.data.value || hoje(),
      diasSemana: [],
      turno: 'MANHA',
      setorId: this.setores()[0]?.id ?? 0,
      colaboradorIds: [],
      trechoIds: [],
      supervisorUsuarioId: this.supervisores()[0]?.id ?? 0,
    });
    this.aplicarValidadoresTipo(tipoInicial);
    this.carregarTrechosDoSetor(Number(this.form.controls.setorId.value));
    this.mostrarForm.set(true);
  }

  fecharForm(): void {
    this.mostrarForm.set(false);
  }

  colaboradoresSelecionados(): number[] {
    return this.form.controls.colaboradorIds.value;
  }

  todosColaboradoresSelecionados(): boolean {
    const ativos = this.colaboradores();
    return ativos.length > 0 && ativos.every((c) => this.colaboradoresSelecionados().includes(c.id));
  }

  toggleTodosColaboradores(marcado: boolean): void {
    this.form.controls.colaboradorIds.setValue(marcado ? this.colaboradores().map((c) => c.id) : []);
    this.form.controls.colaboradorIds.markAsTouched();
  }

  toggleColaborador(id: number, marcado: boolean): void {
    const atual = this.colaboradoresSelecionados();
    const proximo = marcado ? [...atual, id] : atual.filter((item) => item !== id);
    this.form.controls.colaboradorIds.setValue([...new Set(proximo)]);
    this.form.controls.colaboradorIds.markAsTouched();
  }

  trechosSelecionados(): number[] {
    return this.form.controls.trechoIds.value;
  }

  todosTrechosSelecionados(): boolean {
    const trechos = this.trechosDoSetor();
    return trechos.length > 0 && trechos.every((t) => this.trechosSelecionados().includes(t.id));
  }

  toggleTodosTrechos(marcado: boolean): void {
    this.form.controls.trechoIds.setValue(marcado ? this.trechosDoSetor().map((t) => t.id) : []);
    this.form.controls.trechoIds.markAsTouched();
  }

  toggleTrecho(id: number, marcado: boolean): void {
    const atual = this.trechosSelecionados();
    const proximo = marcado ? [...atual, id] : atual.filter((item) => item !== id);
    this.form.controls.trechoIds.setValue([...new Set(proximo)]);
    this.form.controls.trechoIds.markAsTouched();
  }

  diasSelecionados(): DiaSemana[] {
    return this.form.controls.diasSemana.value;
  }

  toggleDiaSemana(dia: DiaSemana, marcado: boolean): void {
    const atual = this.diasSelecionados();
    const proximo = marcado ? [...atual, dia] : atual.filter((item) => item !== dia);
    this.form.controls.diasSemana.setValue([...new Set(proximo)]);
    this.form.controls.diasSemana.markAsTouched();
  }

  nomesColaboradores(escala: Escala): string {
    return escala.colaboradores.map((c) => c.nome).join(', ') || '—';
  }

  codigosLutocar(escala: Escala): string {
    return [...new Set(escala.colaboradores.map((c) => c.equipamentoCodigo))].join(', ') || '—';
  }

  rotuloDias(escala: Escala): string {
    if (!escala.diasSemana?.length) {
      return '—';
    }
    const labels = new Map(this.diaSemanaOpcoes.map((d) => [d.valor, d.label]));
    return escala.diasSemana.map((dia) => labels.get(dia) ?? dia).join(', ');
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
    const frequencia = raw.tipo === 'FREQUENCIA';
    this.api
      .criarEscala({
        tipo: raw.tipo,
        data: frequencia ? null : raw.data,
        diasSemana: frequencia ? raw.diasSemana : [],
        turno: raw.turno,
        setorId: Number(raw.setorId),
        colaboradorIds: raw.colaboradorIds,
        trechoIds: raw.trechoIds,
        supervisorUsuarioId: Number(raw.supervisorUsuarioId),
        ativo: true,
      })
      .subscribe({
        next: (escala) => {
          this.salvando.set(false);
          this.fecharForm();
          void this.router.navigate(['/org/escalas', escala.id]);
        },
        error: (erro: unknown) => {
          this.salvando.set(false);
          this.erro.set(mensagemHttp(erro, 'Falha ao criar escala'));
        },
      });
  }

  private aplicarValidadoresTipo(tipo: TipoEscala): void {
    if (tipo === 'FREQUENCIA') {
      this.form.controls.data.clearValidators();
      this.form.controls.diasSemana.setValidators(minLengthArray(1));
    } else {
      this.form.controls.data.setValidators([Validators.required]);
      this.form.controls.diasSemana.clearValidators();
    }
    this.form.controls.data.updateValueAndValidity({ emitEvent: false });
    this.form.controls.diasSemana.updateValueAndValidity({ emitEvent: false });
  }

  private carregarTrechosDoSetor(setorId: number): void {
    this.form.controls.trechoIds.setValue([]);
    if (!setorId) {
      this.trechosDoSetor.set([]);
      this.aplicarValidadorTrechos(false);
      return;
    }
    this.api.listarTrechos(setorId).subscribe({
      next: (lista) => {
        const ativos = lista.filter((t) => t.ativo);
        this.trechosDoSetor.set(ativos);
        this.aplicarValidadorTrechos(ativos.length > 0);
      },
      error: () => {
        this.trechosDoSetor.set([]);
        this.aplicarValidadorTrechos(false);
      },
    });
  }

  private aplicarValidadorTrechos(obrigatorio: boolean): void {
    if (obrigatorio) {
      this.form.controls.trechoIds.setValidators(minLengthArray(1));
    } else {
      this.form.controls.trechoIds.clearValidators();
    }
    this.form.controls.trechoIds.updateValueAndValidity({ emitEvent: false });
  }
}

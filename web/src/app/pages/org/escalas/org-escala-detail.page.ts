import { DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

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
  TrechoExecucao,
  Turno,
  rotuloSetor,
} from '../../../models/cadastros-operacionais';
import { OrganizacaoUsuario } from '../../../models/organizacao';
import { CadastrosOperacionaisApiService } from '../../../services/cadastros-operacionais-api.service';
import { mensagemHttp } from '../../../services/http-erro';
import { OrganizacaoApiService } from '../../../services/organizacao-api.service';

function minLengthArray(min: number) {
  return (control: AbstractControl) => {
    const valor = control.value as unknown;
    return Array.isArray(valor) && valor.length >= min ? null : { minLengthArray: true };
  };
}

@Component({
  selector: 'app-org-escala-detail-page',
  imports: [ReactiveFormsModule, RouterLink, DecimalPipe],
  templateUrl: './org-escala-detail.page.html',
})
export class OrgEscalaDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(CadastrosOperacionaisApiService);
  private readonly orgApi = inject(OrganizacaoApiService);

  readonly turnoOpcoes = TURNO_OPCOES;
  readonly turnoLabels = TURNO_LABELS;
  readonly tipoLabels = TIPO_ESCALA_LABELS;
  readonly diaSemanaOpcoes = DIA_SEMANA_OPCOES;
  readonly rotuloSetor = rotuloSetor;

  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly ok = signal<string | null>(null);
  readonly escala = signal<Escala | null>(null);

  readonly setores = signal<Setor[]>([]);
  readonly colaboradores = signal<Colaborador[]>([]);
  readonly supervisores = signal<OrganizacaoUsuario[]>([]);

  readonly trechosDoDia = signal<TrechoExecucao[]>([]);
  readonly trechosDisponiveis = signal<Trecho[]>([]);
  readonly carregandoPlano = signal(true);
  readonly salvandoPlano = signal(false);
  readonly erroPlano = signal<string | null>(null);
  readonly mostrarFormTrecho = signal(false);

  readonly formTrecho = this.fb.nonNullable.group({
    trechoId: [0, [Validators.required, Validators.min(1)]],
    ordem: [1, [Validators.required, Validators.min(1)]],
  });

  readonly trechosNaoAdicionados = computed(() => {
    const jaAdicionados = new Set(this.trechosDoDia().map((t) => t.trechoId));
    const setorId = this.escala()?.setorId;
    return this.trechosDisponiveis().filter(
      (t) => t.ativo && !jaAdicionados.has(t.id) && (setorId == null || t.setorId === setorId),
    );
  });

  readonly form = this.fb.nonNullable.group({
    tipo: ['AVULSA' as TipoEscala],
    data: [''],
    diasSemana: this.fb.nonNullable.control<DiaSemana[]>([]),
    turno: ['MANHA' as Turno, [Validators.required]],
    setorId: [0, [Validators.required, Validators.min(1)]],
    colaboradorIds: this.fb.nonNullable.control<number[]>([], minLengthArray(1)),
    supervisorUsuarioId: [0, [Validators.required, Validators.min(1)]],
  });

  readonly editavel = computed(() => {
    const item = this.escala();
    return item != null && item.ativo;
  });

  readonly modeloFrequencia = computed(() => {
    const item = this.escala();
    return item != null && item.tipo === 'FREQUENCIA' && item.origemId == null;
  });

  readonly instanciaFrequencia = computed(() => {
    const item = this.escala();
    return item != null && item.tipo === 'FREQUENCIA' && item.origemId != null;
  });

  readonly planoEditavel = computed(() => this.editavel() && !this.instanciaFrequencia());

  private id = 0;

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.id) {
      this.erro.set('Escala inválida');
      this.carregando.set(false);
      return;
    }
    this.api.listarSetores().subscribe({ next: (lista) => this.setores.set(lista.filter((s) => s.ativo)) });
    this.api.listarColaboradores().subscribe({
      next: (lista) => this.colaboradores.set(lista.filter((c) => c.ativo)),
    });
    this.api.listarTrechos().subscribe({ next: (lista) => this.trechosDisponiveis.set(lista) });
    this.orgApi.listarUsuarios().subscribe({
      next: (lista) => this.supervisores.set(lista.filter((u) => u.ativo)),
    });

    this.carregar();
    this.carregarPlano();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.api.buscarEscala(this.id).subscribe({
      next: (escala) => {
        this.escala.set(escala);
        this.form.patchValue({
          tipo: escala.tipo,
          data: escala.data ?? '',
          diasSemana: escala.diasSemana ?? [],
          turno: escala.turno,
          setorId: escala.setorId,
          colaboradorIds: escala.colaboradores.map((c) => c.id),
          supervisorUsuarioId: escala.supervisorUsuarioId,
        });
        this.aplicarValidadores(escala);
        this.form.controls.tipo.disable({ emitEvent: false });
        this.form.controls.data.disable({ emitEvent: false });
        this.form.controls.turno.disable({ emitEvent: false });
        this.form.controls.setorId.disable({ emitEvent: false });
        if (escala.tipo === 'FREQUENCIA' && escala.origemId == null && escala.ativo) {
          this.form.controls.diasSemana.enable({ emitEvent: false });
        } else {
          this.form.controls.diasSemana.disable({ emitEvent: false });
        }
        if (escala.ativo) {
          this.form.controls.colaboradorIds.enable({ emitEvent: false });
          this.form.controls.supervisorUsuarioId.enable({ emitEvent: false });
        } else {
          this.form.controls.colaboradorIds.disable({ emitEvent: false });
          this.form.controls.supervisorUsuarioId.disable({ emitEvent: false });
        }
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao carregar escala'));
        this.carregando.set(false);
      },
    });
  }

  salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.salvando.set(true);
    this.erro.set(null);
    this.ok.set(null);
    const raw = this.form.getRawValue();
    const item = this.escala();
    const frequenciaModelo = item?.tipo === 'FREQUENCIA' && item.origemId == null;
    this.api
      .atualizarEscala(this.id, {
        tipo: raw.tipo,
        data: frequenciaModelo ? null : raw.data || item?.data,
        diasSemana: frequenciaModelo ? raw.diasSemana : (item?.diasSemana ?? []),
        turno: raw.turno,
        setorId: Number(raw.setorId),
        colaboradorIds: raw.colaboradorIds,
        supervisorUsuarioId: Number(raw.supervisorUsuarioId),
      })
      .subscribe({
        next: (escala) => {
          this.escala.set(escala);
          this.ok.set(
            frequenciaModelo
              ? 'Frequência atualizada. A equipe de hoje, se já gerada, também foi atualizada.'
              : 'Escala atualizada.',
          );
          this.salvando.set(false);
        },
        error: (erro: unknown) => {
          this.erro.set(mensagemHttp(erro, 'Falha ao atualizar escala'));
          this.salvando.set(false);
        },
      });
  }

  cancelar(): void {
    const frequencia = this.modeloFrequencia() || this.instanciaFrequencia();
    const mensagem = frequencia
      ? 'Cancelar esta frequência? Ela deixa de valer nos dias da semana e os colaboradores ficam livres para outra escala.'
      : 'Cancelar esta escala avulsa?';
    if (!confirm(mensagem)) {
      return;
    }
    this.api.cancelarEscala(this.id).subscribe({
      next: () => void this.router.navigateByUrl('/org/escalas'),
      error: (erro: unknown) => this.erro.set(mensagemHttp(erro, 'Falha ao cancelar escala')),
    });
  }

  carregarPlano(): void {
    this.carregandoPlano.set(true);
    this.erroPlano.set(null);
    this.api.listarTrechosDaEscala(this.id).subscribe({
      next: (lista) => {
        this.trechosDoDia.set(lista);
        this.carregandoPlano.set(false);
      },
      error: (erro: unknown) => {
        this.erroPlano.set(mensagemHttp(erro, 'Falha ao carregar plano de trechos'));
        this.carregandoPlano.set(false);
      },
    });
  }

  abrirFormTrecho(): void {
    const proximaOrdem = this.trechosDoDia().length + 1;
    this.formTrecho.reset({ trechoId: this.trechosNaoAdicionados()[0]?.id ?? 0, ordem: proximaOrdem });
    this.mostrarFormTrecho.set(true);
  }

  fecharFormTrecho(): void {
    this.mostrarFormTrecho.set(false);
  }

  adicionarTrecho(): void {
    if (this.formTrecho.invalid) {
      this.formTrecho.markAllAsTouched();
      return;
    }
    this.salvandoPlano.set(true);
    this.erroPlano.set(null);
    const raw = this.formTrecho.getRawValue();
    this.api
      .adicionarTrechoNaEscala(this.id, { trechoId: Number(raw.trechoId), ordem: Number(raw.ordem) })
      .subscribe({
        next: () => {
          this.salvandoPlano.set(false);
          this.fecharFormTrecho();
          this.carregarPlano();
        },
        error: (erro: unknown) => {
          this.salvandoPlano.set(false);
          this.erroPlano.set(mensagemHttp(erro, 'Falha ao adicionar trecho ao plano'));
        },
      });
  }

  atualizarOrdem(item: TrechoExecucao, novaOrdemTexto: string): void {
    const novaOrdem = Number(novaOrdemTexto);
    if (!novaOrdem || novaOrdem === item.ordem) {
      return;
    }
    this.erroPlano.set(null);
    this.api.reordenarTrechoNaEscala(this.id, item.id, { ordem: novaOrdem }).subscribe({
      next: () => this.carregarPlano(),
      error: (erro: unknown) => {
        this.erroPlano.set(mensagemHttp(erro, 'Falha ao reordenar trecho'));
        this.carregarPlano();
      },
    });
  }

  removerTrecho(item: TrechoExecucao): void {
    if (!confirm(`Remover o trecho ${item.trechoCodigo} do plano?`)) {
      return;
    }
    this.erroPlano.set(null);
    this.api.removerTrechoDaEscala(this.id, item.id).subscribe({
      next: () => this.carregarPlano(),
      error: (erro: unknown) => this.erroPlano.set(mensagemHttp(erro, 'Falha ao remover trecho')),
    });
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

  diasSelecionados(): DiaSemana[] {
    return this.form.controls.diasSemana.value;
  }

  toggleDiaSemana(dia: DiaSemana, marcado: boolean): void {
    const atual = this.diasSelecionados();
    const proximo = marcado ? [...atual, dia] : atual.filter((item) => item !== dia);
    this.form.controls.diasSemana.setValue([...new Set(proximo)]);
    this.form.controls.diasSemana.markAsTouched();
  }

  rotuloDias(item: Escala): string {
    if (!item.diasSemana?.length) {
      return '';
    }
    const labels = new Map(this.diaSemanaOpcoes.map((d) => [d.valor, d.label]));
    return item.diasSemana.map((dia) => labels.get(dia) ?? dia).join(', ');
  }

  resumoColaboradores(item: Escala): string {
    return item.colaboradores.map((c) => `${c.nome} (${c.equipamentoCodigo})`).join(' · ') || 'Sem colaboradores';
  }

  private aplicarValidadores(escala: Escala): void {
    if (escala.tipo === 'FREQUENCIA' && escala.origemId == null) {
      this.form.controls.diasSemana.setValidators(minLengthArray(1));
    } else {
      this.form.controls.diasSemana.clearValidators();
    }
    this.form.controls.diasSemana.updateValueAndValidity({ emitEvent: false });
  }
}

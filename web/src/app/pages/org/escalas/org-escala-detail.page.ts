import { DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import {
  Equipamento,
  Equipe,
  Escala,
  Setor,
  TURNO_LABELS,
  TURNO_OPCOES,
  Trecho,
  TrechoExecucao,
  Turno,
} from '../../../models/cadastros-operacionais';
import { OrganizacaoUsuario } from '../../../models/organizacao';
import { CadastrosOperacionaisApiService } from '../../../services/cadastros-operacionais-api.service';
import { mensagemHttp } from '../../../services/http-erro';
import { OrganizacaoApiService } from '../../../services/organizacao-api.service';

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

  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly ok = signal<string | null>(null);
  readonly escala = signal<Escala | null>(null);

  readonly setores = signal<Setor[]>([]);
  readonly equipes = signal<Equipe[]>([]);
  readonly equipamentos = signal<Equipamento[]>([]);
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
    return this.trechosDisponiveis().filter((t) => t.ativo && !jaAdicionados.has(t.id));
  });

  readonly form = this.fb.nonNullable.group({
    data: ['', [Validators.required]],
    turno: ['MANHA' as Turno, [Validators.required]],
    setorId: [0, [Validators.required, Validators.min(1)]],
    equipeId: [0, [Validators.required, Validators.min(1)]],
    equipamentoId: [0, [Validators.required, Validators.min(1)]],
    supervisorUsuarioId: [0, [Validators.required, Validators.min(1)]],
  });

  readonly equipesDoSetor = computed(() => {
    const setorId = this.form.controls.setorId.value;
    return this.equipes().filter((e) => !setorId || e.setorId === setorId);
  });

  readonly equipamentosDaEquipe = computed(() => {
    const equipeId = this.form.controls.equipeId.value;
    return this.equipamentos().filter((e) => !equipeId || e.equipeId === equipeId);
  });

  readonly editavel = computed(() => {
    const item = this.escala();
    return item != null && item.ativo && item.status === 'RASCUNHO';
  });

  private id = 0;

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.id) {
      this.erro.set('Escala inválida');
      this.carregando.set(false);
      return;
    }
    this.api.listarSetores().subscribe({ next: (lista) => this.setores.set(lista.filter((s) => s.ativo)) });
    this.api.listarEquipes().subscribe({ next: (lista) => this.equipes.set(lista) });
    this.api.listarEquipamentos().subscribe({ next: (lista) => this.equipamentos.set(lista) });
    this.api.listarTrechos().subscribe({ next: (lista) => this.trechosDisponiveis.set(lista) });
    this.orgApi.listarUsuarios().subscribe({
      next: (lista) => this.supervisores.set(lista.filter((u) => u.ativo)),
    });

    this.form.controls.setorId.valueChanges.subscribe(() => {
      this.form.controls.equipeId.setValue(0);
      this.form.controls.equipamentoId.setValue(0);
    });
    this.form.controls.equipeId.valueChanges.subscribe(() => {
      this.form.controls.equipamentoId.setValue(0);
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
          data: escala.data,
          turno: escala.turno,
          setorId: escala.setorId,
          equipeId: escala.equipeId,
          equipamentoId: escala.equipamentoId,
          supervisorUsuarioId: escala.supervisorUsuarioId,
        });
        if (escala.status === 'PUBLICADA') {
          this.form.disable();
        } else {
          this.form.enable();
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
    this.api
      .atualizarEscala(this.id, {
        data: raw.data,
        turno: raw.turno,
        setorId: Number(raw.setorId),
        equipeId: Number(raw.equipeId),
        equipamentoId: Number(raw.equipamentoId),
        supervisorUsuarioId: Number(raw.supervisorUsuarioId),
      })
      .subscribe({
        next: (escala) => {
          this.escala.set(escala);
          this.ok.set('Escala atualizada.');
          this.salvando.set(false);
        },
        error: (erro: unknown) => {
          this.erro.set(mensagemHttp(erro, 'Falha ao atualizar escala'));
          this.salvando.set(false);
        },
      });
  }

  publicar(): void {
    this.salvando.set(true);
    this.erro.set(null);
    this.api.publicarEscala(this.id).subscribe({
      next: () => {
        this.salvando.set(false);
        this.ok.set('Escala publicada. O motor de regras já pode processar as posições do dia.');
        this.carregar();
      },
      error: (erro: unknown) => {
        this.salvando.set(false);
        this.erro.set(mensagemHttp(erro, 'Falha ao publicar escala'));
      },
    });
  }

  despublicar(): void {
    this.salvando.set(true);
    this.erro.set(null);
    this.api.despublicarEscala(this.id).subscribe({
      next: () => {
        this.salvando.set(false);
        this.ok.set('Escala voltou para rascunho.');
        this.carregar();
      },
      error: (erro: unknown) => {
        this.salvando.set(false);
        this.erro.set(mensagemHttp(erro, 'Falha ao despublicar escala'));
      },
    });
  }

  cancelar(): void {
    if (!confirm('Cancelar esta escala?')) {
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
    if (!confirm(`Remover o trecho ${item.trechoCodigo} do plano do dia?`)) {
      return;
    }
    this.erroPlano.set(null);
    this.api.removerTrechoDaEscala(this.id, item.id).subscribe({
      next: () => this.carregarPlano(),
      error: (erro: unknown) => this.erroPlano.set(mensagemHttp(erro, 'Falha ao remover trecho')),
    });
  }
}

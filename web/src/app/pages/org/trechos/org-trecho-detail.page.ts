import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { EditorLinhasTrechoComponent } from '../../../components/editor-linhas-trecho/editor-linhas-trecho.component';
import {
  STATUS_TRECHO_OPCOES,
  Setor,
  StatusTrecho,
  Trecho,
  rotuloSetor,
} from '../../../models/cadastros-operacionais';
import { CadastrosOperacionaisApiService } from '../../../services/cadastros-operacionais-api.service';
import { mensagemHttp } from '../../../services/http-erro';

@Component({
  selector: 'app-org-trecho-detail-page',
  imports: [DecimalPipe, ReactiveFormsModule, RouterLink, EditorLinhasTrechoComponent],
  templateUrl: './org-trecho-detail.page.html',
})
export class OrgTrechoDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(CadastrosOperacionaisApiService);

  readonly statusOpcoes = STATUS_TRECHO_OPCOES;
  readonly rotuloSetor = rotuloSetor;
  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly ok = signal<string | null>(null);
  readonly trecho = signal<Trecho | null>(null);
  readonly setores = signal<Setor[]>([]);
  readonly linhaWkt = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    setorId: [0, [Validators.required, Validators.min(1)]],
    codigo: ['', [Validators.required, Validators.maxLength(80)]],
    ruaNome: ['', [Validators.maxLength(200)]],
    geomWkt: ['', [Validators.required]],
    status: ['PLANEJADO' as StatusTrecho],
    percentualExecutado: [0, [Validators.min(0), Validators.max(100)]],
    emCorrecao: [false],
    liberadoInforme: [false],
    ativo: [true],
  });

  private id = 0;

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.id) {
      this.erro.set('Trecho inválido');
      this.carregando.set(false);
      return;
    }
    this.api.listarSetores().subscribe({
      next: (setores) => this.setores.set(setores.filter((s) => s.ativo)),
    });
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.api.buscarTrecho(this.id).subscribe({
      next: (trecho) => {
        this.trecho.set(trecho);
        this.form.patchValue({
          setorId: trecho.setorId,
          codigo: trecho.codigo,
          ruaNome: trecho.ruaNome ?? '',
          geomWkt: trecho.geomWkt,
          status: trecho.status,
          percentualExecutado: trecho.percentualExecutado,
          emCorrecao: trecho.emCorrecao,
          liberadoInforme: trecho.liberadoInforme,
          ativo: trecho.ativo,
        });
        this.linhaWkt.set(trecho.geomWkt);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao carregar trecho'));
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
      .atualizarTrecho(this.id, {
        setorId: Number(raw.setorId),
        codigo: raw.codigo.trim(),
        ruaNome: raw.ruaNome.trim() || null,
        geomWkt: raw.geomWkt.trim(),
        status: raw.status,
        percentualExecutado: Number(raw.percentualExecutado),
        emCorrecao: raw.emCorrecao,
        liberadoInforme: raw.liberadoInforme,
        ativo: raw.ativo,
      })
      .subscribe({
        next: (trecho) => {
          this.trecho.set(trecho);
          this.linhaWkt.set(trecho.geomWkt);
          this.ok.set('Trecho atualizado.');
          this.salvando.set(false);
        },
        error: (erro: unknown) => {
          this.erro.set(mensagemHttp(erro, 'Falha ao atualizar trecho'));
          this.salvando.set(false);
        },
      });
  }

  desativar(): void {
    if (!confirm('Desativar este trecho?')) {
      return;
    }
    this.api.desativarTrecho(this.id).subscribe({
      next: () => void this.router.navigateByUrl('/org/trechos'),
      error: (erro: unknown) => this.erro.set(mensagemHttp(erro, 'Falha ao desativar')),
    });
  }

  onLinhaConfirmada(wkts: string[]): void {
    const wkt = wkts[0];
    if (!wkt) {
      return;
    }
    this.form.controls.geomWkt.setValue(wkt);
    this.linhaWkt.set(wkt);
  }

  onLinhaLimpa(): void {
    this.linhaWkt.set(null);
    this.form.controls.geomWkt.setValue('');
  }
}

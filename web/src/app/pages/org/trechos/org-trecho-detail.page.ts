import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import {
  Rua,
  STATUS_TRECHO_OPCOES,
  StatusTrecho,
  Trecho,
} from '../../../models/cadastros-operacionais';
import { CadastrosOperacionaisApiService } from '../../../services/cadastros-operacionais-api.service';
import { mensagemHttp } from '../../../services/http-erro';

@Component({
  selector: 'app-org-trecho-detail-page',
  imports: [DecimalPipe, ReactiveFormsModule, RouterLink],
  templateUrl: './org-trecho-detail.page.html',
})
export class OrgTrechoDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(CadastrosOperacionaisApiService);

  readonly statusOpcoes = STATUS_TRECHO_OPCOES;
  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly ok = signal<string | null>(null);
  readonly trecho = signal<Trecho | null>(null);
  readonly ruas = signal<Rua[]>([]);

  readonly form = this.fb.nonNullable.group({
    ruaId: [0, [Validators.required, Validators.min(1)]],
    codigo: ['', [Validators.required, Validators.maxLength(80)]],
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
    this.api.listarRuas().subscribe({
      next: (ruas) => this.ruas.set(ruas.filter((r) => r.ativo)),
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
          ruaId: trecho.ruaId,
          codigo: trecho.codigo,
          geomWkt: trecho.geomWkt,
          status: trecho.status,
          percentualExecutado: trecho.percentualExecutado,
          emCorrecao: trecho.emCorrecao,
          liberadoInforme: trecho.liberadoInforme,
          ativo: trecho.ativo,
        });
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
        ruaId: Number(raw.ruaId),
        codigo: raw.codigo.trim(),
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
}

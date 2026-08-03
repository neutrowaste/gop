import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import {
  Rua,
  STATUS_TRECHO_OPCOES,
  StatusTrecho,
  Trecho,
} from '../../../models/cadastros-operacionais';
import { CadastrosOperacionaisApiService } from '../../../services/cadastros-operacionais-api.service';
import { mensagemHttp } from '../../../services/http-erro';

@Component({
  selector: 'app-org-trechos-list-page',
  imports: [DecimalPipe, ReactiveFormsModule, RouterLink],
  templateUrl: './org-trechos-list.page.html',
})
export class OrgTrechosListPage implements OnInit {
  private readonly api = inject(CadastrosOperacionaisApiService);
  private readonly fb = inject(FormBuilder);

  readonly statusOpcoes = STATUS_TRECHO_OPCOES;
  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly trechos = signal<Trecho[]>([]);
  readonly ruas = signal<Rua[]>([]);
  readonly mostrarForm = signal(false);

  readonly form = this.fb.nonNullable.group({
    ruaId: [0, [Validators.required, Validators.min(1)]],
    codigo: ['', [Validators.required, Validators.maxLength(80)]],
    geomWkt: [
      'LINESTRING(-43.1729 -22.9068, -43.1750 -22.9080)',
      [Validators.required],
    ],
    status: ['PLANEJADO' as StatusTrecho],
  });

  ngOnInit(): void {
    this.api.listarRuas().subscribe({
      next: (ruas) => this.ruas.set(ruas.filter((r) => r.ativo)),
    });
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.api.listarTrechos().subscribe({
      next: (lista) => {
        this.trechos.set(lista);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao listar trechos'));
        this.carregando.set(false);
      },
    });
  }

  abrirForm(): void {
    const primeira = this.ruas()[0]?.id ?? 0;
    this.form.reset({
      ruaId: primeira,
      codigo: '',
      geomWkt: 'LINESTRING(-43.1729 -22.9068, -43.1750 -22.9080)',
      status: 'PLANEJADO',
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
    const raw = this.form.getRawValue();
    this.api
      .criarTrecho({
        ruaId: Number(raw.ruaId),
        codigo: raw.codigo.trim(),
        geomWkt: raw.geomWkt.trim(),
        status: raw.status,
        ativo: true,
      })
      .subscribe({
        next: () => {
          this.salvando.set(false);
          this.fecharForm();
          this.carregar();
        },
        error: (erro: unknown) => {
          this.salvando.set(false);
          this.erro.set(mensagemHttp(erro, 'Falha ao criar trecho'));
        },
      });
  }
}

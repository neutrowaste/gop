import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { EditorLinhasTrechoComponent } from '../../../components/editor-linhas-trecho/editor-linhas-trecho.component';
import {
  STATUS_TRECHO_OPCOES,
  Setor,
  StatusTrecho,
  Trecho,
  TrechoImportacao,
  rotuloSetor,
} from '../../../models/cadastros-operacionais';
import { CadastrosOperacionaisApiService } from '../../../services/cadastros-operacionais-api.service';
import { mensagemHttp } from '../../../services/http-erro';

@Component({
  selector: 'app-org-trechos-list-page',
  imports: [DecimalPipe, ReactiveFormsModule, RouterLink, EditorLinhasTrechoComponent],
  templateUrl: './org-trechos-list.page.html',
})
export class OrgTrechosListPage implements OnInit {
  private readonly api = inject(CadastrosOperacionaisApiService);
  private readonly fb = inject(FormBuilder);

  readonly statusOpcoes = STATUS_TRECHO_OPCOES;
  readonly rotuloSetor = rotuloSetor;
  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly ok = signal<string | null>(null);
  readonly trechos = signal<Trecho[]>([]);
  readonly setores = signal<Setor[]>([]);
  readonly mostrarForm = signal(false);
  readonly mostrarImportacao = signal(false);
  readonly arquivoShapefile = signal<File | null>(null);
  readonly resultadoImportacao = signal<TrechoImportacao | null>(null);
  readonly linhasWkt = signal<string[]>([]);

  readonly form = this.fb.nonNullable.group({
    setorId: [0, [Validators.required, Validators.min(1)]],
    codigo: ['', [Validators.maxLength(80)]],
    ruaNome: ['', [Validators.maxLength(200)]],
    status: ['PLANEJADO' as StatusTrecho],
  });

  readonly formImportacao = this.fb.nonNullable.group({
    setorId: [0, [Validators.required, Validators.min(1)]],
    atributoRua: ['RUA'],
    atributoCodigo: [''],
  });

  ngOnInit(): void {
    this.api.listarSetores().subscribe({
      next: (setores) => this.setores.set(setores.filter((s) => s.ativo)),
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
    const primeira = this.setores()[0]?.id ?? 0;
    this.form.reset({
      setorId: primeira,
      codigo: '',
      ruaNome: '',
      status: 'PLANEJADO',
    });
    this.linhasWkt.set([]);
    this.ok.set(null);
    this.mostrarImportacao.set(false);
    this.mostrarForm.set(true);
  }

  fecharForm(): void {
    this.mostrarForm.set(false);
    this.linhasWkt.set([]);
  }

  onLinhasConfirmadas(wkts: string[]): void {
    this.linhasWkt.set(wkts);
  }

  onLinhasLimpas(): void {
    this.linhasWkt.set([]);
  }

  abrirImportacao(): void {
    this.resultadoImportacao.set(null);
    this.arquivoShapefile.set(null);
    this.ok.set(null);
    this.formImportacao.reset({
      setorId: this.setores()[0]?.id ?? 0,
      atributoRua: 'RUA',
      atributoCodigo: '',
    });
    this.mostrarForm.set(false);
    this.mostrarImportacao.set(true);
  }

  fecharImportacao(): void {
    this.mostrarImportacao.set(false);
    this.arquivoShapefile.set(null);
  }

  onArquivoSelecionado(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const arquivo = input.files?.[0] ?? null;
    this.arquivoShapefile.set(arquivo);
  }

  importarShapefile(): void {
    if (this.formImportacao.invalid) {
      this.formImportacao.markAllAsTouched();
      return;
    }
    const arquivo = this.arquivoShapefile();
    if (arquivo == null) {
      this.erro.set('Selecione um arquivo .zip com o shapefile.');
      return;
    }
    this.salvando.set(true);
    this.erro.set(null);
    this.ok.set(null);
    this.resultadoImportacao.set(null);
    const raw = this.formImportacao.getRawValue();
    this.api
      .importarShapefile({
        setorId: Number(raw.setorId),
        arquivo,
        atributoRua: raw.atributoRua.trim() || 'RUA',
        atributoCodigo: raw.atributoCodigo.trim() || undefined,
      })
      .subscribe({
        next: (resultado) => {
          this.salvando.set(false);
          this.resultadoImportacao.set(resultado);
          this.ok.set(
            `${resultado.criados} trecho(s) criado(s) · ${resultado.ignorados} ignorado(s).`,
          );
          this.carregar();
        },
        error: (erro: unknown) => {
          this.salvando.set(false);
          this.erro.set(mensagemHttp(erro, 'Falha ao importar shapefile'));
        },
      });
  }

  criar(): void {
    this.erro.set(null);
    this.ok.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const linhas = this.linhasWkt();
    if (linhas.length === 0) {
      this.erro.set('Confirme ao menos um trecho no mapa.');
      return;
    }
    this.salvando.set(true);
    const raw = this.form.getRawValue();
    this.api
      .criarTrechosDesenho({
        setorId: Number(raw.setorId),
        codigo: raw.codigo.trim() || null,
        ruaNome: raw.ruaNome.trim() || null,
        geomWkts: linhas,
        status: raw.status,
      })
      .subscribe({
        next: (resultado) => {
          this.salvando.set(false);
          this.fecharForm();
          this.ok.set(
            `${resultado.criados} trecho(s) criado(s)` +
              (resultado.ignorados ? ` · ${resultado.ignorados} ignorado(s)` : '') +
              '.',
          );
          this.carregar();
        },
        error: (erro: unknown) => {
          this.salvando.set(false);
          this.erro.set(mensagemHttp(erro, 'Falha ao criar trechos'));
        },
      });
  }
}

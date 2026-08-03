import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';

import { Cidade, Setor } from '../../../models/cadastros-operacionais';
import { CadastrosOperacionaisApiService } from '../../../services/cadastros-operacionais-api.service';
import { mensagemHttp } from '../../../services/http-erro';

@Component({
  selector: 'app-org-setores-list-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './org-setores-list.page.html',
})
export class OrgSetoresListPage implements OnInit {
  private readonly api = inject(CadastrosOperacionaisApiService);
  private readonly fb = inject(FormBuilder);

  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly setores = signal<Setor[]>([]);
  readonly cidades = signal<Cidade[]>([]);
  readonly mostrarForm = signal(false);
  readonly novaCidade = signal(false);

  readonly form = this.fb.nonNullable.group({
    cidadeId: [0],
    novaCidadeNome: [''],
    novaCidadeUf: ['RJ'],
    codigo: [''],
    nome: ['', [Validators.required, Validators.maxLength(200)]],
  });

  ngOnInit(): void {
    this.carregarCidades();
    this.carregar();
  }

  carregarCidades(): void {
    this.api.listarCidades().subscribe({
      next: (cidades) => {
        const ativas = cidades.filter((c) => c.ativo);
        this.cidades.set(ativas);
        this.novaCidade.set(ativas.length === 0);
      },
    });
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.api.listarSetores().subscribe({
      next: (lista) => {
        this.setores.set(lista);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao listar setores'));
        this.carregando.set(false);
      },
    });
  }

  abrirForm(): void {
    const ativas = this.cidades();
    this.novaCidade.set(ativas.length === 0);
    this.form.reset({
      cidadeId: ativas[0]?.id ?? 0,
      novaCidadeNome: '',
      novaCidadeUf: 'RJ',
      codigo: '',
      nome: '',
    });
    this.mostrarForm.set(true);
  }

  fecharForm(): void {
    this.mostrarForm.set(false);
  }

  alternarNovaCidade(criar: boolean): void {
    this.novaCidade.set(criar);
  }

  criar(): void {
    this.erro.set(null);
    const raw = this.form.getRawValue();
    if (!raw.nome.trim()) {
      this.form.controls.nome.markAsTouched();
      return;
    }

    this.salvando.set(true);

    const criarSetor = (cidadeId: number) =>
      this.api.criarSetor({
        cidadeId,
        codigo: raw.codigo.trim() || null,
        nome: raw.nome.trim(),
        ativo: true,
      });

    if (this.novaCidade()) {
      const nomeCidade = raw.novaCidadeNome.trim();
      if (!nomeCidade) {
        this.erro.set('Informe o município do setor');
        this.salvando.set(false);
        return;
      }
      this.api
        .criarCidade({
          nome: nomeCidade,
          uf: raw.novaCidadeUf.trim().toUpperCase() || 'RJ',
          ativo: true,
        })
        .pipe(switchMap((cidade) => criarSetor(cidade.id)))
        .subscribe({
          next: () => {
            this.salvando.set(false);
            this.fecharForm();
            this.carregarCidades();
            this.carregar();
          },
          error: (erro: unknown) => {
            this.salvando.set(false);
            this.erro.set(mensagemHttp(erro, 'Falha ao criar setor'));
          },
        });
      return;
    }

    if (!raw.cidadeId) {
      this.erro.set('Selecione o município ou cadastre um novo');
      this.salvando.set(false);
      return;
    }

    criarSetor(Number(raw.cidadeId)).subscribe({
      next: () => {
        this.salvando.set(false);
        this.fecharForm();
        this.carregar();
      },
      error: (erro: unknown) => {
        this.salvando.set(false);
        this.erro.set(mensagemHttp(erro, 'Falha ao criar setor'));
      },
    });
  }
}

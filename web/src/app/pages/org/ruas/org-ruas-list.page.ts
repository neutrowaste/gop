import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Cidade, Rua } from '../../../models/cadastros-operacionais';
import { CadastrosOperacionaisApiService } from '../../../services/cadastros-operacionais-api.service';
import { mensagemHttp } from '../../../services/http-erro';

@Component({
  selector: 'app-org-ruas-list-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './org-ruas-list.page.html',
})
export class OrgRuasListPage implements OnInit {
  private readonly api = inject(CadastrosOperacionaisApiService);
  private readonly fb = inject(FormBuilder);

  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly ruas = signal<Rua[]>([]);
  readonly cidades = signal<Cidade[]>([]);
  readonly mostrarForm = signal(false);

  readonly form = this.fb.nonNullable.group({
    cidadeId: [0, [Validators.required, Validators.min(1)]],
    nome: ['', [Validators.required, Validators.maxLength(200)]],
    geomWkt: [
      'LINESTRING(-43.1729 -22.9068, -43.1800 -22.9100)',
      [Validators.required],
    ],
  });

  ngOnInit(): void {
    this.api.listarCidades().subscribe({
      next: (cidades) => this.cidades.set(cidades.filter((c) => c.ativo)),
    });
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.api.listarRuas().subscribe({
      next: (lista) => {
        this.ruas.set(lista);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao listar ruas'));
        this.carregando.set(false);
      },
    });
  }

  abrirForm(): void {
    const primeira = this.cidades()[0]?.id ?? 0;
    this.form.reset({
      cidadeId: primeira,
      nome: '',
      geomWkt: 'LINESTRING(-43.1729 -22.9068, -43.1800 -22.9100)',
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
      .criarRua({
        cidadeId: Number(raw.cidadeId),
        nome: raw.nome.trim(),
        geomWkt: raw.geomWkt.trim(),
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
          this.erro.set(mensagemHttp(erro, 'Falha ao criar rua'));
        },
      });
  }
}

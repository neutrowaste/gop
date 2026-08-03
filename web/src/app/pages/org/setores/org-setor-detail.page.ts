import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Cidade, Setor } from '../../../models/cadastros-operacionais';
import { CadastrosOperacionaisApiService } from '../../../services/cadastros-operacionais-api.service';
import { mensagemHttp } from '../../../services/http-erro';

@Component({
  selector: 'app-org-setor-detail-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './org-setor-detail.page.html',
})
export class OrgSetorDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(CadastrosOperacionaisApiService);

  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly ok = signal<string | null>(null);
  readonly setor = signal<Setor | null>(null);
  readonly cidades = signal<Cidade[]>([]);

  readonly form = this.fb.nonNullable.group({
    cidadeId: [0, [Validators.required, Validators.min(1)]],
    codigo: [''],
    nome: ['', [Validators.required, Validators.maxLength(200)]],
    ativo: [true],
  });

  private id = 0;

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.id) {
      this.erro.set('Setor inválido');
      this.carregando.set(false);
      return;
    }
    this.api.listarCidades().subscribe({
      next: (cidades) => this.cidades.set(cidades.filter((c) => c.ativo)),
    });
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.api.buscarSetor(this.id).subscribe({
      next: (setor) => {
        this.setor.set(setor);
        this.form.patchValue({
          cidadeId: setor.cidadeId,
          codigo: setor.codigo ?? '',
          nome: setor.nome,
          ativo: setor.ativo,
        });
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao carregar setor'));
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
      .atualizarSetor(this.id, {
        cidadeId: Number(raw.cidadeId),
        codigo: raw.codigo.trim() || null,
        nome: raw.nome.trim(),
        ativo: raw.ativo,
      })
      .subscribe({
        next: (setor) => {
          this.setor.set(setor);
          this.ok.set('Setor atualizado.');
          this.salvando.set(false);
        },
        error: (erro: unknown) => {
          this.erro.set(mensagemHttp(erro, 'Falha ao atualizar setor'));
          this.salvando.set(false);
        },
      });
  }

  desativar(): void {
    if (!confirm('Desativar este setor?')) {
      return;
    }
    this.api.desativarSetor(this.id).subscribe({
      next: () => void this.router.navigateByUrl('/org/setores'),
      error: (erro: unknown) => this.erro.set(mensagemHttp(erro, 'Falha ao desativar')),
    });
  }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Cidade, Rua } from '../../../models/cadastros-operacionais';
import { CadastrosOperacionaisApiService } from '../../../services/cadastros-operacionais-api.service';
import { mensagemHttp } from '../../../services/http-erro';

@Component({
  selector: 'app-org-rua-detail-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './org-rua-detail.page.html',
})
export class OrgRuaDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(CadastrosOperacionaisApiService);

  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly ok = signal<string | null>(null);
  readonly rua = signal<Rua | null>(null);
  readonly cidades = signal<Cidade[]>([]);

  readonly form = this.fb.nonNullable.group({
    cidadeId: [0, [Validators.required, Validators.min(1)]],
    nome: ['', [Validators.required, Validators.maxLength(200)]],
    geomWkt: ['', [Validators.required]],
    ativo: [true],
  });

  private id = 0;

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.id) {
      this.erro.set('Rua inválida');
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
    this.api.buscarRua(this.id).subscribe({
      next: (rua) => {
        this.rua.set(rua);
        this.form.patchValue({
          cidadeId: rua.cidadeId,
          nome: rua.nome,
          geomWkt: rua.geomWkt,
          ativo: rua.ativo,
        });
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao carregar rua'));
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
      .atualizarRua(this.id, {
        cidadeId: Number(raw.cidadeId),
        nome: raw.nome.trim(),
        geomWkt: raw.geomWkt.trim(),
        ativo: raw.ativo,
      })
      .subscribe({
        next: (rua) => {
          this.rua.set(rua);
          this.ok.set('Rua atualizada.');
          this.salvando.set(false);
        },
        error: (erro: unknown) => {
          this.erro.set(mensagemHttp(erro, 'Falha ao atualizar rua'));
          this.salvando.set(false);
        },
      });
  }

  desativar(): void {
    if (!confirm('Desativar esta rua?')) {
      return;
    }
    this.api.desativarRua(this.id).subscribe({
      next: () => void this.router.navigateByUrl('/org/ruas'),
      error: (erro: unknown) => this.erro.set(mensagemHttp(erro, 'Falha ao desativar')),
    });
  }
}

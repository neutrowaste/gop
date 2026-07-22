import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Empresa } from '../../../models/empresa';
import { EmpresaAdminApiService } from '../../../services/empresa-admin-api.service';
import { mensagemHttp } from '../../../services/http-erro';

@Component({
  selector: 'app-empresas-list-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './empresas-list.page.html',
})
export class EmpresasListPage implements OnInit {
  private readonly empresasApi = inject(EmpresaAdminApiService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);

  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly empresas = signal<Empresa[]>([]);
  readonly filtroAtivo = signal<'todas' | 'ativas' | 'inativas'>('todas');
  readonly mostrarForm = signal(false);

  readonly form = this.fb.nonNullable.group({
    codigo: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9_-]+$/), Validators.maxLength(50)]],
    nome: ['', [Validators.required, Validators.maxLength(200)]],
  });

  ngOnInit(): void {
    if (this.route.snapshot.queryParamMap.get('nova') === '1') {
      this.mostrarForm.set(true);
    }
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    const filtro = this.filtroAtivo();
    const ativo = filtro === 'todas' ? null : filtro === 'ativas';

    this.empresasApi.listar(ativo).subscribe({
      next: (lista) => {
        this.empresas.set(lista);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao listar empresas'));
        this.carregando.set(false);
      },
    });
  }

  aplicarFiltro(filtro: 'todas' | 'ativas' | 'inativas'): void {
    this.filtroAtivo.set(filtro);
    this.carregar();
  }

  abrirForm(): void {
    this.mostrarForm.set(true);
  }

  fecharForm(): void {
    this.mostrarForm.set(false);
    this.form.reset();
  }

  criar(): void {
    this.erro.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.salvando.set(true);
    const { codigo, nome } = this.form.getRawValue();
    this.empresasApi.criar({ codigo: codigo.trim(), nome: nome.trim(), ativo: true }).subscribe({
      next: () => {
        this.salvando.set(false);
        this.fecharForm();
        this.carregar();
      },
      error: (erro: unknown) => {
        this.salvando.set(false);
        this.erro.set(mensagemHttp(erro, 'Falha ao criar empresa'));
      },
    });
  }
}

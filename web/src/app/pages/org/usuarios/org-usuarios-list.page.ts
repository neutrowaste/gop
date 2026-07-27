import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { OrganizacaoUsuario } from '../../../models/organizacao';
import { AutenticacaoService } from '../../../services/autenticacao.service';
import { mensagemHttp } from '../../../services/http-erro';
import { OrganizacaoApiService } from '../../../services/organizacao-api.service';

@Component({
  selector: 'app-org-usuarios-list-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './org-usuarios-list.page.html',
})
export class OrgUsuariosListPage implements OnInit {
  private readonly orgApi = inject(OrganizacaoApiService);
  private readonly autenticacao = inject(AutenticacaoService);
  private readonly fb = inject(FormBuilder);

  readonly usuario = this.autenticacao.usuario;
  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly usuarios = signal<OrganizacaoUsuario[]>([]);
  readonly mostrarForm = signal(false);

  readonly form = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(200)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(200)]],
    senha: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
    administradorEmpresa: [false],
  });

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.orgApi.listarUsuarios().subscribe({
      next: (lista) => {
        this.usuarios.set(lista);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao listar usuários'));
        this.carregando.set(false);
      },
    });
  }

  abrirForm(): void {
    this.mostrarForm.set(true);
  }

  fecharForm(): void {
    this.mostrarForm.set(false);
    this.form.reset({ administradorEmpresa: false });
  }

  criar(): void {
    this.erro.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.salvando.set(true);
    const raw = this.form.getRawValue();
    this.orgApi
      .criarUsuario({
        nome: raw.nome.trim(),
        email: raw.email.trim(),
        senha: raw.senha,
        ativo: true,
        administradorEmpresa: raw.administradorEmpresa,
      })
      .subscribe({
        next: () => {
          this.salvando.set(false);
          this.fecharForm();
          this.carregar();
        },
        error: (erro: unknown) => {
          this.salvando.set(false);
          this.erro.set(mensagemHttp(erro, 'Falha ao criar usuário'));
        },
      });
  }
}

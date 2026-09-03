import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { OrganizacaoUsuario, UsuarioAcessoMenu, UsuarioModulo } from '../../../models/organizacao';
import { AutenticacaoService } from '../../../services/autenticacao.service';
import { mensagemHttp } from '../../../services/http-erro';
import { OrganizacaoApiService } from '../../../services/organizacao-api.service';

@Component({
  selector: 'app-org-usuario-detail-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './org-usuario-detail.page.html',
})
export class OrgUsuarioDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly orgApi = inject(OrganizacaoApiService);
  private readonly autenticacao = inject(AutenticacaoService);

  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly ok = signal<string | null>(null);
  readonly usuario = signal<OrganizacaoUsuario | null>(null);
  readonly modulos = signal<UsuarioModulo[]>([]);
  readonly acessos = signal<UsuarioAcessoMenu[]>([]);

  readonly form = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(200)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(200)]],
    senha: ['', [Validators.minLength(8), Validators.maxLength(100)]],
    ativo: [true],
    administradorEmpresa: [false],
  });

  private usuarioId = 0;

  ngOnInit(): void {
    this.usuarioId = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.usuarioId) {
      this.erro.set('Usuário inválido');
      this.carregando.set(false);
      return;
    }
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    forkJoin({
      usuario: this.orgApi.buscarUsuario(this.usuarioId),
      modulos: this.orgApi.listarModulosDoUsuario(this.usuarioId),
      acessos: this.orgApi.listarAcessosDoUsuario(this.usuarioId),
    }).subscribe({
      next: ({ usuario, modulos, acessos }) => {
        this.usuario.set(usuario);
        this.form.patchValue({
          nome: usuario.nome,
          email: usuario.email,
          ativo: usuario.ativo,
          administradorEmpresa: usuario.administradorEmpresa,
          senha: '',
        });
        this.modulos.set(modulos);
        this.acessos.set(acessos);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao carregar usuário'));
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
    this.orgApi
      .atualizarUsuario(this.usuarioId, {
        nome: raw.nome.trim(),
        email: raw.email.trim(),
        senha: raw.senha ? raw.senha : null,
        ativo: raw.ativo,
        administradorEmpresa: raw.administradorEmpresa,
      })
      .subscribe({
        next: (usuario) => {
          this.usuario.set(usuario);
          this.form.patchValue({ senha: '', administradorEmpresa: usuario.administradorEmpresa });
          this.ok.set('Usuário atualizado.');
          this.salvando.set(false);
          this.orgApi.listarAcessosDoUsuario(this.usuarioId).subscribe({
            next: (acessos) => this.acessos.set(acessos),
          });
        },
        error: (erro: unknown) => {
          this.erro.set(mensagemHttp(erro, 'Falha ao atualizar usuário'));
          this.salvando.set(false);
        },
      });
  }

  desativar(): void {
    const atual = this.autenticacao.usuario();
    if (atual?.usuarioId === this.usuarioId) {
      this.erro.set('Não é possível desativar o próprio usuário.');
      return;
    }
    if (!confirm('Desativar este usuário?')) {
      return;
    }
    this.orgApi.desativarUsuario(this.usuarioId).subscribe({
      next: () => this.carregar(),
      error: (erro: unknown) => this.erro.set(mensagemHttp(erro, 'Falha ao desativar')),
    });
  }

  alternarModulo(modulo: UsuarioModulo): void {
    if (!modulo.contratado) {
      return;
    }
    this.orgApi
      .atualizarModuloDoUsuario(this.usuarioId, modulo.moduloId, !modulo.liberado)
      .subscribe({
        next: (atualizado) => {
          this.modulos.update((lista) =>
            lista.map((item) =>
              item.moduloId === atualizado.moduloId ? atualizado : item,
            ),
          );
          this.ok.set(
            atualizado.liberado
              ? `Módulo ${atualizado.nome} liberado.`
              : `Módulo ${atualizado.nome} revogado.`,
          );
        },
        error: (erro: unknown) =>
          this.erro.set(mensagemHttp(erro, 'Falha ao atualizar módulo do usuário')),
      });
  }

  alternarAcesso(acesso: UsuarioAcessoMenu): void {
    this.orgApi
      .atualizarAcessoDoUsuario(this.usuarioId, acesso.codigo, !acesso.liberado)
      .subscribe({
        next: (atualizado) => {
          this.acessos.update((lista) =>
            lista.map((item) => (item.codigo === atualizado.codigo ? atualizado : item)),
          );
          this.ok.set(
            atualizado.liberado
              ? `Acesso a ${atualizado.nome} liberado.`
              : `Acesso a ${atualizado.nome} revogado.`,
          );
        },
        error: (erro: unknown) =>
          this.erro.set(mensagemHttp(erro, 'Falha ao atualizar acesso do usuário')),
      });
  }

  iconeAcesso(codigo: string): string {
    switch (codigo?.toUpperCase()) {
      case 'SETORES':
        return 'ph-map-trifold';
      case 'TRECHOS':
        return 'ph-path';
      case 'COLABORADORES':
        return 'ph-users-three';
      case 'ESCALAS':
        return 'ph-calendar-check';
      case 'OCORRENCIAS':
        return 'ph-warning-circle';
      default:
        return 'ph-lock-open';
    }
  }
}

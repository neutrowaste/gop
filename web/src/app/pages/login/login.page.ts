import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AutenticacaoService } from '../../services/autenticacao.service';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule],
  templateUrl: './login.page.html',
})
export class LoginPage {
  private readonly fb = inject(FormBuilder);
  private readonly autenticacao = inject(AutenticacaoService);
  private readonly router = inject(Router);

  readonly enviando = signal(false);
  readonly erro = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required, Validators.minLength(6)]],
  });

  constructor() {
    if (this.autenticacao.autenticado()) {
      void this.router.navigateByUrl('/admin');
    }
  }

  enviar(): void {
    this.erro.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.enviando.set(true);
    const { email, senha } = this.form.getRawValue();

    this.autenticacao.login({ email: email.trim(), senha }).subscribe({
      next: () => {
        this.enviando.set(false);
        void this.router.navigateByUrl('/admin');
      },
      error: (erro: unknown) => {
        this.enviando.set(false);
        this.erro.set(this.mensagemErro(erro));
      },
    });
  }

  private mensagemErro(erro: unknown): string {
    if (erro instanceof HttpErrorResponse) {
      if (erro.status === 0) {
        return 'Não foi possível conectar à API. Verifique se ela está em execução.';
      }
      const detalhe = (erro.error as { detail?: string } | null)?.detail;
      if (detalhe) {
        return detalhe;
      }
      if (erro.status === 401) {
        return 'Email ou senha inválidos';
      }
    }
    return 'Falha ao autenticar. Tente novamente.';
  }
}

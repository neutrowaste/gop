import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AutenticacaoService } from '../../services/autenticacao.service';

@Component({
  selector: 'app-funcionario-shell-page',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app-shell.page.html',
})
export class AppShellPage {
  private readonly autenticacao = inject(AutenticacaoService);
  private readonly router = inject(Router);

  readonly usuario = this.autenticacao.usuario;

  sair(): void {
    this.autenticacao.logout();
    void this.router.navigateByUrl('/admin/login');
  }
}

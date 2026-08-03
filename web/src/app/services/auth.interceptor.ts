import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AutenticacaoService } from './autenticacao.service';

/**
 * API stateless por JWT: um 401 fora do login só pode significar "sem token"
 * ou "token inválido/expirado" (falhas de autorização usam 403). Por isso
 * qualquer 401 aqui é tratado como sessão inválida, sem depender do texto
 * da mensagem de erro (que pode não vir preenchido, ex.: erro lançado antes
 * do DispatcherServlet, fora do alcance do ApiExceptionHandler).
 */
function ehFalhaDeAutenticacao(erro: HttpErrorResponse): boolean {
  return erro.status === 401;
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const autenticacao = inject(AutenticacaoService);
  const router = inject(Router);
  const token = autenticacao.accessToken();

  const requisicao =
    !token || req.url.includes('/v1/auth/login')
      ? req
      : req.clone({
          setHeaders: { Authorization: `Bearer ${token}` },
        });

  return next(requisicao).pipe(
    catchError((erro: unknown) => {
      if (
        erro instanceof HttpErrorResponse
        && !req.url.includes('/v1/auth/login')
        && ehFalhaDeAutenticacao(erro)
      ) {
        const tinhaSessao = !!autenticacao.accessToken();
        autenticacao.logout();
        void router.navigate(['/admin/login'], {
          queryParams: tinhaSessao ? { motivo: 'sessao-expirada' } : {},
          replaceUrl: true,
        });
      }
      return throwError(() => erro);
    }),
  );
};

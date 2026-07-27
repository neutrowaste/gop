import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AutenticacaoService } from './autenticacao.service';

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
        erro instanceof HttpErrorResponse &&
        erro.status === 401 &&
        !req.url.includes('/v1/auth/login')
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

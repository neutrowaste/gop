import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { AutenticacaoService } from './autenticacao.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AutenticacaoService).accessToken();
  if (!token || req.url.includes('/v1/auth/login')) {
    return next(req);
  }

  return next(
    req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    }),
  );
};

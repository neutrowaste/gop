import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AutenticacaoService } from '../services/autenticacao.service';

export const authGuard: CanActivateFn = () => {
  const autenticacao = inject(AutenticacaoService);
  const router = inject(Router);

  if (autenticacao.autenticado()) {
    return true;
  }

  return router.createUrlTree(['/admin/login']);
};

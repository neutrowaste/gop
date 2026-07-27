import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';

import { AutenticacaoService } from '../services/autenticacao.service';

export const authGuard: CanActivateFn = () => {
  const autenticacao = inject(AutenticacaoService);
  const router = inject(Router);

  if (autenticacao.autenticado()) {
    return true;
  }

  return router.createUrlTree(['/admin/login']);
};

export const adminPlataformaGuard: CanActivateFn = () => {
  const autenticacao = inject(AutenticacaoService);
  const router = inject(Router);

  if (!autenticacao.autenticado()) {
    return router.createUrlTree(['/admin/login']);
  }
  if (autenticacao.ehAdminPlataforma()) {
    return true;
  }
  return redirecionarAreaPermitida(autenticacao, router);
};

export const adminEmpresaGuard: CanActivateFn = () => {
  const autenticacao = inject(AutenticacaoService);
  const router = inject(Router);

  if (!autenticacao.autenticado()) {
    return router.createUrlTree(['/admin/login']);
  }
  if (autenticacao.ehAdminEmpresa()) {
    return true;
  }
  return redirecionarAreaPermitida(autenticacao, router);
};

export const funcionarioGuard: CanActivateFn = () => {
  const autenticacao = inject(AutenticacaoService);
  const router = inject(Router);

  if (!autenticacao.autenticado()) {
    return router.createUrlTree(['/admin/login']);
  }
  if (autenticacao.ehFuncionario()) {
    return true;
  }
  return redirecionarAreaPermitida(autenticacao, router);
};

function redirecionarAreaPermitida(
  autenticacao: AutenticacaoService,
  router: Router,
): UrlTree {
  const destino = autenticacao.rotaInicial();
  if (destino === '/admin/login') {
    autenticacao.logout();
  }
  return router.createUrlTree([destino]);
}

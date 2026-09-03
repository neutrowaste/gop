import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { map } from 'rxjs';

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

export const orgPainelGuard: CanActivateFn = () => {
  const autenticacao = inject(AutenticacaoService);
  const router = inject(Router);

  if (!autenticacao.autenticado()) {
    return router.createUrlTree(['/admin/login']);
  }
  if (!autenticacao.ehPainelEmpresa()) {
    return redirecionarAreaPermitida(autenticacao, router);
  }
  return autenticacao.garantirPainel().pipe(map(() => true));
};

export const orgMenuGuard =
  (codigo: string): CanActivateFn =>
  () => {
    const autenticacao = inject(AutenticacaoService);
    const router = inject(Router);
    if (autenticacao.temAcessoMenu(codigo)) {
      return true;
    }
    return router.createUrlTree(['/org']);
  };

export const orgEscalaGuard: CanActivateFn = () => {
  const autenticacao = inject(AutenticacaoService);
  const router = inject(Router);
  if (autenticacao.temAcessoMenu('ESCALAS') || autenticacao.temModulo('ESCALA')) {
    return true;
  }
  return router.createUrlTree(['/org']);
};

export const orgModuloGuard =
  (codigo: string): CanActivateFn =>
  () => {
    const autenticacao = inject(AutenticacaoService);
    const router = inject(Router);
    if (autenticacao.temModulo(codigo)) {
      return true;
    }
    return router.createUrlTree(['/org']);
  };

export const orgServicoGuard: CanActivateFn = (route) => {
  const autenticacao = inject(AutenticacaoService);
  const router = inject(Router);
  const codigo = route.paramMap.get('codigo') ?? '';
  if (autenticacao.temModulo(codigo)) {
    return true;
  }
  return router.createUrlTree(['/org']);
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

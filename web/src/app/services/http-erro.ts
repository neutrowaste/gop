import { HttpErrorResponse } from '@angular/common/http';

export function mensagemHttp(erro: unknown, fallback: string): string {
  if (erro instanceof HttpErrorResponse) {
    if (erro.status === 0) {
      return 'Não foi possível conectar à API.';
    }
    const detalhe = (erro.error as { detail?: string } | null)?.detail;
    if (detalhe) {
      return detalhe;
    }
    return `${fallback} (HTTP ${erro.status})`;
  }
  return fallback;
}

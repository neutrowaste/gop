import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ApiService {
  readonly baseUrl = this.resolverBaseUrl();

  private resolverBaseUrl(): string {
    const configurado = window.__GOP_CONFIG__?.apiBaseUrl?.trim();
    if (configurado) {
      return configurado.replace(/\/$/, '');
    }
    const host = window.location.hostname;
    if (host === 'localhost' || host === '127.0.0.1') {
      return 'http://localhost:8080/api';
    }
    return '/api';
  }
}

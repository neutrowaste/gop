import { Injectable } from '@angular/core';

declare global {
  interface Window {
    __GOP_API_URL__?: string;
  }
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  readonly baseUrl: string = this.resolveBaseUrl();

  private resolveBaseUrl(): string {
    if (typeof window !== 'undefined' && window.__GOP_API_URL__ && window.__GOP_API_URL__.trim() !== '') {
      return window.__GOP_API_URL__.trim().replace(/\/+$/, '');
    }

    if (
      typeof window !== 'undefined' &&
      (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') &&
      window.location.port === '4200'
    ) {
      return 'http://localhost:8080/api';
    }

    return '/api';
  }
}

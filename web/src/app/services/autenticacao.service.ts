import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { LoginRequest } from '../models/login-request';
import { TokenResponse } from '../models/token-response';
import { UsuarioSessao } from '../models/usuario-sessao';
import { ApiService } from './api.service';

const STORAGE_KEY = 'gop.admin.sessao';

@Injectable({ providedIn: 'root' })
export class AutenticacaoService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  private readonly sessao = signal<TokenResponse | null>(this.carregarSessao());

  readonly usuario = this.sessao.asReadonly();
  readonly autenticado = computed(() => !!this.sessao()?.accessToken);
  readonly accessToken = computed(() => this.sessao()?.accessToken ?? null);

  login(credenciais: LoginRequest): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>(`${this.api.baseUrl}/v1/auth/login`, credenciais)
      .pipe(tap((resposta) => this.persistir(resposta)));
  }

  me(): Observable<UsuarioSessao> {
    return this.http.get<UsuarioSessao>(`${this.api.baseUrl}/v1/auth/me`);
  }

  logout(): void {
    sessionStorage.removeItem(STORAGE_KEY);
    this.sessao.set(null);
  }

  private persistir(resposta: TokenResponse): void {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(resposta));
    this.sessao.set(resposta);
  }

  private carregarSessao(): TokenResponse | null {
    const bruto = sessionStorage.getItem(STORAGE_KEY);
    if (!bruto) {
      return null;
    }
    try {
      return JSON.parse(bruto) as TokenResponse;
    } catch {
      sessionStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }
}

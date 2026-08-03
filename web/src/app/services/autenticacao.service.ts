import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { LoginRequest } from '../models/login-request';
import { UsuarioModulo } from '../models/organizacao';
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
  readonly ehAdminPlataforma = computed(() => this.temPapel('ADMIN_PLATAFORMA'));
  readonly ehAdminEmpresa = computed(() => this.temPapel('ADMIN_EMPRESA'));
  readonly ehFuncionario = computed(
    () =>
      this.autenticado() &&
      this.sessao()?.empresaId != null &&
      !this.ehAdminPlataforma() &&
      !this.ehAdminEmpresa(),
  );

  login(credenciais: LoginRequest): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>(`${this.api.baseUrl}/v1/auth/login`, credenciais)
      .pipe(tap((resposta) => this.persistir(resposta)));
  }

  me(): Observable<UsuarioSessao> {
    return this.http.get<UsuarioSessao>(`${this.api.baseUrl}/v1/auth/me`);
  }

  meusModulos(): Observable<UsuarioModulo[]> {
    return this.http.get<UsuarioModulo[]>(`${this.api.baseUrl}/v1/auth/me/modulos`);
  }

  logout(): void {
    this.limparStorage();
    this.sessao.set(null);
  }

  rotaInicial(): string {
    if (this.ehAdminPlataforma()) {
      return '/admin';
    }
    if (this.ehAdminEmpresa()) {
      return '/org';
    }
    if (this.ehFuncionario()) {
      return '/app';
    }
    return '/admin/login';
  }

  temPapel(papel: string): boolean {
    return this.papeisAtual().some((item) => item.toUpperCase() === papel.toUpperCase());
  }

  private papeisAtual(): string[] {
    return this.asLista(this.sessao()?.papeis);
  }

  private persistir(resposta: TokenResponse): void {
    const normalizada: TokenResponse = {
      ...resposta,
      papeis: this.asLista(resposta.papeis),
      permissoes: this.asLista(resposta.permissoes),
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(normalizada));
    sessionStorage.removeItem(STORAGE_KEY);
    this.sessao.set(normalizada);
  }

  private carregarSessao(): TokenResponse | null {
    const bruto = localStorage.getItem(STORAGE_KEY) ?? sessionStorage.getItem(STORAGE_KEY);
    if (!bruto) {
      return null;
    }
    try {
      const parsed = JSON.parse(bruto) as TokenResponse;
      if (!parsed.accessToken) {
        this.limparStorage();
        return null;
      }
      if (parsed.expiraEm) {
        const expira = Date.parse(parsed.expiraEm);
        if (!Number.isNaN(expira) && expira <= Date.now()) {
          this.limparStorage();
          return null;
        }
      }
      return {
        ...parsed,
        papeis: this.asLista(parsed.papeis),
        permissoes: this.asLista(parsed.permissoes),
      };
    } catch {
      this.limparStorage();
      return null;
    }
  }

  private limparStorage(): void {
    localStorage.removeItem(STORAGE_KEY);
    sessionStorage.removeItem(STORAGE_KEY);
  }

  private asLista(valor: unknown): string[] {
    if (Array.isArray(valor)) {
      return valor.filter((item): item is string => typeof item === 'string');
    }
    if (typeof valor === 'string' && valor.trim()) {
      return [valor];
    }
    return [];
  }
}

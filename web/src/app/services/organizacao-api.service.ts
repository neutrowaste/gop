import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  EmpresaModulo,
  OrganizacaoUsuario,
  OrganizacaoUsuarioRequest,
  UsuarioModulo,
} from '../models/organizacao';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class OrganizacaoApiService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  private get base(): string {
    return `${this.api.baseUrl}/v1/org`;
  }

  listarUsuarios(): Observable<OrganizacaoUsuario[]> {
    return this.http.get<OrganizacaoUsuario[]>(`${this.base}/usuarios`);
  }

  buscarUsuario(id: number): Observable<OrganizacaoUsuario> {
    return this.http.get<OrganizacaoUsuario>(`${this.base}/usuarios/${id}`);
  }

  criarUsuario(request: OrganizacaoUsuarioRequest): Observable<OrganizacaoUsuario> {
    return this.http.post<OrganizacaoUsuario>(`${this.base}/usuarios`, request);
  }

  atualizarUsuario(
    id: number,
    request: OrganizacaoUsuarioRequest,
  ): Observable<OrganizacaoUsuario> {
    return this.http.put<OrganizacaoUsuario>(`${this.base}/usuarios/${id}`, request);
  }

  desativarUsuario(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/usuarios/${id}`);
  }

  listarModulosContratados(): Observable<EmpresaModulo[]> {
    return this.http.get<EmpresaModulo[]>(`${this.base}/modulos`);
  }

  listarModulosDoUsuario(usuarioId: number): Observable<UsuarioModulo[]> {
    return this.http.get<UsuarioModulo[]>(`${this.base}/usuarios/${usuarioId}/modulos`);
  }

  atualizarModuloDoUsuario(
    usuarioId: number,
    moduloId: number,
    liberado: boolean,
  ): Observable<UsuarioModulo> {
    return this.http.put<UsuarioModulo>(
      `${this.base}/usuarios/${usuarioId}/modulos/${moduloId}`,
      { liberado },
    );
  }
}

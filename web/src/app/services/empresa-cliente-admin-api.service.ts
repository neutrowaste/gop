import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { EmpresaAdminResponse, EmpresaAdminRequest } from '../models/empresa-admin';
import { EmpresaModulo } from '../models/organizacao';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class EmpresaClienteAdminApiService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  private base(empresaId: number): string {
    return `${this.api.baseUrl}/v1/admin/empresas/${empresaId}`;
  }

  listarAdmins(empresaId: number): Observable<EmpresaAdminResponse[]> {
    return this.http.get<EmpresaAdminResponse[]>(`${this.base(empresaId)}/admins`);
  }

  criarAdmin(empresaId: number, request: EmpresaAdminRequest): Observable<EmpresaAdminResponse> {
    return this.http.post<EmpresaAdminResponse>(`${this.base(empresaId)}/admins`, request);
  }

  desativarAdmin(empresaId: number, usuarioId: number): Observable<void> {
    return this.http.delete<void>(`${this.base(empresaId)}/admins/${usuarioId}`);
  }

  listarModulos(empresaId: number): Observable<EmpresaModulo[]> {
    return this.http.get<EmpresaModulo[]>(`${this.base(empresaId)}/modulos`);
  }

  atualizarModulo(
    empresaId: number,
    moduloId: number,
    habilitado: boolean,
  ): Observable<EmpresaModulo> {
    return this.http.put<EmpresaModulo>(`${this.base(empresaId)}/modulos/${moduloId}`, {
      habilitado,
    });
  }
}

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { EmpresaAdmin, EmpresaAdminRequest } from '../models/empresa-admin';
import { EmpresaModulo } from '../models/empresa-modulo';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class EmpresaClienteAdminApiService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  private base(empresaId: number): string {
    return `${this.api.baseUrl}/v1/admin/empresas/${empresaId}`;
  }

  listarAdmins(empresaId: number): Observable<EmpresaAdmin[]> {
    return this.http.get<EmpresaAdmin[]>(`${this.base(empresaId)}/admins`);
  }

  criarAdmin(empresaId: number, request: EmpresaAdminRequest): Observable<EmpresaAdmin> {
    return this.http.post<EmpresaAdmin>(`${this.base(empresaId)}/admins`, request);
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

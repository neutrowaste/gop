import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Empresa, EmpresaRequest } from '../models/empresa';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class EmpresaAdminApiService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  private get base(): string {
    return `${this.api.baseUrl}/v1/admin/empresas`;
  }

  listar(ativo?: boolean | null): Observable<Empresa[]> {
    const params =
      ativo === undefined || ativo === null ? undefined : { ativo: String(ativo) };
    return this.http.get<Empresa[]>(this.base, { params });
  }

  buscar(id: number): Observable<Empresa> {
    return this.http.get<Empresa>(`${this.base}/${id}`);
  }

  criar(request: EmpresaRequest): Observable<Empresa> {
    return this.http.post<Empresa>(this.base, request);
  }

  atualizar(id: number, request: EmpresaRequest): Observable<Empresa> {
    return this.http.put<Empresa>(`${this.base}/${id}`, request);
  }

  desativar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Licenca, LicencaRequest, TetoAtivos } from '../models/licenca';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class LicencaAdminApiService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  private get base(): string {
    return `${this.api.baseUrl}/v1/admin`;
  }

  listarPorEmpresa(empresaId: number): Observable<Licenca[]> {
    return this.http.get<Licenca[]>(`${this.base}/empresas/${empresaId}/licencas`);
  }

  criar(empresaId: number, request: LicencaRequest): Observable<Licenca> {
    return this.http.post<Licenca>(`${this.base}/empresas/${empresaId}/licencas`, request);
  }

  atualizar(id: number, request: LicencaRequest): Observable<Licenca> {
    return this.http.put<Licenca>(`${this.base}/licencas/${id}`, request);
  }

  encerrar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/licencas/${id}`);
  }

  consultarTeto(empresaId: number): Observable<TetoAtivos> {
    return this.http.get<TetoAtivos>(`${this.base}/empresas/${empresaId}/teto-ativos`);
  }
}

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';

export interface RasSystemStatus {
  configurado: boolean;
  baseUrl: string;
  login: string | null;
  idFilial: number | null;
  idRotulo: number | null;
  pingOk: boolean;
  filiais: number;
  erro: string | null;
}

export interface RasSystemSyncItem {
  idVeiculo: number | null;
  codigoLogico: string | null;
  rotuloVeiculo: string | null;
  status: string;
  equipamentoId: number | null;
  detalhe: string | null;
}

export interface RasSystemSync {
  lidos: number;
  ingeridos: number;
  ignorados: number;
  naoVinculados: number;
  itens: RasSystemSyncItem[];
}

export interface RasSystemImportarCadastroItem {
  idVeiculo: number | null;
  codigo: string | null;
  status: string;
  equipamentoId: number | null;
  detalhe: string | null;
}

export interface RasSystemImportarCadastro {
  solicitados: number;
  criados: number;
  jaExistentes: number;
  ignorados: number;
  itens: RasSystemImportarCadastroItem[];
  sync: RasSystemSync | null;
}

@Injectable({ providedIn: 'root' })
export class RasSystemApiService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  private get base(): string {
    return `${this.api.baseUrl}/v1/org/integracoes/rassystem`;
  }

  status(): Observable<RasSystemStatus> {
    return this.http.get<RasSystemStatus>(`${this.base}/status`);
  }

  importarCadastro(limite = 5): Observable<RasSystemImportarCadastro> {
    const params = new HttpParams().set('limite', limite);
    return this.http.post<RasSystemImportarCadastro>(`${this.base}/importar-cadastro`, {}, { params });
  }

  sincronizar(): Observable<RasSystemSync> {
    return this.http.post<RasSystemSync>(`${this.base}/sync`, {});
  }
}

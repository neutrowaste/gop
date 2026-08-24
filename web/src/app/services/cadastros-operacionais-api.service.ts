import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  Cidade,
  CidadeRequest,
  Colaborador,
  ColaboradorRequest,
  Equipamento,
  EquipamentoRequest,
  Escala,
  EscalaRequest,
  Ocorrencia,
  OcorrenciaResolucaoRequest,
  ParametroEmpresa,
  Setor,
  SetorDemarcacaoPreview,
  SetorRequest,
  StatusEscala,
  StatusOcorrencia,
  TipoEscala,
  TipoOcorrencia,
  Trecho,
  TrechoDesenhoRequest,
  TrechoExecucao,
  TrechoExecucaoReordenarRequest,
  TrechoExecucaoRequest,
  TrechoImportacao,
  TrechoRequest,
} from '../models/cadastros-operacionais';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class CadastrosOperacionaisApiService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  private get base(): string {
    return `${this.api.baseUrl}/v1/org`;
  }

  listarCidades(): Observable<Cidade[]> {
    return this.http.get<Cidade[]>(`${this.base}/cidades`);
  }

  buscarCidade(id: number): Observable<Cidade> {
    return this.http.get<Cidade>(`${this.base}/cidades/${id}`);
  }

  criarCidade(request: CidadeRequest): Observable<Cidade> {
    return this.http.post<Cidade>(`${this.base}/cidades`, request);
  }

  atualizarCidade(id: number, request: CidadeRequest): Observable<Cidade> {
    return this.http.put<Cidade>(`${this.base}/cidades/${id}`, request);
  }

  desativarCidade(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/cidades/${id}`);
  }

  listarSetores(cidadeId?: number, moduloId?: number): Observable<Setor[]> {
    let params = new HttpParams();
    if (cidadeId != null) {
      params = params.set('cidadeId', cidadeId);
    }
    if (moduloId != null) {
      params = params.set('moduloId', moduloId);
    }
    return this.http.get<Setor[]>(`${this.base}/setores`, { params });
  }

  buscarSetor(id: number): Observable<Setor> {
    return this.http.get<Setor>(`${this.base}/setores/${id}`);
  }

  criarSetor(request: SetorRequest): Observable<Setor> {
    return this.http.post<Setor>(`${this.base}/setores`, request);
  }

  atualizarSetor(id: number, request: SetorRequest): Observable<Setor> {
    return this.http.put<Setor>(`${this.base}/setores/${id}`, request);
  }

  desativarSetor(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/setores/${id}`);
  }

  importarShapefileSetor(id: number, arquivo: File): Observable<Setor> {
    const body = new FormData();
    body.append('arquivo', arquivo);
    return this.http.post<Setor>(`${this.base}/setores/${id}/shapefile`, body);
  }

  preVisualizarShapefileSetor(arquivo: File): Observable<SetorDemarcacaoPreview> {
    const body = new FormData();
    body.append('arquivo', arquivo);
    return this.http.post<SetorDemarcacaoPreview>(`${this.base}/setores/shapefile-preview`, body);
  }

  removerDemarcacaoSetor(id: number): Observable<Setor> {
    return this.http.delete<Setor>(`${this.base}/setores/${id}/shapefile`);
  }

  atualizarDemarcacaoSetor(id: number, geomWkt: string): Observable<Setor> {
    return this.http.put<Setor>(`${this.base}/setores/${id}/demarcacao`, { geomWkt });
  }

  listarTrechos(setorId?: number): Observable<Trecho[]> {
    let params = new HttpParams();
    if (setorId != null) {
      params = params.set('setorId', setorId);
    }
    return this.http.get<Trecho[]>(`${this.base}/trechos`, { params });
  }

  buscarTrecho(id: number): Observable<Trecho> {
    return this.http.get<Trecho>(`${this.base}/trechos/${id}`);
  }

  criarTrecho(request: TrechoRequest): Observable<Trecho> {
    return this.http.post<Trecho>(`${this.base}/trechos`, request);
  }

  criarTrechosDesenho(request: TrechoDesenhoRequest): Observable<TrechoImportacao> {
    return this.http.post<TrechoImportacao>(`${this.base}/trechos/desenho`, request);
  }

  atualizarTrecho(id: number, request: TrechoRequest): Observable<Trecho> {
    return this.http.put<Trecho>(`${this.base}/trechos/${id}`, request);
  }

  desativarTrecho(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/trechos/${id}`);
  }

  importarShapefile(params: {
    setorId: number;
    arquivo: File;
    atributoRua?: string;
    atributoCodigo?: string;
  }): Observable<TrechoImportacao> {
    const body = new FormData();
    body.append('setorId', String(params.setorId));
    body.append('arquivo', params.arquivo);
    if (params.atributoRua?.trim()) {
      body.append('atributoRua', params.atributoRua.trim());
    }
    if (params.atributoCodigo?.trim()) {
      body.append('atributoCodigo', params.atributoCodigo.trim());
    }
    return this.http.post<TrechoImportacao>(`${this.base}/trechos/importar-shapefile`, body);
  }

  listarColaboradores(): Observable<Colaborador[]> {
    return this.http.get<Colaborador[]>(`${this.base}/colaboradores`);
  }

  buscarColaborador(id: number): Observable<Colaborador> {
    return this.http.get<Colaborador>(`${this.base}/colaboradores/${id}`);
  }

  criarColaborador(request: ColaboradorRequest): Observable<Colaborador> {
    return this.http.post<Colaborador>(`${this.base}/colaboradores`, request);
  }

  atualizarColaborador(id: number, request: ColaboradorRequest): Observable<Colaborador> {
    return this.http.put<Colaborador>(`${this.base}/colaboradores/${id}`, request);
  }

  desativarColaborador(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/colaboradores/${id}`);
  }

  listarEquipamentos(): Observable<Equipamento[]> {
    return this.http.get<Equipamento[]>(`${this.base}/equipamentos`);
  }

  buscarEquipamento(id: number): Observable<Equipamento> {
    return this.http.get<Equipamento>(`${this.base}/equipamentos/${id}`);
  }

  criarEquipamento(request: EquipamentoRequest): Observable<Equipamento> {
    return this.http.post<Equipamento>(`${this.base}/equipamentos`, request);
  }

  atualizarEquipamento(id: number, request: EquipamentoRequest): Observable<Equipamento> {
    return this.http.put<Equipamento>(`${this.base}/equipamentos/${id}`, request);
  }

  desativarEquipamento(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/equipamentos/${id}`);
  }

  listarEscalas(filtros?: {
    data?: string;
    setorId?: number;
    status?: StatusEscala;
    tipo?: TipoEscala;
  }): Observable<Escala[]> {
    let params = new HttpParams();
    if (filtros?.data) {
      params = params.set('data', filtros.data);
    }
    if (filtros?.setorId != null) {
      params = params.set('setorId', filtros.setorId);
    }
    if (filtros?.status) {
      params = params.set('status', filtros.status);
    }
    if (filtros?.tipo) {
      params = params.set('tipo', filtros.tipo);
    }
    return this.http.get<Escala[]>(`${this.base}/escalas`, { params });
  }

  buscarEscala(id: number): Observable<Escala> {
    return this.http.get<Escala>(`${this.base}/escalas/${id}`);
  }

  criarEscala(request: EscalaRequest): Observable<Escala> {
    return this.http.post<Escala>(`${this.base}/escalas`, request);
  }

  atualizarEscala(id: number, request: EscalaRequest): Observable<Escala> {
    return this.http.put<Escala>(`${this.base}/escalas/${id}`, request);
  }

  publicarEscala(id: number): Observable<Escala> {
    return this.http.post<Escala>(`${this.base}/escalas/${id}/publicar`, {});
  }

  despublicarEscala(id: number): Observable<Escala> {
    return this.http.post<Escala>(`${this.base}/escalas/${id}/despublicar`, {});
  }

  cancelarEscala(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/escalas/${id}`);
  }

  listarTrechosDaEscala(escalaId: number): Observable<TrechoExecucao[]> {
    return this.http.get<TrechoExecucao[]>(`${this.base}/escalas/${escalaId}/trechos`);
  }

  adicionarTrechoNaEscala(
    escalaId: number,
    request: TrechoExecucaoRequest,
  ): Observable<TrechoExecucao> {
    return this.http.post<TrechoExecucao>(`${this.base}/escalas/${escalaId}/trechos`, request);
  }

  reordenarTrechoNaEscala(
    escalaId: number,
    id: number,
    request: TrechoExecucaoReordenarRequest,
  ): Observable<TrechoExecucao> {
    return this.http.put<TrechoExecucao>(`${this.base}/escalas/${escalaId}/trechos/${id}`, request);
  }

  removerTrechoDaEscala(escalaId: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/escalas/${escalaId}/trechos/${id}`);
  }

  listarOcorrencias(filtros?: {
    data?: string;
    setorId?: number;
    status?: StatusOcorrencia;
    tipo?: TipoOcorrencia;
  }): Observable<Ocorrencia[]> {
    let params = new HttpParams();
    if (filtros?.data) {
      params = params.set('data', filtros.data);
    }
    if (filtros?.setorId != null) {
      params = params.set('setorId', filtros.setorId);
    }
    if (filtros?.status) {
      params = params.set('status', filtros.status);
    }
    if (filtros?.tipo) {
      params = params.set('tipo', filtros.tipo);
    }
    return this.http.get<Ocorrencia[]>(`${this.base}/ocorrencias`, { params });
  }

  buscarOcorrencia(id: number): Observable<Ocorrencia> {
    return this.http.get<Ocorrencia>(`${this.base}/ocorrencias/${id}`);
  }

  resolverOcorrencia(id: number, request: OcorrenciaResolucaoRequest): Observable<Ocorrencia> {
    return this.http.post<Ocorrencia>(`${this.base}/ocorrencias/${id}/resolver`, request);
  }

  listarParametros(): Observable<ParametroEmpresa[]> {
    return this.http.get<ParametroEmpresa[]>(`${this.base}/parametros`);
  }
}

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
  Equipe,
  EquipeRequest,
  Escala,
  EscalaRequest,
  Ocorrencia,
  OcorrenciaResolucaoRequest,
  ParametroEmpresa,
  Rua,
  RuaRequest,
  Setor,
  SetorRequest,
  StatusEscala,
  StatusOcorrencia,
  TipoOcorrencia,
  Trecho,
  TrechoExecucao,
  TrechoExecucaoReordenarRequest,
  TrechoExecucaoRequest,
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

  listarSetores(cidadeId?: number): Observable<Setor[]> {
    let params = new HttpParams();
    if (cidadeId != null) {
      params = params.set('cidadeId', cidadeId);
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

  listarRuas(cidadeId?: number): Observable<Rua[]> {
    let params = new HttpParams();
    if (cidadeId != null) {
      params = params.set('cidadeId', cidadeId);
    }
    return this.http.get<Rua[]>(`${this.base}/ruas`, { params });
  }

  buscarRua(id: number): Observable<Rua> {
    return this.http.get<Rua>(`${this.base}/ruas/${id}`);
  }

  criarRua(request: RuaRequest): Observable<Rua> {
    return this.http.post<Rua>(`${this.base}/ruas`, request);
  }

  atualizarRua(id: number, request: RuaRequest): Observable<Rua> {
    return this.http.put<Rua>(`${this.base}/ruas/${id}`, request);
  }

  desativarRua(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/ruas/${id}`);
  }

  listarTrechos(ruaId?: number): Observable<Trecho[]> {
    let params = new HttpParams();
    if (ruaId != null) {
      params = params.set('ruaId', ruaId);
    }
    return this.http.get<Trecho[]>(`${this.base}/trechos`, { params });
  }

  buscarTrecho(id: number): Observable<Trecho> {
    return this.http.get<Trecho>(`${this.base}/trechos/${id}`);
  }

  criarTrecho(request: TrechoRequest): Observable<Trecho> {
    return this.http.post<Trecho>(`${this.base}/trechos`, request);
  }

  atualizarTrecho(id: number, request: TrechoRequest): Observable<Trecho> {
    return this.http.put<Trecho>(`${this.base}/trechos/${id}`, request);
  }

  desativarTrecho(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/trechos/${id}`);
  }

  listarEquipes(setorId?: number): Observable<Equipe[]> {
    let params = new HttpParams();
    if (setorId != null) {
      params = params.set('setorId', setorId);
    }
    return this.http.get<Equipe[]>(`${this.base}/equipes`, { params });
  }

  buscarEquipe(id: number): Observable<Equipe> {
    return this.http.get<Equipe>(`${this.base}/equipes/${id}`);
  }

  criarEquipe(request: EquipeRequest): Observable<Equipe> {
    return this.http.post<Equipe>(`${this.base}/equipes`, request);
  }

  atualizarEquipe(id: number, request: EquipeRequest): Observable<Equipe> {
    return this.http.put<Equipe>(`${this.base}/equipes/${id}`, request);
  }

  desativarEquipe(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/equipes/${id}`);
  }

  listarColaboradores(equipeId?: number): Observable<Colaborador[]> {
    let params = new HttpParams();
    if (equipeId != null) {
      params = params.set('equipeId', equipeId);
    }
    return this.http.get<Colaborador[]>(`${this.base}/colaboradores`, { params });
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

  listarEquipamentos(equipeId?: number): Observable<Equipamento[]> {
    let params = new HttpParams();
    if (equipeId != null) {
      params = params.set('equipeId', equipeId);
    }
    return this.http.get<Equipamento[]>(`${this.base}/equipamentos`, { params });
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
    equipeId?: number;
    status?: StatusEscala;
  }): Observable<Escala[]> {
    let params = new HttpParams();
    if (filtros?.data) {
      params = params.set('data', filtros.data);
    }
    if (filtros?.setorId != null) {
      params = params.set('setorId', filtros.setorId);
    }
    if (filtros?.equipeId != null) {
      params = params.set('equipeId', filtros.equipeId);
    }
    if (filtros?.status) {
      params = params.set('status', filtros.status);
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
    equipeId?: number;
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
    if (filtros?.equipeId != null) {
      params = params.set('equipeId', filtros.equipeId);
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

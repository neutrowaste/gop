export type StatusTrecho =
  | 'PLANEJADO'
  | 'EXECUTADO'
  | 'SUSPEITO'
  | 'PENDENTE'
  | 'JUSTIFICADO'
  | 'FORA_DA_ROTA';

export const STATUS_TRECHO_OPCOES: StatusTrecho[] = [
  'PLANEJADO',
  'EXECUTADO',
  'SUSPEITO',
  'PENDENTE',
  'JUSTIFICADO',
  'FORA_DA_ROTA',
];

export interface Cidade {
  id: number;
  nome: string;
  uf: string;
  ativo: boolean;
}

export interface CidadeRequest {
  nome: string;
  uf?: string | null;
  ativo?: boolean | null;
}

export interface Setor {
  id: number;
  cidadeId: number;
  cidadeNome: string;
  codigo: string | null;
  nome: string;
  ativo: boolean;
}

export interface SetorRequest {
  cidadeId: number;
  codigo?: string | null;
  nome: string;
  ativo?: boolean | null;
}

export interface Rua {
  id: number;
  cidadeId: number;
  cidadeNome: string;
  nome: string;
  geomWkt: string;
  ativo: boolean;
}

export interface RuaRequest {
  cidadeId: number;
  nome: string;
  geomWkt: string;
  ativo?: boolean | null;
}

export interface Trecho {
  id: number;
  ruaId: number;
  ruaNome: string;
  codigo: string;
  status: StatusTrecho;
  percentualExecutado: number;
  comprimentoMetros: number;
  ultimaVisitaEm: string | null;
  emCorrecao: boolean;
  liberadoInforme: boolean;
  geomWkt: string;
  ativo: boolean;
}

export interface TrechoRequest {
  ruaId: number;
  codigo: string;
  geomWkt: string;
  status?: StatusTrecho | null;
  percentualExecutado?: number | null;
  comprimentoMetros?: number | null;
  emCorrecao?: boolean | null;
  liberadoInforme?: boolean | null;
  ativo?: boolean | null;
}

export interface Equipe {
  id: number;
  setorId: number;
  setorNome: string;
  nome: string;
  ativo: boolean;
}

export interface EquipeRequest {
  setorId: number;
  nome: string;
  ativo?: boolean | null;
}

export interface Colaborador {
  id: number;
  equipeId: number;
  equipeNome: string;
  nome: string;
  matricula: string;
  ativo: boolean;
}

export interface ColaboradorRequest {
  equipeId: number;
  nome: string;
  matricula: string;
  ativo?: boolean | null;
}

export interface Equipamento {
  id: number;
  equipeId: number;
  equipeNome: string;
  codigo: string;
  descricao: string | null;
  ativo: boolean;
}

export interface EquipamentoRequest {
  equipeId: number;
  codigo: string;
  descricao?: string | null;
  ativo?: boolean | null;
}

export type Turno = 'MANHA' | 'TARDE' | 'NOITE';

export const TURNO_OPCOES: Turno[] = ['MANHA', 'TARDE', 'NOITE'];

export const TURNO_LABELS: Record<Turno, string> = {
  MANHA: 'Manhã',
  TARDE: 'Tarde',
  NOITE: 'Noite',
};

export type StatusEscala = 'RASCUNHO' | 'PUBLICADA';

/**
 * Amarração diária obrigatória do motor de Varrição (RN-001):
 * Setor ↔ Equipe ↔ Ativo GPS (Equipamento) ↔ Supervisor ↔ Turno.
 */
export interface Escala {
  id: number;
  data: string;
  turno: Turno;
  setorId: number;
  setorNome: string;
  equipeId: number;
  equipeNome: string;
  equipamentoId: number;
  equipamentoCodigo: string;
  supervisorUsuarioId: number;
  supervisorNome: string;
  status: StatusEscala;
  publicadaEm: string | null;
  ativo: boolean;
}

export interface EscalaRequest {
  data: string;
  turno: Turno;
  setorId: number;
  equipeId: number;
  equipamentoId: number;
  supervisorUsuarioId: number;
  ativo?: boolean | null;
}

/**
 * Plano diário de trechos+ordem dentro da Escala (RN-001), editável apenas
 * enquanto a Escala está em RASCUNHO. O motor de cobertura atualiza
 * status/percentual quando a Escala é publicada.
 */
export interface TrechoExecucao {
  id: number;
  escalaId: number;
  trechoId: number;
  trechoCodigo: string;
  ruaNome: string;
  ordem: number;
  status: StatusTrecho;
  percentualExecutado: number;
  quantidadePosicoesValidas: number;
  tevePosicaoInicio: boolean;
  tevePosicaoMeio: boolean;
  tevePosicaoFim: boolean;
  primeiraPosicaoValidaEm: string | null;
  ultimaPosicaoValidaEm: string | null;
  ativo: boolean;
}

export interface TrechoExecucaoRequest {
  trechoId: number;
  ordem: number;
}

export interface TrechoExecucaoReordenarRequest {
  ordem: number;
}

export type TipoOcorrencia = 'ORDEM_PLANO_VIOLADA' | 'GPS_SILENCIOSO';

export const TIPO_OCORRENCIA_LABELS: Record<TipoOcorrencia, string> = {
  ORDEM_PLANO_VIOLADA: 'Ordem do plano violada',
  GPS_SILENCIOSO: 'GPS em silêncio',
};

export type StatusOcorrencia = 'ABERTA' | 'RESOLVIDA' | 'JUSTIFICADA';

/** Ocorrência automática do motor de Varrição (RN-001). */
export interface Ocorrencia {
  id: number;
  escalaId: number;
  escalaData: string;
  setorNome: string;
  equipeNome: string;
  equipamentoId: number;
  equipamentoCodigo: string;
  trechoExecucaoId: number | null;
  trechoCodigo: string | null;
  tipo: TipoOcorrencia;
  descricao: string;
  status: StatusOcorrencia;
  detectadaEm: string;
  resolvidaEm: string | null;
  resolvidaPorNome: string | null;
  observacaoResolucao: string | null;
  ativo: boolean;
}

export interface OcorrenciaResolucaoRequest {
  status: 'RESOLVIDA' | 'JUSTIFICADA';
  observacao?: string | null;
}

export interface ParametroEmpresa {
  chave: string;
  valor: string;
  tipo: string;
  descricao: string | null;
  padrao: boolean;
  ativo: boolean;
}

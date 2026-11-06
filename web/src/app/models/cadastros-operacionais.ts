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

export interface SetorDemarcacaoPreview {
  geomWkt: string;
  geomGeoJson: {
    type: string;
    coordinates: unknown;
  } | null;
}

export interface Setor {
  id: number;
  moduloId: number;
  moduloCodigo: string;
  moduloNome: string;
  cidadeId: number;
  cidadeNome: string;
  codigo: string | null;
  nome: string;
  temDemarcacao: boolean;
  geomWkt: string | null;
  geomGeoJson: {
    type: string;
    coordinates: unknown;
  } | null;
  ativo: boolean;
}

export interface SetorRequest {
  moduloId: number;
  cidadeId: number;
  codigo?: string | null;
  nome: string;
  ativo?: boolean | null;
}

export function rotuloSetor(setor: Pick<Setor, 'codigo' | 'nome' | 'moduloNome'>): string {
  const codigo = setor.codigo?.trim();
  const base = codigo ? `${codigo} · ${setor.nome}` : setor.nome;
  return setor.moduloNome ? `${base} · ${setor.moduloNome}` : base;
}

export interface Trecho {
  id: number;
  setorId: number;
  setorNome: string;
  setorCodigo: string | null;
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
  setorId: number;
  ruaNome?: string | null;
  codigo: string;
  geomWkt: string;
  status?: StatusTrecho | null;
  percentualExecutado?: number | null;
  comprimentoMetros?: number | null;
  emCorrecao?: boolean | null;
  liberadoInforme?: boolean | null;
  ativo?: boolean | null;
}

export interface TrechoDesenhoRequest {
  setorId: number;
  ruaNome?: string | null;
  codigo?: string | null;
  geomWkts: string[];
  status?: StatusTrecho | null;
}

export interface TrechoImportacaoItem {
  indice: number;
  codigo: string | null;
  ruaNome: string | null;
  status: string;
  detalhe: string | null;
}

export interface TrechoImportacao {
  lidos: number;
  criados: number;
  ignorados: number;
  itens: TrechoImportacaoItem[];
}

export interface Colaborador {
  id: number;
  nome: string;
  matricula: string;
  equipamentoId: number;
  equipamentoCodigo: string;
  ativo: boolean;
}

export interface ColaboradorRequest {
  nome: string;
  matricula: string;
  equipamentoId: number;
  ativo?: boolean | null;
}

export interface Equipamento {
  id: number;
  codigo: string;
  descricao: string | null;
  codigoExterno?: string | null;
  fonte?: 'MANUAL' | 'RASSYSTEM' | 'PROPRIO' | null;
  ativo: boolean;
}

export interface EquipamentoRequest {
  codigo: string;
  descricao?: string | null;
  codigoExterno?: string | null;
  ativo?: boolean | null;
  fonte?: Equipamento['fonte'];
}

export type Turno = 'MANHA' | 'TARDE' | 'NOITE';

export const TURNO_OPCOES: Turno[] = ['MANHA', 'TARDE', 'NOITE'];

export const TURNO_LABELS: Record<Turno, string> = {
  MANHA: 'Manhã',
  TARDE: 'Tarde',
  NOITE: 'Noite',
};

export type StatusEscala = 'RASCUNHO' | 'PUBLICADA';

export type TipoEscala = 'FREQUENCIA' | 'AVULSA';

export type DiaSemana =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export const TIPO_ESCALA_OPCOES: TipoEscala[] = ['FREQUENCIA', 'AVULSA'];

export const TIPO_ESCALA_LABELS: Record<TipoEscala, string> = {
  FREQUENCIA: 'Frequência',
  AVULSA: 'Avulsa',
};

export const DIA_SEMANA_OPCOES: { valor: DiaSemana; label: string }[] = [
  { valor: 'MONDAY', label: 'Seg' },
  { valor: 'TUESDAY', label: 'Ter' },
  { valor: 'WEDNESDAY', label: 'Qua' },
  { valor: 'THURSDAY', label: 'Qui' },
  { valor: 'FRIDAY', label: 'Sex' },
  { valor: 'SATURDAY', label: 'Sáb' },
  { valor: 'SUNDAY', label: 'Dom' },
];

export interface EscalaColaborador {
  id: number;
  nome: string;
  matricula: string;
  equipamentoId: number;
  equipamentoCodigo: string;
}

/**
 * Amarração do motor de Varrição (RN-001):
 * Setor ↔ Colaboradores (Lutocar designado) ↔ Supervisor ↔ Turno.
 * Frequência: modelo recorrente (data nula). Avulsa e instância do dia têm data.
 */
export interface Escala {
  id: number;
  tipo: TipoEscala;
  data: string | null;
  diasSemana: DiaSemana[];
  origemId: number | null;
  turno: Turno;
  setorId: number;
  setorNome: string;
  colaboradores: EscalaColaborador[];
  supervisorUsuarioId: number;
  supervisorNome: string;
  status: StatusEscala;
  publicadaEm: string | null;
  ativo: boolean;
}

export interface EscalaRequest {
  tipo: TipoEscala;
  data?: string | null;
  diasSemana?: DiaSemana[];
  turno: Turno;
  setorId: number;
  colaboradorIds: number[];
  supervisorUsuarioId: number;
  trechoIds?: number[] | null;
  ativo?: boolean | null;
}

/**
 * Plano de trechos da Escala (RN-001). No modelo de frequência é o plano
 * recorrente; na avulsa e na instância do dia o motor atualiza o progresso.
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
  colaboradoresNomes: string;
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

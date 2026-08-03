import { StatusTrecho, Turno } from './cadastros-operacionais';

/** Situação de sinal do ativo GPS (RN-001: tolerância de 30min, silêncio >= 60min é falha). */
export type SituacaoSinal = 'ONLINE' | 'ATENCAO' | 'OFFLINE';

export const SITUACAO_SINAL_CORES: Record<SituacaoSinal, string> = {
  ONLINE: '#22c55e',
  ATENCAO: '#f59e0b',
  OFFLINE: '#94a3b8',
};

export const SITUACAO_SINAL_LABELS: Record<SituacaoSinal, string> = {
  ONLINE: 'Online',
  ATENCAO: 'Atenção',
  OFFLINE: 'Offline',
};

/** Paleta de cores por status de trecho, herdada do legado (apimonitoragps). */
export const STATUS_TRECHO_CORES: Record<StatusTrecho, string> = {
  EXECUTADO: '#4ade80',
  SUSPEITO: '#fbbf24',
  PENDENTE: '#f87171',
  FORA_DA_ROTA: '#3b82f6',
  PLANEJADO: '#6b7280',
  JUSTIFICADO: '#a78bfa',
};

export type GeoJsonGeometry =
  | { type: 'Point'; coordinates: [number, number] }
  | { type: 'LineString'; coordinates: [number, number][] };

export interface MapaAtivo {
  escalaId: number;
  equipamentoId: number;
  equipamentoCodigo: string;
  equipeId: number;
  equipeNome: string;
  setorId: number;
  setorNome: string;
  turno: Turno;
  supervisorUsuarioId: number;
  supervisorNome: string;
  latitude: number;
  longitude: number;
  velocidadeKmh: number | null;
  recebidoEm: string;
  situacaoSinal: SituacaoSinal;
}

export interface MapaTrecho {
  trechoExecucaoId: number;
  escalaId: number;
  trechoId: number;
  trechoCodigo: string;
  ruaNome: string;
  setorId: number;
  setorNome: string;
  equipeId: number;
  equipeNome: string;
  ordem: number;
  status: StatusTrecho;
  percentualExecutado: number;
  geometry: GeoJsonGeometry;
}

export interface MapaOperacional {
  data: string;
  geradoEm: string;
  ativos: MapaAtivo[];
  trechos: MapaTrecho[];
}

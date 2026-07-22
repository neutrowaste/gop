export type StatusLicenca = 'ATIVA' | 'SUSPENSA' | 'ENCERRADA';

export interface Licenca {
  id: number;
  empresaId: number;
  empresaCodigo: string;
  tetoAtivos: number;
  vigenciaInicio: string;
  vigenciaFim: string | null;
  status: StatusLicenca;
  ativo: boolean;
  criadoEm: string;
  atualizadoEm: string;
}

export interface LicencaRequest {
  tetoAtivos: number;
  vigenciaInicio: string;
  vigenciaFim?: string | null;
  status: StatusLicenca;
  ativo?: boolean | null;
}

export interface TetoAtivos {
  empresaId: number;
  licencaId: number;
  tetoAtivos: number;
  ativosEmUso: number;
  saldoDisponivel: number;
  permiteNovoAtivo: boolean;
}

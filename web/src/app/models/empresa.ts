export interface Empresa {
  id: number;
  codigo: string;
  nome: string;
  ativo: boolean;
  criadoEm: string;
  atualizadoEm: string;
}

export interface EmpresaRequest {
  codigo: string;
  nome: string;
  ativo?: boolean | null;
}

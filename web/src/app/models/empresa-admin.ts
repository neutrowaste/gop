export interface EmpresaAdminRequest {
  email: string;
  senha: string;
  nome: string;
}

export interface EmpresaAdminResponse {
  id: number;
  empresaId: number;
  email: string;
  nome: string;
  ativo: boolean;
  criadoEm: string;
  atualizadoEm: string;
}

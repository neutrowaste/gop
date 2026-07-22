export interface EmpresaAdmin {
  id: number;
  empresaId: number;
  email: string;
  nome: string;
  ativo: boolean;
  criadoEm: string;
  atualizadoEm: string;
}

export interface EmpresaAdminRequest {
  email: string;
  senha: string;
  nome: string;
}

export interface OrganizacaoUsuario {
  id: number;
  empresaId: number;
  email: string;
  nome: string;
  ativo: boolean;
  administradorEmpresa: boolean;
  papeis: string[];
  criadoEm: string;
  atualizadoEm: string;
}

export interface OrganizacaoUsuarioRequest {
  email: string;
  senha?: string | null;
  nome: string;
  ativo?: boolean;
  administradorEmpresa?: boolean;
}

export interface UsuarioModulo {
  moduloId: number;
  codigo: string;
  nome: string;
  categoria: string;
  ordem: number;
  contratado: boolean;
  liberado: boolean;
  desde: string | null;
}

export interface EmpresaModulo {
  moduloId: number;
  codigo: string;
  nome: string;
  categoria: string;
  ordem: number;
  habilitado: boolean;
  desde: string | null;
}

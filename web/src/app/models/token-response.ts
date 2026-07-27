export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiraEm: string;
  usuarioId: number;
  empresaId: number | null;
  empresaNome: string | null;
  empresaCodigo: string | null;
  nome: string;
  email: string;
  papeis: string[];
  permissoes: string[];
}

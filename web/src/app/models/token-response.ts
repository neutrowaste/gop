export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiraEm: string;
  usuarioId: number;
  empresaId: number | null;
  nome: string;
  email: string;
  papeis: string[];
  permissoes: string[];
}

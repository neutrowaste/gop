export interface UsuarioSessao {
  usuarioId: number;
  empresaId: number | null;
  nome: string;
  email: string;
  papeis: string[];
  permissoes: string[];
}

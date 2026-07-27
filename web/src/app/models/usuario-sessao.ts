export interface UsuarioSessao {
  usuarioId: number;
  empresaId: number | null;
  empresaNome: string | null;
  empresaCodigo: string | null;
  nome: string;
  email: string;
  papeis: string[];
  permissoes: string[];
}

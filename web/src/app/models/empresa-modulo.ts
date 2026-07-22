export type CategoriaModulo = 'OPERACIONAL' | 'SUPORTE' | 'TRANSVERSAL';

export interface EmpresaModulo {
  moduloId: number;
  codigo: string;
  nome: string;
  categoria: CategoriaModulo;
  ordem: number;
  habilitado: boolean;
  desde: string | null;
}

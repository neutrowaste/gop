import {
  LIMIAR_VERDE_COMPLETO,
  recortarLinhaAte,
  trechoComProgressoParcial,
  trechoVerdeCompleto,
} from './cobertura-trecho-visual';

describe('cobertura visual do trecho', () => {
  const linha: [number, number][] = [
    [-46.0, -23.0],
    [-46.0, -23.01],
    [-46.0, -23.02],
  ];

  it('recorta metade do comprimento a partir da origem', () => {
    const recorte = recortarLinhaAte(linha, 0.5);
    expect(recorte[0]).toEqual(linha[0]);
    expect(recorte[recorte.length - 1][0]).toBeCloseTo(-46);
    expect(recorte[recorte.length - 1][1]).toBeCloseTo(-23.01, 5);
  });

  it('não desenha progresso zerado e devolve a linha inteira em 100%', () => {
    expect(recortarLinhaAte(linha, 0)).toEqual([]);
    expect(recortarLinhaAte(linha, 1)).toEqual(linha);
  });

  it('pinta o trecho inteiro de verde a partir do limiar e no executado', () => {
    expect(trechoVerdeCompleto({ status: 'PENDENTE', percentualExecutado: LIMIAR_VERDE_COMPLETO })).toBe(true);
    expect(trechoVerdeCompleto({ status: 'PENDENTE', percentualExecutado: 79.9 })).toBe(false);
    expect(trechoVerdeCompleto({ status: 'EXECUTADO', percentualExecutado: 40 })).toBe(true);
    expect(trechoVerdeCompleto({ status: 'JUSTIFICADO', percentualExecutado: 100 })).toBe(false);
  });

  it('mostra a faixa parcial só enquanto o trecho ainda está em andamento', () => {
    expect(trechoComProgressoParcial({ status: 'PENDENTE', percentualExecutado: 35 })).toBe(true);
    expect(trechoComProgressoParcial({ status: 'PLANEJADO', percentualExecutado: 0 })).toBe(false);
    expect(trechoComProgressoParcial({ status: 'PENDENTE', percentualExecutado: 80 })).toBe(false);
    expect(trechoComProgressoParcial({ status: 'SUSPEITO', percentualExecutado: 40 })).toBe(false);
  });
});

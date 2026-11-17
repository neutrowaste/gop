import { StatusTrecho } from '../../models/cadastros-operacionais';

/** Limiar visual da RN-001: abaixo disso a linha verde acompanha o avanço; a partir daqui o trecho inteiro fica verde. */
export const LIMIAR_VERDE_COMPLETO = 80;

type TrechoVisual = {
  status: StatusTrecho;
  percentualExecutado: number;
};

/** Trecho pintado por inteiro de verde: já executado, ou progresso em andamento que atingiu o limiar. */
export function trechoVerdeCompleto(trecho: TrechoVisual): boolean {
  if (trecho.status === 'EXECUTADO') {
    return true;
  }
  if (trecho.status !== 'PENDENTE' && trecho.status !== 'PLANEJADO') {
    return false;
  }
  return trecho.percentualExecutado >= LIMIAR_VERDE_COMPLETO;
}

/** Faixa verde parcial, do início da geometria até o progresso linear já alcançado. */
export function trechoComProgressoParcial(trecho: TrechoVisual): boolean {
  return (
    (trecho.status === 'PENDENTE' || trecho.status === 'PLANEJADO') &&
    trecho.percentualExecutado > 0 &&
    trecho.percentualExecutado < LIMIAR_VERDE_COMPLETO
  );
}

/**
 * Recorta a linha do início até `fracao` (0–1) do comprimento.
 * O percentual executado é o maior ST_LineLocatePoint, então o verde cresce a partir da origem do trecho.
 */
export function recortarLinhaAte(coordenadas: [number, number][], fracao: number): [number, number][] {
  if (coordenadas.length < 2 || fracao <= 0) {
    return [];
  }
  if (fracao >= 1) {
    return coordenadas.map((ponto) => [ponto[0], ponto[1]]);
  }

  const acumulado: number[] = [0];
  for (let indice = 1; indice < coordenadas.length; indice++) {
    acumulado.push(acumulado[indice - 1] + distanciaMetros(coordenadas[indice - 1], coordenadas[indice]));
  }
  const total = acumulado[acumulado.length - 1];
  if (total === 0) {
    return [copiar(coordenadas[0]), copiar(coordenadas[coordenadas.length - 1])];
  }

  const alvo = total * fracao;
  const resultado: [number, number][] = [copiar(coordenadas[0])];
  for (let indice = 1; indice < coordenadas.length; indice++) {
    if (acumulado[indice] < alvo) {
      resultado.push(copiar(coordenadas[indice]));
      continue;
    }
    const anterior = acumulado[indice - 1];
    const segmento = acumulado[indice] - anterior;
    const t = segmento === 0 ? 0 : (alvo - anterior) / segmento;
    const a = coordenadas[indice - 1];
    const b = coordenadas[indice];
    resultado.push([a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t]);
    return resultado;
  }
  return resultado;
}

function copiar(ponto: [number, number]): [number, number] {
  return [ponto[0], ponto[1]];
}

function distanciaMetros(a: [number, number], b: [number, number]): number {
  const radiano = Math.PI / 180;
  const latitudeA = a[1] * radiano;
  const latitudeB = b[1] * radiano;
  const deltaLatitude = latitudeB - latitudeA;
  const deltaLongitude = (b[0] - a[0]) * radiano;
  const h =
    Math.sin(deltaLatitude / 2) ** 2 +
    Math.cos(latitudeA) * Math.cos(latitudeB) * Math.sin(deltaLongitude / 2) ** 2;
  return 2 * 6_371_000 * Math.asin(Math.min(1, Math.sqrt(h)));
}

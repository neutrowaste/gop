export type AnelLngLat = [number, number][];

export type GeometriaSetorGeoJson =
  | { type: 'Polygon'; coordinates: number[][][] }
  | { type: 'MultiPolygon'; coordinates: number[][][][] };

export function poligonoParaWkt(vertices: AnelLngLat): string | null {
  const anel = anelFechado(vertices);
  if (anel.length < 4) {
    return null;
  }
  const corpo = anel.map(([lng, lat]) => `${lng} ${lat}`).join(', ');
  return `POLYGON((${corpo}))`;
}

export function linhaParaWkt(vertices: AnelLngLat): string | null {
  if (vertices.length < 2) {
    return null;
  }
  const corpo = vertices.map(([lng, lat]) => `${lng} ${lat}`).join(', ');
  return `LINESTRING(${corpo})`;
}

export function wktParaLinha(wkt: string | null | undefined): AnelLngLat | null {
  const linhas = wktParaLinhas(wkt);
  return linhas.length === 1 ? linhas[0] : null;
}

export function wktParaLinhas(wkt: string | null | undefined): AnelLngLat[] {
  if (!wkt?.trim()) {
    return [];
  }
  const texto = wkt.trim();
  if (/^MULTILINESTRING/i.test(texto)) {
    const inner = conteudoParenteses(texto.replace(/^MULTILINESTRING/i, '').trim());
    if (!inner) {
      return [];
    }
    const linhas: AnelLngLat[] = [];
    for (const parte of dividirNivelZero(inner)) {
      const coords = parte.trim().startsWith('(') ? conteudoParenteses(parte.trim()) : parte;
      const linha = paresParaVertices(coords ?? parte, 2);
      if (linha) {
        linhas.push(linha);
      }
    }
    return linhas;
  }
  if (/^LINESTRING/i.test(texto)) {
    const inner = conteudoParenteses(texto.replace(/^LINESTRING/i, '').trim());
    const linha = paresParaVertices(inner ?? texto, 2);
    return linha ? [linha] : [];
  }
  return [];
}

export function wktParaAnel(wkt: string | null | undefined): AnelLngLat | null {
  const geo = wktParaGeoJson(wkt);
  if (!geo) {
    return null;
  }
  const anel = geo.type === 'Polygon' ? geo.coordinates[0] : geo.coordinates[0]?.[0];
  return anelParaVertices(anel);
}

export function wktParaGeoJson(wkt: string | null | undefined): GeometriaSetorGeoJson | null {
  if (!wkt?.trim()) {
    return null;
  }
  const texto = wkt.trim();
  if (/^MULTIPOLYGON/i.test(texto)) {
    const inner = conteudoParenteses(texto.replace(/^MULTIPOLYGON/i, '').trim());
    if (!inner) {
      return null;
    }
    const poligonos: number[][][][] = [];
    for (const parte of dividirNivelZero(inner)) {
      const aneis = parseAneisPoligono(parte);
      if (!aneis) {
        return null;
      }
      poligonos.push(aneis);
    }
    return poligonos.length ? { type: 'MultiPolygon', coordinates: poligonos } : null;
  }
  if (/^POLYGON/i.test(texto)) {
    const aneis = parseAneisPoligono(texto.replace(/^POLYGON/i, '').trim());
    return aneis ? { type: 'Polygon', coordinates: aneis } : null;
  }
  return null;
}

export function normalizarGeometriaSetor(valor: unknown): GeometriaSetorGeoJson | null {
  if (!valor || typeof valor !== 'object') {
    return null;
  }
  const geo = valor as { type?: string; coordinates?: unknown };
  if (geo.type === 'Polygon' && Array.isArray(geo.coordinates)) {
    return { type: 'Polygon', coordinates: geo.coordinates as number[][][] };
  }
  if (geo.type === 'MultiPolygon' && Array.isArray(geo.coordinates)) {
    return { type: 'MultiPolygon', coordinates: geo.coordinates as number[][][][] };
  }
  return null;
}

export function pontosDaGeometria(geo: GeometriaSetorGeoJson): AnelLngLat {
  const pontos: AnelLngLat = [];
  if (geo.type === 'Polygon') {
    coletarAnel(pontos, geo.coordinates);
  } else {
    for (const poligono of geo.coordinates) {
      coletarAnel(pontos, poligono);
    }
  }
  return pontos;
}

export function anelFechado(vertices: AnelLngLat): AnelLngLat {
  if (vertices.length < 3) {
    return vertices;
  }
  const primeiro = vertices[0];
  const ultimo = vertices[vertices.length - 1];
  if (primeiro[0] === ultimo[0] && primeiro[1] === ultimo[1]) {
    return vertices;
  }
  return [...vertices, primeiro];
}

function coletarAnel(destino: AnelLngLat, aneis: number[][][]): void {
  for (const anel of aneis) {
    for (const ponto of anel) {
      if (ponto.length >= 2 && Number.isFinite(ponto[0]) && Number.isFinite(ponto[1])) {
        destino.push([ponto[0], ponto[1]]);
      }
    }
  }
}

function anelParaVertices(anel: number[][] | undefined): AnelLngLat | null {
  if (!anel || anel.length < 4) {
    return null;
  }
  const pontos: AnelLngLat = [];
  for (const ponto of anel) {
    if (ponto.length < 2 || !Number.isFinite(ponto[0]) || !Number.isFinite(ponto[1])) {
      return null;
    }
    pontos.push([ponto[0], ponto[1]]);
  }
  const primeiro = pontos[0];
  const ultimo = pontos[pontos.length - 1];
  if (primeiro[0] === ultimo[0] && primeiro[1] === ultimo[1]) {
    pontos.pop();
  }
  return pontos.length >= 3 ? pontos : null;
}

function parseAneisPoligono(texto: string): number[][][] | null {
  const inner = texto.trim().startsWith('(') ? conteudoParenteses(texto.trim()) : texto;
  if (!inner) {
    return null;
  }
  const aneis: number[][][] = [];
  for (const parte of dividirNivelZero(inner)) {
    const coords = parte.trim().startsWith('(') ? conteudoParenteses(parte.trim()) : parte;
    const anel = parsePares(coords ?? parte);
    if (!anel) {
      return null;
    }
    aneis.push(anel);
  }
  return aneis.length ? aneis : null;
}

function parsePares(texto: string): number[][] | null {
  return paresParaVertices(texto, 4);
}

function paresParaVertices(texto: string | null, minimo: number): AnelLngLat | null {
  if (!texto) {
    return null;
  }
  const pontos: AnelLngLat = [];
  const regex = /(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)\s+(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)/g;
  let match: RegExpExecArray | null;
  while ((match = regex.exec(texto))) {
    pontos.push([Number(match[1]), Number(match[2])]);
  }
  return pontos.length >= minimo ? pontos : null;
}

function conteudoParenteses(texto: string): string | null {
  const inicio = texto.indexOf('(');
  if (inicio < 0) {
    return null;
  }
  let nivel = 0;
  for (let i = inicio; i < texto.length; i++) {
    if (texto[i] === '(') {
      nivel++;
    } else if (texto[i] === ')') {
      nivel--;
      if (nivel === 0) {
        return texto.slice(inicio + 1, i);
      }
    }
  }
  return null;
}

function dividirNivelZero(texto: string): string[] {
  const partes: string[] = [];
  let nivel = 0;
  let inicio = 0;
  for (let i = 0; i < texto.length; i++) {
    const char = texto[i];
    if (char === '(') {
      nivel++;
    } else if (char === ')') {
      nivel--;
    } else if (char === ',' && nivel === 0) {
      partes.push(texto.slice(inicio, i));
      inicio = i + 1;
    }
  }
  partes.push(texto.slice(inicio));
  return partes.map((parte) => parte.trim()).filter(Boolean);
}

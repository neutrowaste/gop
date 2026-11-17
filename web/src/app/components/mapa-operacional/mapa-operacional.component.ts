import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { NgClass } from '@angular/common';
import type { FeatureCollection, Geometry } from 'geojson';
import maplibregl, {
  GeoJSONSource,
  LngLatBoundsLike,
  Map as MapLibreMap,
  Popup,
} from 'maplibre-gl';

import {
  MapaAtivo,
  MapaOperacional,
  MapaTrecho,
  SITUACAO_SINAL_CORES,
  SITUACAO_SINAL_LABELS,
  STATUS_TRECHO_CORES,
} from '../../models/mapa-operacional';
import { STATUS_TRECHO_OPCOES, StatusTrecho } from '../../models/cadastros-operacionais';
import { ESTILO_BASEMAP } from '../../services/basemap';
import {
  GeometriaSetorGeoJson,
  normalizarGeometriaSetor,
  pontosDaGeometria,
  wktParaGeoJson,
} from '../../services/geometria-wkt';
import { recortarLinhaAte, trechoComProgressoParcial, trechoVerdeCompleto } from './cobertura-trecho-visual';

const FONTE_SETOR = 'gop-setor-operacao';
const CAMADA_SETOR_FILL = 'gop-setor-operacao-fill';
const CAMADA_SETOR_LINHA = 'gop-setor-operacao-linha';
const FONTE_TRECHOS = 'gop-trechos';
const FONTE_PERCURSO = 'gop-trechos-percurso';
const FONTE_ATIVOS = 'gop-ativos';
const CAMADA_TRECHOS = 'gop-trechos-linha';
const CAMADA_PERCURSO = 'gop-trechos-percurso';
const CAMADA_ATIVOS = 'gop-ativos-ponto';

/**
 * Mapa operacional (RN-001): trechos do plano do dia coloridos por status e
 * posição atual dos ativos GPS. TODO(fase futura): trocar o basemap público
 * por tiles pg_tileserv/Martin quando a infra existir, e migrar a atualização
 * de polling para WebSocket incremental.
 */
export interface RecorteMapa {
  setorId: number;
  escalaId: number;
}

@Component({
  selector: 'app-mapa-operacional',
  standalone: true,
  imports: [NgClass],
  templateUrl: './mapa-operacional.component.html',
  host: {
    class: 'block h-full min-h-0 w-full',
  },
})
export class MapaOperacionalComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() dados: MapaOperacional | null = null;
  @Input() compacto = false;
  @Input() setorGeom: unknown = null;
  @Input() setorNome: string | null = null;
  /** Com setor escolhido, o enquadramento segue as linhas dos trechos. */
  @Input() enquadrarTrechos = false;
  @Output() readonly recorteSelecionado = new EventEmitter<RecorteMapa>();

  @ViewChild('mapaContainer', { static: true }) private mapaContainer!: ElementRef<HTMLDivElement>;

  readonly statusOpcoes = STATUS_TRECHO_OPCOES;
  readonly statusCores = STATUS_TRECHO_CORES;
  readonly situacaoLabels = SITUACAO_SINAL_LABELS;
  readonly situacaoCores = SITUACAO_SINAL_CORES;
  setorDestacado = false;

  private mapa: MapLibreMap | null = null;
  private mapaPronto = false;
  private popup: Popup | null = null;
  private resizeObserver: ResizeObserver | null = null;
  private assinaturaEnquadramento = '';

  ngAfterViewInit(): void {
    const container = this.mapaContainer.nativeElement;
    this.mapa = new maplibregl.Map({
      container,
      style: ESTILO_BASEMAP,
      center: [-46.63, -23.55],
      zoom: 12,
    });
    this.mapa.addControl(new maplibregl.NavigationControl(), 'top-right');

    this.resizeObserver = new ResizeObserver(() => {
      this.mapa?.resize();
    });
    this.resizeObserver.observe(container);

    this.mapa.on('load', () => {
      this.mapa!.resize();
      this.mapa!.addSource(FONTE_SETOR, {
        type: 'geojson',
        data: colecaoVazia(),
      });
      this.mapa!.addLayer({
        id: CAMADA_SETOR_FILL,
        type: 'fill',
        source: FONTE_SETOR,
        paint: {
          'fill-color': '#2563eb',
          'fill-opacity': 0.2,
        },
      });
      this.mapa!.addLayer({
        id: CAMADA_SETOR_LINHA,
        type: 'line',
        source: FONTE_SETOR,
        paint: {
          'line-color': '#1d4ed8',
          'line-width': 3,
        },
      });
      this.mapa!.addSource(FONTE_TRECHOS, {
        type: 'geojson',
        data: paraFeatureCollectionTrechos([]),
      });
      this.mapa!.addLayer({
        id: CAMADA_TRECHOS,
        type: 'line',
        source: FONTE_TRECHOS,
        layout: {
          'line-cap': 'round',
          'line-join': 'round',
        },
        paint: {
          'line-width': 5,
          'line-opacity': 0.9,
          'line-color': [
            'case',
            ['==', ['get', 'verdeCompleto'], true],
            this.statusCores.EXECUTADO,
            [
              'match',
              ['get', 'status'],
              'EXECUTADO',
              this.statusCores.EXECUTADO,
              'SUSPEITO',
              this.statusCores.SUSPEITO,
              'PENDENTE',
              this.statusCores.PENDENTE,
              'FORA_DA_ROTA',
              this.statusCores.FORA_DA_ROTA,
              'JUSTIFICADO',
              this.statusCores.JUSTIFICADO,
              'PLANEJADO',
              this.statusCores.PLANEJADO,
              this.statusCores.PLANEJADO,
            ],
          ],
        },
      });

      this.mapa!.addSource(FONTE_PERCURSO, {
        type: 'geojson',
        data: paraFeatureCollectionPercurso([]),
      });
      this.mapa!.addLayer({
        id: CAMADA_PERCURSO,
        type: 'line',
        source: FONTE_PERCURSO,
        layout: {
          'line-cap': 'round',
          'line-join': 'round',
        },
        paint: {
          'line-width': 5,
          'line-opacity': 0.95,
          'line-color': this.statusCores.EXECUTADO,
        },
      });

      this.mapa!.addSource(FONTE_ATIVOS, {
        type: 'geojson',
        data: paraFeatureCollectionAtivos([]),
      });
      this.mapa!.addLayer({
        id: CAMADA_ATIVOS,
        type: 'circle',
        source: FONTE_ATIVOS,
        paint: {
          'circle-radius': 8,
          'circle-stroke-width': 2,
          'circle-stroke-color': '#ffffff',
          'circle-color': [
            'match',
            ['get', 'situacaoSinal'],
            'ONLINE',
            this.situacaoCores.ONLINE,
            'ATENCAO',
            this.situacaoCores.ATENCAO,
            'OFFLINE',
            this.situacaoCores.OFFLINE,
            this.situacaoCores.OFFLINE,
          ],
        },
      });

      this.mapa!.on('click', CAMADA_ATIVOS, (evento) => {
        const feature = evento.features?.[0];
        if (!feature) {
          return;
        }
        const propriedades = feature.properties as Record<string, string | number>;
        this.emitirRecorte(propriedades);
        this.popup?.remove();
        this.popup = new maplibregl.Popup({ closeButton: true })
          .setLngLat(evento.lngLat)
          .setHTML(
            `<div style="font-size:12px;line-height:1.4">
              <strong>${propriedades['colaboradoresNomes'] || propriedades['equipamentoCodigo']}</strong> · ${propriedades['equipamentoCodigo']}<br/>
              Setor: ${propriedades['setorNome']}<br/>
              Velocidade: ${propriedades['velocidadeKmh'] ?? '—'} km/h<br/>
              Última atualização: ${new Date(String(propriedades['recebidoEm'])).toLocaleTimeString('pt-BR')}
            </div>`,
          )
          .addTo(this.mapa!);
      });
      this.mapa!.on('mouseenter', CAMADA_ATIVOS, () => {
        this.mapa!.getCanvas().style.cursor = 'pointer';
      });
      this.mapa!.on('mouseleave', CAMADA_ATIVOS, () => {
        this.mapa!.getCanvas().style.cursor = '';
      });

      for (const camada of [CAMADA_TRECHOS, CAMADA_PERCURSO]) {
        this.mapa!.on('click', camada, (evento) => {
          const feature = evento.features?.[0];
          if (!feature) {
            return;
          }
          const propriedades = feature.properties as Record<string, string | number>;
          this.emitirRecorte(propriedades);
          this.popup?.remove();
          this.popup = new maplibregl.Popup({ closeButton: true })
            .setLngLat(evento.lngLat)
            .setHTML(
              `<div style="font-size:12px;line-height:1.4">
              <strong>${propriedades['ruaNome'] || propriedades['trechoCodigo']}</strong>${propriedades['ruaNome'] ? ' (' + propriedades['trechoCodigo'] + ')' : ''}<br/>
              ${propriedades['setorNome'] ? propriedades['setorNome'] + '<br/>' : ''}
              Status: ${propriedades['status']} · ${propriedades['percentualExecutado']}%
            </div>`,
            )
            .addTo(this.mapa!);
        });
        this.mapa!.on('mouseenter', camada, () => {
          this.mapa!.getCanvas().style.cursor = 'pointer';
        });
        this.mapa!.on('mouseleave', camada, () => {
          this.mapa!.getCanvas().style.cursor = '';
        });
      }

      this.mapaPronto = true;
      this.atualizarCamadas();
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['dados'] || changes['setorGeom'] || changes['enquadrarTrechos']) && this.mapaPronto) {
      this.atualizarCamadas();
    }
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    this.resizeObserver = null;
    this.popup?.remove();
    this.mapa?.remove();
    this.mapa = null;
  }

  private emitirRecorte(propriedades: Record<string, string | number> | null | undefined): void {
    if (!propriedades) {
      return;
    }
    const setorId = Number(propriedades['setorId']);
    const escalaId = Number(propriedades['escalaId']);
    if (!Number.isFinite(setorId) || !Number.isFinite(escalaId)) {
      return;
    }
    this.recorteSelecionado.emit({ setorId, escalaId });
  }

  private atualizarCamadas(): void {
    if (!this.mapa) {
      return;
    }
    const trechos = this.dados?.trechos ?? [];
    const ativos = this.dados?.ativos ?? [];

    const fonteTrechos = this.mapa.getSource(FONTE_TRECHOS) as GeoJSONSource | undefined;
    fonteTrechos?.setData(paraFeatureCollectionTrechos(trechos));

    const fontePercurso = this.mapa.getSource(FONTE_PERCURSO) as GeoJSONSource | undefined;
    fontePercurso?.setData(paraFeatureCollectionPercurso(trechos));

    const fonteAtivos = this.mapa.getSource(FONTE_ATIVOS) as GeoJSONSource | undefined;
    fonteAtivos?.setData(paraFeatureCollectionAtivos(ativos));

    const area = geometriaSetor(this.setorGeom);
    const pontosArea = area ? pontosDaGeometria(area) : [];
    this.setorDestacado = pontosArea.length > 0;
    const fonteSetor = this.mapa.getSource(FONTE_SETOR) as GeoJSONSource | undefined;
    fonteSetor?.setData(colecaoSetor(area));

    if (this.mapa.getLayer(CAMADA_TRECHOS)) {
      const largura = this.enquadrarTrechos ? 7 : 5;
      this.mapa.setPaintProperty(CAMADA_TRECHOS, 'line-width', largura);
      this.mapa.setPaintProperty(CAMADA_PERCURSO, 'line-width', largura);
      this.mapa.setPaintProperty(CAMADA_ATIVOS, 'circle-radius', this.enquadrarTrechos ? 6 : 8);
    }

    const bounds = calcularLimites(trechos, ativos, pontosArea, this.enquadrarTrechos);
    const assinatura = `${this.enquadrarTrechos ? 'trechos' : 'tudo'}|${assinaturaEnquadramento(trechos, ativos)}|${assinaturaSetor(area, pontosArea)}`;
    if (bounds && assinatura !== this.assinaturaEnquadramento) {
      this.assinaturaEnquadramento = assinatura;
      const padding = this.compacto ? { top: 48, right: 188, bottom: 40, left: 40 } : 60;
      this.mapa.fitBounds(bounds, { padding, maxZoom: 16, duration: 500 });
    }
  }
}

function paraFeatureCollectionTrechos(trechos: MapaTrecho[]): FeatureCollection {
  return {
    type: 'FeatureCollection',
    features: trechos.map((trecho) => ({
      type: 'Feature',
      geometry: trecho.geometry as Geometry,
      properties: {
        trechoExecucaoId: trecho.trechoExecucaoId,
        escalaId: trecho.escalaId,
        trechoCodigo: trecho.trechoCodigo,
        ruaNome: trecho.ruaNome,
        setorId: trecho.setorId,
        setorNome: trecho.setorNome,
        status: trecho.status,
        percentualExecutado: Math.round(trecho.percentualExecutado),
        verdeCompleto: trechoVerdeCompleto(trecho),
      },
    })),
  };
}

function paraFeatureCollectionPercurso(trechos: MapaTrecho[]): FeatureCollection {
  return {
    type: 'FeatureCollection',
    features: trechos.flatMap((trecho) => {
      if (trecho.geometry.type !== 'LineString' || !trechoComProgressoParcial(trecho)) {
        return [];
      }
      const coordenadas = recortarLinhaAte(
        trecho.geometry.coordinates,
        trecho.percentualExecutado / 100,
      );
      if (coordenadas.length < 2) {
        return [];
      }
      return [
        {
          type: 'Feature' as const,
          geometry: { type: 'LineString' as const, coordinates: coordenadas },
          properties: {
            trechoExecucaoId: trecho.trechoExecucaoId,
            escalaId: trecho.escalaId,
            trechoCodigo: trecho.trechoCodigo,
            ruaNome: trecho.ruaNome,
            setorId: trecho.setorId,
            setorNome: trecho.setorNome,
            status: trecho.status,
            percentualExecutado: Math.round(trecho.percentualExecutado),
          },
        },
      ];
    }),
  };
}

function paraFeatureCollectionAtivos(ativos: MapaAtivo[]): FeatureCollection {
  return {
    type: 'FeatureCollection',
    features: ativos.map((ativo) => ({
      type: 'Feature',
      geometry: { type: 'Point', coordinates: [ativo.longitude, ativo.latitude] },
      properties: {
        equipamentoId: ativo.equipamentoId,
        equipamentoCodigo: ativo.equipamentoCodigo,
        colaboradoresNomes: ativo.colaboradoresNomes,
        escalaId: ativo.escalaId,
        setorId: ativo.setorId,
        setorNome: ativo.setorNome,
        velocidadeKmh: ativo.velocidadeKmh,
        recebidoEm: ativo.recebidoEm,
        situacaoSinal: ativo.situacaoSinal,
      },
    })),
  };
}

function colecaoVazia(): FeatureCollection {
  return { type: 'FeatureCollection', features: [] };
}

function geometriaSetor(valor: unknown): GeometriaSetorGeoJson | null {
  if (typeof valor === 'string') {
    return wktParaGeoJson(valor);
  }
  return normalizarGeometriaSetor(valor);
}

function colecaoSetor(geo: GeometriaSetorGeoJson | null): FeatureCollection {
  if (!geo) {
    return colecaoVazia();
  }
  return {
    type: 'FeatureCollection',
    features: [
      {
        type: 'Feature',
        properties: {},
        geometry: geo,
      },
    ],
  };
}

function assinaturaSetor(geo: GeometriaSetorGeoJson | null, pontos: [number, number][]): string {
  if (!geo || pontos.length === 0) {
    return '';
  }
  const primeiro = pontos[0];
  const ultimo = pontos[pontos.length - 1];
  return `${geo.type}:${pontos.length}:${primeiro[0]},${primeiro[1]}:${ultimo[0]},${ultimo[1]}`;
}

function assinaturaEnquadramento(trechos: MapaTrecho[], ativos: MapaAtivo[]): string {
  const trechosIds = trechos.map((trecho) => trecho.trechoExecucaoId).sort((a, b) => a - b);
  const ativosIds = ativos.map((ativo) => ativo.equipamentoId).sort((a, b) => a - b);
  return `${trechosIds.join(',')}|${ativosIds.join(',')}`;
}

function calcularLimites(
  trechos: MapaTrecho[],
  ativos: MapaAtivo[],
  pontosArea: [number, number][] = [],
  priorizarTrechos = false,
): LngLatBoundsLike | null {
  const pontosTrechos = pontosDosTrechos(trechos);
  const pontos: [number, number][] = [];
  if (priorizarTrechos && pontosTrechos.length > 0) {
    pontos.push(...pontosTrechos);
  } else if (priorizarTrechos && pontosArea.length > 0) {
    pontos.push(...pontosArea);
  } else {
    pontos.push(...pontosArea);
    for (const ativo of ativos) {
      pontos.push([ativo.longitude, ativo.latitude]);
    }
    pontos.push(...pontosTrechos);
  }
  if (pontos.length === 0) {
    return null;
  }
  const bounds = new maplibregl.LngLatBounds(pontos[0], pontos[0]);
  for (const ponto of pontos) {
    bounds.extend(ponto);
  }
  return bounds;
}

function pontosDosTrechos(trechos: MapaTrecho[]): [number, number][] {
  const pontos: [number, number][] = [];
  for (const trecho of trechos) {
    if (trecho.geometry.type === 'LineString') {
      pontos.push(...trecho.geometry.coordinates);
    } else {
      pontos.push(trecho.geometry.coordinates);
    }
  }
  return pontos;
}

export const STATUS_TRECHO_LEGENDA: StatusTrecho[] = STATUS_TRECHO_OPCOES;

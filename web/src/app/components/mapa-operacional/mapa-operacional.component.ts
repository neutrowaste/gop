import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import type { FeatureCollection, Geometry } from 'geojson';
import maplibregl, {
  GeoJSONSource,
  LngLatBoundsLike,
  Map as MapLibreMap,
  Popup,
} from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';

import {
  MapaAtivo,
  MapaOperacional,
  MapaTrecho,
  SITUACAO_SINAL_CORES,
  SITUACAO_SINAL_LABELS,
  STATUS_TRECHO_CORES,
} from '../../models/mapa-operacional';
import { STATUS_TRECHO_OPCOES, StatusTrecho } from '../../models/cadastros-operacionais';

const FONTE_TRECHOS = 'gop-trechos';
const FONTE_ATIVOS = 'gop-ativos';
const CAMADA_TRECHOS = 'gop-trechos-linha';
const CAMADA_ATIVOS = 'gop-ativos-ponto';

/**
 * Mapa operacional (RN-001): trechos do plano do dia coloridos por status e
 * posição atual dos ativos GPS. TODO(fase futura): trocar o basemap público
 * (raster OSM) por tiles pg_tileserv/Martin quando a infra existir, e migrar
 * a atualização de polling para WebSocket incremental.
 */
@Component({
  selector: 'app-mapa-operacional',
  standalone: true,
  templateUrl: './mapa-operacional.component.html',
})
export class MapaOperacionalComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() dados: MapaOperacional | null = null;

  @ViewChild('mapaContainer', { static: true }) private mapaContainer!: ElementRef<HTMLDivElement>;

  readonly statusOpcoes = STATUS_TRECHO_OPCOES;
  readonly statusCores = STATUS_TRECHO_CORES;
  readonly situacaoLabels = SITUACAO_SINAL_LABELS;
  readonly situacaoCores = SITUACAO_SINAL_CORES;

  private mapa: MapLibreMap | null = null;
  private mapaPronto = false;
  private popup: Popup | null = null;

  ngAfterViewInit(): void {
    this.mapa = new maplibregl.Map({
      container: this.mapaContainer.nativeElement,
      style: {
        version: 8,
        sources: {
          osm: {
            type: 'raster',
            tiles: ['https://tile.openstreetmap.org/{z}/{x}/{y}.png'],
            tileSize: 256,
            attribution: '© OpenStreetMap contributors',
          },
        },
        layers: [{ id: 'osm', type: 'raster', source: 'osm' }],
      },
      center: [-46.63, -23.55],
      zoom: 12,
    });
    this.mapa.addControl(new maplibregl.NavigationControl(), 'top-right');

    this.mapa.on('load', () => {
      this.mapa!.addSource(FONTE_TRECHOS, {
        type: 'geojson',
        data: paraFeatureCollectionTrechos([]),
      });
      this.mapa!.addLayer({
        id: CAMADA_TRECHOS,
        type: 'line',
        source: FONTE_TRECHOS,
        paint: {
          'line-width': 5,
          'line-opacity': 0.9,
          'line-color': [
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
        this.popup?.remove();
        this.popup = new maplibregl.Popup({ closeButton: true })
          .setLngLat(evento.lngLat)
          .setHTML(
            `<div style="font-size:12px;line-height:1.4">
              <strong>${propriedades['equipeNome']}</strong> · ${propriedades['equipamentoCodigo']}<br/>
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

      this.mapa!.on('click', CAMADA_TRECHOS, (evento) => {
        const feature = evento.features?.[0];
        if (!feature) {
          return;
        }
        const propriedades = feature.properties as Record<string, string | number>;
        this.popup?.remove();
        this.popup = new maplibregl.Popup({ closeButton: true })
          .setLngLat(evento.lngLat)
          .setHTML(
            `<div style="font-size:12px;line-height:1.4">
              <strong>${propriedades['ruaNome']}</strong> (${propriedades['trechoCodigo']})<br/>
              Status: ${propriedades['status']} · ${propriedades['percentualExecutado']}%
            </div>`,
          )
          .addTo(this.mapa!);
      });

      this.mapaPronto = true;
      this.atualizarCamadas();
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['dados'] && this.mapaPronto) {
      this.atualizarCamadas();
    }
  }

  ngOnDestroy(): void {
    this.mapa?.remove();
  }

  private atualizarCamadas(): void {
    if (!this.mapa) {
      return;
    }
    const trechos = this.dados?.trechos ?? [];
    const ativos = this.dados?.ativos ?? [];

    const fonteTrechos = this.mapa.getSource(FONTE_TRECHOS) as GeoJSONSource | undefined;
    fonteTrechos?.setData(paraFeatureCollectionTrechos(trechos));

    const fonteAtivos = this.mapa.getSource(FONTE_ATIVOS) as GeoJSONSource | undefined;
    fonteAtivos?.setData(paraFeatureCollectionAtivos(ativos));

    const bounds = calcularLimites(trechos, ativos);
    if (bounds) {
      this.mapa.fitBounds(bounds, { padding: 60, maxZoom: 16, duration: 500 });
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
        trechoCodigo: trecho.trechoCodigo,
        ruaNome: trecho.ruaNome,
        status: trecho.status,
        percentualExecutado: Math.round(trecho.percentualExecutado),
      },
    })),
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
        equipeNome: ativo.equipeNome,
        setorNome: ativo.setorNome,
        velocidadeKmh: ativo.velocidadeKmh,
        recebidoEm: ativo.recebidoEm,
        situacaoSinal: ativo.situacaoSinal,
      },
    })),
  };
}

function calcularLimites(trechos: MapaTrecho[], ativos: MapaAtivo[]): LngLatBoundsLike | null {
  const pontos: [number, number][] = [];
  for (const ativo of ativos) {
    pontos.push([ativo.longitude, ativo.latitude]);
  }
  for (const trecho of trechos) {
    if (trecho.geometry.type === 'LineString') {
      pontos.push(...trecho.geometry.coordinates);
    } else {
      pontos.push(trecho.geometry.coordinates);
    }
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

export const STATUS_TRECHO_LEGENDA: StatusTrecho[] = STATUS_TRECHO_OPCOES;

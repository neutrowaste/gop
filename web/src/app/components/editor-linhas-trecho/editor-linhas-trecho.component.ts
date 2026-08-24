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
  signal,
} from '@angular/core';
import type { Feature, FeatureCollection, LineString, Point } from 'geojson';
import maplibregl, { GeoJSONSource, Map as MapLibreMap } from 'maplibre-gl';

import { AnelLngLat, linhaParaWkt, wktParaLinha, wktParaLinhas } from '../../services/geometria-wkt';

const FONTE = 'gop-trecho-linhas';
const CAMADA_CONCLUIDA = 'gop-trecho-concluida';
const CAMADA_ATUAL = 'gop-trecho-atual';
const CAMADA_VERTICES = 'gop-trecho-vertices';
const CENTRO_RECIFE: [number, number] = [-34.881, -8.047];

@Component({
  selector: 'app-editor-linhas-trecho',
  standalone: true,
  templateUrl: './editor-linhas-trecho.component.html',
  host: {
    class: 'block h-full min-h-[24rem]',
  },
})
export class EditorLinhasTrechoComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() geomWkt: string | null = null;
  @Input() geomWkts: string[] | null = null;
  @Input() modoMultiplo = true;
  @Output() readonly confirmado = new EventEmitter<string[]>();
  @Output() readonly limpo = new EventEmitter<void>();

  @ViewChild('mapaContainer', { static: true }) private mapaContainer!: ElementRef<HTMLDivElement>;

  readonly bloqueado = signal(false);
  readonly confirmadoLocal = signal(false);
  readonly verticesCount = signal(0);
  readonly linhasCount = signal(0);

  private mapa: MapLibreMap | null = null;
  private resizeObserver: ResizeObserver | null = null;
  private concluidas: AnelLngLat[] = [];
  private vertices: AnelLngLat = [];
  private preview: [number, number] | null = null;
  private mapaPronto = false;

  ngAfterViewInit(): void {
    const container = this.mapaContainer.nativeElement;
    this.mapa = new maplibregl.Map({
      container,
      style: {
        version: 8,
        sources: {
          basemap: {
            type: 'raster',
            tiles: [
              'https://a.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png',
              'https://b.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png',
              'https://c.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png',
            ],
            tileSize: 256,
            attribution: '© OpenStreetMap contributors © CARTO',
          },
        },
        layers: [{ id: 'basemap', type: 'raster', source: 'basemap' }],
      },
      center: CENTRO_RECIFE,
      zoom: 12,
    });
    this.mapa.doubleClickZoom.disable();
    this.mapa.addControl(new maplibregl.NavigationControl({ showCompass: false }), 'top-right');

    this.resizeObserver = new ResizeObserver(() => this.mapa?.resize());
    this.resizeObserver.observe(container);

    this.mapa.on('load', () => {
      this.mapaPronto = true;
      this.mapa!.resize();
      this.mapa!.addSource(FONTE, { type: 'geojson', data: this.colecaoVazia() });
      this.mapa!.addLayer({
        id: CAMADA_CONCLUIDA,
        type: 'line',
        source: FONTE,
        filter: ['==', ['get', 'papel'], 'concluida'],
        paint: {
          'line-color': '#1e4a7a',
          'line-width': 3,
        },
      });
      this.mapa!.addLayer({
        id: CAMADA_ATUAL,
        type: 'line',
        source: FONTE,
        filter: ['==', ['get', 'papel'], 'atual'],
        paint: {
          'line-color': '#c45c26',
          'line-width': 2,
          'line-dasharray': [2, 1.4],
        },
      });
      this.mapa!.addLayer({
        id: CAMADA_VERTICES,
        type: 'circle',
        source: FONTE,
        filter: ['==', ['geometry-type'], 'Point'],
        paint: {
          'circle-radius': ['case', ['==', ['get', 'papel'], 'vertice-concluida'], 3.5, 5],
          'circle-color': '#ffffff',
          'circle-stroke-width': 2,
          'circle-stroke-color': ['case', ['==', ['get', 'papel'], 'vertice-concluida'], '#1e4a7a', '#c45c26'],
        },
      });
      this.carregarGeometria();
    });

    this.mapa.on('click', (evento) => {
      if (this.bloqueado()) {
        return;
      }
      this.vertices.push([evento.lngLat.lng, evento.lngLat.lat]);
      this.verticesCount.set(this.vertices.length);
      this.confirmadoLocal.set(false);
      this.atualizarContagem();
      this.renderizar();
    });

    this.mapa.on('dblclick', (evento) => {
      evento.preventDefault();
      this.concluirLinha();
    });

    this.mapa.on('mousemove', (evento) => {
      if (this.bloqueado() || this.vertices.length === 0) {
        return;
      }
      this.preview = [evento.lngLat.lng, evento.lngLat.lat];
      this.renderizar();
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['geomWkt'] || changes['geomWkts']) && this.mapaPronto) {
      this.carregarGeometria();
    }
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    this.mapa?.remove();
    this.mapa = null;
  }

  concluirLinha(): void {
    this.removerVerticeDuplicado();
    if (this.vertices.length < 2) {
      return;
    }
    this.concluidas.push([...this.vertices]);
    this.vertices = [];
    this.preview = null;
    this.verticesCount.set(0);
    this.confirmadoLocal.set(false);
    if (!this.modoMultiplo) {
      this.bloqueado.set(true);
    }
    this.atualizarContagem();
    this.renderizar();
    this.encaixar(this.pontosVisiveis());
  }

  confirmar(): void {
    if (this.vertices.length >= 2) {
      this.concluirLinha();
    }
    const wkts = this.concluidas
      .map((linha) => linhaParaWkt(linha))
      .filter((wkt): wkt is string => wkt != null);
    if (wkts.length === 0) {
      return;
    }
    this.confirmadoLocal.set(true);
    this.bloqueado.set(!this.modoMultiplo);
    this.confirmado.emit(wkts);
  }

  desfazer(): void {
    this.confirmadoLocal.set(false);
    if (this.vertices.length > 0) {
      this.vertices.pop();
      this.preview = this.vertices.length === 0 ? null : this.preview;
      this.verticesCount.set(this.vertices.length);
      this.atualizarContagem();
      this.renderizar();
      return;
    }
    if (this.concluidas.length === 0) {
      return;
    }
    this.vertices = this.concluidas.pop() ?? [];
    this.bloqueado.set(false);
    this.verticesCount.set(this.vertices.length);
    this.atualizarContagem();
    this.renderizar();
  }

  limpar(): void {
    this.concluidas = [];
    this.vertices = [];
    this.preview = null;
    this.bloqueado.set(false);
    this.confirmadoLocal.set(false);
    this.verticesCount.set(0);
    this.atualizarContagem();
    this.renderizar();
    this.limpo.emit();
  }

  redesenhar(): void {
    this.bloqueado.set(false);
    this.confirmadoLocal.set(false);
    this.renderizar();
  }

  podeDesfazer(): boolean {
    return this.vertices.length > 0 || this.concluidas.length > 0;
  }

  podeLimpar(): boolean {
    return this.vertices.length > 0 || this.concluidas.length > 0;
  }

  private carregarGeometria(): void {
    const linhas = this.linhasDeEntrada();
    this.preview = null;
    this.vertices = [];
    this.verticesCount.set(0);
    this.concluidas = linhas;
    this.bloqueado.set(!this.modoMultiplo && linhas.length > 0);
    this.confirmadoLocal.set(linhas.length > 0);
    this.atualizarContagem();
    this.renderizar();
    if (linhas.length > 0) {
      this.encaixar(this.pontosVisiveis());
    }
  }

  private linhasDeEntrada(): AnelLngLat[] {
    if (this.geomWkts && this.geomWkts.length > 0) {
      return this.geomWkts.flatMap((wkt) => wktParaLinhas(wkt));
    }
    const unica = wktParaLinha(this.geomWkt);
    return unica ? [unica] : wktParaLinhas(this.geomWkt);
  }

  private removerVerticeDuplicado(): void {
    if (this.vertices.length < 2) {
      return;
    }
    const atual = this.vertices[this.vertices.length - 1];
    const anterior = this.vertices[this.vertices.length - 2];
    if (Math.hypot(atual[0] - anterior[0], atual[1] - anterior[1]) < 1e-8) {
      this.vertices.pop();
    }
  }

  private atualizarContagem(): void {
    this.linhasCount.set(this.concluidas.length + (this.vertices.length >= 2 ? 1 : 0));
  }

  private pontosVisiveis(): AnelLngLat {
    return [...this.concluidas.flat(), ...this.vertices];
  }

  private encaixar(pontos: AnelLngLat): void {
    if (!this.mapa || pontos.length === 0) {
      return;
    }
    const bounds = new maplibregl.LngLatBounds(pontos[0], pontos[0]);
    for (const ponto of pontos) {
      bounds.extend(ponto);
    }
    this.mapa.resize();
    this.mapa.fitBounds(bounds, { padding: 48, maxZoom: 16, duration: 400 });
  }

  private renderizar(): void {
    const fonte = this.mapa?.getSource(FONTE) as GeoJSONSource | undefined;
    fonte?.setData(this.colecaoAtual());
  }

  private colecaoAtual(): FeatureCollection {
    const features: Feature[] = [];

    this.concluidas.forEach((linha, indice) => {
      features.push(this.featureLinha(linha, 'concluida', indice));
      for (const ponto of linha) {
        features.push(this.featurePonto(ponto, 'vertice-concluida'));
      }
    });

    if (this.vertices.length >= 1) {
      const coords = this.preview ? [...this.vertices, this.preview] : this.vertices;
      features.push(this.featureLinha(coords, 'atual', this.concluidas.length));
      for (const ponto of this.vertices) {
        features.push(this.featurePonto(ponto, 'vertice-atual'));
      }
    }

    return { type: 'FeatureCollection', features };
  }

  private featureLinha(coordinates: AnelLngLat, papel: string, indice: number): Feature<LineString> {
    return {
      type: 'Feature',
      properties: { papel, indice },
      geometry: { type: 'LineString', coordinates },
    };
  }

  private featurePonto(coordinates: [number, number], papel: string): Feature<Point> {
    return {
      type: 'Feature',
      properties: { papel },
      geometry: { type: 'Point', coordinates },
    };
  }

  private colecaoVazia(): FeatureCollection {
    return { type: 'FeatureCollection', features: [] };
  }
}

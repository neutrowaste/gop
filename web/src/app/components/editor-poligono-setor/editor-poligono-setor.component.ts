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
import type { Feature, FeatureCollection, LineString, Point, Polygon } from 'geojson';
import maplibregl, { GeoJSONSource, Map as MapLibreMap } from 'maplibre-gl';

import {
  AnelLngLat,
  GeometriaSetorGeoJson,
  anelFechado,
  normalizarGeometriaSetor,
  poligonoParaWkt,
  pontosDaGeometria,
  wktParaGeoJson,
} from '../../services/geometria-wkt';
import { ESTILO_BASEMAP } from '../../services/basemap';

const FONTE = 'gop-setor-demarcacao';
const CAMADA_PREENCHIMENTO = 'gop-setor-fill';
const CAMADA_LINHA = 'gop-setor-linha';
const CAMADA_VERTICES = 'gop-setor-vertices';
const CENTRO_RECIFE: [number, number] = [-34.881, -8.047];
const MAX_VERTICES_EDICAO = 250;

@Component({
  selector: 'app-editor-poligono-setor',
  standalone: true,
  templateUrl: './editor-poligono-setor.component.html',
  host: {
    class: 'block h-full min-h-[24rem]',
  },
})
export class EditorPoligonoSetorComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() geomWkt: string | null = null;
  @Input() geomGeoJson: unknown = null;
  @Output() readonly confirmado = new EventEmitter<string>();
  @Output() readonly limpo = new EventEmitter<void>();

  @ViewChild('mapaContainer', { static: true }) private mapaContainer!: ElementRef<HTMLDivElement>;

  readonly fechado = signal(false);
  readonly confirmadoLocal = signal(false);
  readonly verticesCount = signal(0);
  readonly origemShapefile = signal(false);

  private mapa: MapLibreMap | null = null;
  private resizeObserver: ResizeObserver | null = null;
  private vertices: AnelLngLat = [];
  private preview: [number, number] | null = null;
  private geometriaExibida: GeometriaSetorGeoJson | null = null;
  private mapaPronto = false;

  ngAfterViewInit(): void {
    const container = this.mapaContainer.nativeElement;
    this.mapa = new maplibregl.Map({
      container,
      style: ESTILO_BASEMAP,
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
        id: CAMADA_PREENCHIMENTO,
        type: 'fill',
        source: FONTE,
        filter: ['==', ['geometry-type'], 'Polygon'],
        paint: {
          'fill-color': '#1e4a7a',
          'fill-opacity': 0.28,
        },
      });
      this.mapa!.addLayer({
        id: CAMADA_LINHA,
        type: 'line',
        source: FONTE,
        filter: ['!=', ['geometry-type'], 'Point'],
        paint: {
          'line-color': '#1e4a7a',
          'line-width': 2,
        },
      });
      this.mapa!.addLayer({
        id: CAMADA_VERTICES,
        type: 'circle',
        source: FONTE,
        filter: ['==', ['geometry-type'], 'Point'],
        paint: {
          'circle-radius': 5,
          'circle-color': '#ffffff',
          'circle-stroke-width': 2,
          'circle-stroke-color': '#1e4a7a',
        },
      });
      this.carregarGeometria();
    });

    this.mapa.on('click', (evento) => {
      if (this.fechado()) {
        return;
      }
      this.geometriaExibida = null;
      this.origemShapefile.set(false);
      this.vertices.push([evento.lngLat.lng, evento.lngLat.lat]);
      this.verticesCount.set(this.vertices.length);
      this.confirmadoLocal.set(false);
      this.renderizar();
    });

    this.mapa.on('dblclick', (evento) => {
      evento.preventDefault();
      this.fecharPoligono();
    });

    this.mapa.on('mousemove', (evento) => {
      if (this.fechado() || this.vertices.length === 0) {
        return;
      }
      this.preview = [evento.lngLat.lng, evento.lngLat.lat];
      this.renderizar();
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['geomWkt'] || changes['geomGeoJson']) && this.mapaPronto) {
      this.carregarGeometria();
    }
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    this.mapa?.remove();
    this.mapa = null;
  }

  fecharPoligono(): void {
    this.removerVerticeDuplicado();
    if (this.vertices.length < 3) {
      return;
    }
    this.fechado.set(true);
    this.preview = null;
    this.verticesCount.set(this.vertices.length);
    this.renderizar();
    this.encaixar(this.vertices);
  }

  confirmar(): void {
    if (!this.fechado() && this.vertices.length >= 3) {
      this.fecharPoligono();
    }
    const wkt = poligonoParaWkt(this.vertices);
    if (!wkt) {
      return;
    }
    this.confirmadoLocal.set(true);
    this.confirmado.emit(wkt);
  }

  limpar(): void {
    this.vertices = [];
    this.preview = null;
    this.geometriaExibida = null;
    this.fechado.set(false);
    this.confirmadoLocal.set(false);
    this.origemShapefile.set(false);
    this.verticesCount.set(0);
    this.renderizar();
    this.limpo.emit();
  }

  redesenhar(): void {
    this.geometriaExibida = null;
    this.origemShapefile.set(false);
    this.fechado.set(false);
    this.confirmadoLocal.set(false);
    this.renderizar();
  }

  private carregarGeometria(): void {
    const geo =
      normalizarGeometriaSetor(this.geomGeoJson) ?? wktParaGeoJson(this.geomWkt);
    if (!geo) {
      this.vertices = [];
      this.preview = null;
      this.geometriaExibida = null;
      this.fechado.set(false);
      this.confirmadoLocal.set(false);
      this.origemShapefile.set(false);
      this.verticesCount.set(0);
      this.renderizar();
      return;
    }

    const anelSimples = this.anelEditavel(geo);
    this.preview = null;
    this.fechado.set(true);
    this.confirmadoLocal.set(true);
    if (anelSimples) {
      this.vertices = anelSimples;
      this.geometriaExibida = null;
      this.origemShapefile.set(false);
      this.verticesCount.set(anelSimples.length);
      this.renderizar();
      this.encaixar(anelSimples);
      return;
    }

    this.vertices = [];
    this.geometriaExibida = geo;
    this.origemShapefile.set(true);
    this.verticesCount.set(0);
    this.renderizar();
    this.encaixar(pontosDaGeometria(geo));
  }

  private anelEditavel(geo: GeometriaSetorGeoJson): AnelLngLat | null {
    const anel =
      geo.type === 'Polygon'
        ? geo.coordinates.length === 1
          ? geo.coordinates[0]
          : null
        : geo.coordinates.length === 1 && geo.coordinates[0].length === 1
          ? geo.coordinates[0][0]
          : null;
    if (!anel || anel.length > MAX_VERTICES_EDICAO + 1) {
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
    if (this.geometriaExibida) {
      return {
        type: 'FeatureCollection',
        features: this.featuresDaGeometria(this.geometriaExibida),
      };
    }

    const features: Feature[] = this.vertices.map(
      (ponto, indice): Feature<Point> => ({
        type: 'Feature',
        properties: { indice },
        geometry: { type: 'Point', coordinates: ponto },
      }),
    );

    if (this.fechado() && this.vertices.length >= 3) {
      const poligono: Feature<Polygon> = {
        type: 'Feature',
        properties: {},
        geometry: { type: 'Polygon', coordinates: [anelFechado(this.vertices)] },
      };
      features.push(poligono);
    } else if (this.vertices.length >= 1) {
      const linha: Feature<LineString> = {
        type: 'Feature',
        properties: {},
        geometry: {
          type: 'LineString',
          coordinates: this.preview ? [...this.vertices, this.preview] : this.vertices,
        },
      };
      features.push(linha);
    }

    return { type: 'FeatureCollection', features };
  }

  private featuresDaGeometria(geo: GeometriaSetorGeoJson): Feature<Polygon>[] {
    if (geo.type === 'Polygon') {
      return [
        {
          type: 'Feature',
          properties: {},
          geometry: geo,
        },
      ];
    }
    return geo.coordinates.map((coordinates) => ({
      type: 'Feature',
      properties: {},
      geometry: { type: 'Polygon', coordinates },
    }));
  }

  private colecaoVazia(): FeatureCollection {
    return { type: 'FeatureCollection', features: [] };
  }
}

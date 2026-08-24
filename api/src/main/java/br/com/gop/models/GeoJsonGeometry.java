package br.com.gop.models;

/**
 * Representação mínima de uma geometria GeoJSON (RFC 7946) para o frontend (MapLibre GL).
 * {@code coordinates} é {@code double[]} (Point), {@code double[][]} (LineString),
 * {@code double[][][]} (Polygon) ou {@code double[][][][]} (MultiPolygon).
 */
public record GeoJsonGeometry(String type, Object coordinates) {
}

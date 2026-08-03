package br.com.gop.models;

/**
 * Representação mínima de uma geometria GeoJSON (RFC 7946) usada apenas para
 * expor Point/LineString do PostGIS ao frontend (MapLibre GL). {@code coordinates}
 * é {@code double[]} para Point ou {@code double[][]} para LineString.
 */
public record GeoJsonGeometry(String type, Object coordinates) {
}

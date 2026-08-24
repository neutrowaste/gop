package br.com.gop.integration.rassystem;

import br.com.gop.models.FonteGps;
import br.com.gop.models.TelemetriaGps;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RasSystemGeoJsonParserTests {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void parseUltimasCoordenadasComNumeroECamposExtras() throws Exception {
		JsonNode json = mapper.readTree("""
			{
			  "type": "FeatureCollection",
			  "features": [
			    {
			      "type": "Feature",
			      "properties": {
			        "veiculo": "LUT-029",
			        "rd": 13579010,
			        "situacao": "DESLOCANDO",
			        "data": "2026-08-18 10:10:57-03",
			        "tipoServico": 16,
			        "idVeiculo": 37280,
			        "rotulos": [8018, 7880],
			        "modelo": 23
			      },
			      "geometry": { "type": "Point", "coordinates": [-34.931264, -8.064821] }
			    }
			  ]
			}
			""");

		List<TelemetriaGps> pontos = RasSystemGeoJsonParser.parseColecao(json);
		assertThat(pontos).hasSize(1);
		TelemetriaGps ponto = pontos.get(0);
		assertThat(ponto.fonte()).isEqualTo(FonteGps.RASSYSTEM);
		assertThat(ponto.idExterno()).isEqualTo(37280L);
		assertThat(ponto.codigoLogico()).isEqualTo("LUT-029");
		assertThat(ponto.rotuloVeiculo()).isEqualTo("LUT-029");
		assertThat(ponto.longitude()).isEqualTo(-34.931264);
		assertThat(ponto.latitude()).isEqualTo(-8.064821);
		assertThat(ponto.rd()).isEqualTo(13579010L);
		assertThat(ponto.situacao()).isEqualTo("DESLOCANDO");
		assertThat(ponto.tipoServico()).isEqualTo(16);
		assertThat(ponto.modelo()).isEqualTo(23);
		assertThat(ponto.rotulos()).containsExactly(8018, 7880);
		assertThat(ponto.velocidadeKmh()).isNull();
	}

	@Test
	void parseRastreioComNumerosEmString() throws Exception {
		JsonNode json = mapper.readTree("""
			{
			  "type": "FeatureCollection",
			  "features": [
			    {
			      "type": "Feature",
			      "properties": {
			        "altitude": "0",
			        "rd": "13579010",
			        "data": "2026-08-18T06:34:03.000-03:00",
			        "velocidade": "8",
			        "odometro": "2839464",
			        "direcao": "41",
			        "satelites": "5",
			        "idVeiculo": 37280
			      },
			      "geometry": { "type": "Point", "coordinates": [-34.941358, -8.042847] }
			    }
			  ]
			}
			""");

		TelemetriaGps ponto = RasSystemGeoJsonParser.parseColecao(json).get(0);
		assertThat(ponto.velocidadeKmh()).isEqualTo(8f);
		assertThat(ponto.direcaoGraus()).isEqualTo(41f);
		assertThat(ponto.altitudeMetros()).isEqualTo(0f);
		assertThat(ponto.odometro()).isEqualTo(2839464L);
		assertThat(ponto.satelites()).isEqualTo(5);
		assertThat(ponto.rd()).isEqualTo(13579010L);
	}

	@Test
	void descartaFeatureSemIdVeiculo() throws Exception {
		JsonNode json = mapper.readTree("""
			{
			  "type": "FeatureCollection",
			  "features": [
			    {
			      "type": "Feature",
			      "properties": { "veiculo": "LUT-001" },
			      "geometry": { "type": "Point", "coordinates": [-34.9, -8.0] }
			    }
			  ]
			}
			""");
		assertThat(RasSystemGeoJsonParser.parseColecao(json)).isEmpty();
	}

	@Test
	void parseEventosExigeTipo() throws Exception {
		JsonNode json = mapper.readTree("""
			{
			  "type": "FeatureCollection",
			  "features": [
			    {
			      "type": "Feature",
			      "properties": {
			        "idVeiculo": 1,
			        "tipo": "IGNICAO",
			        "valor": "ON",
			        "data": "2026-08-18T06:25:03.000-03:00"
			      },
			      "geometry": { "type": "Point", "coordinates": [-34.9, -8.0] }
			    },
			    {
			      "type": "Feature",
			      "properties": { "idVeiculo": 1, "data": "2026-08-18T06:26:03.000-03:00" },
			      "geometry": { "type": "Point", "coordinates": [-34.9, -8.0] }
			    }
			  ]
			}
			""");
		List<TelemetriaGps> eventos = RasSystemGeoJsonParser.parseEventos(json);
		assertThat(eventos).hasSize(1);
		assertThat(eventos.get(0).tipoEvento()).isEqualTo("IGNICAO");
		assertThat(eventos.get(0).valorEvento()).isEqualTo("ON");
	}
}

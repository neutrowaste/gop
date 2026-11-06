package br.com.gop.integration.gt06;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class Gt06ProtocoloTests {

	@Test
	void crc16x25DoVetorPadrao() {
		byte[] dados = "123456789".getBytes(StandardCharsets.US_ASCII);
		assertThat(Gt06Protocolo.crc(dados, 0, dados.length)).isEqualTo(0x906E);
	}

	@Test
	void loginExtraiImeiEConfirmaQuadro() {
		byte[] info = new byte[] { 0x08, 0x67, 0x12, 0x34, 0x56, 0x78, (byte) 0x90, 0x12 };
		byte[] pacote = quadro(0x01, info, 1);
		Gt06Fluxo fluxo = new Gt06Fluxo();
		fluxo.acrescentar(pacote, 0, 4);
		assertThat(fluxo.drenar()).isEmpty();
		fluxo.acrescentar(pacote, 4, pacote.length - 4);

		var quadros = fluxo.drenar();
		assertThat(quadros).hasSize(1);
		assertThat(Gt06Protocolo.imei(quadros.get(0))).contains("867123456789012");
		assertThat(quadros.get(0).serial()).isEqualTo(1);

		byte[] ack = Gt06Protocolo.ack(0x01, 1);
		Gt06Fluxo retorno = new Gt06Fluxo();
		retorno.acrescentar(ack, 0, ack.length);
		var confirmacao = retorno.drenar();
		assertThat(confirmacao).hasSize(1);
		assertThat(confirmacao.get(0).protocolo()).isEqualTo(0x01);
		assertThat(confirmacao.get(0).serial()).isEqualTo(1);
	}

	@Test
	void localizacaoNoHemisferioSul() {
		byte[] info = new byte[] {
			0x1A, 0x0B, 0x03, 0x0E, 0x2D, 0x00,
			(byte) 0xC8,
			0x00, (byte) 0xDD, 0x19, (byte) 0x90,
			0x03, (byte) 0xBE, 0x02, 0x00,
			36,
			0x18, (byte) 0xB4
		};
		Gt06Fluxo fluxo = new Gt06Fluxo();
		byte[] pacote = quadro(0x12, info, 7);
		fluxo.acrescentar(pacote, 0, pacote.length);

		var quadros = fluxo.drenar();
		assertThat(quadros).hasSize(1);
		Gt06Posicao posicao = Gt06Protocolo.posicao(quadros.get(0)).orElseThrow();
		assertThat(posicao.latitude()).isCloseTo(-8.05, within(1e-9));
		assertThat(posicao.longitude()).isCloseTo(-34.88, within(1e-9));
		assertThat(posicao.velocidadeKmh()).isEqualTo(36f);
		assertThat(posicao.direcaoGraus()).isEqualTo(180f);
		assertThat(posicao.satelites()).isEqualTo(8);
		assertThat(posicao.valido()).isTrue();
		assertThat(posicao.capturadoEm()).isEqualTo(Instant.parse("2026-11-03T14:45:00Z"));
	}

	@Test
	void coordenadaRealEntraMesmoSemBitDePosicionado() {
		byte[] info = new byte[] {
			0x1A, 0x0B, 0x03, 0x0E, 0x2D, 0x00,
			(byte) 0xC8,
			0x00, (byte) 0xDD, 0x19, (byte) 0x90,
			0x03, (byte) 0xBE, 0x02, 0x00,
			0,
			0x08, (byte) 0xB4
		};
		Gt06Fluxo fluxo = new Gt06Fluxo();
		byte[] pacote = quadro(0x22, info, 3);
		fluxo.acrescentar(pacote, 0, pacote.length);

		Gt06Posicao posicao = Gt06Protocolo.posicao(fluxo.drenar().get(0)).orElseThrow();
		assertThat(posicao.valido()).isTrue();
		assertThat(posicao.latitude()).isCloseTo(-8.05, within(1e-9));
		assertThat(posicao.longitude()).isCloseTo(-34.88, within(1e-9));
	}

	@Test
	void quadroComCrcInvalidoEhRejeitado() {
		byte[] pacote = quadro(0x13, new byte[] { 0x01, 0x00, 0x00 }, 2);
		pacote[5] ^= 0xFF;
		Gt06Fluxo fluxo = new Gt06Fluxo();
		fluxo.acrescentar(pacote, 0, pacote.length);
		assertThat(fluxo.drenar()).isEmpty();
		assertThat(fluxo.rejeitados()).isNotEmpty();
	}

	private static byte[] quadro(int protocolo, byte[] info, int serial) {
		int corpo = 1 + info.length + 4;
		byte[] pacote = new byte[2 + 1 + corpo + 2];
		pacote[0] = 0x78;
		pacote[1] = 0x78;
		pacote[2] = (byte) corpo;
		pacote[3] = (byte) protocolo;
		System.arraycopy(info, 0, pacote, 4, info.length);
		int serialEm = 4 + info.length;
		pacote[serialEm] = (byte) (serial >>> 8);
		pacote[serialEm + 1] = (byte) serial;
		int crc = Gt06Protocolo.crc(pacote, 2, 1 + 1 + info.length + 2);
		pacote[serialEm + 2] = (byte) (crc >>> 8);
		pacote[serialEm + 3] = (byte) crc;
		pacote[serialEm + 4] = 0x0D;
		pacote[serialEm + 5] = 0x0A;
		return pacote;
	}
}

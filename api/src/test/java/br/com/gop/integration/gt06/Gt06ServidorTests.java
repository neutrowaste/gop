package br.com.gop.integration.gt06;

import br.com.gop.services.Gt06IngestaoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class Gt06ServidorTests {

	@Mock
	private Gt06IngestaoService ingestaoService;

	@Test
	void loginELocalizacaoChegamNaIngestao() throws Exception {
		Gt06Properties properties = new Gt06Properties();
		properties.setHost("127.0.0.1");
		properties.setPort(0);
		Gt06Servidor servidor = new Gt06Servidor(properties, ingestaoService);
		servidor.start();
		try {
			aguardarPorta(servidor);
			try (Socket socket = new Socket("127.0.0.1", servidor.portaLocal())) {
				socket.setSoTimeout(3_000);
				OutputStream saida = socket.getOutputStream();
				saida.write(quadro(0x01, new byte[] { 0x08, 0x67, 0x12, 0x34, 0x56, 0x78, (byte) 0x90, 0x12 }, 1));
				saida.write(quadro(0x22, gpsRecife(), 2));
				saida.flush();
				InputStream entrada = socket.getInputStream();
				assertLeu(entrada, 20);
			}
			verify(ingestaoService, timeout(3_000)).ingerir(
				eq("867123456789012"),
				argThat(posicao -> posicao.valido()
					&& Math.abs(posicao.latitude() + 8.05) < 1e-6
					&& Math.abs(posicao.longitude() + 34.88) < 1e-6
					&& posicao.capturadoEm().equals(Instant.parse("2026-11-03T14:45:00Z")))
			);
		} finally {
			servidor.stop();
		}
	}

	private static void aguardarPorta(Gt06Servidor servidor) throws InterruptedException {
		long limite = System.currentTimeMillis() + 3_000;
		while (servidor.portaLocal() <= 0) {
			if (System.currentTimeMillis() > limite) {
				throw new AssertionError("Receptor GT06 não abriu a porta");
			}
			Thread.sleep(20);
		}
	}

	private static void assertLeu(InputStream entrada, int quantidade) throws Exception {
		int lidos = 0;
		while (lidos < quantidade) {
			int byteLido = entrada.read();
			if (byteLido < 0) {
				break;
			}
			lidos++;
		}
		if (lidos != quantidade) {
			throw new AssertionError("ACK incompleto: " + lidos);
		}
	}

	private static byte[] gpsRecife() {
		return new byte[] {
			0x1A, 0x0B, 0x03, 0x0E, 0x2D, 0x00,
			(byte) 0xC8,
			0x00, (byte) 0xDD, 0x19, (byte) 0x90,
			0x03, (byte) 0xBE, 0x02, 0x00,
			36,
			0x18, (byte) 0xB4
		};
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

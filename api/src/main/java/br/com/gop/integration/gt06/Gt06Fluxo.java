package br.com.gop.integration.gt06;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Remonta quadros GT06 a partir de leituras TCP parciais.
 */
public final class Gt06Fluxo {

	private static final int LIMITE = 65_536;

	private byte[] dados = new byte[256];
	private int tamanho;
	private final List<byte[]> rejeitados = new ArrayList<>();

	public void acrescentar(byte[] bloco, int offset, int comprimento) {
		garantir(tamanho + comprimento);
		System.arraycopy(bloco, offset, dados, tamanho, comprimento);
		tamanho += comprimento;
		if (tamanho > LIMITE) {
			int manter = 1024;
			System.arraycopy(dados, tamanho - manter, dados, 0, manter);
			tamanho = manter;
		}
	}

	public List<Gt06Quadro> drenar() {
		List<Gt06Quadro> quadros = new ArrayList<>();
		int cursor = 0;
		while (true) {
			int inicio = acharInicio(cursor);
			if (inicio < 0) {
				cursor = tamanho > 0 && podeSerInicio(dados[tamanho - 1]) ? tamanho - 1 : tamanho;
				break;
			}
			boolean longo = (dados[inicio] & 0xFF) == 0x79;
			int cabecalho = longo ? 4 : 3;
			if (tamanho - inicio < cabecalho) {
				cursor = inicio;
				break;
			}
			int corpo = longo
				? ((dados[inicio + 2] & 0xFF) << 8) | (dados[inicio + 3] & 0xFF)
				: (dados[inicio + 2] & 0xFF);
			int total = 2 + (longo ? 2 : 1) + corpo + 2;
			if (corpo < 5 || total > 2048) {
				rejeitar(inicio, Math.min(tamanho, inicio + 2));
				cursor = inicio + 2;
				continue;
			}
			if (tamanho - inicio < total) {
				cursor = inicio;
				break;
			}
			int fim = inicio + total;
			if ((dados[fim - 2] & 0xFF) != 0x0D || (dados[fim - 1] & 0xFF) != 0x0A) {
				rejeitar(inicio, fim);
				cursor = inicio + 2;
				continue;
			}
			int crcTamanho = (longo ? 2 : 1) + corpo - 2;
			int calculado = Gt06Protocolo.crc(dados, inicio + 2, crcTamanho);
			int lido = ((dados[fim - 4] & 0xFF) << 8) | (dados[fim - 3] & 0xFF);
			if (calculado != lido) {
				rejeitar(inicio, fim);
				cursor = inicio + 2;
				continue;
			}
			int indiceProtocolo = inicio + (longo ? 4 : 3);
			int infoInicio = indiceProtocolo + 1;
			int infoFim = fim - 6;
			if (infoFim < infoInicio) {
				rejeitar(inicio, fim);
				cursor = inicio + 2;
				continue;
			}
			int serial = ((dados[infoFim] & 0xFF) << 8) | (dados[infoFim + 1] & 0xFF);
			quadros.add(new Gt06Quadro(
				dados[indiceProtocolo] & 0xFF,
				serial,
				Arrays.copyOfRange(dados, infoInicio, infoFim)
			));
			cursor = fim;
		}
		compactar(cursor);
		return quadros;
	}

	public List<byte[]> rejeitados() {
		return rejeitados;
	}

	public void limparRejeitados() {
		rejeitados.clear();
	}

	private void rejeitar(int de, int ate) {
		if (ate > de) {
			rejeitados.add(Arrays.copyOfRange(dados, de, ate));
		}
	}

	private int acharInicio(int de) {
		for (int i = de; i < tamanho - 1; i++) {
			int primeiro = dados[i] & 0xFF;
			int segundo = dados[i + 1] & 0xFF;
			if ((primeiro == 0x78 && segundo == 0x78) || (primeiro == 0x79 && segundo == 0x79)) {
				return i;
			}
		}
		return -1;
	}

	private static boolean podeSerInicio(byte valor) {
		int byteValor = valor & 0xFF;
		return byteValor == 0x78 || byteValor == 0x79;
	}

	private void compactar(int cursor) {
		if (cursor <= 0) {
			return;
		}
		int resto = tamanho - cursor;
		if (resto > 0) {
			System.arraycopy(dados, cursor, dados, 0, resto);
		}
		tamanho = Math.max(resto, 0);
	}

	private void garantir(int capacidade) {
		if (capacidade <= dados.length) {
			return;
		}
		int novoTamanho = dados.length;
		while (novoTamanho < capacidade) {
			novoTamanho *= 2;
		}
		dados = Arrays.copyOf(dados, novoTamanho);
	}
}

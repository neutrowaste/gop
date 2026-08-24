package br.com.gop.integration.rassystem;

import br.com.gop.models.Equipamento;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RasSystemLutocarMatcherTests {

	@Test
	void extraiCodigoLut() {
		assertThat(RasSystemLutocarMatcher.codigoLogico("LUT-0313")).isEqualTo("LUT-0313");
		assertThat(RasSystemLutocarMatcher.codigoLogico("LUT-050")).isEqualTo("LUT-050");
		assertThat(RasSystemLutocarMatcher.codigoLogico("frota LUT-004 varrição")).isEqualTo("LUT-004");
		assertThat(RasSystemLutocarMatcher.codigoLogico("RZR7C60")).isNull();
	}

	@Test
	void vinculaPorCodigoExterno() {
		Equipamento equipamento = equipamento("LUT-029", "37280");
		assertThat(RasSystemLutocarMatcher.vincular(List.of(equipamento), 37280L, "LUT-029"))
			.contains(equipamento);
	}

	@Test
	void vinculaLutComPaddingDiferente() {
		Equipamento equipamento = equipamento("LUT-0004", null);
		assertThat(RasSystemLutocarMatcher.vincular(List.of(equipamento), 99L, "LUT-004"))
			.contains(equipamento);
	}

	@Test
	void naoVinculaQuandoNaoHaMatch() {
		Equipamento equipamento = equipamento("LUT-0001", null);
		assertThat(RasSystemLutocarMatcher.vincular(List.of(equipamento), 37280L, "LUT-029"))
			.isEmpty();
	}

	private static Equipamento equipamento(String codigo, String codigoExterno) {
		Equipamento equipamento = new Equipamento();
		equipamento.setCodigo(codigo);
		equipamento.setCodigoExterno(codigoExterno);
		return equipamento;
	}
}

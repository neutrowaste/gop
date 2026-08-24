package br.com.gop.integration.rassystem;

import br.com.gop.models.Equipamento;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Vincula lutocar RasSystem a {@link Equipamento} pelo código LUT ou
 * {@code codigoExterno} (idVeiculo).
 */
public final class RasSystemLutocarMatcher {

	private static final Pattern LUT = Pattern.compile("(?i)\\bLUT[-_\\s]?(\\d+)\\b");

	private RasSystemLutocarMatcher() {
	}

	public static String codigoLogico(String... rotulos) {
		if (rotulos == null) {
			return null;
		}
		for (String rotulo : rotulos) {
			if (rotulo == null || rotulo.isBlank()) {
				continue;
			}
			Matcher matcher = LUT.matcher(rotulo);
			if (matcher.find()) {
				return "LUT-" + matcher.group(1);
			}
		}
		return null;
	}

	public static String chaveNormalizada(String valor) {
		if (valor == null || valor.isBlank()) {
			return "";
		}
		String codigo = codigoLogico(valor);
		if (codigo != null) {
			Matcher matcher = LUT.matcher(codigo);
			if (matcher.find()) {
				return "LUT-" + matcher.group(1).replaceFirst("^0+", "");
			}
		}
		return valor.strip().toUpperCase(Locale.ROOT);
	}

	public static Optional<Equipamento> vincular(
		Collection<Equipamento> equipamentos,
		Long idVeiculo,
		String... rotulos
	) {
		if (equipamentos == null || equipamentos.isEmpty()) {
			return Optional.empty();
		}
		if (idVeiculo != null) {
			String externo = String.valueOf(idVeiculo);
			for (Equipamento equipamento : equipamentos) {
				if (externo.equals(equipamento.getCodigoExterno())) {
					return Optional.of(equipamento);
				}
			}
		}
		String codigoLogico = codigoLogico(rotulos);
		if (codigoLogico != null) {
			String chave = chaveNormalizada(codigoLogico);
			for (Equipamento equipamento : equipamentos) {
				if (chave.equals(chaveNormalizada(equipamento.getCodigo()))) {
					return Optional.of(equipamento);
				}
			}
		}
		for (String rotulo : rotulos == null ? new String[0] : rotulos) {
			if (rotulo == null || rotulo.isBlank()) {
				continue;
			}
			String needle = rotulo.strip().toUpperCase(Locale.ROOT);
			for (Equipamento equipamento : equipamentos) {
				String codigo = equipamento.getCodigo() == null
					? ""
					: equipamento.getCodigo().toUpperCase(Locale.ROOT);
				String descricao = equipamento.getDescricao() == null
					? ""
					: equipamento.getDescricao().toUpperCase(Locale.ROOT);
				if (codigo.equals(needle) || descricao.equals(needle)
					|| codigo.contains(needle) || descricao.contains(needle)) {
					return Optional.of(equipamento);
				}
			}
		}
		return Optional.empty();
	}
}

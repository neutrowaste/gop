package br.com.gop.models;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Itens da sidebar do painel da empresa que o admin pode liberar a um usuário comum.
 * "Usuários" permanece exclusivo de ADMIN_EMPRESA.
 */
public enum AcessoMenu {
	SETORES("Setores", "Cadastros", 10),
	TRECHOS("Trechos", "Cadastros", 20),
	COLABORADORES("Colaboradores", "Cadastros", 30),
	ESCALAS("Escala", "Operacional", 40),
	OCORRENCIAS("Ocorrências", "Operacional", 50);

	public static final String PREFIXO_AUTHORITY = "ORG_MENU_";

	private final String nome;
	private final String grupo;
	private final int ordem;

	AcessoMenu(String nome, String grupo, int ordem) {
		this.nome = nome;
		this.grupo = grupo;
		this.ordem = ordem;
	}

	public String nome() {
		return nome;
	}

	public String grupo() {
		return grupo;
	}

	public int ordem() {
		return ordem;
	}

	public String authority() {
		return PREFIXO_AUTHORITY + name();
	}

	public static Optional<AcessoMenu> buscar(String codigo) {
		if (codigo == null || codigo.isBlank()) {
			return Optional.empty();
		}
		String normalizado = codigo.strip().toUpperCase(Locale.ROOT);
		return Arrays.stream(values())
			.filter(item -> item.name().equals(normalizado))
			.findFirst();
	}

	public static AcessoMenu exigir(String codigo) {
		return buscar(codigo).orElseThrow(() -> new IllegalArgumentException("Acesso de menu inválido"));
	}

	public static List<AcessoMenu> catalogo() {
		return List.of(values());
	}
}

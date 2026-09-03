package br.com.gop.security;

/**
 * Expressões SpEL reutilizadas em @PreAuthorize do painel da empresa.
 * Admin da empresa sempre passa; usuário comum depende do item liberado.
 */
public final class OrgAuth {

	public static final String ADMIN_EMPRESA = "hasRole('ADMIN_EMPRESA')";

	public static final String DA_EMPRESA = ADMIN_EMPRESA + " or @acessoMenuService.usuarioDaEmpresa()";

	public static final String SETORES = ADMIN_EMPRESA + " or @acessoMenuService.temAcesso('SETORES')";

	public static final String LER_SETORES = ADMIN_EMPRESA
		+ " or @acessoMenuService.temQualquer('SETORES', 'TRECHOS', 'ESCALAS', 'OCORRENCIAS')";

	public static final String TRECHOS = ADMIN_EMPRESA + " or @acessoMenuService.temAcesso('TRECHOS')";

	public static final String LER_TRECHOS = ADMIN_EMPRESA
		+ " or @acessoMenuService.temQualquer('TRECHOS', 'ESCALAS')";

	public static final String COLABORADORES = ADMIN_EMPRESA
		+ " or @acessoMenuService.temAcesso('COLABORADORES')";

	public static final String LER_COLABORADORES = ADMIN_EMPRESA
		+ " or @acessoMenuService.temQualquer('COLABORADORES', 'ESCALAS')";

	public static final String LER_CIDADES = ADMIN_EMPRESA
		+ " or @acessoMenuService.temQualquer('SETORES', 'TRECHOS', 'COLABORADORES', 'ESCALAS')";

	public static final String ESCALAS = ADMIN_EMPRESA + " or @acessoMenuService.temAcesso('ESCALAS')";

	public static final String OCORRENCIAS = ADMIN_EMPRESA
		+ " or @acessoMenuService.temAcesso('OCORRENCIAS')";

	public static final String LER_PARAMETROS = ADMIN_EMPRESA
		+ " or @acessoMenuService.temQualquer('SETORES', 'TRECHOS', 'COLABORADORES', 'ESCALAS', 'OCORRENCIAS')";

	private OrgAuth() {
	}
}

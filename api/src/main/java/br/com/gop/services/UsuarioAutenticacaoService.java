package br.com.gop.services;

import br.com.gop.models.AcessoMenu;
import br.com.gop.models.EscopoPapel;
import br.com.gop.models.Papel;
import br.com.gop.models.PapelPermissao;
import br.com.gop.models.Usuario;
import br.com.gop.models.UsuarioPapel;
import br.com.gop.repositories.EmpresaModuloRepository;
import br.com.gop.repositories.PapelPermissaoRepository;
import br.com.gop.repositories.UsuarioAcessoMenuRepository;
import br.com.gop.repositories.UsuarioModuloRepository;
import br.com.gop.repositories.UsuarioPapelRepository;
import br.com.gop.repositories.UsuarioRepository;
import br.com.gop.security.UsuarioPrincipal;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Profile("postgres")
public class UsuarioAutenticacaoService implements UserDetailsService {

	private final UsuarioRepository usuarioRepository;
	private final UsuarioPapelRepository usuarioPapelRepository;
	private final PapelPermissaoRepository papelPermissaoRepository;
	private final EmpresaModuloRepository empresaModuloRepository;
	private final UsuarioModuloRepository usuarioModuloRepository;
	private final UsuarioAcessoMenuRepository usuarioAcessoMenuRepository;

	public UsuarioAutenticacaoService(
		UsuarioRepository usuarioRepository,
		UsuarioPapelRepository usuarioPapelRepository,
		PapelPermissaoRepository papelPermissaoRepository,
		EmpresaModuloRepository empresaModuloRepository,
		UsuarioModuloRepository usuarioModuloRepository,
		UsuarioAcessoMenuRepository usuarioAcessoMenuRepository
	) {
		this.usuarioRepository = usuarioRepository;
		this.usuarioPapelRepository = usuarioPapelRepository;
		this.papelPermissaoRepository = papelPermissaoRepository;
		this.empresaModuloRepository = empresaModuloRepository;
		this.usuarioModuloRepository = usuarioModuloRepository;
		this.usuarioAcessoMenuRepository = usuarioAcessoMenuRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email.strip())
			.filter(Usuario::isAtivo)
			.orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));

		Long empresaId = usuario.getEmpresa() == null ? null : usuario.getEmpresa().getId();
		String empresaNome = usuario.getEmpresa() == null ? null : usuario.getEmpresa().getNome();
		String empresaCodigo = usuario.getEmpresa() == null ? null : usuario.getEmpresa().getCodigo();
		List<UsuarioPapel> atribuicoes = usuarioPapelRepository
			.findPapeisAtivosByUsuarioId(usuario.getId())
			.stream()
			.filter(atribuicao -> atribuicaoValida(atribuicao, empresaId))
			.toList();

		Set<String> papeis = new LinkedHashSet<>();
		Set<Long> papelIds = new LinkedHashSet<>();
		for (UsuarioPapel atribuicao : atribuicoes) {
			Papel papel = atribuicao.getPapel();
			papeis.add(normalizar(papel.getCodigo()));
			papelIds.add(papel.getId());
		}

		Set<Long> modulosEmpresa = empresaId == null
			? Set.of()
			: empresaModuloRepository.findModuloIdsHabilitadosByEmpresaId(empresaId);
		Set<Long> modulosUsuario = empresaId == null
			? Set.of()
			: usuarioModuloRepository.findModuloIdsLiberadosByUsuarioId(usuario.getId());
		Set<String> permissoes = carregarPermissoes(
			usuario.getId(),
			papelIds,
			empresaId,
			modulosEmpresa,
			modulosUsuario
		);

		Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
		papeis.stream()
			.map(codigo -> new SimpleGrantedAuthority("ROLE_" + codigo))
			.forEach(authorities::add);
		permissoes.stream()
			.map(SimpleGrantedAuthority::new)
			.forEach(authorities::add);

		return new UsuarioPrincipal(
			usuario.getId(),
			empresaId,
			empresaNome,
			empresaCodigo,
			usuario.getNome(),
			usuario.getEmail(),
			usuario.getSenhaHash(),
			usuario.isAtivo(),
			Set.copyOf(papeis),
			Set.copyOf(permissoes),
			Set.copyOf(authorities)
		);
	}

	private Set<String> carregarPermissoes(
		Long usuarioId,
		Set<Long> papelIds,
		Long empresaId,
		Set<Long> modulosEmpresa,
		Set<Long> modulosUsuario
	) {
		Set<String> permissoes = new LinkedHashSet<>();
		if (!papelIds.isEmpty()) {
			for (PapelPermissao vinculo : papelPermissaoRepository.findPermissoesAtivasByPapelIds(papelIds)) {
				var permissao = vinculo.getPermissao();
				if (empresaId == null || permissao.getModulo() == null) {
					permissoes.add(normalizar(permissao.getCodigo()));
					continue;
				}
				Long moduloId = permissao.getModulo().getId();
				if (modulosEmpresa.contains(moduloId) && modulosUsuario.contains(moduloId)) {
					permissoes.add(normalizar(permissao.getCodigo()));
				}
			}
		}
		if (empresaId != null) {
			for (String codigo : usuarioAcessoMenuRepository.findCodigosLiberadosByUsuarioId(usuarioId)) {
				AcessoMenu.buscar(codigo).ifPresent(item -> permissoes.add(item.authority()));
			}
		}
		return permissoes;
	}

	private boolean atribuicaoValida(UsuarioPapel atribuicao, Long empresaId) {
		Papel papel = atribuicao.getPapel();
		Long empresaAtribuicaoId = atribuicao.getEmpresa() == null
			? null
			: atribuicao.getEmpresa().getId();
		Long empresaPapelId = papel.getEmpresa() == null ? null : papel.getEmpresa().getId();

		if (empresaId == null) {
			return papel.getEscopo() == EscopoPapel.PLATAFORMA
				&& empresaAtribuicaoId == null
				&& empresaPapelId == null;
		}

		if (papel.getEscopo() == EscopoPapel.PLATAFORMA
			|| !empresaId.equals(empresaAtribuicaoId)
			|| (empresaPapelId != null && !empresaId.equals(empresaPapelId))) {
			return false;
		}

		return atribuicao.getFilial() == null
			|| empresaId.equals(atribuicao.getFilial().getEmpresa().getId());
	}

	private String normalizar(String codigo) {
		return codigo.strip().toUpperCase(Locale.ROOT);
	}
}

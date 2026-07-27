package br.com.gop.services;

import br.com.gop.models.AuditoriaLog;
import br.com.gop.models.Empresa;
import br.com.gop.repositories.AuditoriaLogRepository;
import br.com.gop.repositories.UsuarioRepository;
import br.com.gop.security.UsuarioTokenPrincipal;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@Profile("postgres")
public class AuditoriaService {

	private final AuditoriaLogRepository auditoriaLogRepository;
	private final UsuarioRepository usuarioRepository;

	public AuditoriaService(
		AuditoriaLogRepository auditoriaLogRepository,
		UsuarioRepository usuarioRepository
	) {
		this.auditoriaLogRepository = auditoriaLogRepository;
		this.usuarioRepository = usuarioRepository;
	}

	public void registrar(
		Empresa empresa,
		String acao,
		String entidade,
		Long entidadeId,
		Map<String, Object> payload
	) {
		AuditoriaLog log = new AuditoriaLog();
		log.setEmpresa(empresa);
		log.setAcao(acao);
		log.setEntidade(entidade);
		log.setEntidadeId(entidadeId);
		log.setPayload(payload);
		log.setOcorridoEm(Instant.now());

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null
			&& authentication.getPrincipal() instanceof UsuarioTokenPrincipal usuario) {
			log.setUsuario(usuarioRepository.getReferenceById(usuario.usuarioId()));
		}
		auditoriaLogRepository.save(log);
	}
}

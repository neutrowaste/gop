package br.com.gop.services;

import br.com.gop.models.Colaborador;
import br.com.gop.models.Empresa;
import br.com.gop.models.Equipamento;
import br.com.gop.models.Escala;
import br.com.gop.models.EscalaColaboradorResponse;
import br.com.gop.models.EscalaRequest;
import br.com.gop.models.EscalaResponse;
import br.com.gop.models.Setor;
import br.com.gop.models.StatusEscala;
import br.com.gop.models.StatusTrecho;
import br.com.gop.models.TipoEscala;
import br.com.gop.models.Trecho;
import br.com.gop.models.TrechoExecucao;
import br.com.gop.models.Turno;
import br.com.gop.models.Usuario;
import br.com.gop.repositories.EscalaRepository;
import br.com.gop.repositories.TrechoExecucaoRepository;
import br.com.gop.repositories.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Amarração do módulo Escala (RN-001): pré-requisito do motor de
 * regras da Varrição, que só processa posições de escalas PUBLICADA do dia.
 * Frequência é um modelo que gera a instância diária; avulsa já nasce publicada.
 */
@Service
@Profile("postgres")
public class EscalaService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EscalaService.class);
	static final ZoneId FUSO_OPERACIONAL = ZoneId.of("America/Sao_Paulo");

	private static final Sort ORDENACAO_PADRAO = Sort.by(
		Sort.Order.desc("data"),
		Sort.Order.asc("turno"),
		Sort.Order.asc("setor.nome")
	);

	private final EmpresaService empresaService;
	private final SetorService setorService;
	private final ColaboradorService colaboradorService;
	private final TrechoService trechoService;
	private final UsuarioRepository usuarioRepository;
	private final EscalaRepository escalaRepository;
	private final TrechoExecucaoRepository trechoExecucaoRepository;
	private final AuditoriaService auditoriaService;
	private final Clock clock;

	@Autowired
	public EscalaService(
		EmpresaService empresaService,
		SetorService setorService,
		ColaboradorService colaboradorService,
		TrechoService trechoService,
		UsuarioRepository usuarioRepository,
		EscalaRepository escalaRepository,
		TrechoExecucaoRepository trechoExecucaoRepository,
		AuditoriaService auditoriaService
	) {
		this(
			empresaService,
			setorService,
			colaboradorService,
			trechoService,
			usuarioRepository,
			escalaRepository,
			trechoExecucaoRepository,
			auditoriaService,
			Clock.system(FUSO_OPERACIONAL)
		);
	}

	EscalaService(
		EmpresaService empresaService,
		SetorService setorService,
		ColaboradorService colaboradorService,
		TrechoService trechoService,
		UsuarioRepository usuarioRepository,
		EscalaRepository escalaRepository,
		TrechoExecucaoRepository trechoExecucaoRepository,
		AuditoriaService auditoriaService,
		Clock clock
	) {
		this.empresaService = empresaService;
		this.setorService = setorService;
		this.colaboradorService = colaboradorService;
		this.trechoService = trechoService;
		this.usuarioRepository = usuarioRepository;
		this.escalaRepository = escalaRepository;
		this.trechoExecucaoRepository = trechoExecucaoRepository;
		this.auditoriaService = auditoriaService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<EscalaResponse> listar(
		Long empresaId,
		LocalDate data,
		Long setorId,
		StatusEscala status,
		TipoEscala tipo
	) {
		empresaService.buscarEntidade(empresaId);
		Specification<Escala> filtro = comFiltros(empresaId, data, setorId, status, tipo);
		return escalaRepository.findAll(filtro, ORDENACAO_PADRAO)
			.stream()
			.map(this::paraResponse)
			.toList();
	}

	/**
	 * Usa Specification (em vez de "(:param is null or campo = :param)" em JPQL)
	 * porque o driver do PostgreSQL não consegue inferir o tipo do parâmetro
	 * quando ele só aparece em "is null", retornando erro em runtime.
	 */
	private Specification<Escala> comFiltros(
		Long empresaId,
		LocalDate data,
		Long setorId,
		StatusEscala status,
		TipoEscala tipo
	) {
		return (root, query, cb) -> {
			List<Predicate> predicados = new ArrayList<>();
			predicados.add(cb.equal(root.get("empresa").get("id"), empresaId));
			if (data != null) {
				predicados.add(cb.equal(root.get("data"), data));
				if (tipo != null) {
					predicados.add(cb.equal(root.get("tipo"), tipo));
				}
			} else if (tipo == TipoEscala.FREQUENCIA) {
				predicados.add(cb.equal(root.get("tipo"), TipoEscala.FREQUENCIA));
				predicados.add(cb.isNull(root.get("data")));
				predicados.add(cb.isNull(root.get("origem")));
			} else {
				predicados.add(cb.isNotNull(root.get("data")));
				if (tipo != null) {
					predicados.add(cb.equal(root.get("tipo"), tipo));
				}
			}
			if (setorId != null) {
				predicados.add(cb.equal(root.get("setor").get("id"), setorId));
			}
			if (status != null) {
				predicados.add(cb.equal(root.get("status"), status));
			}
			return cb.and(predicados.toArray(new Predicate[0]));
		};
	}

	@Transactional(readOnly = true)
	public EscalaResponse buscar(Long empresaId, Long id) {
		return paraResponse(buscarEntidade(empresaId, id));
	}

	@Transactional
	public EscalaResponse criar(Long empresaId, EscalaRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Setor setor = setorService.buscarEntidade(empresaId, request.setorId());
		Usuario supervisor = buscarSupervisor(empresaId, request.supervisorUsuarioId());
		Set<Colaborador> colaboradores = carregarColaboradores(empresaId, request.colaboradorIds());
		TipoEscala tipo = request.tipo() == null ? TipoEscala.AVULSA : request.tipo();

		if (tipo == TipoEscala.FREQUENCIA) {
			return criarFrequencia(empresa, setor, supervisor, colaboradores, request);
		}
		return criarAvulsa(empresa, setor, supervisor, colaboradores, request);
	}

	private EscalaResponse criarAvulsa(
		Empresa empresa,
		Setor setor,
		Usuario supervisor,
		Set<Colaborador> colaboradores,
		EscalaRequest request
	) {
		if (request.data() == null) {
			throw new RegraNegocioException("Escala avulsa exige a data da execução");
		}
		garantirColaboradoresLivresNoTurno(empresa.getId(), request.data(), request.turno(), colaboradores, null);

		Escala escala = new Escala();
		escala.setEmpresa(empresa);
		escala.setTipo(TipoEscala.AVULSA);
		escala.setData(request.data());
		escala.setTurno(request.turno());
		escala.setSetor(setor);
		escala.setColaboradores(colaboradores);
		escala.setSupervisor(supervisor);
		escala.setStatus(StatusEscala.PUBLICADA);
		escala.setPublicadaEm(Instant.now(clock));
		escala.setAtivo(request.ativo() == null || request.ativo());
		escalaRepository.save(escala);
		anexarTrechos(empresa, escala, request.trechoIds());

		auditoriaService.registrar(empresa, "ESCALA_CRIADA", "Escala", escala.getId(), payloadBase(escala));
		LOGGER.info(
			"Escala avulsa criada: id={}, empresaId={}, data={}, turno={}",
			escala.getId(),
			empresa.getId(),
			escala.getData(),
			escala.getTurno()
		);
		return paraResponse(escala);
	}

	private EscalaResponse criarFrequencia(
		Empresa empresa,
		Setor setor,
		Usuario supervisor,
		Set<Colaborador> colaboradores,
		EscalaRequest request
	) {
		Set<DayOfWeek> diasSemana = diasSemanaDoPedido(request);
		if (diasSemana.isEmpty()) {
			throw new RegraNegocioException("Escala de frequência exige ao menos um dia da semana");
		}
		garantirColaboradoresLivresNaFrequencia(empresa.getId(), request.turno(), diasSemana, colaboradores, null);

		Escala modelo = new Escala();
		modelo.setEmpresa(empresa);
		modelo.setTipo(TipoEscala.FREQUENCIA);
		modelo.setData(null);
		modelo.setTurno(request.turno());
		modelo.setSetor(setor);
		modelo.setColaboradores(colaboradores);
		modelo.setSupervisor(supervisor);
		modelo.getDiasSemana().addAll(diasSemana);
		modelo.setStatus(StatusEscala.PUBLICADA);
		modelo.setPublicadaEm(Instant.now(clock));
		modelo.setAtivo(request.ativo() == null || request.ativo());
		escalaRepository.save(modelo);
		anexarTrechos(empresa, modelo, request.trechoIds());

		auditoriaService.registrar(empresa, "ESCALA_CRIADA", "Escala", modelo.getId(), payloadBase(modelo));
		LOGGER.info(
			"Escala de frequência criada: id={}, empresaId={}, turno={}, dias={}",
			modelo.getId(),
			empresa.getId(),
			modelo.getTurno(),
			modelo.getDiasSemana()
		);

		if (modelo.isAtivo()) {
			gerarInstanciaDoModelo(modelo, hoje());
		}
		return paraResponse(modelo);
	}

	@Transactional
	public EscalaResponse atualizar(Long empresaId, Long id, EscalaRequest request) {
		Empresa empresa = empresaAtiva(empresaId);
		Escala escala = buscarEntidade(empresaId, id);
		if (!escala.isAtivo()) {
			throw new RegraNegocioException("Escala cancelada não pode ser editada");
		}

		Usuario supervisor = buscarSupervisor(empresaId, request.supervisorUsuarioId());
		Set<Colaborador> colaboradores = carregarColaboradores(empresaId, request.colaboradorIds());

		if (escala.isModeloFrequencia()) {
			atualizarModeloFrequencia(empresa, escala, supervisor, colaboradores, request);
		} else if (escala.isInstanciaFrequencia()) {
			atualizarInstanciaFrequencia(empresa, escala, supervisor, colaboradores);
		} else {
			atualizarAvulsa(empresa, escala, supervisor, colaboradores);
		}
		return paraResponse(escala);
	}

	private void atualizarAvulsa(
		Empresa empresa,
		Escala escala,
		Usuario supervisor,
		Set<Colaborador> colaboradores
	) {
		garantirColaboradoresLivresNoTurno(
			empresa.getId(),
			escala.getData(),
			escala.getTurno(),
			colaboradores,
			escala.getId()
		);
		aplicarEquipe(escala, colaboradores, supervisor);
		escalaRepository.save(escala);
		auditoriaService.registrar(empresa, "ESCALA_ATUALIZADA", "Escala", escala.getId(), payloadBase(escala));
		LOGGER.info("Escala avulsa atualizada: id={}, empresaId={}", escala.getId(), empresa.getId());
	}

	private void atualizarModeloFrequencia(
		Empresa empresa,
		Escala modelo,
		Usuario supervisor,
		Set<Colaborador> colaboradores,
		EscalaRequest request
	) {
		Set<DayOfWeek> diasSemana = diasSemanaDoPedido(request);
		if (diasSemana.isEmpty()) {
			throw new RegraNegocioException("Escala de frequência exige ao menos um dia da semana");
		}
		garantirColaboradoresLivresNaFrequencia(
			empresa.getId(),
			modelo.getTurno(),
			diasSemana,
			colaboradores,
			modelo.getId()
		);

		modelo.getDiasSemana().clear();
		modelo.getDiasSemana().addAll(diasSemana);
		aplicarEquipe(modelo, colaboradores, supervisor);
		escalaRepository.save(modelo);

		Optional<Escala> instanciaHoje = escalaRepository.findByOrigemIdAndDataAndAtivoTrue(modelo.getId(), hoje());
		if (instanciaHoje.isPresent()) {
			Escala instancia = instanciaHoje.get();
			garantirColaboradoresLivresNoTurno(
				empresa.getId(),
				instancia.getData(),
				instancia.getTurno(),
				colaboradores,
				instancia.getId()
			);
			aplicarEquipe(instancia, colaboradores, supervisor);
			escalaRepository.save(instancia);
		} else {
			gerarInstanciaDoModelo(modelo, hoje());
		}

		auditoriaService.registrar(empresa, "ESCALA_ATUALIZADA", "Escala", modelo.getId(), payloadBase(modelo));
		LOGGER.info("Escala de frequência atualizada: id={}, empresaId={}", modelo.getId(), empresa.getId());
	}

	private void atualizarInstanciaFrequencia(
		Empresa empresa,
		Escala instancia,
		Usuario supervisor,
		Set<Colaborador> colaboradores
	) {
		garantirColaboradoresLivresNoTurno(
			empresa.getId(),
			instancia.getData(),
			instancia.getTurno(),
			colaboradores,
			instancia.getId()
		);
		aplicarEquipe(instancia, colaboradores, supervisor);
		escalaRepository.save(instancia);

		Escala modelo = instancia.getOrigem();
		if (modelo != null && modelo.isAtivo()) {
			garantirColaboradoresLivresNaFrequencia(
				empresa.getId(),
				modelo.getTurno(),
				modelo.getDiasSemana(),
				colaboradores,
				modelo.getId()
			);
			aplicarEquipe(modelo, colaboradores, supervisor);
			escalaRepository.save(modelo);
		}

		auditoriaService.registrar(empresa, "ESCALA_ATUALIZADA", "Escala", instancia.getId(), payloadBase(instancia));
		LOGGER.info("Instância de frequência atualizada: id={}, empresaId={}", instancia.getId(), empresa.getId());
	}

	@Transactional
	public EscalaResponse publicar(Long empresaId, Long id) {
		Empresa empresa = empresaAtiva(empresaId);
		Escala escala = buscarEntidade(empresaId, id);
		if (!escala.isAtivo()) {
			throw new RegraNegocioException("Escala cancelada não pode ser publicada");
		}
		if (escala.getColaboradores().isEmpty()) {
			throw new RegraNegocioException("Escala sem colaboradores não pode ser publicada");
		}
		if (escala.getStatus() == StatusEscala.PUBLICADA) {
			return paraResponse(escala);
		}
		escala.setStatus(StatusEscala.PUBLICADA);
		escala.setPublicadaEm(Instant.now(clock));
		escalaRepository.save(escala);

		auditoriaService.registrar(empresa, "ESCALA_PUBLICADA", "Escala", id, payloadBase(escala));
		LOGGER.info("Escala publicada: id={}, empresaId={}", id, empresaId);
		return paraResponse(escala);
	}

	@Transactional
	public EscalaResponse despublicar(Long empresaId, Long id) {
		Empresa empresa = empresaAtiva(empresaId);
		Escala escala = buscarEntidade(empresaId, id);
		if (escala.getStatus() == StatusEscala.RASCUNHO) {
			return paraResponse(escala);
		}
		escala.setStatus(StatusEscala.RASCUNHO);
		escala.setPublicadaEm(null);
		escalaRepository.save(escala);

		auditoriaService.registrar(empresa, "ESCALA_DESPUBLICADA", "Escala", id, payloadBase(escala));
		LOGGER.info("Escala despublicada: id={}, empresaId={}", id, empresaId);
		return paraResponse(escala);
	}

	@Transactional
	public void desativar(Long empresaId, Long id) {
		Empresa empresa = empresaAtiva(empresaId);
		Escala escala = buscarEntidade(empresaId, id);
		escala.setAtivo(false);
		escalaRepository.save(escala);

		if (escala.isModeloFrequencia()) {
			cancelarInstanciasAtivas(escala.getId());
		} else if (escala.isInstanciaFrequencia()) {
			Escala modelo = escala.getOrigem();
			if (modelo != null && modelo.isAtivo()) {
				modelo.setAtivo(false);
				escalaRepository.save(modelo);
				LOGGER.info("Modelo de frequência cancelado com a instância: id={}", modelo.getId());
			}
			if (modelo != null) {
				cancelarInstanciasAtivas(modelo.getId());
			}
		}

		auditoriaService.registrar(empresa, "ESCALA_CANCELADA", "Escala", id, payloadBase(escala));
		LOGGER.info("Escala cancelada: id={}, empresaId={}", id, empresaId);
	}

	private void cancelarInstanciasAtivas(Long modeloId) {
		for (Escala instancia : escalaRepository.findByOrigemIdAndAtivoTrue(modeloId)) {
			instancia.setAtivo(false);
			escalaRepository.save(instancia);
			LOGGER.info("Instância de frequência cancelada com o modelo: id={}, data={}", instancia.getId(), instancia.getData());
		}
	}

	/**
	 * Materializa a instância PUBLICADA do dia para cada modelo de frequência ativo
	 * cujo dia da semana corresponde a {@code data}. Idempotente: não recria se já
	 * existir instância (mesmo cancelada) para o modelo naquela data.
	 */
	@Transactional
	public int gerarInstanciasDoDia(LocalDate data) {
		List<Escala> modelos = escalaRepository.findByTipoAndOrigemIsNullAndAtivoTrue(TipoEscala.FREQUENCIA);
		int geradas = 0;
		for (Escala modelo : modelos) {
			if (gerarInstanciaDoModelo(modelo, data).isPresent()) {
				geradas++;
			}
		}
		return geradas;
	}

	Optional<Escala> gerarInstanciaDoModelo(Escala modelo, LocalDate data) {
		if (!modelo.isAtivo() || !modelo.isModeloFrequencia()) {
			return Optional.empty();
		}
		if (!modelo.getDiasSemana().contains(data.getDayOfWeek())) {
			return Optional.empty();
		}
		if (escalaRepository.existsByOrigemIdAndData(modelo.getId(), data)) {
			return Optional.empty();
		}
		if (temConflitoNoTurno(modelo.getEmpresa().getId(), data, modelo.getTurno(), modelo.getColaboradores(), null)) {
			LOGGER.error(
				"Frequência não gerou o dia: conflito de colaborador/lutocar. modeloId={}, data={}, turno={}",
				modelo.getId(),
				data,
				modelo.getTurno()
			);
			return Optional.empty();
		}

		Escala instancia = new Escala();
		instancia.setEmpresa(modelo.getEmpresa());
		instancia.setTipo(TipoEscala.FREQUENCIA);
		instancia.setOrigem(modelo);
		instancia.setData(data);
		instancia.setTurno(modelo.getTurno());
		instancia.setSetor(modelo.getSetor());
		instancia.setColaboradores(new LinkedHashSet<>(modelo.getColaboradores()));
		instancia.setSupervisor(modelo.getSupervisor());
		instancia.setStatus(StatusEscala.PUBLICADA);
		instancia.setPublicadaEm(Instant.now(clock));
		instancia.setAtivo(true);
		escalaRepository.save(instancia);
		copiarPlanoTrechos(modelo, instancia);

		auditoriaService.registrar(
			modelo.getEmpresa(),
			"ESCALA_FREQUENCIA_GERADA",
			"Escala",
			instancia.getId(),
			payloadBase(instancia)
		);
		LOGGER.info(
			"Instância de frequência gerada: id={}, modeloId={}, data={}",
			instancia.getId(),
			modelo.getId(),
			data
		);
		return Optional.of(instancia);
	}

	@Transactional(readOnly = true)
	public Escala buscarEntidade(Long empresaId, Long id) {
		return escalaRepository.findByIdAndEmpresaId(id, empresaId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Escala não encontrada"));
	}

	private void anexarTrechos(Empresa empresa, Escala escala, List<Long> trechoIds) {
		if (trechoIds == null || trechoIds.isEmpty()) {
			return;
		}
		List<Long> ids = trechoIds.stream().distinct().toList();
		int ordem = 1;
		for (Long trechoId : ids) {
			Trecho trecho = trechoService.buscarEntidade(empresa.getId(), trechoId);
			if (!trecho.isAtivo()) {
				throw new RegraNegocioException("O trecho " + trecho.getCodigo() + " está inativo");
			}
			if (!trecho.getSetor().getId().equals(escala.getSetor().getId())) {
				throw new RegraNegocioException("O trecho " + trecho.getCodigo() + " não pertence ao setor desta escala");
			}
			TrechoExecucao execucao = new TrechoExecucao();
			execucao.setEmpresa(empresa);
			execucao.setEscala(escala);
			execucao.setTrecho(trecho);
			execucao.setOrdem(ordem++);
			execucao.setStatus(StatusTrecho.PLANEJADO);
			trechoExecucaoRepository.save(execucao);
		}
		if (!ids.isEmpty()) {
			trechoExecucaoRepository.flush();
		}
	}

	/**
	 * Se a instância de hoje ainda não recebeu GPS, substitui o plano do dia
	 * pelo plano atual do modelo. Assim trechos cadastrados depois da geração
	 * do dia ainda entram na execução de hoje.
	 */
	@Transactional
	public void replicarPlanoNaInstanciaDeHoje(Escala modelo) {
		if (!modelo.isModeloFrequencia() || !modelo.isAtivo()) {
			return;
		}
		Optional<Escala> instanciaHoje = escalaRepository.findByOrigemIdAndDataAndAtivoTrue(modelo.getId(), hoje());
		if (instanciaHoje.isEmpty()) {
			return;
		}
		Escala instancia = instanciaHoje.get();
		List<TrechoExecucao> planoDoDia = trechoExecucaoRepository.findByEscalaIdAndAtivoTrueOrderByOrdemAsc(instancia.getId());
		boolean jaComecou = planoDoDia.stream().anyMatch(item -> item.getQuantidadePosicoesValidas() > 0);
		if (jaComecou) {
			return;
		}
		trechoExecucaoRepository.deleteAll(planoDoDia);
		trechoExecucaoRepository.flush();
		copiarPlanoTrechos(modelo, instancia);
		LOGGER.info(
			"Plano da frequência replicado na instância de hoje: modeloId={}, instanciaId={}",
			modelo.getId(),
			instancia.getId()
		);
	}

	private void copiarPlanoTrechos(Escala modelo, Escala instancia) {
		for (TrechoExecucao original : trechoExecucaoRepository.findByEscalaIdAndAtivoTrueOrderByOrdemAsc(modelo.getId())) {
			TrechoExecucao copia = new TrechoExecucao();
			copia.setEmpresa(instancia.getEmpresa());
			copia.setEscala(instancia);
			copia.setTrecho(original.getTrecho());
			copia.setOrdem(original.getOrdem());
			copia.setStatus(StatusTrecho.PLANEJADO);
			trechoExecucaoRepository.save(copia);
		}
	}

	private static void aplicarEquipe(Escala escala, Set<Colaborador> colaboradores, Usuario supervisor) {
		escala.getColaboradores().clear();
		escala.getColaboradores().addAll(colaboradores);
		escala.setSupervisor(supervisor);
	}

	private Usuario buscarSupervisor(Long empresaId, Long usuarioId) {
		Usuario supervisor = usuarioRepository.findByIdAndEmpresaId(usuarioId, empresaId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Supervisor não encontrado"));
		if (!supervisor.isAtivo()) {
			throw new RegraNegocioException("Supervisor selecionado está inativo");
		}
		return supervisor;
	}

	private Set<Colaborador> carregarColaboradores(Long empresaId, List<Long> colaboradorIds) {
		List<Long> ids = colaboradorIds.stream().distinct().toList();
		List<Colaborador> encontrados = colaboradorService.buscarEntidades(empresaId, ids);
		for (Colaborador colaborador : encontrados) {
			if (!colaborador.isAtivo()) {
				throw new RegraNegocioException("O colaborador " + colaborador.getNome() + " está inativo");
			}
			if (colaborador.getEquipamento() == null || !colaborador.getEquipamento().isAtivo()) {
				throw new RegraNegocioException(
					"O colaborador " + colaborador.getNome() + " não possui lutocar ativo designado"
				);
			}
		}
		return new LinkedHashSet<>(encontrados);
	}

	private void garantirColaboradoresLivresNaFrequencia(
		Long empresaId,
		Turno turno,
		Set<DayOfWeek> diasSemana,
		Set<Colaborador> colaboradores,
		Long idAtual
	) {
		List<Escala> modelos = escalaRepository.findByEmpresaIdAndTipoAndOrigemIsNullAndAtivoTrue(
			empresaId,
			TipoEscala.FREQUENCIA
		);
		Set<Long> colaboradorIds = colaboradores.stream().map(Colaborador::getId).collect(Collectors.toSet());
		Set<Long> equipamentoIds = colaboradores.stream()
			.map(colaborador -> colaborador.getEquipamento().getId())
			.collect(Collectors.toSet());

		for (Escala outro : modelos) {
			if (idAtual != null && idAtual.equals(outro.getId())) {
				continue;
			}
			if (outro.getTurno() != turno) {
				continue;
			}
			if (Collections.disjoint(outro.getDiasSemana(), diasSemana)) {
				continue;
			}
			boolean colaboradorSobreposto = outro.getColaboradores().stream()
				.map(Colaborador::getId)
				.anyMatch(colaboradorIds::contains);
			if (colaboradorSobreposto) {
				throw new ConflitoNegocioException(
					"Um ou mais colaboradores já estão em outra escala de frequência neste turno e dia da semana"
				);
			}
			boolean lutocarSobreposto = outro.equipamentosDosColaboradores().stream()
				.map(Equipamento::getId)
				.anyMatch(equipamentoIds::contains);
			if (lutocarSobreposto) {
				throw new ConflitoNegocioException(
					"Um ou mais lutocares já estão em outra escala de frequência neste turno e dia da semana"
				);
			}
		}
	}

	private void garantirColaboradoresLivresNoTurno(
		Long empresaId,
		LocalDate data,
		Turno turno,
		Set<Colaborador> colaboradores,
		Long idAtual
	) {
		if (temConflitoColaboradores(empresaId, data, turno, colaboradores, idAtual)) {
			throw new ConflitoNegocioException(
				"Um ou mais colaboradores já estão escalados nesta data e turno"
			);
		}
		if (temConflitoEquipamentos(empresaId, data, turno, colaboradores, idAtual)) {
			throw new ConflitoNegocioException(
				"Um ou mais lutocares já estão escalados nesta data e turno"
			);
		}
	}

	private boolean temConflitoNoTurno(
		Long empresaId,
		LocalDate data,
		Turno turno,
		Set<Colaborador> colaboradores,
		Long idAtual
	) {
		return temConflitoColaboradores(empresaId, data, turno, colaboradores, idAtual)
			|| temConflitoEquipamentos(empresaId, data, turno, colaboradores, idAtual);
	}

	private boolean temConflitoColaboradores(
		Long empresaId,
		LocalDate data,
		Turno turno,
		Set<Colaborador> colaboradores,
		Long idAtual
	) {
		List<Long> colaboradorIds = colaboradores.stream().map(Colaborador::getId).toList();
		List<Escala> conflito = idAtual == null
			? escalaRepository.findAtivasComColaboradoresNoTurno(empresaId, data, turno, colaboradorIds)
			: escalaRepository.findAtivasComColaboradoresNoTurnoExcluindo(
				empresaId, data, turno, colaboradorIds, idAtual
			);
		return !conflito.isEmpty();
	}

	private boolean temConflitoEquipamentos(
		Long empresaId,
		LocalDate data,
		Turno turno,
		Set<Colaborador> colaboradores,
		Long idAtual
	) {
		List<Long> equipamentoIds = colaboradores.stream()
			.map(colaborador -> colaborador.getEquipamento().getId())
			.distinct()
			.toList();
		List<Escala> conflito = idAtual == null
			? escalaRepository.findAtivasComEquipamentosNoTurno(empresaId, data, turno, equipamentoIds)
			: escalaRepository.findAtivasComEquipamentosNoTurnoExcluindo(
				empresaId, data, turno, equipamentoIds, idAtual
			);
		return !conflito.isEmpty();
	}

	private Empresa empresaAtiva(Long empresaId) {
		Empresa empresa = empresaService.buscarEntidade(empresaId);
		if (!empresa.isAtivo()) {
			throw new RegraNegocioException("Empresa inativa");
		}
		return empresa;
	}

	private LocalDate hoje() {
		return LocalDate.now(clock);
	}

	private static Set<DayOfWeek> diasSemanaDoPedido(EscalaRequest request) {
		if (request.diasSemana() == null) {
			return Set.of();
		}
		return new LinkedHashSet<>(request.diasSemana());
	}

	private Map<String, Object> payloadBase(Escala escala) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("tipo", escala.getTipo().name());
		payload.put("data", escala.getData() == null ? null : escala.getData().toString());
		payload.put("turno", escala.getTurno().name());
		payload.put("setorId", escala.getSetor().getId());
		payload.put(
			"colaboradorIds",
			escala.getColaboradores().stream().map(Colaborador::getId).toList()
		);
		payload.put("supervisorUsuarioId", escala.getSupervisor().getId());
		payload.put("status", escala.getStatus().name());
		payload.put("origemId", escala.getOrigem() == null ? null : escala.getOrigem().getId());
		payload.put("diasSemana", escala.getDiasSemana().stream().map(DayOfWeek::name).toList());
		return payload;
	}

	private EscalaResponse paraResponse(Escala escala) {
		List<EscalaColaboradorResponse> colaboradores = escala.getColaboradores().stream()
			.sorted(Comparator.comparing(Colaborador::getNome, String.CASE_INSENSITIVE_ORDER))
			.map(colaborador -> new EscalaColaboradorResponse(
				colaborador.getId(),
				colaborador.getNome(),
				colaborador.getMatricula(),
				colaborador.getEquipamento().getId(),
				colaborador.getEquipamento().getCodigo()
			))
			.collect(Collectors.toList());
		List<DayOfWeek> diasSemana = escala.getDiasSemana().stream()
			.sorted()
			.toList();
		return new EscalaResponse(
			escala.getId(),
			escala.getTipo(),
			escala.getData(),
			diasSemana,
			escala.getOrigem() == null ? null : escala.getOrigem().getId(),
			escala.getTurno(),
			escala.getSetor().getId(),
			escala.getSetor().getNome(),
			colaboradores,
			escala.getSupervisor().getId(),
			escala.getSupervisor().getNome(),
			escala.getStatus(),
			escala.getPublicadaEm(),
			escala.isAtivo()
		);
	}
}

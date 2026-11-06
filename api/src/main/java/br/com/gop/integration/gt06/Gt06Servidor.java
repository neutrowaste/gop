package br.com.gop.integration.gt06;

import br.com.gop.services.Gt06IngestaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Receptor TCP do GPS próprio (GT06). Confirma login, heartbeat e localização
 * e entrega o ponto normalizado ao {@link Gt06IngestaoService}.
 */
@Component
@Profile("postgres")
@ConditionalOnProperty(prefix = "gop.gps.tcp", name = "enabled", havingValue = "true", matchIfMissing = true)
public class Gt06Servidor implements SmartLifecycle {

	private static final Logger LOGGER = LoggerFactory.getLogger(Gt06Servidor.class);

	private final Gt06Properties properties;
	private final Gt06IngestaoService ingestaoService;
	private final AtomicBoolean ativo = new AtomicBoolean(false);
	private final Set<Socket> clientes = ConcurrentHashMap.newKeySet();
	private volatile ServerSocket servidor;
	private volatile Thread thread;

	public Gt06Servidor(Gt06Properties properties, Gt06IngestaoService ingestaoService) {
		this.properties = properties;
		this.ingestaoService = ingestaoService;
	}

	@Override
	public void start() {
		if (!ativo.compareAndSet(false, true)) {
			return;
		}
		thread = Thread.ofPlatform().name("gt06-tcp").daemon(true).start(this::loop);
	}

	@Override
	public void stop() {
		ativo.set(false);
		fechar(servidor);
		clientes.forEach(this::fechar);
		Thread atual = thread;
		if (atual != null) {
			atual.interrupt();
		}
	}

	@Override
	public boolean isRunning() {
		return ativo.get();
	}

	@Override
	public int getPhase() {
		return Integer.MAX_VALUE;
	}

	public int portaLocal() {
		ServerSocket atual = servidor;
		if (atual == null || atual.isClosed()) {
			return -1;
		}
		return atual.getLocalPort();
	}

	private void loop() {
		while (ativo.get()) {
			ServerSocket escuta;
			try {
				escuta = new ServerSocket();
				escuta.setReuseAddress(true);
				escuta.bind(new InetSocketAddress(properties.getHost(), properties.getPort()));
				servidor = escuta;
				LOGGER.info("Receptor GT06 escutando {}:{}", properties.getHost(), escuta.getLocalPort());
			} catch (IOException excecao) {
				if (!ativo.get()) {
					return;
				}
				LOGGER.warn(
					"Receptor GT06 não abriu {}:{} ({}). Feche a sonda tools/gps-tcp-probe se ela estiver na mesma porta.",
					properties.getHost(),
					properties.getPort(),
					excecao.getMessage()
				);
				if (!dormir()) {
					return;
				}
				continue;
			}
			aceitar(escuta);
			fechar(escuta);
			if (servidor == escuta) {
				servidor = null;
			}
		}
	}

	private void aceitar(ServerSocket escuta) {
		while (ativo.get() && !escuta.isClosed()) {
			try {
				Socket socket = escuta.accept();
				socket.setTcpNoDelay(true);
				socket.setKeepAlive(true);
				clientes.add(socket);
				Thread.ofVirtual().name("gt06-cliente").start(() -> atender(socket));
			} catch (SocketException excecao) {
				if (ativo.get() && !escuta.isClosed()) {
					LOGGER.warn("Accept GT06 interrompido: {}", excecao.getMessage());
				}
				return;
			} catch (IOException excecao) {
				if (ativo.get()) {
					LOGGER.warn("Falha ao aceitar GPS: {}", excecao.getMessage());
				}
				return;
			}
		}
	}

	private void atender(Socket socket) {
		String origem = socket.getRemoteSocketAddress().toString();
		String imei = null;
		int avisos = 0;
		LOGGER.info("GPS conectado {}", origem);
		Gt06Fluxo fluxo = new Gt06Fluxo();
		byte[] buffer = new byte[1024];
		try (Socket cliente = socket) {
			InputStream entrada = cliente.getInputStream();
			OutputStream saida = cliente.getOutputStream();
			int lidos;
			while ((lidos = entrada.read(buffer)) >= 0) {
				fluxo.acrescentar(buffer, 0, lidos);
				var quadros = fluxo.drenar();
				for (byte[] rejeitado : fluxo.rejeitados()) {
					if (avisos < 5) {
						LOGGER.warn("Quadro GT06 rejeitado de {}: {}", origem, Gt06Protocolo.hex(rejeitado, 48));
						avisos++;
					}
				}
				fluxo.limparRejeitados();
				for (Gt06Quadro quadro : quadros) {
					if (Gt06Protocolo.exigeAck(quadro.protocolo())) {
						saida.write(Gt06Protocolo.ack(quadro.protocolo(), quadro.serial()));
						saida.flush();
					}
					var login = Gt06Protocolo.imei(quadro);
					if (login.isPresent()) {
						imei = login.get();
						LOGGER.info("GT06 login {} IMEI {}", origem, imei);
					}
					var ponto = Gt06Protocolo.posicao(quadro);
					if (ponto.isEmpty()) {
						continue;
					}
					if (imei == null) {
						LOGGER.warn("Localização GT06 sem login prévio de {}", origem);
						continue;
					}
					try {
						ingestaoService.ingerir(imei, ponto.get());
					} catch (RuntimeException excecao) {
						LOGGER.error("Falha ao gravar posição GT06 {}: {}", imei, excecao.getMessage());
					}
				}
			}
		} catch (IOException excecao) {
			if (ativo.get()) {
				LOGGER.debug("Leitura GT06 encerrada {}: {}", origem, excecao.getMessage());
			}
		} catch (RuntimeException excecao) {
			LOGGER.error("Falha ao tratar GPS {}: {}", origem, excecao.getMessage());
		} finally {
			clientes.remove(socket);
			LOGGER.info("Conexão GT06 encerrada {}", origem);
		}
	}

	private boolean dormir() {
		try {
			Thread.sleep(5_000);
			return true;
		} catch (InterruptedException excecao) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private void fechar(ServerSocket socket) {
		if (socket == null) {
			return;
		}
		try {
			socket.close();
		} catch (IOException excecao) {
			LOGGER.debug("Falha ao fechar escuta GT06: {}", excecao.getMessage());
		}
	}

	private void fechar(Socket socket) {
		try {
			socket.close();
		} catch (IOException excecao) {
			LOGGER.debug("Falha ao fechar cliente GT06: {}", excecao.getMessage());
		}
	}
}

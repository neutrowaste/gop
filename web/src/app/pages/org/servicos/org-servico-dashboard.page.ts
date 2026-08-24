import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, Injector, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EMPTY, catchError, forkJoin, interval, map, of, startWith, switchMap } from 'rxjs';

import { MapaOperacionalComponent } from '../../../components/mapa-operacional/mapa-operacional.component';
import {
  Escala,
  Ocorrencia,
  Setor,
  StatusTrecho,
  TIPO_OCORRENCIA_LABELS,
  TURNO_LABELS,
} from '../../../models/cadastros-operacionais';
import {
  MapaAtivo,
  MapaOperacional,
  MapaTrecho,
  SITUACAO_SINAL_CORES,
  SITUACAO_SINAL_LABELS,
  STATUS_TRECHO_CORES,
  SituacaoSinal,
} from '../../../models/mapa-operacional';
import { EmpresaModulo } from '../../../models/organizacao';
import { CadastrosOperacionaisApiService } from '../../../services/cadastros-operacionais-api.service';
import { hojeIso } from '../../../services/data-local';
import { mensagemHttp } from '../../../services/http-erro';
import { MapaOperacionalApiService } from '../../../services/mapa-operacional-api.service';
import { OrganizacaoApiService } from '../../../services/organizacao-api.service';
import { NOMES_SERVICO } from './org-servico-placeholder.page';

const INTERVALO_POLLING_MS = 15_000;
const RING_RAIO = 19;
const CONFORMES: StatusTrecho[] = ['EXECUTADO', 'JUSTIFICADO'];
const PENDENTES_TABELA: StatusTrecho[] = ['PENDENTE', 'SUSPEITO', 'FORA_DA_ROTA', 'PLANEJADO'];

export type FiltroKpiMapa = 'all' | 'conformidade' | 'EXECUTADO' | 'SUSPEITO' | 'PENDENTE';

export const STATUS_TRECHO_LABELS: Record<StatusTrecho, string> = {
  PLANEJADO: 'Planejado',
  EXECUTADO: 'Executado',
  SUSPEITO: 'Suspeito',
  PENDENTE: 'Pendente',
  JUSTIFICADO: 'Justificado',
  FORA_DA_ROTA: 'Fora da rota',
};

export interface ResumoDashboard {
  previstos: number;
  executados: number;
  suspeitos: number;
  pendentes: number;
  planejados: number;
  justificados: number;
  foraDaRota: number;
  conformidade: number;
  riscoGlosa: number;
}

export interface EscalaPainel {
  escalaId: number;
  nome: string;
  setorNome: string;
  turnoLabel: string;
  equipamentoCodigo: string;
  percentual: number;
  executados: number;
  total: number;
  status: Escala['status'] | null;
  situacaoSinal: SituacaoSinal | null;
}

@Component({
  selector: 'app-org-servico-dashboard-page',
  imports: [DatePipe, DecimalPipe, RouterLink, MapaOperacionalComponent],
  templateUrl: './org-servico-dashboard.page.html',
  host: {
    class: 'block h-full min-h-0',
  },
})
export class OrgServicoDashboardPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly mapaApi = inject(MapaOperacionalApiService);
  private readonly cadastrosApi = inject(CadastrosOperacionaisApiService);
  private readonly orgApi = inject(OrganizacaoApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);

  private readonly codigoParam = toSignal(
    this.route.paramMap.pipe(map((params) => (params.get('codigo') ?? '').toUpperCase())),
    { initialValue: (this.route.snapshot.paramMap.get('codigo') ?? '').toUpperCase() },
  );

  readonly codigo = computed(() => this.codigoParam());
  readonly nome = computed(
    () => this.modulo()?.nome || NOMES_SERVICO[this.codigo()] || this.codigo(),
  );

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly dados = signal<MapaOperacional | null>(null);
  readonly escalas = signal<Escala[]>([]);
  readonly ocorrencias = signal<Ocorrencia[]>([]);
  readonly modulo = signal<EmpresaModulo | null>(null);
  readonly setorIds = signal<Set<number> | null>(null);
  readonly ultimaAtualizacaoEm = signal<Date | null>(null);
  readonly dataSelecionada = signal<string>(hojeIso());
  readonly filtroKpi = signal<FiltroKpiMapa>('all');

  readonly statusCores = STATUS_TRECHO_CORES;
  readonly statusLabels = STATUS_TRECHO_LABELS;
  readonly situacaoLabels = SITUACAO_SINAL_LABELS;
  readonly situacaoCores = SITUACAO_SINAL_CORES;
  readonly tipoOcorrenciaLabels = TIPO_OCORRENCIA_LABELS;
  readonly ringCirc = 2 * Math.PI * RING_RAIO;

  readonly trechosDoModulo = computed(() => {
    const trechos = this.dados()?.trechos ?? [];
    const ids = this.setorIds();
    return ids ? trechos.filter((trecho) => ids.has(trecho.setorId)) : trechos;
  });

  readonly ativosDoModulo = computed(() => {
    const ativos = this.dados()?.ativos ?? [];
    const ids = this.setorIds();
    return ids ? ativos.filter((ativo) => ids.has(ativo.setorId)) : ativos;
  });

  readonly resumo = computed(() => resumirTrechos(this.trechosDoModulo()));

  readonly dadosMapa = computed((): MapaOperacional | null => {
    const dados = this.dados();
    if (!dados) {
      return null;
    }
    return {
      ...dados,
      trechos: filtrarTrechos(this.trechosDoModulo(), this.filtroKpi()),
      ativos: this.ativosDoModulo(),
    };
  });

  readonly escalasPainel = computed(() =>
    montarEscalasPainel(this.escalas(), this.trechosDoModulo(), this.ativosDoModulo(), this.setorIds()),
  );

  readonly ocorrenciasPainel = computed(() => {
    const ids = this.setorIds();
    if (!ids) {
      return this.ocorrencias();
    }
    const escalaIds = new Set(this.escalasPainel().map((item) => item.escalaId));
    return this.ocorrencias().filter((ocorrencia) => escalaIds.has(ocorrencia.escalaId));
  });

  readonly trechosPendentes = computed(() =>
    this.trechosDoModulo()
      .filter((trecho) => PENDENTES_TABELA.includes(trecho.status) && trecho.status !== 'EXECUTADO')
      .filter((trecho) => !CONFORMES.includes(trecho.status))
      .slice()
      .sort((a, b) => a.percentualExecutado - b.percentualExecutado)
      .slice(0, 40),
  );

  readonly risco = computed(() => nivelRisco(this.resumo().riscoGlosa));
  readonly donutCss = computed(() => montarDonutCss(this.resumo()));
  readonly ativosOnline = computed(
    () => this.ativosDoModulo().filter((ativo) => ativo.situacaoSinal === 'ONLINE').length,
  );
  readonly fatiasLegenda = computed(() => {
    const resumo = this.resumo();
    return [
      { status: 'EXECUTADO' as const, valor: resumo.executados },
      { status: 'SUSPEITO' as const, valor: resumo.suspeitos },
      { status: 'PENDENTE' as const, valor: resumo.pendentes },
      { status: 'PLANEJADO' as const, valor: resumo.planejados },
    ];
  });

  ngOnInit(): void {
    toObservable(this.codigo, { injector: this.injector })
      .pipe(
        switchMap((codigo) =>
          this.orgApi.listarModulosContratados().pipe(
            catchError(() => of([] as EmpresaModulo[])),
            switchMap((modulos) => {
              const modulo = modulos.find((item) => item.codigo.toUpperCase() === codigo) ?? null;
              this.modulo.set(modulo);
              if (!modulo) {
                this.setorIds.set(null);
                return of([] as Setor[]);
              }
              return this.cadastrosApi
                .listarSetores(undefined, modulo.moduloId)
                .pipe(catchError(() => of([] as Setor[])));
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((setores) => {
        const ativos = setores.filter((setor) => setor.ativo);
        this.setorIds.set(ativos.length > 0 ? new Set(ativos.map((setor) => setor.id)) : null);
      });

    toObservable(this.dataSelecionada, { injector: this.injector })
      .pipe(
        switchMap((data) =>
          interval(INTERVALO_POLLING_MS).pipe(
            startWith(0),
            switchMap(() =>
              forkJoin({
                mapa: this.mapaApi.obter(data),
                escalas: this.cadastrosApi.listarEscalas({ data }),
                ocorrencias: this.cadastrosApi.listarOcorrencias({ data, status: 'ABERTA' }),
              }).pipe(
                catchError((erro: unknown) => {
                  this.erro.set(mensagemHttp(erro, 'Falha ao carregar o dashboard'));
                  this.carregando.set(false);
                  return EMPTY;
                }),
              ),
            ),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ mapa, escalas, ocorrencias }) => {
        this.dados.set(mapa);
        this.escalas.set(escalas);
        this.ocorrencias.set(ocorrencias);
        this.ultimaAtualizacaoEm.set(new Date());
        this.carregando.set(false);
        this.erro.set(null);
      });
  }

  onDataChange(valor: string): void {
    if (!valor || valor === this.dataSelecionada()) {
      return;
    }
    this.carregando.set(true);
    this.erro.set(null);
    this.dataSelecionada.set(valor);
  }

  voltarParaHoje(): void {
    this.onDataChange(hojeIso());
  }

  selecionarFiltro(filtro: FiltroKpiMapa): void {
    this.filtroKpi.set(this.filtroKpi() === filtro ? 'all' : filtro);
  }

  pct(parte: number, total: number): number {
    return total > 0 ? Math.round((parte / total) * 100) : 0;
  }

  ringOffset(valor: number): number {
    return this.ringCirc - (Math.min(100, valor) / 100) * this.ringCirc;
  }

  estadoEscala(item: EscalaPainel): { label: string; classe: string } {
    if (item.percentual >= 100 && item.total > 0) {
      return { label: 'Concluída', classe: 'text-emerald-600' };
    }
    if (item.situacaoSinal === 'ONLINE') {
      return { label: 'Em execução', classe: 'text-emerald-600' };
    }
    if (item.situacaoSinal === 'ATENCAO') {
      return { label: 'Atenção no GPS', classe: 'text-amber-600' };
    }
    if (item.status === 'RASCUNHO') {
      return { label: 'Rascunho', classe: 'text-slate-400' };
    }
    return { label: 'Sem sinal', classe: 'text-slate-400' };
  }

  corBarra(percentual: number): string {
    if (percentual >= 70) {
      return 'bg-emerald-500';
    }
    if (percentual >= 40) {
      return 'bg-amber-500';
    }
    return 'bg-rose-400';
  }

  motivoTrecho(trecho: MapaTrecho): string {
    if (trecho.status === 'SUSPEITO') {
      return 'Suspeito';
    }
    if (trecho.status === 'FORA_DA_ROTA') {
      return 'Fora da rota';
    }
    if (trecho.percentualExecutado > 0 && trecho.percentualExecutado < 100) {
      return `Execução parcial (${Math.round(trecho.percentualExecutado)}%)`;
    }
    return 'Não executado';
  }

  rotuloVelocidade(kmh: number | null): string {
    return kmh != null ? `${Math.round(kmh)} km/h` : '—';
  }

  corOcorrencia(tipo: Ocorrencia['tipo']): string {
    return tipo === 'GPS_SILENCIOSO' ? 'text-rose-600' : 'text-amber-600';
  }

  barraOcorrencia(tipo: Ocorrencia['tipo']): string {
    return tipo === 'GPS_SILENCIOSO' ? 'border-l-rose-500' : 'border-l-amber-500';
  }
}

function resumirTrechos(trechos: MapaTrecho[]): ResumoDashboard {
  const previstos = trechos.length;
  const executados = trechos.filter((item) => item.status === 'EXECUTADO').length;
  const suspeitos = trechos.filter((item) => item.status === 'SUSPEITO').length;
  const pendentes = trechos.filter((item) => item.status === 'PENDENTE').length;
  const planejados = trechos.filter((item) => item.status === 'PLANEJADO').length;
  const justificados = trechos.filter((item) => item.status === 'JUSTIFICADO').length;
  const foraDaRota = trechos.filter((item) => item.status === 'FORA_DA_ROTA').length;
  const conformidade =
    previstos > 0 ? Math.round(((executados + justificados) / previstos) * 100) : 0;
  const riscoGlosa = previstos > 0 ? Math.round((pendentes / previstos) * 100) : 0;
  return {
    previstos,
    executados,
    suspeitos,
    pendentes,
    planejados,
    justificados,
    foraDaRota,
    conformidade,
    riscoGlosa,
  };
}

function filtrarTrechos(trechos: MapaTrecho[], filtro: FiltroKpiMapa): MapaTrecho[] {
  if (filtro === 'all') {
    return trechos;
  }
  if (filtro === 'conformidade') {
    return trechos.filter((trecho) => CONFORMES.includes(trecho.status));
  }
  return trechos.filter((trecho) => trecho.status === filtro);
}

function montarEscalasPainel(
  escalas: Escala[],
  trechos: MapaTrecho[],
  ativos: MapaAtivo[],
  setorIds: Set<number> | null,
): EscalaPainel[] {
  const porEscala = new Map<number, MapaTrecho[]>();
  for (const trecho of trechos) {
    const lista = porEscala.get(trecho.escalaId) ?? [];
    lista.push(trecho);
    porEscala.set(trecho.escalaId, lista);
  }

  const ativosPorEscala = new Map<number, MapaAtivo[]>();
  for (const ativo of ativos) {
    const lista = ativosPorEscala.get(ativo.escalaId) ?? [];
    lista.push(ativo);
    ativosPorEscala.set(ativo.escalaId, lista);
  }

  const ids = new Set<number>();
  for (const escala of escalas) {
    if (!setorIds || setorIds.has(escala.setorId)) {
      ids.add(escala.id);
    }
  }
  for (const trecho of trechos) {
    ids.add(trecho.escalaId);
  }
  for (const ativo of ativos) {
    ids.add(ativo.escalaId);
  }

  const porId = new Map(escalas.map((escala) => [escala.id, escala]));
  const painel: EscalaPainel[] = [];
  for (const escalaId of ids) {
    const escala = porId.get(escalaId);
    const trechosEscala = porEscala.get(escalaId) ?? [];
    const ativosEscala = ativosPorEscala.get(escalaId) ?? [];
    const total = trechosEscala.length;
    const executados = trechosEscala.filter((item) => item.status === 'EXECUTADO').length;
    const percentual =
      total > 0
        ? Math.round(trechosEscala.reduce((acc, item) => acc + item.percentualExecutado, 0) / total)
        : 0;
    const nome =
      escala?.colaboradores.map((item) => item.nome).join(', ') ||
      trechosEscala[0]?.colaboradoresNomes ||
      ativosEscala[0]?.colaboradoresNomes ||
      escala?.setorNome ||
      `Escala #${escalaId}`;
    painel.push({
      escalaId,
      nome,
      setorNome: escala?.setorNome || trechosEscala[0]?.setorNome || ativosEscala[0]?.setorNome || '—',
      turnoLabel: escala ? TURNO_LABELS[escala.turno] : ativosEscala[0] ? TURNO_LABELS[ativosEscala[0].turno] : '—',
      equipamentoCodigo:
        [...new Set((escala?.colaboradores ?? []).map((item) => item.equipamentoCodigo))].join(', ') ||
        ativosEscala[0]?.equipamentoCodigo ||
        '—',
      percentual,
      executados,
      total,
      status: escala?.status ?? null,
      situacaoSinal: piorSinal(ativosEscala.map((item) => item.situacaoSinal)),
    });
  }

  return painel.sort((a, b) => a.setorNome.localeCompare(b.setorNome, 'pt-BR'));
}

function piorSinal(sinais: SituacaoSinal[]): SituacaoSinal | null {
  if (sinais.includes('OFFLINE')) {
    return 'OFFLINE';
  }
  if (sinais.includes('ATENCAO')) {
    return 'ATENCAO';
  }
  if (sinais.includes('ONLINE')) {
    return 'ONLINE';
  }
  return null;
}

function nivelRisco(risco: number): { label: string; classe: string; fundo: string } {
  if (risco >= 25) {
    return { label: 'ALTO', classe: 'text-rose-600', fundo: 'bg-rose-50' };
  }
  if (risco >= 10) {
    return { label: 'MÉDIO', classe: 'text-amber-600', fundo: 'bg-amber-50' };
  }
  return { label: 'BAIXO', classe: 'text-emerald-600', fundo: 'bg-emerald-50' };
}

function montarDonutCss(resumo: ResumoDashboard): string {
  const total = resumo.previstos || 1;
  const fatias = [
    { cor: STATUS_TRECHO_CORES.EXECUTADO, valor: resumo.executados },
    { cor: STATUS_TRECHO_CORES.SUSPEITO, valor: resumo.suspeitos },
    { cor: STATUS_TRECHO_CORES.PENDENTE, valor: resumo.pendentes },
    { cor: STATUS_TRECHO_CORES.JUSTIFICADO, valor: resumo.justificados },
    { cor: STATUS_TRECHO_CORES.FORA_DA_ROTA, valor: resumo.foraDaRota },
    { cor: STATUS_TRECHO_CORES.PLANEJADO, valor: resumo.planejados },
  ];
  let acumulado = 0;
  const paradas: string[] = [];
  for (const fatia of fatias) {
    if (fatia.valor <= 0) {
      continue;
    }
    const inicio = acumulado;
    acumulado += (fatia.valor / total) * 100;
    paradas.push(`${fatia.cor} ${inicio}% ${acumulado}%`);
  }
  if (paradas.length === 0) {
    return `conic-gradient(${STATUS_TRECHO_CORES.PLANEJADO} 0% 100%)`;
  }
  return `conic-gradient(${paradas.join(', ')})`;
}

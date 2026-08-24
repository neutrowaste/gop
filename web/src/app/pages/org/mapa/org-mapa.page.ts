import { DatePipe } from '@angular/common';
import { Component, DestroyRef, Injector, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, interval, startWith, switchMap } from 'rxjs';

import { MapaOperacionalComponent } from '../../../components/mapa-operacional/mapa-operacional.component';
import { MapaOperacional } from '../../../models/mapa-operacional';
import { hojeIso } from '../../../services/data-local';
import { MapaOperacionalApiService } from '../../../services/mapa-operacional-api.service';
import { mensagemHttp } from '../../../services/http-erro';

const INTERVALO_POLLING_MS = 15_000;

/**
 * Mapa operacional (RN-001): posições dos ativos + trechos do plano da data
 * selecionada, atualizado por polling (sem WebSocket nesta fase). Reaproveitada
 * tanto em /org/mapa (admin empresa) quanto em /app/modulos/MONITORAMENTO
 * (funcionário com o módulo liberado) — o controller do backend decide o acesso.
 */
@Component({
  selector: 'app-org-mapa-page',
  imports: [DatePipe, MapaOperacionalComponent],
  templateUrl: './org-mapa.page.html',
})
export class OrgMapaPage implements OnInit {
  private readonly api = inject(MapaOperacionalApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly dados = signal<MapaOperacional | null>(null);
  readonly ultimaAtualizacaoEm = signal<Date | null>(null);
  readonly dataSelecionada = signal<string>(hojeIso());

  ngOnInit(): void {
    toObservable(this.dataSelecionada, { injector: this.injector })
      .pipe(
        switchMap((data) =>
          interval(INTERVALO_POLLING_MS).pipe(
            startWith(0),
            switchMap(() =>
              this.api.obter(data).pipe(
                catchError((erro: unknown) => {
                  this.erro.set(mensagemHttp(erro, 'Falha ao carregar o mapa operacional'));
                  this.carregando.set(false);
                  return EMPTY;
                }),
              ),
            ),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((dados) => {
        this.dados.set(dados);
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
}

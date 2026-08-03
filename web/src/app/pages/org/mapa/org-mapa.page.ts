import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, interval, startWith, switchMap } from 'rxjs';

import { MapaOperacionalComponent } from '../../../components/mapa-operacional/mapa-operacional.component';
import { MapaOperacional } from '../../../models/mapa-operacional';
import { MapaOperacionalApiService } from '../../../services/mapa-operacional-api.service';
import { mensagemHttp } from '../../../services/http-erro';

const INTERVALO_POLLING_MS = 15_000;

/**
 * Mapa operacional (RN-001): posições dos ativos + trechos do plano do dia,
 * atualizado por polling (sem WebSocket nesta fase). Reaproveitada tanto em
 * /org/mapa (admin empresa) quanto em /app/modulos/MONITORAMENTO (funcionário
 * com o módulo liberado) — o controller do backend decide o acesso.
 */
@Component({
  selector: 'app-org-mapa-page',
  imports: [DatePipe, MapaOperacionalComponent],
  templateUrl: './org-mapa.page.html',
})
export class OrgMapaPage implements OnInit {
  private readonly api = inject(MapaOperacionalApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly dados = signal<MapaOperacional | null>(null);
  readonly ultimaAtualizacaoEm = signal<Date | null>(null);

  ngOnInit(): void {
    interval(INTERVALO_POLLING_MS)
      .pipe(
        startWith(0),
        switchMap(() =>
          this.api.obter().pipe(
            catchError((erro: unknown) => {
              this.erro.set(mensagemHttp(erro, 'Falha ao carregar o mapa operacional'));
              this.carregando.set(false);
              return EMPTY;
            }),
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
}

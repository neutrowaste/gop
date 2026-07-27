import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';

const NOMES: Record<string, string> = {
  ESCALA: 'Escala Diária',
  VARRIACAO_VIAS: 'Varrição de Vias',
  COLETA_PAPELEIRAS: 'Coleta de Papeleiras',
  MONITORAMENTO: 'Monitoramento e Mapa',
  AUDITORIA: 'Auditoria e Informe',
};

@Component({
  selector: 'app-funcionario-modulo-placeholder-page',
  imports: [RouterLink],
  templateUrl: './app-modulo-placeholder.page.html',
})
export class AppModuloPlaceholderPage {
  private readonly route = inject(ActivatedRoute);

  private readonly codigoParam = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('codigo') ?? '')),
    { initialValue: this.route.snapshot.paramMap.get('codigo') ?? '' },
  );

  readonly codigo = computed(() => this.codigoParam().toUpperCase());
  readonly nome = computed(() => NOMES[this.codigo()] ?? this.codigo());
}

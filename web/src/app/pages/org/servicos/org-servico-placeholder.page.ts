import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';

export const NOMES_SERVICO: Record<string, string> = {
  ESCALA: 'Escala Diária',
  VARRIACAO_VIAS: 'Varrição de Vias',
  COLETA_PAPELEIRAS: 'Coleta de Papeleiras',
  MONITORAMENTO: 'Monitoramento e Mapa',
  AUDITORIA: 'Auditoria e Informe',
};

@Component({
  selector: 'app-org-servico-placeholder-page',
  imports: [RouterLink],
  templateUrl: './org-servico-placeholder.page.html',
})
export class OrgServicoPlaceholderPage {
  private readonly route = inject(ActivatedRoute);

  private readonly codigoParam = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('codigo') ?? '')),
    { initialValue: this.route.snapshot.paramMap.get('codigo') ?? '' },
  );

  readonly codigo = computed(() => this.codigoParam().toUpperCase());
  readonly nome = computed(() => NOMES_SERVICO[this.codigo()] ?? this.codigo());
}

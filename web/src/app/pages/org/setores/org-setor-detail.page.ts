import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Cidade, Setor } from '../../../models/cadastros-operacionais';
import { EmpresaModulo } from '../../../models/organizacao';
import { EditorPoligonoSetorComponent } from '../../../components/editor-poligono-setor/editor-poligono-setor.component';
import { CadastrosOperacionaisApiService } from '../../../services/cadastros-operacionais-api.service';
import { mensagemHttp } from '../../../services/http-erro';
import { OrganizacaoApiService } from '../../../services/organizacao-api.service';

@Component({
  selector: 'app-org-setor-detail-page',
  imports: [ReactiveFormsModule, RouterLink, EditorPoligonoSetorComponent],
  templateUrl: './org-setor-detail.page.html',
})
export class OrgSetorDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(CadastrosOperacionaisApiService);
  private readonly orgApi = inject(OrganizacaoApiService);

  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly ok = signal<string | null>(null);
  readonly setor = signal<Setor | null>(null);
  readonly cidades = signal<Cidade[]>([]);
  readonly servicos = signal<EmpresaModulo[]>([]);
  readonly arquivoDemarcacao = signal<File | null>(null);
  readonly enviandoDemarcacao = signal(false);
  readonly geomPreview = signal<unknown>(null);
  readonly previewandoShapefile = signal(false);
  readonly opcoesServico = computed(() => {
    const lista = [...this.servicos()];
    const setor = this.setor();
    if (setor && !lista.some((item) => item.moduloId === setor.moduloId)) {
      lista.unshift({
        moduloId: setor.moduloId,
        codigo: setor.moduloCodigo,
        nome: setor.moduloNome,
        categoria: '',
        ordem: 0,
        habilitado: true,
        desde: null,
      });
    }
    return lista;
  });

  readonly form = this.fb.nonNullable.group({
    moduloId: [0, [Validators.required, Validators.min(1)]],
    cidadeId: [0, [Validators.required, Validators.min(1)]],
    codigo: [''],
    nome: ['', [Validators.required, Validators.maxLength(200)]],
    ativo: [true],
  });

  private id = 0;

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.id) {
      this.erro.set('Setor inválido');
      this.carregando.set(false);
      return;
    }
    this.api.listarCidades().subscribe({
      next: (cidades) => this.cidades.set(cidades.filter((c) => c.ativo)),
    });
    this.orgApi.listarModulosContratados().subscribe({
      next: (modulos) => this.servicos.set(modulos),
    });
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.api.buscarSetor(this.id).subscribe({
      next: (setor) => {
        this.setor.set(setor);
        this.form.patchValue({
          moduloId: setor.moduloId,
          cidadeId: setor.cidadeId,
          codigo: setor.codigo ?? '',
          nome: setor.nome,
          ativo: setor.ativo,
        });
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao carregar setor'));
        this.carregando.set(false);
      },
    });
  }

  salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.salvando.set(true);
    this.erro.set(null);
    this.ok.set(null);
    const raw = this.form.getRawValue();
    this.api
      .atualizarSetor(this.id, {
        moduloId: Number(raw.moduloId),
        cidadeId: Number(raw.cidadeId),
        codigo: raw.codigo.trim() || null,
        nome: raw.nome.trim(),
        ativo: raw.ativo,
      })
      .subscribe({
        next: (setor) => {
          this.setor.set(setor);
          this.ok.set('Setor atualizado.');
          this.salvando.set(false);
        },
        error: (erro: unknown) => {
          this.erro.set(mensagemHttp(erro, 'Falha ao atualizar setor'));
          this.salvando.set(false);
        },
      });
  }

  desativar(): void {
    if (!confirm('Desativar este setor?')) {
      return;
    }
    this.api.desativarSetor(this.id).subscribe({
      next: () => void this.router.navigateByUrl('/org/setores'),
      error: (erro: unknown) => this.erro.set(mensagemHttp(erro, 'Falha ao desativar')),
    });
  }

  onArquivoDemarcacao(event: Event): void {
    const input = event.target as HTMLInputElement;
    const arquivo = input.files?.[0] ?? null;
    this.arquivoDemarcacao.set(arquivo);
    this.geomPreview.set(null);
    if (!arquivo) {
      return;
    }
    this.previewandoShapefile.set(true);
    this.erro.set(null);
    this.ok.set(null);
    this.api.preVisualizarShapefileSetor(arquivo).subscribe({
      next: (preview) => {
        this.geomPreview.set(preview.geomGeoJson);
        this.previewandoShapefile.set(false);
      },
      error: (erro: unknown) => {
        this.arquivoDemarcacao.set(null);
        input.value = '';
        this.previewandoShapefile.set(false);
        this.erro.set(mensagemHttp(erro, 'Falha ao ler o shapefile'));
      },
    });
  }

  enviarDemarcacao(): void {
    const arquivo = this.arquivoDemarcacao();
    if (!arquivo) {
      this.erro.set('Selecione um ZIP com o shapefile de área');
      return;
    }
    this.enviandoDemarcacao.set(true);
    this.erro.set(null);
    this.ok.set(null);
    this.api.importarShapefileSetor(this.id, arquivo).subscribe({
      next: (setor) => {
        this.setor.set(setor);
        this.arquivoDemarcacao.set(null);
        this.geomPreview.set(null);
        this.ok.set('Demarcação importada.');
        this.enviandoDemarcacao.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao importar a demarcação'));
        this.enviandoDemarcacao.set(false);
      },
    });
  }

  removerDemarcacao(): void {
    if (!confirm('Remover a demarcação em área deste setor?')) {
      return;
    }
    this.enviandoDemarcacao.set(true);
    this.erro.set(null);
    this.ok.set(null);
    this.api.removerDemarcacaoSetor(this.id).subscribe({
      next: (setor) => {
        this.setor.set(setor);
        this.geomPreview.set(null);
        this.ok.set('Demarcação removida.');
        this.enviandoDemarcacao.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao remover a demarcação'));
        this.enviandoDemarcacao.set(false);
      },
    });
  }

  onPoligonoConfirmado(wkt: string): void {
    this.enviandoDemarcacao.set(true);
    this.erro.set(null);
    this.ok.set(null);
    this.arquivoDemarcacao.set(null);
    this.api.atualizarDemarcacaoSetor(this.id, wkt).subscribe({
      next: (setor) => {
        this.setor.set(setor);
        this.geomPreview.set(null);
        this.ok.set('Polígono confirmado e salvo como demarcação.');
        this.enviandoDemarcacao.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao salvar o polígono'));
        this.enviandoDemarcacao.set(false);
      },
    });
  }
}

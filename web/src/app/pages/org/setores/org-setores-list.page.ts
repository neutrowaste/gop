import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable, of, switchMap } from 'rxjs';

import { Cidade, Setor } from '../../../models/cadastros-operacionais';
import { EmpresaModulo } from '../../../models/organizacao';
import { EditorPoligonoSetorComponent } from '../../../components/editor-poligono-setor/editor-poligono-setor.component';
import { CadastrosOperacionaisApiService } from '../../../services/cadastros-operacionais-api.service';
import { mensagemHttp } from '../../../services/http-erro';
import { OrganizacaoApiService } from '../../../services/organizacao-api.service';

@Component({
  selector: 'app-org-setores-list-page',
  imports: [ReactiveFormsModule, RouterLink, EditorPoligonoSetorComponent],
  templateUrl: './org-setores-list.page.html',
})
export class OrgSetoresListPage implements OnInit {
  private readonly api = inject(CadastrosOperacionaisApiService);
  private readonly orgApi = inject(OrganizacaoApiService);
  private readonly fb = inject(FormBuilder);

  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly setores = signal<Setor[]>([]);
  readonly cidades = signal<Cidade[]>([]);
  readonly servicos = signal<EmpresaModulo[]>([]);
  readonly mostrarForm = signal(false);
  readonly novaCidade = signal(false);
  readonly arquivoShapefile = signal<File | null>(null);
  readonly poligonoWkt = signal<string | null>(null);
  readonly geomPreview = signal<unknown>(null);
  readonly previewandoShapefile = signal(false);

  readonly filtro = this.fb.nonNullable.group({
    moduloId: [0],
  });

  readonly form = this.fb.nonNullable.group({
    moduloId: [0, [Validators.required, Validators.min(1)]],
    cidadeId: [0],
    novaCidadeNome: [''],
    novaCidadeUf: ['RJ'],
    codigo: [''],
    nome: ['', [Validators.required, Validators.maxLength(200)]],
  });

  ngOnInit(): void {
    this.carregarCidades();
    this.carregarServicos();
    this.carregar();
    this.filtro.controls.moduloId.valueChanges.subscribe(() => this.carregar());
  }

  carregarCidades(): void {
    this.api.listarCidades().subscribe({
      next: (cidades) => {
        const ativas = cidades.filter((c) => c.ativo);
        this.cidades.set(ativas);
        this.novaCidade.set(ativas.length === 0);
      },
    });
  }

  carregarServicos(): void {
    this.orgApi.listarModulosContratados().subscribe({
      next: (modulos) => this.servicos.set(modulos),
    });
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    const moduloId = this.filtro.getRawValue().moduloId || undefined;
    this.api.listarSetores(undefined, moduloId).subscribe({
      next: (lista) => {
        this.setores.set(lista);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao listar setores'));
        this.carregando.set(false);
      },
    });
  }

  abrirForm(): void {
    const ativas = this.cidades();
    const servicos = this.servicos();
    this.novaCidade.set(ativas.length === 0);
    this.form.reset({
      moduloId: servicos[0]?.moduloId ?? 0,
      cidadeId: ativas[0]?.id ?? 0,
      novaCidadeNome: '',
      novaCidadeUf: 'RJ',
      codigo: '',
      nome: '',
    });
    this.arquivoShapefile.set(null);
    this.poligonoWkt.set(null);
    this.geomPreview.set(null);
    this.mostrarForm.set(true);
  }

  fecharForm(): void {
    this.mostrarForm.set(false);
    this.arquivoShapefile.set(null);
    this.poligonoWkt.set(null);
    this.geomPreview.set(null);
  }

  onArquivoSelecionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    const arquivo = input.files?.[0] ?? null;
    this.arquivoShapefile.set(arquivo);
    this.poligonoWkt.set(null);
    this.geomPreview.set(null);
    if (!arquivo) {
      return;
    }
    this.previewandoShapefile.set(true);
    this.erro.set(null);
    this.api.preVisualizarShapefileSetor(arquivo).subscribe({
      next: (preview) => {
        this.geomPreview.set(preview.geomGeoJson);
        this.previewandoShapefile.set(false);
      },
      error: (erro: unknown) => {
        this.arquivoShapefile.set(null);
        input.value = '';
        this.previewandoShapefile.set(false);
        this.erro.set(mensagemHttp(erro, 'Falha ao ler o shapefile'));
      },
    });
  }

  onPoligonoConfirmado(wkt: string): void {
    this.poligonoWkt.set(wkt);
    this.arquivoShapefile.set(null);
    this.geomPreview.set(null);
  }

  onPoligonoLimpo(): void {
    this.poligonoWkt.set(null);
    this.geomPreview.set(null);
    this.arquivoShapefile.set(null);
  }

  alternarNovaCidade(criar: boolean): void {
    this.novaCidade.set(criar);
  }

  criar(): void {
    this.erro.set(null);
    const raw = this.form.getRawValue();
    if (!raw.moduloId) {
      this.erro.set('Selecione o serviço contratado');
      this.form.controls.moduloId.markAsTouched();
      return;
    }
    if (!raw.nome.trim()) {
      this.form.controls.nome.markAsTouched();
      return;
    }

    this.salvando.set(true);

    const criarSetor = (cidadeId: number): Observable<Setor> =>
      this.api
        .criarSetor({
          moduloId: Number(raw.moduloId),
          cidadeId,
          codigo: raw.codigo.trim() || null,
          nome: raw.nome.trim(),
          ativo: true,
        })
        .pipe(switchMap((setor) => this.aplicarDemarcacao(setor)));

    if (this.novaCidade()) {
      const nomeCidade = raw.novaCidadeNome.trim();
      if (!nomeCidade) {
        this.erro.set('Informe o município do setor');
        this.salvando.set(false);
        return;
      }
      this.api
        .criarCidade({
          nome: nomeCidade,
          uf: raw.novaCidadeUf.trim().toUpperCase() || 'RJ',
          ativo: true,
        })
        .pipe(switchMap((cidade) => criarSetor(cidade.id)))
        .subscribe({
          next: () => {
            this.salvando.set(false);
            this.fecharForm();
            this.carregarCidades();
            this.carregar();
          },
          error: (erro: unknown) => {
            this.salvando.set(false);
            this.erro.set(this.mensagemFalhaCriacao(erro));
          },
        });
      return;
    }

    if (!raw.cidadeId) {
      this.erro.set('Selecione o município ou cadastre um novo');
      this.salvando.set(false);
      return;
    }

    criarSetor(Number(raw.cidadeId)).subscribe({
      next: () => {
        this.salvando.set(false);
        this.fecharForm();
        this.carregar();
      },
      error: (erro: unknown) => {
        this.salvando.set(false);
        this.erro.set(this.mensagemFalhaCriacao(erro));
      },
    });
  }

  private aplicarDemarcacao(setor: Setor): Observable<Setor> {
    const arquivo = this.arquivoShapefile();
    if (arquivo) {
      return this.api.importarShapefileSetor(setor.id, arquivo);
    }
    const wkt = this.poligonoWkt();
    if (wkt) {
      return this.api.atualizarDemarcacaoSetor(setor.id, wkt);
    }
    return of(setor);
  }

  private mensagemFalhaCriacao(erro: unknown): string {
    if (this.arquivoShapefile() || this.poligonoWkt()) {
      return mensagemHttp(
        erro,
        'Setor criado, mas a demarcação não foi salva. Abra o setor para enviar a área.',
      );
    }
    return mensagemHttp(erro, 'Falha ao criar setor');
  }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import {
  Colaborador,
  Equipamento,
  Equipe,
  Setor,
} from '../../../models/cadastros-operacionais';
import { TetoAtivos } from '../../../models/licenca';
import { CadastrosOperacionaisApiService } from '../../../services/cadastros-operacionais-api.service';
import { mensagemHttp } from '../../../services/http-erro';
import { OrganizacaoApiService } from '../../../services/organizacao-api.service';

type AbaEquipes = 'equipes' | 'garis' | 'lutocares';

/**
 * Paridade com /equipes do legado NeutroWaste:
 * uma tela com abas Equipes | Garis | Lutocares (não menus separados).
 */
@Component({
  selector: 'app-org-equipes-list-page',
  imports: [ReactiveFormsModule],
  templateUrl: './org-equipes-list.page.html',
})
export class OrgEquipesListPage implements OnInit {
  private readonly api = inject(CadastrosOperacionaisApiService);
  private readonly orgApi = inject(OrganizacaoApiService);
  private readonly fb = inject(FormBuilder);

  readonly aba = signal<AbaEquipes>('equipes');
  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly ok = signal<string | null>(null);

  readonly setores = signal<Setor[]>([]);
  readonly equipes = signal<Equipe[]>([]);
  readonly garis = signal<Colaborador[]>([]);
  readonly lutocares = signal<Equipamento[]>([]);
  readonly teto = signal<TetoAtivos | null>(null);
  readonly tetoErro = signal<string | null>(null);

  readonly mostrarFormEquipe = signal(false);
  readonly mostrarFormGari = signal(false);
  readonly mostrarFormLutocar = signal(false);
  readonly editandoEquipeId = signal<number | null>(null);
  readonly editandoGariId = signal<number | null>(null);
  readonly editandoLutocarId = signal<number | null>(null);

  readonly formEquipe = this.fb.nonNullable.group({
    setorId: [0, [Validators.required, Validators.min(1)]],
    nome: ['', [Validators.required, Validators.maxLength(200)]],
  });

  readonly formGari = this.fb.nonNullable.group({
    equipeId: [0, [Validators.required, Validators.min(1)]],
    nome: ['', [Validators.required, Validators.maxLength(200)]],
    matricula: ['', [Validators.required, Validators.maxLength(60)]],
  });

  readonly formLutocar = this.fb.nonNullable.group({
    equipeId: [0, [Validators.required, Validators.min(1)]],
    codigo: ['', [Validators.required, Validators.maxLength(60)]],
    descricao: [''],
  });

  ngOnInit(): void {
    this.recarregarTudo();
  }

  selecionarAba(aba: AbaEquipes): void {
    this.aba.set(aba);
    this.erro.set(null);
    this.ok.set(null);
  }

  recarregarTudo(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.api.listarSetores().subscribe({
      next: (setores) => this.setores.set(setores.filter((s) => s.ativo)),
    });
    this.api.listarEquipes().subscribe({
      next: (equipes) => {
        this.equipes.set(equipes);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao listar equipes'));
        this.carregando.set(false);
      },
    });
    this.api.listarColaboradores().subscribe({
      next: (lista) => this.garis.set(lista),
      error: () => undefined,
    });
    this.api.listarEquipamentos().subscribe({
      next: (lista) => this.lutocares.set(lista),
      error: () => undefined,
    });
    this.orgApi.consultarTeto().subscribe({
      next: (teto) => {
        this.teto.set(teto);
        this.tetoErro.set(null);
      },
      error: (erro: unknown) => {
        this.teto.set(null);
        this.tetoErro.set(mensagemHttp(erro, 'Sem licença vigente para Lutocares'));
      },
    });
  }

  podeCadastrarLutocar(): boolean {
    const t = this.teto();
    return t != null && t.permiteNovoAtivo;
  }

  abrirFormEquipe(equipe?: Equipe): void {
    if (equipe) {
      this.editandoEquipeId.set(equipe.id);
      this.formEquipe.reset({ setorId: equipe.setorId, nome: equipe.nome });
    } else {
      this.editandoEquipeId.set(null);
      this.formEquipe.reset({
        setorId: this.setores()[0]?.id ?? 0,
        nome: '',
      });
    }
    this.mostrarFormEquipe.set(true);
  }

  fecharFormEquipe(): void {
    this.mostrarFormEquipe.set(false);
    this.editandoEquipeId.set(null);
  }

  salvarEquipe(): void {
    if (this.formEquipe.invalid) {
      this.formEquipe.markAllAsTouched();
      return;
    }
    this.salvando.set(true);
    this.erro.set(null);
    const raw = this.formEquipe.getRawValue();
    const body = {
      setorId: Number(raw.setorId),
      nome: raw.nome.trim(),
      ativo: true,
    };
    const id = this.editandoEquipeId();
    const req$ = id == null
      ? this.api.criarEquipe(body)
      : this.api.atualizarEquipe(id, body);
    req$.subscribe({
      next: () => {
        this.salvando.set(false);
        this.fecharFormEquipe();
        this.ok.set(id == null ? 'Equipe criada.' : 'Equipe atualizada.');
        this.recarregarTudo();
      },
      error: (erro: unknown) => {
        this.salvando.set(false);
        this.erro.set(mensagemHttp(erro, 'Falha ao salvar equipe'));
      },
    });
  }

  desativarEquipe(equipe: Equipe): void {
    if (!confirm(`Desativar equipe "${equipe.nome}"?`)) {
      return;
    }
    this.api.desativarEquipe(equipe.id).subscribe({
      next: () => this.recarregarTudo(),
      error: (erro: unknown) =>
        this.erro.set(mensagemHttp(erro, 'Falha ao desativar equipe')),
    });
  }

  abrirFormGari(gari?: Colaborador): void {
    if (gari) {
      this.editandoGariId.set(gari.id);
      this.formGari.reset({
        equipeId: gari.equipeId,
        nome: gari.nome,
        matricula: gari.matricula,
      });
    } else {
      this.editandoGariId.set(null);
      this.formGari.reset({
        equipeId: this.equipes().find((e) => e.ativo)?.id ?? 0,
        nome: '',
        matricula: '',
      });
    }
    this.mostrarFormGari.set(true);
  }

  fecharFormGari(): void {
    this.mostrarFormGari.set(false);
    this.editandoGariId.set(null);
  }

  salvarGari(): void {
    if (this.formGari.invalid) {
      this.formGari.markAllAsTouched();
      return;
    }
    this.salvando.set(true);
    this.erro.set(null);
    const raw = this.formGari.getRawValue();
    const body = {
      equipeId: Number(raw.equipeId),
      nome: raw.nome.trim(),
      matricula: raw.matricula.trim(),
      ativo: true,
    };
    const id = this.editandoGariId();
    const req$ = id == null
      ? this.api.criarColaborador(body)
      : this.api.atualizarColaborador(id, body);
    req$.subscribe({
      next: () => {
        this.salvando.set(false);
        this.fecharFormGari();
        this.ok.set(id == null ? 'Gari cadastrado.' : 'Gari atualizado.');
        this.recarregarTudo();
      },
      error: (erro: unknown) => {
        this.salvando.set(false);
        this.erro.set(mensagemHttp(erro, 'Falha ao salvar gari'));
      },
    });
  }

  desativarGari(gari: Colaborador): void {
    if (!confirm(`Desativar gari "${gari.nome}"?`)) {
      return;
    }
    this.api.desativarColaborador(gari.id).subscribe({
      next: () => this.recarregarTudo(),
      error: (erro: unknown) =>
        this.erro.set(mensagemHttp(erro, 'Falha ao desativar gari')),
    });
  }

  abrirFormLutocar(lutocar?: Equipamento): void {
    if (!lutocar && !this.podeCadastrarLutocar()) {
      this.erro.set(
        this.tetoErro()
          ?? 'Teto de Lutocares (GPS) atingido. Solicite aumento à organização.',
      );
      return;
    }
    if (lutocar) {
      this.editandoLutocarId.set(lutocar.id);
      this.formLutocar.reset({
        equipeId: lutocar.equipeId,
        codigo: lutocar.codigo,
        descricao: lutocar.descricao ?? '',
      });
    } else {
      this.editandoLutocarId.set(null);
      this.formLutocar.reset({
        equipeId: this.equipes().find((e) => e.ativo)?.id ?? 0,
        codigo: '',
        descricao: '',
      });
    }
    this.mostrarFormLutocar.set(true);
  }

  fecharFormLutocar(): void {
    this.mostrarFormLutocar.set(false);
    this.editandoLutocarId.set(null);
  }

  salvarLutocar(): void {
    if (this.formLutocar.invalid) {
      this.formLutocar.markAllAsTouched();
      return;
    }
    this.salvando.set(true);
    this.erro.set(null);
    const raw = this.formLutocar.getRawValue();
    const body = {
      equipeId: Number(raw.equipeId),
      codigo: raw.codigo.trim(),
      descricao: raw.descricao.trim() || null,
      ativo: true,
    };
    const id = this.editandoLutocarId();
    const req$ = id == null
      ? this.api.criarEquipamento(body)
      : this.api.atualizarEquipamento(id, body);
    req$.subscribe({
      next: () => {
        this.salvando.set(false);
        this.fecharFormLutocar();
        this.ok.set(id == null ? 'Lutocar cadastrado.' : 'Lutocar atualizado.');
        this.recarregarTudo();
      },
      error: (erro: unknown) => {
        this.salvando.set(false);
        this.erro.set(mensagemHttp(erro, 'Falha ao salvar lutocar'));
      },
    });
  }

  desativarLutocar(lutocar: Equipamento): void {
    if (!confirm(`Desativar lutocar "${lutocar.codigo}"?`)) {
      return;
    }
    this.api.desativarEquipamento(lutocar.id).subscribe({
      next: () => this.recarregarTudo(),
      error: (erro: unknown) =>
        this.erro.set(mensagemHttp(erro, 'Falha ao desativar lutocar')),
    });
  }
}

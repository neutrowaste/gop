import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { Colaborador, Equipamento } from '../../../models/cadastros-operacionais';
import { TetoAtivos } from '../../../models/licenca';
import { CadastrosOperacionaisApiService } from '../../../services/cadastros-operacionais-api.service';
import { mensagemHttp } from '../../../services/http-erro';
import { OrganizacaoApiService } from '../../../services/organizacao-api.service';
import { RasSystemApiService } from '../../../services/ras-system-api.service';

type AbaCadastro = 'colaboradores' | 'lutocares';

@Component({
  selector: 'app-org-equipes-list-page',
  imports: [ReactiveFormsModule],
  templateUrl: './org-equipes-list.page.html',
})
export class OrgEquipesListPage implements OnInit {
  private readonly api = inject(CadastrosOperacionaisApiService);
  private readonly orgApi = inject(OrganizacaoApiService);
  private readonly rasApi = inject(RasSystemApiService);
  private readonly fb = inject(FormBuilder);

  readonly aba = signal<AbaCadastro>('colaboradores');
  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly importandoRas = signal(false);
  readonly sincronizandoRas = signal(false);
  readonly erro = signal<string | null>(null);
  readonly ok = signal<string | null>(null);

  readonly colaboradores = signal<Colaborador[]>([]);
  readonly lutocares = signal<Equipamento[]>([]);
  readonly teto = signal<TetoAtivos | null>(null);
  readonly tetoErro = signal<string | null>(null);

  readonly mostrarFormColaborador = signal(false);
  readonly mostrarFormLutocar = signal(false);
  readonly editandoColaboradorId = signal<number | null>(null);
  readonly editandoLutocarId = signal<number | null>(null);

  readonly formColaborador = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(200)]],
    matricula: ['', [Validators.required, Validators.maxLength(60)]],
    equipamentoId: [0, [Validators.required, Validators.min(1)]],
  });

  readonly formLutocar = this.fb.nonNullable.group({
    codigo: ['', [Validators.required, Validators.maxLength(60)]],
    descricao: [''],
  });

  ngOnInit(): void {
    this.recarregarTudo();
  }

  selecionarAba(aba: AbaCadastro): void {
    this.aba.set(aba);
    this.erro.set(null);
    this.ok.set(null);
  }

  recarregarTudo(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.api.listarColaboradores().subscribe({
      next: (lista) => {
        this.colaboradores.set(lista);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao listar colaboradores'));
        this.carregando.set(false);
      },
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

  lutocaresAtivos(): Equipamento[] {
    return this.lutocares().filter((item) => item.ativo);
  }

  podeCadastrarLutocar(): boolean {
    const t = this.teto();
    return t != null && t.permiteNovoAtivo;
  }

  abrirFormColaborador(colaborador?: Colaborador): void {
    if (colaborador) {
      this.editandoColaboradorId.set(colaborador.id);
      this.formColaborador.reset({
        nome: colaborador.nome,
        matricula: colaborador.matricula,
        equipamentoId: colaborador.equipamentoId,
      });
    } else {
      this.editandoColaboradorId.set(null);
      this.formColaborador.reset({
        nome: '',
        matricula: '',
        equipamentoId: this.lutocaresAtivos()[0]?.id ?? 0,
      });
    }
    this.mostrarFormColaborador.set(true);
  }

  fecharFormColaborador(): void {
    this.mostrarFormColaborador.set(false);
    this.editandoColaboradorId.set(null);
  }

  salvarColaborador(): void {
    if (this.formColaborador.invalid) {
      this.formColaborador.markAllAsTouched();
      return;
    }
    this.salvando.set(true);
    this.erro.set(null);
    const raw = this.formColaborador.getRawValue();
    const body = {
      nome: raw.nome.trim(),
      matricula: raw.matricula.trim(),
      equipamentoId: Number(raw.equipamentoId),
      ativo: true,
    };
    const id = this.editandoColaboradorId();
    const req$ = id == null
      ? this.api.criarColaborador(body)
      : this.api.atualizarColaborador(id, body);
    req$.subscribe({
      next: () => {
        this.salvando.set(false);
        this.fecharFormColaborador();
        this.ok.set(id == null ? 'Colaborador cadastrado.' : 'Colaborador atualizado.');
        this.recarregarTudo();
      },
      error: (erro: unknown) => {
        this.salvando.set(false);
        this.erro.set(mensagemHttp(erro, 'Falha ao salvar colaborador'));
      },
    });
  }

  desativarColaborador(colaborador: Colaborador): void {
    if (!confirm(`Desativar colaborador "${colaborador.nome}"?`)) {
      return;
    }
    this.api.desativarColaborador(colaborador.id).subscribe({
      next: () => this.recarregarTudo(),
      error: (erro: unknown) =>
        this.erro.set(mensagemHttp(erro, 'Falha ao desativar colaborador')),
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
        codigo: lutocar.codigo,
        descricao: lutocar.descricao ?? '',
      });
    } else {
      this.editandoLutocarId.set(null);
      this.formLutocar.reset({
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

  importarDaRasSystem(): void {
    if (!this.podeCadastrarLutocar() || this.importandoRas()) {
      return;
    }
    this.importandoRas.set(true);
    this.erro.set(null);
    this.ok.set(null);
    this.rasApi.importarCadastro(5).subscribe({
      next: (resultado) => {
        this.importandoRas.set(false);
        const ingeridos = resultado.sync?.ingeridos ?? 0;
        if (resultado.criados === 0 && resultado.jaExistentes === 0) {
          this.ok.set(
            'Nenhum lutocar novo na RasSystem para cadastrar. ' +
              (ingeridos > 0 ? `${ingeridos} posição(ões) já vinculada(s) atualizada(s).` : ''),
          );
        } else {
          this.ok.set(
            `${resultado.criados} lutocar(es) cadastrado(s)` +
              (resultado.jaExistentes ? `, ${resultado.jaExistentes} já existia(m)` : '') +
              `. Sync: ${ingeridos} posição(ões) ingerida(s).`,
          );
        }
        this.recarregarTudo();
      },
      error: (erro: unknown) => {
        this.importandoRas.set(false);
        this.erro.set(mensagemHttp(erro, 'Falha ao importar lutocares da RasSystem'));
      },
    });
  }

  sincronizarPosicoesRas(): void {
    if (this.sincronizandoRas()) {
      return;
    }
    this.sincronizandoRas.set(true);
    this.erro.set(null);
    this.ok.set(null);
    this.rasApi.sincronizar().subscribe({
      next: (sync) => {
        this.sincronizandoRas.set(false);
        this.ok.set(
          `Posições atualizadas: ${sync.ingeridos} ingerida(s), ${sync.ignorados} ignorada(s), ${sync.naoVinculados} sem cadastro no GOP.`,
        );
        this.recarregarTudo();
      },
      error: (erro: unknown) => {
        this.sincronizandoRas.set(false);
        this.erro.set(mensagemHttp(erro, 'Falha ao sincronizar posições da RasSystem'));
      },
    });
  }

  rotuloFonte(lutocar: Equipamento): string {
    switch (lutocar.fonte) {
      case 'RASSYSTEM':
        return 'RasSystem';
      case 'PROPRIO':
        return 'GPS próprio';
      default:
        return 'Manual';
    }
  }
}

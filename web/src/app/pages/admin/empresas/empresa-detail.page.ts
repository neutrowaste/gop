import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { EmpresaAdminResponse } from '../../../models/empresa-admin';
import { Empresa } from '../../../models/empresa';
import { Licenca, StatusLicenca, TetoAtivos } from '../../../models/licenca';
import { EmpresaModulo } from '../../../models/organizacao';
import { EmpresaAdminApiService } from '../../../services/empresa-admin-api.service';
import { EmpresaClienteAdminApiService } from '../../../services/empresa-cliente-admin-api.service';
import { mensagemHttp } from '../../../services/http-erro';
import { LicencaAdminApiService } from '../../../services/licenca-admin-api.service';

type Aba = 'dados' | 'licencas' | 'admins' | 'modulos';

@Component({
  selector: 'app-empresa-detail-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './empresa-detail.page.html',
})
export class EmpresaDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly empresasApi = inject(EmpresaAdminApiService);
  private readonly licencasApi = inject(LicencaAdminApiService);
  private readonly clienteAdminApi = inject(EmpresaClienteAdminApiService);

  readonly aba = signal<Aba>('dados');
  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly ok = signal<string | null>(null);

  readonly empresa = signal<Empresa | null>(null);
  readonly licencas = signal<Licenca[]>([]);
  readonly teto = signal<TetoAtivos | null>(null);
  readonly tetoErro = signal<string | null>(null);
  readonly admins = signal<EmpresaAdminResponse[]>([]);
  readonly modulos = signal<EmpresaModulo[]>([]);

  readonly statusOpcoes: StatusLicenca[] = ['ATIVA', 'SUSPENSA', 'ENCERRADA'];

  readonly formEmpresa = this.fb.nonNullable.group({
    codigo: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9_-]+$/)]],
    nome: ['', Validators.required],
    ativo: [true],
  });

  readonly formLicenca = this.fb.nonNullable.group({
    tetoAtivos: [10, [Validators.required, Validators.min(0)]],
    vigenciaInicio: [EmpresaDetailPage.dataIsoHoje(), Validators.required],
    vigenciaFim: [''],
    status: ['ATIVA' as StatusLicenca, Validators.required],
  });

  readonly formAdmin = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(200)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(200)]],
    senha: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
  });

  private empresaId = 0;

  private static dataIsoHoje(): string {
    const agora = new Date();
    const ano = agora.getFullYear();
    const mes = String(agora.getMonth() + 1).padStart(2, '0');
    const dia = String(agora.getDate()).padStart(2, '0');
    return `${ano}-${mes}-${dia}`;
  }

  ngOnInit(): void {
    this.empresaId = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.empresaId) {
      this.erro.set('Empresa inválida');
      this.carregando.set(false);
      return;
    }
    this.carregarTudo();
  }

  selecionarAba(aba: Aba): void {
    this.aba.set(aba);
    this.erro.set(null);
    this.ok.set(null);
  }

  carregarTudo(): void {
    this.carregando.set(true);
    this.erro.set(null);

    forkJoin({
      empresa: this.empresasApi.buscar(this.empresaId),
      licencas: this.licencasApi.listarPorEmpresa(this.empresaId),
      teto: this.licencasApi.consultarTeto(this.empresaId).pipe(catchError(() => of(null))),
      admins: this.clienteAdminApi.listarAdmins(this.empresaId).pipe(catchError(() => of([]))),
      modulos: this.clienteAdminApi.listarModulos(this.empresaId).pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ empresa, licencas, teto, admins, modulos }) => {
        this.empresa.set(empresa);
        this.formEmpresa.patchValue({
          codigo: empresa.codigo,
          nome: empresa.nome,
          ativo: empresa.ativo,
        });
        this.licencas.set(licencas);
        this.teto.set(teto);
        this.tetoErro.set(teto ? null : this.explicarTetoAusente(licencas));
        this.admins.set(admins);
        this.modulos.set(modulos);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemHttp(erro, 'Falha ao carregar empresa'));
        this.carregando.set(false);
      },
    });
  }

  salvarEmpresa(): void {
    if (this.formEmpresa.invalid) {
      this.formEmpresa.markAllAsTouched();
      return;
    }
    this.salvando.set(true);
    this.erro.set(null);
    const raw = this.formEmpresa.getRawValue();
    this.empresasApi
      .atualizar(this.empresaId, {
        codigo: raw.codigo.trim(),
        nome: raw.nome.trim(),
        ativo: raw.ativo,
      })
      .subscribe({
        next: (empresa) => {
          this.empresa.set(empresa);
          this.ok.set('Dados da empresa atualizados.');
          this.salvando.set(false);
        },
        error: (erro: unknown) => {
          this.erro.set(mensagemHttp(erro, 'Falha ao atualizar empresa'));
          this.salvando.set(false);
        },
      });
  }

  desativarEmpresa(): void {
    if (!confirm('Desativar esta empresa cliente?')) {
      return;
    }
    this.empresasApi.desativar(this.empresaId).subscribe({
      next: () => this.carregarTudo(),
      error: (erro: unknown) => this.erro.set(mensagemHttp(erro, 'Falha ao desativar')),
    });
  }

  criarLicenca(): void {
    if (this.formLicenca.invalid) {
      this.formLicenca.markAllAsTouched();
      return;
    }
    this.salvando.set(true);
    this.erro.set(null);
    const raw = this.formLicenca.getRawValue();
    this.licencasApi
      .criar(this.empresaId, {
        tetoAtivos: Number(raw.tetoAtivos),
        vigenciaInicio: raw.vigenciaInicio,
        vigenciaFim: raw.vigenciaFim ? raw.vigenciaFim : null,
        status: raw.status,
        ativo: true,
      })
      .subscribe({
        next: () => {
          this.formLicenca.reset({
            tetoAtivos: 10,
            vigenciaInicio: EmpresaDetailPage.dataIsoHoje(),
            vigenciaFim: '',
            status: 'ATIVA',
          });
          this.ok.set('Licença criada.');
          this.salvando.set(false);
          this.carregarTudo();
        },
        error: (erro: unknown) => {
          this.erro.set(mensagemHttp(erro, 'Falha ao criar licença'));
          this.salvando.set(false);
        },
      });
  }

  encerrarLicenca(licenca: Licenca): void {
    if (!confirm(`Encerrar licença #${licenca.id}?`)) {
      return;
    }
    this.licencasApi.encerrar(licenca.id).subscribe({
      next: () => this.carregarTudo(),
      error: (erro: unknown) => this.erro.set(mensagemHttp(erro, 'Falha ao encerrar licença')),
    });
  }

  criarAdmin(): void {
    if (this.formAdmin.invalid) {
      this.formAdmin.markAllAsTouched();
      return;
    }
    this.salvando.set(true);
    this.erro.set(null);
    const raw = this.formAdmin.getRawValue();
    this.clienteAdminApi
      .criarAdmin(this.empresaId, {
        nome: raw.nome.trim(),
        email: raw.email.trim(),
        senha: raw.senha,
      })
      .subscribe({
        next: () => {
          this.formAdmin.reset();
          this.ok.set('Administrador da empresa criado.');
          this.salvando.set(false);
          this.carregarTudo();
        },
        error: (erro: unknown) => {
          this.erro.set(mensagemHttp(erro, 'Falha ao criar administrador'));
          this.salvando.set(false);
        },
      });
  }

  desativarAdmin(admin: EmpresaAdminResponse): void {
    if (!confirm(`Desativar admin ${admin.email}?`)) {
      return;
    }
    this.clienteAdminApi.desativarAdmin(this.empresaId, admin.id).subscribe({
      next: () => this.carregarTudo(),
      error: (erro: unknown) => this.erro.set(mensagemHttp(erro, 'Falha ao desativar admin')),
    });
  }

  alternarModulo(modulo: EmpresaModulo): void {
    this.clienteAdminApi
      .atualizarModulo(this.empresaId, modulo.moduloId, !modulo.habilitado)
      .subscribe({
        next: (atualizado) => {
          this.modulos.update((lista) =>
            lista.map((item) =>
              item.moduloId === atualizado.moduloId ? atualizado : item,
            ),
          );
          this.ok.set(
            atualizado.habilitado
              ? `Módulo ${atualizado.nome} habilitado para o tenant.`
              : `Módulo ${atualizado.nome} desabilitado.`,
          );
        },
        error: (erro: unknown) =>
          this.erro.set(mensagemHttp(erro, 'Falha ao atualizar módulo')),
      });
  }

  private explicarTetoAusente(licencas: Licenca[]): string {
    const hoje = EmpresaDetailPage.dataIsoHoje();
    const candidatas = licencas.filter((l) => l.ativo && l.status === 'ATIVA');

    const futuras = candidatas
      .filter((l) => l.vigenciaInicio > hoje)
      .sort((a, b) => a.vigenciaInicio.localeCompare(b.vigenciaInicio));
    if (futuras.length > 0) {
      const proxima = futuras[0];
      return `Há licença #${proxima.id} ativa, mas ainda não vigente (início em ${proxima.vigenciaInicio}).`;
    }

    const expiradas = candidatas.filter(
      (l) => l.vigenciaFim != null && l.vigenciaFim < hoje,
    );
    if (expiradas.length > 0 && candidatas.every((l) => l.vigenciaFim != null && l.vigenciaFim < hoje)) {
      return 'As licenças ativas cadastradas já expiraram.';
    }

    return 'A empresa não possui licença vigente para a data de hoje.';
  }
}

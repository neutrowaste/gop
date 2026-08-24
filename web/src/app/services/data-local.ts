/**
 * Utilitários de data no fuso local do navegador.
 *
 * `Date.toISOString()` converte para UTC antes de formatar, então em fusos
 * negativos (ex.: Brasil, UTC-3) o resultado pode antecipar o dia em até 3h
 * (entre ~21h e 23h59 do horário local). Isso causava divergência entre a
 * data "de hoje" usada como padrão em formulários/filtros (Escala, Ocorrência,
 * Mapa Operacional) e a data efetivamente vigente para o usuário.
 */
export function dataIsoLocal(data: Date): string {
  const ano = data.getFullYear();
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const dia = String(data.getDate()).padStart(2, '0');
  return `${ano}-${mes}-${dia}`;
}

export function hojeIso(): string {
  return dataIsoLocal(new Date());
}

-- Extensão espacial (Fase 1+). Instale o PostGIS no PostgreSQL via Stack Builder
-- (Application Stack Builder → Spatial Extensions → PostGIS) e reinicie o serviço.
-- Se a extensão ainda não estiver no servidor, o boot continua (continue-on-error)
-- e as tabelas com geometry serão criadas no próximo restart após a instalação.
--
-- separator ";;" em application-postgres.yml: o ScriptUtils do Spring parte em ";"
-- e quebraria o bloco DO $$ abaixo.
CREATE EXTENSION IF NOT EXISTS postgis;;

-- Trecho passa a ligar-se ao Setor. Hibernate (ddl-auto: update) cria setor_id
-- depois deste script; trechos sem setor não são migráveis.
ALTER TABLE IF EXISTS trecho DROP COLUMN IF EXISTS rua_id;;
DROP TABLE IF EXISTS rua CASCADE;;

DO $$
BEGIN
	IF to_regclass('public.trecho') IS NOT NULL
		AND NOT EXISTS (
			SELECT 1 FROM information_schema.columns
			WHERE table_schema = 'public' AND table_name = 'trecho' AND column_name = 'setor_id'
		)
	THEN
		TRUNCATE TABLE trecho CASCADE;
	END IF;
END $$;;

-- Escala deixa de amarrar Equipe/Lutocar direto: colaborador passa a ter o lutocar
-- designado e a escala seleciona os colaboradores do dia.
ALTER TABLE IF EXISTS escala DROP CONSTRAINT IF EXISTS uk_escala_empresa_data_turno_equipamento;;

DO $$
BEGIN
	IF to_regclass('public.colaborador') IS NOT NULL
		AND NOT EXISTS (
			SELECT 1 FROM information_schema.columns
			WHERE table_schema = 'public' AND table_name = 'colaborador' AND column_name = 'equipamento_id'
		)
	THEN
		TRUNCATE TABLE colaborador CASCADE;
	END IF;
END $$;;

ALTER TABLE IF EXISTS colaborador DROP COLUMN IF EXISTS equipe_id;;
ALTER TABLE IF EXISTS equipamento DROP COLUMN IF EXISTS equipe_id;;
ALTER TABLE IF EXISTS escala DROP COLUMN IF EXISTS equipe_id;;
ALTER TABLE IF EXISTS escala DROP COLUMN IF EXISTS equipamento_id;;

-- Setor passa a pertencer a um módulo/serviço contratado. O código é único
-- por empresa+serviço (o mesmo código pode existir em serviços diferentes).
ALTER TABLE IF EXISTS setor DROP CONSTRAINT IF EXISTS uk_setor_empresa_codigo;;

DO $$
BEGIN
	IF to_regclass('public.setor') IS NOT NULL
		AND NOT EXISTS (
			SELECT 1 FROM information_schema.columns
			WHERE table_schema = 'public' AND table_name = 'setor' AND column_name = 'modulo_id'
		)
	THEN
		TRUNCATE TABLE setor CASCADE;
	END IF;

	IF EXISTS (
		SELECT 1 FROM information_schema.columns
		WHERE table_schema = 'public' AND table_name = 'setor' AND column_name = 'modulo_id'
	) AND NOT EXISTS (
		SELECT 1 FROM pg_constraint WHERE conname = 'uk_setor_empresa_modulo_codigo'
	)
	THEN
		ALTER TABLE setor ADD CONSTRAINT uk_setor_empresa_modulo_codigo UNIQUE (empresa_id, modulo_id, codigo);
	END IF;
END $$;;

-- Escala de frequência: o modelo não tem data; a instância do dia (e a avulsa) têm.
ALTER TABLE IF EXISTS escala ALTER COLUMN data DROP NOT NULL;;
ALTER TABLE IF EXISTS escala ADD COLUMN IF NOT EXISTS tipo varchar(20);;
UPDATE escala SET tipo = 'AVULSA' WHERE tipo IS NULL;;

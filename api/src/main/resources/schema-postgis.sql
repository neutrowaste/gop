-- Extensão espacial (Fase 1+). Instale o PostGIS no PostgreSQL via Stack Builder
-- (Application Stack Builder → Spatial Extensions → PostGIS) e reinicie o serviço.
-- Se a extensão ainda não estiver no servidor, o boot continua (continue-on-error)
-- e as tabelas com geometry serão criadas no próximo restart após a instalação.
CREATE EXTENSION IF NOT EXISTS postgis;

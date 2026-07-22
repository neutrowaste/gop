-- Setup PostGIS (Fase 0). Geometrias de negócio entram nas fases 1+.
-- Instale o PostGIS no PostgreSQL (Stack Builder) e execute manualmente:
--   CREATE EXTENSION IF NOT EXISTS postgis;
-- Enquanto a extensão não estiver disponível, este script permanece no-op
-- para não bloquear a criação do schema da Fase 0.
SELECT 1;

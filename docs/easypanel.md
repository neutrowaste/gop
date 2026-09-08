# Plano de deploy — GOP no EasyPanel

Este documento descreve como subir o GOP no EasyPanel a partir do repositório [neutrowaste/gop](https://github.com/neutrowaste/gop), usando o `docker-compose.yml` já preparado para o painel.

Arquitetura alvo:

```
Internet → Traefik (TLS) → web:80 (nginx + Angular)
                              └─ /api → api:8080 (Spring Boot)
                                           └─ PostgreSQL + PostGIS
```

O domínio público aponta **somente** para o serviço `web`. A API não fica exposta; o nginx encaminha `/api`.

---

## Checklist

- [ ] VPS com EasyPanel e pelo menos **4 GB RAM** (build Maven + Angular no mesmo host)
- [ ] Repositório GitHub acessível pelo EasyPanel (token se o repo for privado)
- [ ] PostgreSQL **com PostGIS** (banco `bd_gop` já criado)
- [ ] `JWT_SECRET` com 32+ caracteres
- [ ] Senha forte do admin seed (e depois desligar o seed)
- [ ] Domínio apontando para o IP do EasyPanel
- [ ] Health: `/` (front) e `/api/actuator/health` (API)

---

## 1. O que sobe e o que fica de fora

| Serviço | Neste deploy | Observação |
|---|---|---|
| `web` (nginx + Angular) | Sim | Porta 80, domínio público |
| `api` (Spring Boot / Java 21) | Sim | Porta 8080 interna |
| PostgreSQL 16 + PostGIS | Sim, **serviço separado** no EasyPanel | Não está no compose |
| Redis / RabbitMQ / tileserv | Não | Fora do compose atual |

O schema operacional é criado no boot da API (`ddl-auto=update`) + `schema-postgis.sql` (`CREATE EXTENSION postgis`).

---

## 2. Pré-requisitos no EasyPanel

1. Painel acessível (ex.: `https://devpanel.neutrowaste.cloud`).
2. Projeto criado, por exemplo **`gop`**.
3. Se o GitHub for privado: **Settings → Git Credentials** com um Personal Access Token (`repo` read).
4. Recursos sugeridos no host:
   - **Build:** 3–4 GB livres (Maven + `npm ci` / `ng build`).
   - **Runtime da API:** 1,5–2 GB (`JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75.0`).
   - **web:** ~128 MB.

---

## 3. PostgreSQL + PostGIS (serviço de banco)

O compose **não** sobe o banco. Use um serviço PostgreSQL no EasyPanel (já existe um exemplo em `.env.example` em `devpanel.neutrowaste.cloud:12011`).

### 3.1 Imagem

O template padrão do EasyPanel (`postgres:16`) **não** inclui PostGIS. Troque a imagem para:

```text
postgis/postgis:16-3.5
```

Sem isso, colunas `geometry` e `ST_DWithin` falham. A API não derruba o boot se a extensão falhar (`continue-on-error`), mas o módulo geo fica inutilizável até instalar PostGIS e **reiniciar a API**.

### 3.2 Banco e usuário

No serviço Postgres (console SQL ou Adminer):

```sql
CREATE DATABASE bd_gop;
CREATE USER usr_bd_gop WITH PASSWORD 'SENHA_FORTE';
GRANT ALL PRIVILEGES ON DATABASE bd_gop TO usr_bd_gop;
\c bd_gop
GRANT ALL ON SCHEMA public TO usr_bd_gop;
CREATE EXTENSION IF NOT EXISTS postgis;
```

Confirme:

```sql
SELECT PostGIS_Version();
```

### 3.3 Rede: mesmo projeto vs. porta publicada

**Mesmo projeto EasyPanel (recomendado)** — hostname interno, sem expor o banco:

```text
jdbc:postgresql://<nome-do-servico-postgres>:5432/bd_gop
```

O nome interno aparece no card do serviço (algo como `gop_postgres` ou o nome que você deu).

**Projetos diferentes / porta publicada** (caso atual, porta `12011`):

```text
jdbc:postgresql://devpanel.neutrowaste.cloud:12011/bd_gop?sslmode=disable
```

Não exponha a porta do Postgres na internet em produção. Prefira rede interna do mesmo projeto.

---

## 4. App Compose (API + web)

No projeto **gop**:

1. **+ Service → App**.
2. Source: **GitHub** → `neutrowaste/gop`, branch **`main`**.
3. Build: **Docker Compose**.
4. Compose file: `docker-compose.yml` (raiz).
5. **Não** marque domínio no serviço `api`.
6. No serviço **`web`**: domínio público, porta **80**.

O EasyPanel constrói as duas imagens (`api/Dockerfile` e `web/Dockerfile`) e sobe a stack. O `web` espera o healthcheck da API (`/api/actuator/health`) antes de ficar ready (`start_period` da API: 90s — Hibernate + seed).

Primeiro build: **5–15 min**, conforme cache Maven/npm no host.

---

## 5. Variáveis de ambiente

Preencha no App Compose (Environment). Não versione `.env`.

### 5.1 Obrigatórias

| Variável | Exemplo | Notas |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://gop_postgres:5432/bd_gop` | Hostname interno se o PG estiver no mesmo projeto |
| `SPRING_DATASOURCE_USERNAME` | `usr_bd_gop` | |
| `SPRING_DATASOURCE_PASSWORD` | *(secreto)* | Obrigatória |
| `JWT_SECRET` | *(32+ chars aleatórios)* | Obrigatória em produção |

Gerar o JWT:

```bash
openssl rand -base64 48
```

### 5.2 CORS e front

Com o nginx proxy (`/api` no mesmo origin), o browser chama `/api`. Ainda assim defina o origin público:

| Variável | Produção |
|---|---|
| `GOP_CORS_ORIGINS` | `https://gop.seudominio.com` |
| `GOP_CORS_ORIGIN_PATTERNS` | `https://*.neutrowaste.cloud` (opcional, vários subdomínios) |
| `GOP_API_URL` | **vazio** |

Deixe `GOP_API_URL` vazio. O `99-gop-runtime.sh` gera `runtime-config.js`; se `apiBaseUrl` estiver vazio, o Angular usa `/api` fora de localhost.

Só preencha `GOP_API_URL` (ex.: `https://api.seudominio.com/api`) se o front e a API tiverem **domínios separados**.

### 5.3 Seed do admin de plataforma

No **primeiro** boot:

| Variável | Valor sugerido |
|---|---|
| `GOP_SEED_ADMIN_ENABLED` | `true` |
| `GOP_SEED_ADMIN_EMAIL` | e-mail real de operação |
| `GOP_SEED_ADMIN_SENHA` | senha forte (não use o default `Admin@Gop2026`) |
| `GOP_SEED_ADMIN_NOME` | `Admin Plataforma GOP` |

Depois do primeiro login, mude para `GOP_SEED_ADMIN_ENABLED=false` e faça **Redeploy**. O seed não deve ficar ligado em produção.

### 5.4 Opcionais

| Variável | Default | Quando usar |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `postgres` (já no compose) | Não precisa repetir |
| `RASSYSTEM_POLLER_ENABLED` | `false` | Ligar só com login/senha RasSystem válidos |
| `RASSYSTEM_LOGIN` / `RASSYSTEM_SENHA` | — | Integração GPS RasSystem |
| `RASSYSTEM_ID_FILIAL` | — | Filial padrão da integração |

---

## 6. Domínio e TLS

1. DNS: registro **A** (ou CNAME) do host (ex. `gop.neutrowaste.cloud`) → IP do EasyPanel.
2. No serviço **`web`**: Add Domain → esse hostname → porta **80**.
3. Ative **HTTPS** (Let's Encrypt via Traefik).
4. Confirme `GOP_CORS_ORIGINS` com `https://...` (não `http://`).

A API usa `server.forward-headers-strategy: framework` para respeitar `X-Forwarded-Proto` do Traefik.

---

## 7. Subir e validar

1. **Deploy** no App Compose.
2. Logs da `api`: aguarde Hibernate (`ddl-auto=update`) e health `UP`. O `web` só sobe depois disso.
3. Testes:

```bash
# Front
curl -I https://gop.seudominio.com/

# API via proxy nginx
curl -s https://gop.seudominio.com/api/actuator/health
# esperado: {"status":"UP",...}
```

4. Login no Angular com o e-mail/senha do seed.
5. Confirme PostGIS de novo se o mapa/trechos falharem:

```sql
SELECT PostGIS_Version();
```

---

## 8. Atualizações (CI simples)

1. Push em `main`.
2. No App: **Enable Auto Deploy** (webhook GitHub) **ou** Deploy manual.
3. EasyPanel rebuilda as imagens. O schema continua a cargo do Hibernate no boot.

Não use `ddl-auto=update` como estratégia de migração de longo prazo; para este estágio do GOP, é o comportamento atual.

---

## 9. Problemas comuns

| Sintoma | Causa provável | Ação |
|---|---|---|
| `web` restart loop / waiting for `api` | API não fica healthy em 90s | Logs da API; RAM; Postgres inacessível |
| `Connection refused` / timeout JDBC | Hostname/porta do PG errados | Usar hostname interno do mesmo projeto; conferir usuário/senha |
| Boot OK mas geometria quebra | PostGIS ausente | Imagem `postgis/postgis`; `CREATE EXTENSION`; restart da API |
| Front carrega, `/api` 502 | `api` down ou nome DNS `api` | Os dois serviços precisam estar no **mesmo compose** |
| CORS no browser | Origin não listado | `GOP_CORS_ORIGINS=https://dominio-exato` |
| Login 401 no seed | Seed desligado ou senha antiga | Conferir env e se o usuário já existia no banco |
| Build OOM / killed | RAM insuficiente no VPS | 4 GB+; build sequencial; não buildar `api` e `web` em paralelo se o host for pequeno |
| 413 no upload de shapefile | Body maior que o nginx | `client_max_body_size 55m` já está no `nginx.conf` |

Healthchecks úteis nos logs:

- API: `curl http://localhost:8080/api/actuator/health`
- web: `wget -qO- http://127.0.0.1/`

---

## 10. Backup e operação

- **Postgres:** backup automático do serviço EasyPanel (diário) + dump manual antes de mudanças de schema.
- **Volumes:** a API e o web são stateless; o estado está no banco.
- **Segredos:** só no Environment do painel; nunca no Git.
- **Porta do banco:** feche `12011` (e similares) no firewall se o acesso interno já funcionar.

---

## 11. Ordem de execução resumida

1. Criar projeto `gop` no EasyPanel.
2. Subir Postgres com imagem PostGIS; criar `bd_gop` + extensão.
3. Criar App Compose a partir de `main` / `docker-compose.yml`.
4. Preencher env (JDBC, `JWT_SECRET`, CORS, seed).
5. Apontar o domínio para `web:80` e ligar TLS.
6. Deploy → validar `/` e `/api/actuator/health` → login admin.
7. Desligar o seed (`GOP_SEED_ADMIN_ENABLED=false`) e redeploy.
8. Ligar auto-deploy da branch `main`.

# 🛰️ monitor-fornecedores

[![CI](https://github.com/Arthurpereiraa/monitor-fornecedores/actions/workflows/ci.yml/badge.svg)](https://github.com/Arthurpereiraa/monitor-fornecedores/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)
![Spring Batch](https://img.shields.io/badge/Spring%20Batch-6-6DB33F?logo=spring&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?logo=springsecurity&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-pronto-2496ED?logo=docker&logoColor=white)

**API REST para monitorar o risco de fornecedores pelo CNPJ.** Cadastre os fornecedores da sua empresa e a API consulta os dados públicos da Receita Federal, calcula o risco, guarda o histórico de cada avaliação e **detecta quando o risco muda**: um fornecedor que foi baixado, virou MEI ou mudou de endereço.

## Funcionalidades

- **Cadastro e primeira avaliação** de fornecedores, com validação do CNPJ (inclusive o **alfanumérico** de 2026).
- **Regras de risco** para situação cadastral, empresa recém-aberta, MEI, capital social, CEP que não bate com o município (cruzando duas fontes), falta de contato e quadro societário vazio.
- **Recomendações** concretas para cada alerta.
- **Reavaliação com detecção de mudança:** cada avaliação guarda o nível anterior e informa se o risco `mudou` ou `piorou`.
- **Histórico** completo de avaliações por fornecedor.
- **Reavaliação noturna em lote (Spring Batch):** um job reavalia todos os fornecedores toda noite e aponta quem piorou. Uma API fora do ar não derruba o job: o fornecedor é pulado e contado.
- **Fontes reserva:** se a BrasilAPI limitar consultas (HTTP 429) ou cair, usa a Minha Receita para CNPJ e o ViaCEP para CEP. "Não encontrado" não aciona a reserva.
- **Autenticação com JWT** (Spring Security): perfil `analista` só lê, perfil `admin` cadastra, reavalia e remove.
- **Documentação interativa** com Swagger UI, já integrada ao login.
- **Erros padronizados** em Problem Details (RFC 9457), com mensagens em português.

## Stack

**Java 21** · **Spring Boot 4.1** (Web MVC, Data JPA, Validation, Actuator) · **Spring Batch 6** · **Spring Security (JWT)** · **springdoc-openapi (Swagger)** · **Flyway** · **PostgreSQL** (H2 para desenvolvimento) · **JUnit 5 + Mockito + MockMvc** · **Docker** · **GitHub Actions**

## Arquitetura

```
src/main/java/io/github/arthurpereiraa/monitor/
├── dominio/       # regras de negócio em Java puro, sem framework: Cnpj, RegrasRisco, AnaliseRisco
├── integracao/    # porta ConsultaEmpresaGateway + cliente HTTP (RestClient) com fonte reserva
├── persistencia/  # entidades JPA e repositórios; esquema versionado pelo Flyway
├── servico/       # casos de uso: cadastrar, listar, buscar, reavaliar, histórico, remover
├── lote/          # job do Spring Batch: reavaliação de todos os fornecedores, agendada por cron
├── seguranca/     # Spring Security: filtros, emissão e validação do token JWT, usuários
├── web/           # controllers REST e tratamento de erros (Problem Details)
└── config/        # beans: Clock injetável e o gateway com timeouts
```

Algumas decisões de projeto:

- **O domínio não depende do Spring.** As regras de risco são Java puro e testadas sem subir contexto. São **as mesmas regras** do [due-diligence-agentes](https://github.com/Arthurpereiraa/due-diligence-agentes) (Python) e do [radar-cnpj](https://github.com/Arthurpereiraa/radar-cnpj) (TypeScript).
- **Porta e adaptador:** o serviço conhece só a interface `ConsultaEmpresaGateway`. Trocar ou acrescentar uma API não mexe nas regras.
- **`Clock` injetado**, para os testes controlarem "hoje" nas regras que dependem de data.
- **O Flyway é dono do esquema** (`ddl-auto: none`). As migrações rodam iguais no PostgreSQL e no H2.
- **Respostas paginadas em formato próprio**, sem expor a classe `Page` do Spring Data no JSON.

## Autenticação

A API usa **token JWT** (Spring Security como resource server, assinatura HS256, sem sessão):

1. Faça login em `POST /api/auth/token` e receba o token (válido por 1 hora).
2. Envie o token nas outras chamadas, no cabeçalho `Authorization: Bearer <token>`.

| Usuário | Senha (desenvolvimento) | Pode |
|---|---|---|
| `analista` | `analista123` | Consultar (`GET`) |
| `admin` | `admin123` | Consultar, cadastrar, reavaliar, remover e rodar o job em lote |

As senhas e o segredo do token vêm das variáveis `ADMIN_SENHA`, `ANALISTA_SENHA` e `JWT_SEGREDO`; os valores acima são só para rodar localmente. Health check e documentação continuam públicos.

## Endpoints

| Método | Rota | O que faz |
|---|---|---|
| `POST` | `/api/auth/token` | Login (`{"usuario", "senha"}`): devolve o token JWT |
| `POST` | `/api/fornecedores` | Cadastra (`{"cnpj": "..."}`) e faz a primeira avaliação |
| `GET` | `/api/fornecedores?nivel=ALTO&pagina=0&tamanho=20` | Lista, com filtro opcional por nível de risco |
| `GET` | `/api/fornecedores/{cnpj}` | Detalha, com a última avaliação |
| `GET` | `/api/fornecedores/{cnpj}/avaliacoes` | Histórico de avaliações |
| `POST` | `/api/fornecedores/{cnpj}/reavaliacoes` | Consulta de novo e registra uma avaliação |
| `DELETE` | `/api/fornecedores/{cnpj}` | Remove do monitoramento |
| `POST` | `/api/reavaliacoes` | Roda agora o job de reavaliação de todos (o mesmo da noite) |
| `GET` | `/actuator/health` | Saúde da aplicação |
| `GET` | `/swagger-ui.html` | Documentação interativa da API |

A documentação interativa (OpenAPI/Swagger) fica em **http://localhost:8080/swagger-ui.html**, com a especificação em `/v3/api-docs`. Dá para testar cada rota direto pelo navegador: faça login em `/api/auth/token`, clique em **Authorize** e cole o token.

Exemplos prontos para rodar também estão em [`http/fornecedores.http`](http/fornecedores.http). Na URL, use só os dígitos do CNPJ.

<details>
<summary>Exemplo de resposta de uma reavaliação em que o risco piorou</summary>

```json
{
  "nivel": "ALTO",
  "nivelAnterior": "BAIXO",
  "mudou": true,
  "piorou": true,
  "fonte": "BrasilAPI",
  "avaliadoEm": "2026-11-19T15:00:00Z",
  "alertas": [
    { "nivel": "ALTO", "codigo": "situacao_irregular", "mensagem": "Situação cadastral BAIXADA na Receita Federal." }
  ],
  "recomendacoes": [
    "Não formalizar a contratação até a empresa regularizar a situação cadastral na Receita Federal.",
    "Solicitar as certidões negativas de débitos (CND federal, FGTS e trabalhista).",
    "Consultar protestos e processos judiciais em nome da empresa."
  ]
}
```
</details>

## Reavaliação em lote (Spring Batch)

Toda noite às 2h (configurável em `app.reavaliacao.cron`), um job reavalia todos os fornecedores monitorados:

```
[leitor]  RepositoryItemReader → CNPJs, paginados de 10 em 10
   ↓
[processor] consulta as APIs e recalcula o risco (sem transação, sem gravar)
   ↓
[writer]  grava a nova avaliação no histórico e anota quem piorou
```

- **Chunks de 10:** cada bloco é uma transação. Se algo falhar no meio, o que já foi gravado fica.
- **Tolerância a falhas:** `FonteIndisponivelException` e `EmpresaNaoEncontradaException` fazem o fornecedor ser **pulado** (skip), não derrubam o job. Se as falhas passarem de `app.reavaliacao.limite-falhas`, o job termina como `FAILED`.
- **Respeita o limite das APIs públicas** com uma pausa entre consultas (`app.reavaliacao.intervalo`).
- **Metadados persistidos** nas tabelas `BATCH_*` do Spring Batch: cada execução fica registrada com contagens e status.
- **Uma execução por vez**, seja agendada ou disparada pela API.

Resposta de `POST /api/reavaliacoes`:

```json
{
  "execucaoId": 7,
  "status": "COMPLETED",
  "lidos": 120,
  "reavaliados": 118,
  "ignorados": 2,
  "pioraram": ["11.222.333/0001-81"]
}
```

## Rodando

Precisa só do **Java 21**. O Gradle Wrapper baixa o resto.

```bash
./gradlew bootRun          # http://localhost:8080, com banco H2 em memória
./gradlew test             # testes
```

Testando pelo terminal:

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/token -H "Content-Type: application/json" \
  -d '{"usuario":"admin","senha":"admin123"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')
curl -X POST localhost:8080/api/fornecedores -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"cnpj":"00.000.000/0001-91"}'
```

Com PostgreSQL, via Docker:

```bash
docker compose up --build
```

## Testes

| Camada | Como é testada |
|---|---|
| Domínio | JUnit 5 puro: validação de CNPJ e cada regra de risco |
| Integração HTTP | `MockRestServiceServer`: fonte reserva no 429, 404 definitivo, as duas fontes fora, CEP |
| Serviço | Mockito: cadastro, duplicado, CNPJ inválido, detecção de piora |
| Web | MockMvc: status, `Location`, Problem Details, filtro por nível |
| Ponta a ponta | `@SpringBootTest` com Flyway e H2: cadastrar, listar, reavaliar, histórico, remover |
| Documentação | `/v3/api-docs` gerado com título, versão e rotas |
| Segurança | MockMvc com os filtros do Spring Security: 401 sem token, 403 para analista, admin altera |
| Job em lote | `@SpringBootTest` rodando o job de verdade: quem piorou, quem foi pulado, banco vazio |

## Próximos passos

- [x] **Spring Batch:** job noturno que reavalia todos os fornecedores
- [ ] **RabbitMQ:** evento quando o risco de um fornecedor piora
- [x] **Spring Security + JWT**
- [x] **OpenAPI/Swagger**
- [ ] **Testcontainers** com PostgreSQL real

## Feito com IA

Projeto escrito em pair programming com o [Claude](https://claude.ai). Ele faz parte de uma série que aplica as mesmas regras de negócio em quatro stacks:

- [mcp-brasil](https://github.com/Arthurpereiraa/mcp-brasil): servidor MCP (TypeScript, publicado no npm)
- [brasil-toolkit](https://github.com/Arthurpereiraa/brasil-toolkit): plugin do Claude Code
- [due-diligence-agentes](https://github.com/Arthurpereiraa/due-diligence-agentes): sistema multiagente (Python + Ollama)
- [radar-cnpj](https://github.com/Arthurpereiraa/radar-cnpj): app web (React + TypeScript)
- **monitor-fornecedores:** esta API (Java + Spring Boot)

## Licença

[MIT](LICENSE)

# 🛰️ monitor-fornecedores

[![CI](https://github.com/Arthurpereiraa/monitor-fornecedores/actions/workflows/ci.yml/badge.svg)](https://github.com/Arthurpereiraa/monitor-fornecedores/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-pronto-2496ED?logo=docker&logoColor=white)

**API REST para monitorar o risco de fornecedores pelo CNPJ.** Cadastre os fornecedores da sua empresa e a API consulta os dados públicos da Receita Federal, calcula o risco, guarda o histórico de cada avaliação e **detecta quando o risco muda**: um fornecedor que foi baixado, virou MEI ou mudou de endereço.

## Funcionalidades

- **Cadastro e primeira avaliação** de fornecedores, com validação do CNPJ (inclusive o **alfanumérico** de 2026).
- **Regras de risco** para situação cadastral, empresa recém-aberta, MEI, capital social, CEP que não bate com o município (cruzando duas fontes), falta de contato e quadro societário vazio.
- **Recomendações** concretas para cada alerta.
- **Reavaliação com detecção de mudança:** cada avaliação guarda o nível anterior e informa se o risco `mudou` ou `piorou`.
- **Histórico** completo de avaliações por fornecedor.
- **Fontes reserva:** se a BrasilAPI limitar consultas (HTTP 429) ou cair, usa a Minha Receita para CNPJ e o ViaCEP para CEP. "Não encontrado" não aciona a reserva.
- **Erros padronizados** em Problem Details (RFC 9457), com mensagens em português.

## Stack

**Java 21** · **Spring Boot 4.1** (Web MVC, Data JPA, Validation, Actuator) · **Flyway** · **PostgreSQL** (H2 para desenvolvimento) · **JUnit 5 + Mockito + MockMvc** · **Docker** · **GitHub Actions**

## Arquitetura

```
src/main/java/io/github/arthurpereiraa/monitor/
├── dominio/       # regras de negócio em Java puro, sem framework: Cnpj, RegrasRisco, AnaliseRisco
├── integracao/    # porta ConsultaEmpresaGateway + cliente HTTP (RestClient) com fonte reserva
├── persistencia/  # entidades JPA e repositórios; esquema versionado pelo Flyway
├── servico/       # casos de uso: cadastrar, listar, buscar, reavaliar, histórico, remover
├── web/           # controller REST e tratamento de erros (Problem Details)
└── config/        # beans: Clock injetável e o gateway com timeouts
```

Algumas decisões de projeto:

- **O domínio não depende do Spring.** As regras de risco são Java puro e testadas sem subir contexto. São **as mesmas regras** do [due-diligence-agentes](https://github.com/Arthurpereiraa/due-diligence-agentes) (Python) e do [radar-cnpj](https://github.com/Arthurpereiraa/radar-cnpj) (TypeScript).
- **Porta e adaptador:** o serviço conhece só a interface `ConsultaEmpresaGateway`. Trocar ou acrescentar uma API não mexe nas regras.
- **`Clock` injetado**, para os testes controlarem "hoje" nas regras que dependem de data.
- **O Flyway é dono do esquema** (`ddl-auto: none`). As migrações rodam iguais no PostgreSQL e no H2.
- **Respostas paginadas em formato próprio**, sem expor a classe `Page` do Spring Data no JSON.

## Endpoints

| Método | Rota | O que faz |
|---|---|---|
| `POST` | `/api/fornecedores` | Cadastra (`{"cnpj": "..."}`) e faz a primeira avaliação |
| `GET` | `/api/fornecedores?nivel=ALTO&pagina=0&tamanho=20` | Lista, com filtro opcional por nível de risco |
| `GET` | `/api/fornecedores/{cnpj}` | Detalha, com a última avaliação |
| `GET` | `/api/fornecedores/{cnpj}/avaliacoes` | Histórico de avaliações |
| `POST` | `/api/fornecedores/{cnpj}/reavaliacoes` | Consulta de novo e registra uma avaliação |
| `DELETE` | `/api/fornecedores/{cnpj}` | Remove do monitoramento |
| `GET` | `/actuator/health` | Saúde da aplicação |

Exemplos prontos para rodar estão em [`http/fornecedores.http`](http/fornecedores.http). Na URL, use só os dígitos do CNPJ.

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

## Rodando

Precisa só do **Java 21**. O Gradle Wrapper baixa o resto.

```bash
./gradlew bootRun          # http://localhost:8080, com banco H2 em memória
./gradlew test             # testes
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

## Próximos passos

- [ ] **Spring Batch:** job noturno que reavalia todos os fornecedores
- [ ] **RabbitMQ:** evento quando o risco de um fornecedor piora
- [ ] **Spring Security + JWT**
- [ ] **OpenAPI/Swagger** e **Testcontainers** com PostgreSQL real

## Feito com IA

Projeto escrito em pair programming com o [Claude](https://claude.ai). Ele faz parte de uma série que aplica as mesmas regras de negócio em quatro stacks:

- [mcp-brasil](https://github.com/Arthurpereiraa/mcp-brasil): servidor MCP (TypeScript, publicado no npm)
- [brasil-toolkit](https://github.com/Arthurpereiraa/brasil-toolkit): plugin do Claude Code
- [due-diligence-agentes](https://github.com/Arthurpereiraa/due-diligence-agentes): sistema multiagente (Python + Ollama)
- [radar-cnpj](https://github.com/Arthurpereiraa/radar-cnpj): app web (React + TypeScript)
- **monitor-fornecedores:** esta API (Java + Spring Boot)

## Licença

[MIT](LICENSE)

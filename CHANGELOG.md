# Changelog

## [0.3.0] - 2026-11-24

### Adicionado
- Documentação OpenAPI com Swagger UI em `/swagger-ui.html` (springdoc-openapi 3).
- Resumo e descrição de cada rota, agrupadas por assunto.

## [0.2.0] - 2026-11-19

### Adicionado
- Job do Spring Batch que reavalia todos os fornecedores em chunks de 10, agendado toda noite às 2h.
- Tolerância a falhas: fornecedores com API fora do ar ou CNPJ não encontrado são pulados (skip), até um limite configurável.
- Endpoint `POST /api/reavaliacoes` para rodar o job sob demanda, com resumo da execução (lidos, reavaliados, ignorados, quem piorou).
- Metadados das execuções persistidos nas tabelas do Spring Batch.

### Alterado
- `FornecedorService` separa a consulta às APIs (sem transação) da gravação da avaliação.

## [0.1.0] - 2026-11-19

### Adicionado
- API REST de fornecedores: cadastrar, listar (com filtro por nível), detalhar, reavaliar, histórico e remover.
- Regras de risco em Java puro (as mesmas do due-diligence-agentes e do radar-cnpj), com validação do CNPJ alfanumérico.
- Cliente HTTP com fonte reserva: BrasilAPI → Minha Receita (CNPJ) e BrasilAPI → ViaCEP (CEP).
- Persistência com Spring Data JPA e migrações Flyway (PostgreSQL e H2).
- Erros em Problem Details (RFC 9457).
- Testes de domínio, cliente HTTP, serviço, controller e ponta a ponta.
- Dockerfile multi-stage, docker-compose com PostgreSQL e CI no GitHub Actions.

# Changelog

## [0.1.0] - 2026-11-19

### Adicionado
- API REST de fornecedores: cadastrar, listar (com filtro por nível), detalhar, reavaliar, histórico e remover.
- Regras de risco em Java puro (as mesmas do due-diligence-agentes e do radar-cnpj), com validação do CNPJ alfanumérico.
- Cliente HTTP com fonte reserva: BrasilAPI → Minha Receita (CNPJ) e BrasilAPI → ViaCEP (CEP).
- Persistência com Spring Data JPA e migrações Flyway (PostgreSQL e H2).
- Erros em Problem Details (RFC 9457).
- Testes de domínio, cliente HTTP, serviço, controller e ponta a ponta.
- Dockerfile multi-stage, docker-compose com PostgreSQL e CI no GitHub Actions.

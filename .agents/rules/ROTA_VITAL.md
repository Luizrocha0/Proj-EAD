# Regras do Projeto Rota Vital (AED + SO)

## Contexto
Projeto integrador 3º semestre ADS 2026.2. Backend Spring Boot 3.3.3 + Java 21.
Prazos: AED AV1 = 02/10/2026 | AED AV2 = 04/12/2026

## Regras Obrigatórias

1. **Nunca modificar** evidências históricas em `docs/threads/` e `bench/` — apenas preservar.
2. **Estruturas AED devem usar nós próprios** — sem ArrayList, LinkedList do Java para as estruturas de AED; criar `No` com campo `proximo`.
3. **C sempre com malloc/free** — toda alocação de nó deve ter free correspondente; testar com AddressSanitizer.
4. **Mesmas fixtures C e Java** — os dados de teste em `fixtures/` devem produzir os mesmos resultados nos dois idiomas.
5. **Controllers apenas delegam** — nenhuma lógica de negócio no controller; tudo vai para o Service.
6. **Sem banco de dados na U1** — dados sintéticos em memória; persistência somente se o professor exigir.
7. **Maven Wrapper deve funcionar** — `./mvnw test` deve executar do zero sem configuração manual.

## Stack

- Java 21 + Spring Boot 3.3.3 + Maven
- JUnit 5 + Spring Boot Test
- C11 + GCC + Makefile
- Docker + GitHub Actions
- Render (deploy)

## Nomenclatura

- Domínio: `Bolsa`, `Requisicao`, `Hospital`, `TipoSanguineo`
- Pacotes Java: `com.rotavital.{controller,service,domain,aed.u1,aed.u2,util}`
- Estruturas C: `lista_*`, `fila_*`, `pilha_*`, `hash_*`, `fefo_*`

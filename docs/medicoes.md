# Medições — Sequencial vs. Threads

Endpoint real: `GET /api/duplicates/detect?size={n}&threads={t}&mode={platform|virtual}`.
Dados gerados via `DataGeneratorService` (seed fixa, reprodutível). Ambiente: 16 núcleos lógicos, Java 21 (virtual threads habilitadas), execução local via `mvnw spring-boot:run`.

Script de medição e geração do gráfico: `bench/plot_speedup.py`. Dados brutos: `bench/measurements.csv`. Gráfico: `bench/speedup.png`.

O endpoint valida `size` (máx. 20.000) e `threads` (máx. `núcleos disponíveis × 4`) na borda, para evitar que uma única requisição esgote memória (`size` sem teto) ou threads do SO (`threads` sem teto) — achado da auditoria de segurança (`security-auditor`). Todas as medições abaixo estão dentro desses limites.

## Por que 1.000–8.000 registros, e não 100 mil / 1 milhão diretamente

O algoritmo é O(n²) (ver `docs/justificativa.md`). A escala de tempo medida é quadrática e confirmada nos dados abaixo. Extrapolando a partir do ponto medido em `n=8000` (23.017 ms sequencial):

| n | tempo sequencial estimado | com 8 threads (speedup ~3.7x medido) |
|---|---|---|
| 100.000 | ≈ 3.596 s (~1h) | ≈ 970 s (~16 min) |
| 1.000.000 | ≈ 359.641 s (~4,2 dias) | ≈ 96.700 s (~27h) |

Rodar a versão sequencial em 1 milhão de registros de fato, uma única vez, levaria dias — inviável para esta atividade e, mais importante, inviável em produção. Por isso a tabela de medições reais usa tamanhos de 1.000 a 8.000, onde o crescimento O(n²) já é claramente visível e mensurável em segundos, e a extrapolação para 100 mil / 1 milhão é calculada analiticamente a partir da própria Big-O confirmada — o mesmo raciocínio que se aplicaria à escala nacional do enunciado.

## Tabela de resultados medidos

| n | threads | modo | tempo (ms) | speedup (vs. 1 thread) |
|---|---------|------|------------|-------------------------|
| 1000 | 1 | platform | 362 | 1.00x |
| 1000 | 2 | platform | 288 | 1.26x |
| 1000 | 4 | platform | 176 | 2.06x |
| 1000 | 8 | platform | 106 | 3.42x |
| 1000 | 8 | virtual  | 118 | 3.07x |
| 2000 | 1 | platform | 1494 | 1.00x |
| 2000 | 2 | platform | 1137 | 1.31x |
| 2000 | 4 | platform | 682 | 2.19x |
| 2000 | 8 | platform | 408 | 3.66x |
| 2000 | 8 | virtual  | 395 | 3.78x |
| 4000 | 1 | platform | 5842 | 1.00x |
| 4000 | 2 | platform | 4590 | 1.27x |
| 4000 | 4 | platform | 2625 | 2.23x |
| 4000 | 8 | platform | 1575 | 3.71x |
| 4000 | 8 | virtual  | 1567 | 3.73x |
| 8000 | 1 | platform | 23017 | 1.00x |
| 8000 | 2 | platform | 17486 | 1.32x |
| 8000 | 4 | platform | 10737 | 2.14x |
| 8000 | 8 | platform | 6194 | 3.72x |
| 8000 | 8 | virtual  | 6343 | 3.63x |

`duplicatesFound` é idêntico entre sequencial, paralelo e virtual para cada `n` (confirmado em `bench/measurements.csv` e nos testes automatizados) — sem race condition.

## Gráfico

![speedup](speedup.png)

(gerado em `bench/speedup.png`: tempo vs. threads por tamanho de entrada, e speedup vs. threads comparado ao speedup ideal linear)

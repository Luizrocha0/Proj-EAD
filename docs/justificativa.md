# Justificativa — Paralelização da Detecção de Duplicatas (Rota Vital)

## Operação escolhida

Detecção de duplicatas fuzzy no histórico de requisições/medicamentos: para cada par de registros textuais (ex.: nomes de medicamento digitados por atendentes), calcula-se a distância de Levenshtein e conta-se como duplicata todo par com distância ≤ 2.

## Por que esta operação, e não outra

Das candidatas levantadas (relatórios/estatísticas, cruzamento requisição × estoque, ordenação de filas por prioridade, detecção de duplicatas, validações em lote), a detecção de duplicatas foi a única em que o tempo é dominado por **cálculo**, não por espera de I/O:

- **Relatórios/estatísticas** e **cruzamento requisição × estoque** dependem majoritariamente de consultas e joins no banco — o gargalo ali é a query SQL, não a CPU da aplicação. Paralelizar em threads na aplicação não ataca a causa.
- **Ordenação de filas por prioridade** é O(n log n) com uma heap/`Comparator` — rápida mesmo em milhões de itens; não há gargalo de CPU relevante na escala do problema.
- **Detecção de duplicatas** por comparação par a par é O(n²) — cresce quadraticamente com o volume, e cada comparação é uma operação de CPU pura (programação dinâmica sobre strings), sem tocar banco ou rede depois que os dados estão em memória.

## Big-O da solução sequencial

Para `n` registros de tamanho médio `L`:

- Comparações de pares: `n·(n-1)/2` = O(n²).
- Cada comparação roda Levenshtein, que é O(L²) no pior caso (aqui `L` é pequeno e roughly constante — nomes de medicamento — então o termo dominante na prática é o O(n²) de pares).
- Custo total: **O(n² · L²)**, dominado por O(n²) para `L` fixo.

Confirmado empiricamente (`bench/measurements.csv`, sequencial): dobrar `n` quadruplica o tempo (1000→362ms, 2000→1494ms, 4000→5842ms, 8000→23017ms — fator ~4x a cada duplicação de `n`, exatamente O(n²)).

## Onde está o gargalo

100% CPU, sem I/O no meio do laço: os dados já estão carregados em uma `List<String>` em memória; o laço duplo só faz aritmética de programação dinâmica. Em escala nacional (milhões de requisições), rodar isso em uma única thread bloqueia a aplicação por minutos a horas — inviável para um endpoint que precisa responder a um painel.

## Por que os dados são particionáveis

A comparação de duplicatas é **embaraçosamente paralela** no índice externo `i`: o par `(i, j)` com `j > i` é independente de qualquer outro par `(i', j')`. É possível dividir o intervalo `[0, n)` de valores de `i` em fatias disjuntas, atribuir uma fatia a cada thread, e cada thread produz um contador parcial (`localDuplicates`) sem escrever em memória compartilhada. A soma final dos contadores parciais (na thread principal, após `Future.get()`) é a única seção que toca estado compartilhado, e isso acontece fora do laço paralelo — não há necessidade de lock nem de estrutura atômica dentro do laço quente.

Essa é a razão pela qual as versões sequencial e paralela devolvem exatamente o mesmo resultado nos testes (`DuplicateDetectionServiceTest`): não há race condition porque não há escrita compartilhada durante o processamento, só na agregação de resultados já prontos.

# Justificativa — Paralelização da detecção de duplicatas

## Operação escolhida

A detecção fuzzy de duplicatas compara nomes textuais de medicamentos e conta pares cuja distância de Levenshtein é menor ou igual a dois. A demonstração usa dados sintéticos reprodutíveis de `DataGeneratorService`.

Entre candidatas como relatórios, cruzamento de dados, ordenação e validações em lote, esta operação oferece trabalho de CPU explícito e independente por par. Em `DuplicateDetectionService`, os textos já estão em memória e o laço de comparação não acessa banco nem rede. A escolha se apoia nesse código e no custo medido, sem generalizar o gargalo das outras operações, que dependeria de suas implementações e cargas.

## Custo e evidência

A versão sequencial percorre `i` de zero a `n-1` e `j` de `i+1` a `n-1`. Cada par aparece uma vez: são `n(n-1)/2` comparações. `FuzzyMatcher` calcula Levenshtein por programação dinâmica, com custo O(L²) para textos de comprimento limitado por `L`. O trabalho total é **O(n² · L²)**, ou O(n²) quando `L` permanece limitado.

Na série atual, a média HTTP sequencial é 23.883,00 ms para 8.000 registros e 97.867,00 ms para 16.000: crescimento de aproximadamente 4,10x. Isso é compatível com o custo quadrático, sem provar a complexidade apenas por dois pontos. O endpoint inclui também geração de dados e transporte HTTP; a identificação de trabalho de CPU vem do laço e do cálculo de Levenshtein. Os dados completos e os limites de extrapolação constam em [medições](medicoes.md).

## Divisão e agregação

`runPartitioned` divide o intervalo de índices externos `i` em fatias disjuntas, usando `ceil(n / slices)`. Cada tarefa compara seus índices com todos os `j > i`, sem perder ou repetir pares. A lista é apenas lida durante a detecção.

Cada tarefa mantém seu próprio `localDuplicates` e retorna uma contagem. `invokeAll` aguarda as tarefas; a thread chamadora soma os valores obtidos com `Future.get()`. Não há contador compartilhado atualizado no laço, portanto essa organização dispensa locks ou operações atômicas para contar pares.

As fatias têm quantidades semelhantes de índices, mas cargas diferentes: os primeiros índices possuem mais parceiros. Esse desbalanceamento e o custo do pool e da agregação limitam o ganho. Com oito threads, a série atual registra speedups de 3,31x e 2,37x, respectivamente.

As 40 execuções retornam contagens iguais para cada tamanho. `DuplicateDetectionServiceTest` verifica a equivalência entre versões e casos de borda com testes automatizados. A [análise](analise.md) relaciona o resultado a concorrência, paralelismo e possibilidades futuras da Unidade 2.

# Rota Vital · AED Unidade 2

A API atende o primeiro pedido da fila FIFO. Calcula a rota, baixa a quantidade integral e retorna as bolsas em ordem FEFO. A mesma operação funciona com estoque em memória e PostgreSQL.

## Fluxo do atendimento

1. `POST /api/requisicoes` recebe o pedido hospitalar.
2. `POST /api/requisicoes/atender` confere o ID na cabeça da fila e valida o percurso. O destino deve corresponder ao hospital do pedido.
3. O estoque cria um índice por tipo sanguíneo. A matriz escolhe os grupos doadores compatíveis. A consulta hash recupera as posições das bolsas desses grupos.
4. A inserção ordenada guarda até a quantidade pedida. Vem primeiro a menor validade. Empates seguem a ordem LIFO do estoque. Uma bolsa com validade igual à data de referência continua elegível. A data de referência é a data local do servidor.
5. A API confirma a baixa e retira o pedido. No PostgreSQL, a baixa usa a chave técnica `id_seq` em uma transação. Em memória, usa a posição da ocorrência na lista. Assim, IDs públicos repetidos preservam as outras ocorrências.

A compatibilidade didática corresponde à de concentrados de hemácias para ABO e RhD. A matriz usa linhas para receptores e colunas para doadores. [Australian Red Cross Lifeblood](https://www.lifeblood.com.au/patients-recipients/blood-plasma-platelets/blood-for-transfusion/matching-blood-groups)

O roteirizador escolhe a parada intermediária mais próxima da posição atual. A origem abre o percurso e o hospital fica no final. A distância soma os trechos euclidianos em quilômetros. Empates seguem a ordem das paradas recebidas.

## Demonstrar pela API

Inicie uma instância com estoque vazio no perfil `u1`.

```powershell
.\mvnw.cmd spring-boot:run
```

Em outro terminal, execute o cenário sintético.

```powershell
$api = 'http://localhost:8080/api'
$hoje = (Get-Date).Date
$bolsas = @(
    @{ id = 'RV-O-MELHOR'; tipoSanguineo = 'O-'; validade = $hoje }
    @{ id = 'RV-A-DEPOIS'; tipoSanguineo = 'A-'; validade = $hoje.AddDays(5) }
    @{ id = 'RV-A-RH'; tipoSanguineo = 'A+'; validade = $hoje }
    @{ id = 'RV-O-VENCIDA'; tipoSanguineo = 'O-'; validade = $hoje.AddDays(-1) }
)
foreach ($bolsa in $bolsas) {
    $body = @{
        id            = $bolsa.id
        tipoSanguineo = $bolsa.tipoSanguineo
        volumeMl      = 450
        dataColeta    = $hoje.AddDays(-10).ToString('yyyy-MM-dd')
        validade      = $bolsa.validade.ToString('yyyy-MM-dd')
        doador        = 'Sintético'
    }
    Invoke-RestMethod -Uri "$api/estoque" -Method Post -ContentType 'application/json' `
        -Body ($body | ConvertTo-Json)
}

$pedido = @{
    id = 'REQ-AV2'; hospitalId = 'HOSP'; tipoSanguineo = 'A-'; quantidadeBolsas = 2
}
Invoke-RestMethod -Uri "$api/requisicoes" -Method Post -ContentType 'application/json' `
    -Body ($pedido | ConvertTo-Json)

$atendimento = @{
    requisicaoId = 'REQ-AV2'
    origem       = @{ id = 'CENTRO'; xKm = 0; yKm = 0 }
    paradas      = @(
        @{ id = 'LONGE'; xKm = 5; yKm = 0 }
        @{ id = 'PERTO'; xKm = 2; yKm = 0 }
    )
    destino      = @{ id = 'HOSP'; xKm = 8; yKm = 0 }
}
Invoke-RestMethod -Uri "$api/requisicoes/atender" -Method Post -ContentType 'application/json' `
    -Body ($atendimento | ConvertTo-Json -Depth 4) | ConvertTo-Json -Depth 6
```

O resultado traz `RV-O-MELHOR` e `RV-A-DEPOIS`, nessa ordem. O trajeto é `CENTRO → PERTO → LONGE → HOSP`, com 8 km. A resposta lista ID, tipo, volume e validade de cada unidade. `GET /api/estoque` permite conferir as duas ocorrências restantes.

| Situação | Resposta |
| --- | --- |
| Pedido atendido | `200` com `requisicao`, `bolsas`, `rota` e `restantes` |
| Fila vazia com uma solicitação bem formada | `200` com mensagem de fila vazia |
| Corpo inválido, destino diferente ou ponto duplicado | `400` |
| ID já pendente, pedido fora da cabeça ou estoque insuficiente | `409` |
| Outra operação retira uma ocorrência selecionada | `409` com reversão da baixa em PostgreSQL |

`paradas` é opcional. Cada ponto usa `id`, `xKm` e `yKm`. São aceitas até 256 paradas e coordenadas entre −50.000 e 50.000 km. A fila continua em memória por instância. No perfil `infra`, o estoque e suas baixas ficam no PostgreSQL.

## Tradução comentada de C para Java

Os fontes C ficam em `aed-c/src/u2/`. Os módulos Java ficam em `src/main/java/com/rotavital/aed/u2/`. Os dois lados usam as mesmas prioridades, a mesma ordem dos oito tipos e os mesmos desempates. As versões C recebem vetores preparados pelo chamador. Os adaptadores Java montam esses vetores usando as estruturas da aplicação.

### Matriz ABO/Rh

O `enum` C representa cada tipo por inteiro. O Java usa a posição do valor do `enum`. A leitura direta preserva a orientação receptor × doador.

```c
/* A validação impede que o sentinela TIPO_INVALIDO indexe a tabela. */
return tipo_valido(doador) && tipo_valido(receptor) && matriz[receptor][doador];
```

```java
// TipoSanguineo.exigir valida o rótulo na borda.
return MATRIZ[receptor.ordinal()][doador.ordinal()];
```

### Índice de estoque

O resto da divisão distribui os oito tipos em cinco baldes. Uma chave nova entra na frente da cadeia do balde. Outra bolsa da mesma chave aumenta seu vetor de posições. As posições conservam a ordem em que entram.

```c
/* proxima aponta para a entrada anterior desse balde. */
*entrada = (Entrada){.tipo = tipo, .proxima = indice->baldes[chave]};
indice->baldes[chave] = entrada;
```

```java
// A referência proxima preserva a mesma cadeia de colisões.
entrada = new Entrada(tipo, baldes[balde]);
baldes[balde] = entrada;
```

Em C, `realloc` amplia cada vetor e `indice_estoque_liberar` libera as cadeias. Em Java, `ArrayList` gerencia o vetor de posições. O coletor de lixo recupera a memória do índice.

### Inserção FEFO

Cada comparação usa validade e posição no estoque. `strcmp` compara datas ISO em C. `LocalDate.compareTo` compara datas em Java. A busca binária encontra a primeira posição que pode receber a candidata. Uma lista cheia descarta o último elemento se a candidata entrar.

```c
/* inicio já contém o limite inferior calculado pela busca. */
if (*usadas == quantidade) {
    (*usadas)--;
}
memmove(ordenadas + inicio + 1, ordenadas + inicio, (*usadas - inicio) * sizeof(*ordenadas));
ordenadas[inicio] = candidata;
(*usadas)++;
```

```java
// removeLast libera a última posição. add desloca o sufixo.
if (ordenadas.size() == quantidade) {
    ordenadas.removeLast();
}
ordenadas.add(posicao, candidata);
```

O código C preenche o vetor de saída apenas com quantidade integral. O Java retorna a lista de índices completos ou uma lista vazia. Ambos mantêm a bolsa com validade da data de referência.

### Vizinho mais próximo

Os dois laços começam com distância infinita, ignoram pontos visitados e guardam o menor trecho. O trecho seguinte parte do ponto selecionado.

```c
/* O teste estrito preserva a ordem de entrada nos empates. */
if (trecho < menor_distancia) {
    mais_proxima = i;
    menor_distancia = trecho;
}
```

```java
// A mesma comparação garante o mesmo critério de desempate.
if (distancia < menorDistancia) {
    maisProxima = i;
    menorDistancia = distancia;
}
```

`atender_requisicao` combina percurso e seleção em C. `RequisicaoService.atenderProxima` usa os mesmos algoritmos em Java. A baixa fica com o adaptador de estoque.

## Complexidade derivada da implementação

Considere $n$ registros no estoque, $c$ bolsas válidas e compatíveis, $q$ bolsas pedidas e $p$ paradas intermediárias. Considere $k = \min(c,q)$. Os oito tipos sanguíneos constituem um domínio fixo.

| Algoritmo | Tempo | Espaço auxiliar | Justificativa |
| --- | --- | --- | --- |
| Matriz ABO/Rh | $O(1)$ por par | $O(1)$ | Uma célula da matriz 8 × 8. A seleção consulta oito células da linha do receptor. |
| Índice hash | $O(n)$ para montar. $O(1)$ para localizar um grupo | $O(n)$ | Um percurso do estoque. Cada cadeia tem até duas chaves. Acrescentar uma posição custa $O(1)$ amortizado. Percorrer o grupo custa $O(g)$ para $g$ posições. |
| Inserção FEFO | $O(n + c \log(k+1) + ck)$ | $O(n + k)$ em Java e $O(n + q)$ em C | Depois do índice, cada candidata faz busca binária no prefixo de até $k$ posições. A inserção pode deslocar até $k$ posições. Em C, o vetor de trabalho tem capacidade $q$. |
| Vizinho mais próximo | $O(1 + p^2)$ | $O(p)$ incluindo o trajeto | Há $p$ passagens pelo vetor de $p$ índices. Calculam-se $p(p+1)/2$ distâncias candidatas, mais o último trecho. Em C, o vetor de visitados tem 256 posições fixas. |
| Busca sequencial | $O(1)$ no melhor caso e $O(n)$ no pior | $O(1)$ | Retorna na primeira igualdade ou visita o vetor inteiro. |
| Busca binária | $O(\log(n+1))$ | $O(1)$ | Cada passo reduz o intervalo aproximadamente à metade. A igualdade é testada após encontrar o limite inferior. O vetor chega ordenado. |

Se $q$ é pequeno, a seleção FEFO desloca poucos elementos por candidata. Se $q$ cresce com $c$, os deslocamentos podem ser quadráticos. No PostgreSQL, o atendimento também lê o estoque e emite até $q$ exclusões parametrizadas pela chave primária. Essa leitura e essas escritas têm custos de execução próprios do banco.

### Ordenações da AV2

As seis implementações recebem vetores de inteiros em C e Java. O atendimento usa a inserção FEFO.

| Ordenação | Melhor | Médio | Pior | Memória auxiliar e justificativa |
| --- | --- | --- | --- | --- |
| Bubble Sort | $O(n)$ | $O(n^2)$ | $O(n^2)$ | $O(1)$. Para após a primeira passagem sem troca. |
| Selection Sort | $O(n^2)$ | $O(n^2)$ | $O(n^2)$ | $O(1)$. Busca o mínimo de cada sufixo inteiro. |
| Insertion Sort | $O(n)$ | $O(n^2)$ | $O(n^2)$ | $O(1)$. Desloca somente os itens maiores que a chave. |
| Merge Sort | $O(n \log n)$ | $O(n \log n)$ | $O(n \log n)$ | $O(n)$. Cada nível divide, intercala e copia. As chamadas reaproveitam um vetor auxiliar. |
| Quick Sort | $O(n)$ | $O(n \log n)$ | $O(n^2)$ | $O(\log n)$ de pilha. Partição em três faixas. Recursão sobre a menor faixa e laço sobre a maior. Se todos os valores empatam, basta uma partição. |
| Heap Sort | $O(n)$ | $O(n \log n)$ | $O(n \log n)$ | $O(1)$. Construção de baixo para cima. Cada extração pode descer a altura do heap. Com valores iguais, cada descida encerra na raiz. |

Os custos médios da tabela consideram permutações aleatórias de chaves distintas. `memcpy` em C e `System.arraycopy` em Java copiam os trechos intercalados do Merge Sort. O Quick Sort usa o mesmo pivô central e os mesmos limites semiabertos nos dois idiomas.

## Testes

```powershell
.\mvnw.cmd test
.\mvnw.cmd -Pintegration verify
```

A segunda execução usa Docker e PostgreSQL descartável via Testcontainers.

```bash
make -C aed-c test
```

| Evidência | Cenários |
| --- | --- |
| `AlgoritmosAedTest` e `test_u2.c` | 64 pares ABO/Rh, colisão no hash, FEFO e empates, rotas diretas e com paradas, buscas e seis ordenações |
| `AtendimentoAedTest` | Estoque em memória, pedido FIFO, baixa da ocorrência correta, quantidade insuficiente, entrada inválida e repetição de chamada |
| `AtendimentoAedIT` | API e PostgreSQL reais. Impede a segunda exclusão em uma transação e confere a reversão, a permanência na fila e a tentativa bem-sucedida. |

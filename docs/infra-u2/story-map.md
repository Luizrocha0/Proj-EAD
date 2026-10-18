# Story Map, Critérios de Aceite e Desenho Arquitetural da Persistência — Release 1

**Projeto:** Rota Vital — Infraestrutura de Software (Unidade 2 — Desafios PI — Parte 1)  
**Documento:** Story Map e Critérios de Aceite da Release 1  
**Data:** 17/10/2026  
**Status:** Aprovado para Implementação (Etapa 1 Concluída)  
**Autores:** Equipe Rota Vital (Eduardo Borges, Luiz Henrique Rocha, Eliziane Mota, Pedro Iranildo, Ricardo Severiano de Souza Filho)

---

## 1. Visão Geral e Contexto da Release 1

O projeto **Rota Vital** evolui da entrega da Unidade 1 (estruturas de dados manuais em C e Java para fins pedagógicos de AED e concorrência/paralelismo em SO) para a entrega da **Unidade 2** (Infraestrutura de Software: Containerização Docker/Compose, Persistência Relacional com PostgreSQL, TDD rigoroso em três níveis e Esteira de Integração Contínua com GitHub Flow e Deploy Automatizado).

### 1.1. Propósito e Diretrizes de Escopo
A **Release 1** tem como objetivo central fornecer **persistência relacional real e verificável** para a gestão do estoque de bolsas de sangue, garantindo a sobrevivência e a consistência dos dados após reinicializações do contêiner ou do processo da aplicação, sem descaracterizar nem descartar o patrimônio acadêmico da U1.

- **O que entra na Release 1 (Escopo Ativo):**
  - Implementação de persistência relacional para o estoque de bolsas de sangue no PostgreSQL.
  - Arquitetura desacoplada via interface de armazenamento (`EstoqueStorage`) controlada por **perfis Spring**:
    - Perfil **`u1`** (padrão): opera 100% em memória usando a lista simplesmente encadeada manual com nós próprios (`ListaEstoque`), garantindo equivalência C $\leftrightarrow$ Java e execução livre de banco.
    - Perfil **`infra`**: opera contra o banco PostgreSQL real via Spring JDBC (`JdbcTemplate`) e migrações versionadas Flyway, ativado no Compose e em produção.
  - Implementação orientada a testes (TDD) das três histórias centrais de persistência (**H1**, **H2**, **H3**).
  - Resolução transparente de identificadores de bolsa repetidos e ordenação temporal $LIFO$ através de chave substituta técnica autoincrementável (`id_seq`).
  - Resiliência na deserialização REST para números literais das fixtures oficiais.

- **O que permanece inalterado (Fora do Escopo da Release 1):**
  - A fila FIFO de requisições hospitalares (`FilaRequisicoes` / `/api/requisicoes`) permanece operando exclusivamente com a estrutura de nós encadeados em memória da U1.
  - Os algoritmos de detecção de duplicatas com threads e cálculo de similaridade Levenshtein (`/api/duplicates/detect`) permanecem intactos.
  - Não serão introduzidos algoritmos não solicitados (como FEFO, roteirização de rotas, índice de estoque por hash ou regras hematológicas ABO/Rh complexas).

---

## 2. Backbone e Story Map da Release 1

### 2.1. Jornada do Operador do Banco de Sangue

A jornada do operador hospitalar no sistema Rota Vital é estruturada em quatro macroatividades essenciais (o **Backbone** do sistema):

1. **Recepção e Triagem de Doações:**
   O operador recebe o sangue coletado, realiza a triagem sorológica inicial e insere a bolsa no sistema para torná-la disponível para transfusões.
2. **Gestão e Consulta do Estoque:**
   O operador monitora a quantidade de bolsas disponíveis, verifica datas de validade, inspeciona tipos sanguíneos e localiza bolsas específicas para atendimento.
3. **Dispensação e Baixa de Bolsas:**
   Quando uma bolsa é selecionada para transfusão, descarte por vencimento ou transferência inter-hospitalar, o operador efetua a baixa (remoção) do estoque.
4. **Auditoria e Atendimento de Demandas:**
   O operador acompanha a fila de solicitações hospitalares e valida a consistência histórica do banco mesmo após quedas de energia ou reinicializações do servidor.

---

### 2.2. Diagrama do Story Map da Release 1

```
+=================================================================================================================+
| JORNADA DO OPERADOR (BACKBONE)                                                                                  |
+--------------------------+-----------------------------+---------------------------+----------------------------+
| 1. Recepção & Triagem    | 2. Gestão de Estoque        | 3. Dispensação & Baixa    | 4. Auditoria & Atendimento |
+==========================+=============================+===========================+============================+
|                          |                             |                           |                            |
| RELEASE 1 (PERSISTÊNCIA) |                             |                           |                            |
|                          |                             |                           |                            |
| [H1] Cadastrar e         | [H2] Consultar estoque      | [H3] Remover bolsa de     | [Auditoria Operacional]    |
| recuperar bolsa          | persistido em ordem         | forma persistente         | Comprovar sobrevivência    |
| persistida no PostgreSQL | LIFO, com envelope e        | garantindo remoção da     | e consistência dos dados   |
| pós-reinício             | integridade de duplicatas   | ocorrência mais recente   | após reinício do serviço   |
|                          |                             |                           |                            |
+--------------------------+-----------------------------+---------------------------+----------------------------+
|                          |                             |                           |                            |
| RELEASES FUTURAS         |                             |                           |                            |
|                          |                             |                           |                            |
| - Cadastro em lote       | - Filtro avançado por ABO   | - Baixa automática por    | - Persistência da fila de  |
| - Leitura de código de   | - Regra FEFO (validade)     | requisição atendida       | requisições em banco       |
|   barras GS1-128         | - Alertas de validade       | - Descarte de vencidos    | - Histórico e auditoria    |
|                          |                             |                           |   de transações (log CDC)  |
+=================================================================================================================+
```

---

## 3. Especificação Exaustiva das Histórias da Release 1

---

### 3.1. História H1: Cadastrar e Recuperar Bolsa Persistida

#### Narrativa Ágil
> **Como** operador do banco de sangue,  
> **Quero** cadastrar uma nova bolsa de sangue através da API REST sob o perfil de infraestrutura e recuperá-la posteriormente pelo seu identificador,  
> **Para que** os dados da coleta fiquem gravados de forma segura e durável no PostgreSQL, sobrevivendo a reinicializações da aplicação e garantindo a rastreabilidade do estoque.

#### Regras de Negócio Envolvidas
- **RN1.1 (Campos Obrigatórios):** O cadastro de uma bolsa exige todos os 6 atributos canônicos preenchidos: `id` (String não vazia), `tipoSanguineo` (ex: `"O+"`, `"A-"`), `volumeMl` (double positivo, ex: `450.0`), `dataColeta` (LocalDate no formato ISO-8601 `YYYY-MM-DD`), `validade` (LocalDate no formato `YYYY-MM-DD`) e `doador` (String não vazia).
- **RN1.2 (Resiliência de Tipos na Entrada):** A API deve receber requisições com `volumeMl` informado tanto como número literal JSON (`450.0`) quanto como String numérica (`"450.0"`), garantindo interoperabilidade com as fixtures da U1 (`fixtures/bolsas_u1.json`).
- **RN1.3 (Suporte a Identificadores Repetidos):** O sistema não deve impor restrição de unicidade (`UNIQUE`) ao campo de negócio `id`. Se uma bolsa com o mesmo ID for cadastrada novamente (ex: reentrada de lote ou devolução com mesmo protocolo), o sistema deve aceitar e gravar como um novo registro independente.
- **RN1.4 (Contrato de Resposta do Cadastro):** O endpoint `POST /api/estoque` deve retornar HTTP 200 OK com corpo JSON contendo exatamente as chaves `mensagem` e `tamanho`:
  ```json
  {
    "mensagem": "Bolsa adicionada ao estoque",
    "tamanho": 1
  }
  ```
- **RN1.5 (Persistência Efetiva e Sem Fallback Silencioso):** Sob o perfil `infra`, a gravação deve ser confirmada no PostgreSQL. Caso o banco esteja indisponível ou ocorra falha de I/O, a operação deve falhar imediatamente (HTTP 500 / DataAccessException), sem mascarar o erro e sem realizar fallback silencioso para a memória.
- **RN1.6 (Recuperação Pós-Reinício):** Ao consultar uma bolsa recém-cadastrada via `GET /api/estoque/{id}`, a resposta deve retornar todos os campos idênticos aos gravados, mesmo após o processo da aplicação ter sido finalizado e reiniciado.

#### Critérios de Aceite Observáveis
- **CA1.1:** Enviar `POST /api/estoque` com dados válidos no perfil `infra` resulta em HTTP 200 OK com `{"mensagem": "Bolsa adicionada ao estoque", "tamanho": N}` e insere um registro na tabela `bolsas` do PostgreSQL.
- **CA1.2:** Enviar payload contendo `volumeMl` como literal numérico `450.0` é processado com sucesso sem erros de deserialização Jackson.
- **CA1.3:** Enviar `GET /api/estoque/{id}` imediatamente após o cadastro retorna HTTP 200 OK com o JSON completo da bolsa (`id`, `tipoSanguineo`, `volumeMl`, `dataColeta`, `validade`, `doador`).
- **CA1.4:** Após cadastrar uma bolsa via API, reiniciar o contexto do Spring Boot (ou container da API) mantendo o banco intacto; um novo `GET /api/estoque/{id}` deve retornar HTTP 200 OK com os dados exatos da bolsa.
- **CA1.5:** Se a conexão com o PostgreSQL falhar no perfil `infra`, o `POST /api/estoque` não pode retornar HTTP 200 nem responder tamanho incrementado falsamente.

#### Tarefas Granulares de Desenvolvimento
- **T1.1 (Infra/DDL):** Criar script de migração Flyway `V1__criar_tabela_bolsas.sql` modelando a tabela `bolsas` com chave substituta `id_seq BIGSERIAL PRIMARY KEY`, campos de domínio e índice composto `idx_bolsas_id_seq ON bolsas(id, id_seq DESC)` para otimização de busca e remoção temporal LIFO em $O(1)$.
- **T1.2 (Storage Adapter):** Implementar o adaptador `JdbcEstoqueStorage` ativado sob `@Profile("infra")` com os métodos `salvar(Bolsa)` e `buscarPorId(String)`.
- **T1.3 (DTO/Deserialização):** Ajustar o payload do `EstoqueController` para utilizar DTO resiliente ou deserializador customizado tolerante a números literais e strings.
- **T1.4 (Verificação de Reinício):** Implementar teste de integração que realiza escrita no banco via perfil `infra`, encerra o contexto Spring, recria o contexto e valida a leitura dos dados persistidos.

---

### 3.2. História H2: Consultar Estoque Persistido

#### Narrativa Ágil
> **Como** operador do banco de sangue,  
> **Quero** listar todas as bolsas disponíveis no estoque e consultar bolsas específicas pelo identificador sob o perfil de infraestrutura,  
> **Para que** eu possa visualizar o inventário atualizado em ordem LIFO (mais recente primeiro) no formato de envelope padronizado, inclusive com estoque vazio e múltiplos registros com o mesmo ID.

#### Regras de Negócio Envolvidas
- **RN2.1 (Contrato do Envelope de Listagem):** O endpoint `GET /api/estoque` deve retornar HTTP 200 OK contendo o envelope JSON padronizado com os campos `bolsas` (array de objetos) e `tamanho` (inteiro):
  ```json
  {
    "bolsas": [ ... ],
    "tamanho": 5
  }
  ```
- **RN2.2 (Tratamento de Estoque Vazio):** Quando não houver nenhuma bolsa cadastrada no banco, `GET /api/estoque` deve retornar HTTP 200 OK com array vazio e tamanho zero:
  ```json
  {
    "bolsas": [],
    "tamanho": 0
  }
  ```
- **RN2.3 (Ordenação Temporal Estrita LIFO):** A listagem de bolsas deve seguir a semântica da pilha / inserção na cabeça da lista encadeada da U1: as bolsas cadastradas mais recentemente devem aparecer primeiro no array (`ORDER BY id_seq DESC`).
- **RN2.4 (Exibição Completa de IDs Repetidos):** Se o banco contiver duas ou mais bolsas com o mesmo identificador de negócio `id`, a listagem geral `GET /api/estoque` deve exibir todas as ocorrências no array `bolsas`, cada uma preservando sua posição temporal LIFO.
- **RN2.5 (Consulta Pontual do Mais Recente):** O endpoint `GET /api/estoque/{id}`, caso existam múltiplos registros com o mesmo `id`, deve retornar a ocorrência **mais recente** cadastrada (`ORDER BY id_seq DESC LIMIT 1`).
- **RN2.6 (Tratamento de Bolsa Inexistente):** O endpoint `GET /api/estoque/{id}` deve retornar HTTP 404 Not Found caso o ID informado não exista no estoque.

#### Critérios de Aceite Observáveis
- **CA2.1:** Em banco limpo, `GET /api/estoque` retorna status 200 e payload `{"bolsas": [], "tamanho": 0}`.
- **CA2.2:** Ao inserir as 5 bolsas das fixtures canônicas na ordem 001 $\rightarrow$ 002 $\rightarrow$ 003 $\rightarrow$ 004 $\rightarrow$ 005, o `GET /api/estoque` retorna `tamanho: 5` com o array `bolsas` exatamente na ordem reversa: `["BOLSA-005", "BOLSA-004", "BOLSA-003", "BOLSA-002", "BOLSA-001"]`.
- **CA2.3:** Ao inserir `BOLSA-001` (coleta A), depois `BOLSA-002`, e depois `BOLSA-001` (coleta B com doador diferente), `GET /api/estoque` retorna `tamanho: 3` contendo ambas as ocorrências de `BOLSA-001`.
- **CA2.4:** No cenário de CA2.3, o endpoint `GET /api/estoque/BOLSA-001` retorna HTTP 200 com os dados exclusivos da coleta B (ocorrência mais recente).
- **CA2.5:** O endpoint `GET /api/estoque/BOLSA-INEXISTENTE` retorna status 404 Not Found sem expor stack trace.
- **CA2.6:** A integridade da ordenação e do envelope é comprovada após reinicialização do contexto da aplicação.

#### Tarefas Granulares de Desenvolvimento
- **T2.1 (Storage Query All):** Implementar em `JdbcEstoqueStorage` o método `listarTodas()` executando consulta SQL com `ORDER BY id_seq DESC` e mapeamento de ResultSet para `Bolsa[]`.
- **T2.2 (Storage Count):** Implementar em `JdbcEstoqueStorage` o método `tamanho()` executando `SELECT COUNT(*) FROM bolsas`.
- **T2.3 (LIFO Single Query):** Implementar em `JdbcEstoqueStorage` o método `buscarPorId(String id)` com cláusula `WHERE id = ? ORDER BY id_seq DESC LIMIT 1`.
- **T2.4 (Controller Envelope Validation):** Garantir que `EstoqueController.listar()` monte o envelope `{"bolsas": [...], "tamanho": N}` com precisão tipada e sem nulls.
- **T2.5 (Verificação de Reinício):** Implementar teste de integração cobrindo persistência e recuperação da ordenação LIFO completa e integridade de IDs repetidos após reinício do contexto Spring mantendo o banco PostgreSQL.

---

### 3.3. História H3: Remover Bolsa de Forma Persistente

#### Narrativa Ágil
> **Como** operador do banco de sangue,  
> **Quero** remover uma bolsa do estoque pelo seu identificador via API REST sob o perfil de infraestrutura,  
> **Para que** a baixa seja refletida de forma durável no PostgreSQL, removendo exclusivamente a ocorrência mais recente em caso de duplicatas e preservando o histórico das demais.

#### Regras de Negócio Envolvidas
- **RN3.1 (Contrato de Resposta da Remoção):** O endpoint `DELETE /api/estoque/{id}` deve retornar HTTP 200 OK com o envelope JSON:
  ```json
  {
    "mensagem": "Bolsa removida",
    "tamanho": N
  }
  ```
  onde `tamanho` reflete o total de bolsas restantes após a exclusão.
- **RN3.2 (Remoção Inexistente ou Estoque Vazio):** Se o identificador não for encontrado no banco ou o estoque estiver vazio, o endpoint `DELETE /api/estoque/{id}` deve retornar HTTP 404 Not Found, sem alterar o estado do banco.
- **RN3.3 (Regra Estrita de IDs Repetidos):** Caso existam duas ou mais bolsas cadastradas com o mesmo identificador `id`, a remoção deve excluir **exclusivamente a ocorrência mais recente** ($LIFO$). As ocorrências mais antigas daquele mesmo ID devem permanecer intocadas no banco de dados.
- **RN3.4 (Consulta Imediata Pós-Remoção de Duplicata):** Após a remoção da ocorrência mais recente de um ID repetido, uma consulta subsequente `GET /api/estoque/{id}` deve retornar com sucesso (HTTP 200) a ocorrência imediatamente anterior daquele ID.
- **RN3.5 (Persistência Durável da Deleção Pós-Reinício):** Uma bolsa removida com sucesso não deve reaparecer após reinicialização da aplicação ou do contêiner.

#### Critérios de Aceite Observáveis
- **CA3.1:** Executar `DELETE /api/estoque/{id}` em bolsa existente retorna HTTP 200 OK com `{"mensagem": "Bolsa removida", "tamanho": N-1}` e o registro é fisicamente deletado da tabela `bolsas`.
- **CA3.2:** Executar `DELETE /api/estoque/BOLSA-INEXISTENTE` retorna status 404 Not Found e o tamanho do estoque permanece inalterado.
- **CA3.3:** Cadastrar `BOLSA-001` (Doador A), depois `BOLSA-001` (Doador B). Executar `DELETE /api/estoque/BOLSA-001`. A operação retorna HTTP 200 e `tamanho: 1`. Uma consulta `GET /api/estoque/BOLSA-001` subsequente retorna com sucesso a bolsa de Doador A.
- **CA3.4:** Após remoção bem-sucedida, reiniciar a aplicação e executar `GET /api/estoque/{idRemovido}`; a resposta deve permanecer 404 Not Found.
- **CA3.5:** A exclusão concorrente ou falha de conectividade com o banco durante o `DELETE` não deve retornar mensagem de sucesso.

#### Tarefas Granulares de Desenvolvimento
- **T3.1 (Storage Delete Query):** Implementar em `JdbcEstoqueStorage` o método `removerPorId(String id)` utilizando subquery SQL de exclusão direcionada pela chave técnica serial:
  ```sql
  DELETE FROM bolsas 
  WHERE id_seq = (
      SELECT id_seq FROM bolsas WHERE id = ? ORDER BY id_seq DESC LIMIT 1
  );
  ```
- **T3.2 (Affect Rows Check):** Validar o número de linhas afetadas pela query no PostgreSQL: se for $1$, `removerPorId` retorna `true`; se for $0$ (bolsa inexistente ou estoque vazio), retorna `false`. Essa verificação é comprovada no nível de integração (`JdbcEstoqueStorageIT.deveRetornarFalseAoTentarRemoverInexistente`) e propagada pelo controller como HTTP 404 (`EstoqueControllerIT.deveRetornar404AoRemoverInexistente`).
- **T3.3 (Service & Controller Delegation):** Garantir que `EstoqueService.remover(id)` e `EstoqueController.remover(id)` propaguem o status 404 quando o retorno for `false`.
- **T3.4 (Post-Restart Validation):** Implementar teste de integração cobrindo o ciclo completo: cadastro de duplicatas $\rightarrow$ remoção da mais recente $\rightarrow$ reinício do contexto $\rightarrow$ verificação da sobrevivência da ocorrência mais antiga.

---

## 4. Matriz Rastreável História $\rightarrow$ Tarefa $\rightarrow$ Teste $\rightarrow$ Evidência

A matriz abaixo estabelece a rastreabilidade inequívoca entre requisitos de negócio, tarefas de desenvolvimento, planejamento de testes nos três níveis e comportamento auditável no ciclo Red $\rightarrow$ Green de TDD.

### 4.1. Classificação dos Níveis de Teste
- **Unitário (Nível 1):** Valida lógica de negócio isolada, mapeamentos, validações de domínio e adaptadores mockados/em memória. Executado via `mvn test` (Surefire).
- **Integração (Nível 2):** Valida a interação real com PostgreSQL via Testcontainers e migrações Flyway. Executa comandos SQL reais e testa conectividade, isolamento transacional e consultas temporais. Executado via `mvn verify` (Failsafe).
- **Aceitação (Nível 3):** Valida a ponta a ponta da API HTTP utilizando `MockMvc` ou `TestRestTemplate` sob `@SpringBootTest` com o perfil `infra` ativo e PostgreSQL em contêiner, simulando a perspectiva do operador do banco de sangue.

---

### 4.2. Matriz Rastreável Completa

| Hist. | Tarefa | Nível | Classe / Método de Teste Planejado | Cenário / Entrada | Falha Esperada no Ciclo Red | Sucesso no Ciclo Green | Evidência Auditável |
|:---:|:---:|:---:|---|---|---|---|---|
| **H1** | **T1.1** | Integração | `FlywayMigrationIT.deveAplicarMigracoesComSucesso()` | Execução da migração `V1` no Postgres Testcontainers | `FlywayException` / Tabela `bolsas` não existe | Tabela `bolsas` criada com colunas e índice `idx_bolsas_id_seq` | Log Flyway: `Successfully applied 1 migration` |
| **H1** | **T1.2** | Unitário | `EstoqueServiceTest.deveSalvarBolsaDelegandoParaStorage()` | Chamada `service.adicionar(BOLSA_001)` com mock de `EstoqueStorage` | Falha de compilação ou injeção (acoplamento rígido a `new ListaEstoque()`) | `service.adicionar()` invoca `storage.salvar(bolsa)` | `verify(storage).salvar(bolsa)` aprovado no Mockito/Surefire |
| **H1** | **T1.2** | Unitário | `ListaEstoqueStorageTest.deveDelegarOperacoesParaListaEstoque()` | Invocações em `ListaEstoqueStorage` sob perfil `u1` | Falha de compilação / classe de adaptador ausente | Métodos delegam estritamente para `ListaEstoque` em memória | Teste unitário aprovado no Surefire |
| **H1** | **T1.2** | Integração | `JdbcEstoqueStorageIT.deveInserirERecuperarBolsa()` | `salvar(BOLSA_001)` e `buscarPorId("BOLSA-001")` sob `@Profile("infra")` | `NoSuchBeanDefinitionException: EstoqueStorage` ou erro SQL de tabela ausente | Bolsa recuperada do banco com todos os campos idênticos | Asserções do JDBC aprovadas contra PostgreSQL |
| **H1** | **T1.3** | Unitário | `BolsaRequestDTOTest.deveDesserializarNumeroLiteralEConverterParaDominio()` | Payload JSON com `volumeMl: 450.0` literal e conversão para `Bolsa` | `ClassNotFoundException` / DTO não modelado | DTO converte para entidade `Bolsa` com volume `450.0` | Asserções de conversão do DTO aprovadas no Surefire |
| **H1** | **T1.3** | Unitário | `BolsaRequestDTOTest.deveDesserializarStringNumericaEValoresPadrao()` | Payload JSON com `volumeMl: "450.0"` (string) ou ausente | Falha de coerção ou `NullPointerException` | DTO converte para `Bolsa` com volume `450.0` padrão | Asserções de conversão aprovadas no Surefire |
| **H1** | **T1.3** | Aceitação | `EstoqueControllerIT.deveCadastrarBolsaViaPost()` | `POST /api/estoque` com JSON contendo `volumeMl: 450.0` literal sob perfil `infra` | `HttpMessageNotReadableException` / `MismatchedInputException` no `Map<String, String>` ou falha de bean | HTTP 200 OK com `{"mensagem": "Bolsa adicionada ao estoque", "tamanho": 1}` | Log HTTP MockMvc / TestRestTemplate com status 200 |
| **H1** | **T1.3** | Aceitação | `EstoqueControllerIT.deveConsultarBolsaCadastradaViaGet()` | `GET /api/estoque/{id}` imediatamente após o cadastro sob perfil `infra` | Status 404 ou corpo vazio | HTTP 200 OK com todos os atributos exatos da bolsa | `jsonPath("$.id").value("BOLSA-001")` aprovado |
| **H1** | **T1.4** | Integração | `BolsaPersistidaIT.devePersistirBolsaAposReinicioContexto()` | Salva bolsa, encerra contexto Spring, inicia novo contexto e lê | `AssertionError`: registro não encontrado no novo contexto (se volátil) | Bolsa recuperada intacta do PostgreSQL pós-reinício | Contexto reiniciado com sucesso e asserção confirmada |
| **H2** | **T2.1** | Unitário | `EstoqueServiceTest.deveDelegarListagemParaStorage()` | Chamada `service.listarTodas()` com mock de `EstoqueStorage` retornando `Bolsa[]` | Falha na delegação ou retorno nulo | Retorna exatamente o array fornecido por `storage.listarTodas()` | Teste unitário Mockito/Surefire aprovado |
| **H2** | **T2.1** | Integração | `JdbcEstoqueStorageIT.deveListarBolsasEmOrdemLIFO()` | Inserir bolsas 001 a 005 e chamar `listarTodas()` sob perfil `infra` | Ordem retornada diferente de 005 $\rightarrow$ 001 (ex: ordenação ascendente por erro de query) | Array retornado com exatamente `[005, 004, 003, 002, 001]` | `assertArrayEquals` comprovando ordem LIFO |
| **H2** | **T2.2** | Unitário | `EstoqueServiceTest.deveDelegarTamanhoParaStorage()` | Chamada `service.tamanho()` com mock de `EstoqueStorage` retornando N | Falha na delegação ou retorno fixo incorreto | Retorna exatamente o valor fornecido por `storage.tamanho()` | Teste unitário Mockito/Surefire aprovado |
| **H2** | **T2.2** | Aceitação | `EstoqueControllerIT.deveRetornarEnvelopeVazioQuandoSemBolsas()` | `GET /api/estoque` em banco limpo sob perfil `infra` | Falha de inicialização de storage ou envelope ausente | HTTP 200 OK com payload exato `{"bolsas": [], "tamanho": 0}` | Resposta JSON validada via `jsonPath` |
| **H2** | **T2.3** | Unitário | `EstoqueServiceTest.deveDelegarConsultaParaStorage()` | Chamada `service.consultar("BOLSA-001")` com mock retornando `Optional<Bolsa>` | Falha na delegação ou retorno vazio | Retorna o `Optional` fornecido por `storage.buscarPorId()` | `verify(storage).buscarPorId("BOLSA-001")` aprovado |
| **H2** | **T2.3** | Integração | `JdbcEstoqueStorageIT.deveRetornarOcorrenciaMaisRecenteDeIdDuplicado()` | Inserir 2 bolsas com mesmo ID e dados distintos; chamar `buscarPorId` | Retorna ocorrência antiga ou lança `IncorrectResultSizeDataAccessException` | Retorna exclusivamente a ocorrência mais recente | `assertEquals` comprovando campos da última inserção |
| **H2** | **T2.3** | Aceitação | `EstoqueControllerIT.deveConsultarBolsaExistenteRetornandoMaisRecente()` | `GET /api/estoque/{id}` com IDs duplicados sob perfil `infra` | Retorna HTTP 404, 500 ou ocorrência antiga | HTTP 200 OK com dados exclusivos da última coleta inserida | Asserções MockMvc validando campos da bolsa mais recente |
| **H2** | **T2.3** | Aceitação | `EstoqueControllerIT.deveRetornar404ParaBolsaInexistente()` | `GET /api/estoque/BOLSA-INEXISTENTE` sob perfil `infra` | Retorna status 200 com nulo ou 500 | Retorna estritamente HTTP 404 Not Found | `status().isNotFound()` aprovado no MockMvc |
| **H2** | **T2.4** | Aceitação | `EstoqueControllerIT.deveListarTodasBolsasComEnvelope()` | `GET /api/estoque` após inserir 5 fixtures sob perfil `infra` | `jsonPath("$.tamanho")` falha ou envelope malformado | HTTP 200 com array `bolsas` de 5 itens e `tamanho: 5` | Asserções de cabeçalhos e corpo JSON aprovadas |
| **H2** | **T2.5** | Integração | `BolsaPersistidaIT.deveManterOrdenacaoLIFOAposReinicio()` | Insere bolsas com IDs repetidos, reinicia contexto Spring e lista estoque | Inversão de ordem LIFO ou perda de registros após reinício | Listagem pós-reinício mantém ordem LIFO estrita e todas as duplicatas | Asserções de array confirmadas pós-reinício |
| **H3** | **T3.1** | Unitário | `EstoqueServiceTest.deveDelegarRemocaoParaStorage()` | Chamada `service.remover("BOLSA-001")` com mock retornando `true` | Falha na delegação ou retorno incorreto | Retorna o booleano fornecido por `storage.removerPorId()` | `verify(storage).removerPorId("BOLSA-001")` aprovado |
| **H3** | **T3.1** | Integração | `JdbcEstoqueStorageIT.deveRemoverBolsaExistente()` | Inserir bolsa e chamar `removerPorId(id)` sob perfil `infra` | Método retorna `false` ou registro permanece no Postgres | Método retorna `true` e `SELECT` posterior retorna vazio | `assertTrue` e `Optional.isEmpty` aprovados |
| **H3** | **T3.2** | Integração | `JdbcEstoqueStorageIT.deveRetornarFalseAoTentarRemoverInexistente()` | Chamada `removerPorId("INEXISTENTE")` sob perfil `infra` | Retorna `true` ou lança exceção não tratada | Retorna estritamente `false` quando 0 linhas forem afetadas | `assertFalse(storage.removerPorId(...))` aprovado |
| **H3** | **T3.2** | Aceitação | `EstoqueControllerIT.deveRetornar404AoRemoverInexistente()` | `DELETE /api/estoque/BOLSA-999` sob perfil `infra` | Retorna HTTP 200 ou 500 indevido | Retorna estritamente HTTP 404 Not Found | `status().isNotFound()` aprovado no MockMvc |
| **H3** | **T3.3** | Integração | `JdbcEstoqueStorageIT.deveRemoverApenasOcorrenciaMaisRecente()` | Inserir bolsa A (antiga) e bolsa B (recente) com mesmo ID; remover ID | Remove ambas as bolsas (se `DELETE WHERE id = ?` ingênuo) ou remove a antiga | Remove exclusivamente a bolsa B; bolsa A continua no banco | `assertEquals(1, storage.tamanho())` e dados de A intactos |
| **H3** | **T3.3** | Aceitação | `EstoqueControllerIT.deveRemoverBolsaComSucessoViaDelete()` | Cadastrar bolsa e executar `DELETE /api/estoque/{id}` sob perfil `infra` | Status HTTP incorreto ou envelope não decrementa tamanho | HTTP 200 OK com `{"mensagem": "Bolsa removida", "tamanho": 0}` | Asserções MockMvc de status e JSON aprovadas |
| **H3** | **T3.3** | Aceitação | `EstoqueControllerIT.deveRemoverMaisRecenteEManterDuplicataAnterior()` | Inserir bolsas com mesmo ID; `DELETE /api/estoque/{id}`; `GET` subsequente | `GET` subsequente retorna 404 (se deletou ambas) ou bolsa errada | HTTP 200 no DELETE com tamanho decrementado; `GET` subsequente retorna a anterior | Asserções MockMvc comprovando sobrevivência da anterior |
| **H3** | **T3.4** | Integração | `BolsaPersistidaIT.deveManterRemocaoAposReinicio()` | Remove bolsa, encerra contexto Spring, inicia novo contexto e busca | Bolsa reaparece após reinício (persistência volátil ou cache) | Busca no novo contexto retorna `Optional.empty()` | Comprovação definitiva de exclusão pós-reinício |

---

## 5. Desenho Arquitetural da Persistência e Preservação dos Contratos da U1

A arquitetura para suportar persistência relacional sem descaracterizar nem quebrar as estruturas manuais de AED da U1 adota o padrão arquitetural de **Portas e Adaptadores (Arquitetura Hexagonal Mínima)** acoplado ao mecanismo de **Perfis do Spring Framework**.

---

### 5.1. Padrão de Desacoplamento: A Interface `EstoqueStorage`

Atualmente, `EstoqueService` possui acoplamento rígido com a implementação em memória:
```java
// Estado Legado (U1):
private final ListaEstoque estoque = new ListaEstoque();
```

Para a Release 1, introduz-se a porta de armazenamento `EstoqueStorage`. `EstoqueService` passa a depender exclusivamente dessa abstração via injeção por construtor:

```
               +-----------------------+
               |   EstoqueController   |
               +-----------+-----------+
                           |
                           v
               +-----------------------+
               |    EstoqueService     |
               +-----------+-----------+
                           |
                           v
               +-----------------------+
               |   <<interface>>       |
               |    EstoqueStorage     |
               +-----------+-----------+
                           |
            +--------------+--------------+
            |                             |
            v                             v
+-----------------------+     +-----------------------+
|  ListaEstoqueStorage  |     |   JdbcEstoqueStorage  |
|  (Perfil Spring "u1") |     | (Perfil Spring "infra")|
+-----------+-----------+     +-----------+-----------+
            |                             |
            v                             v
+-----------------------+     +-----------------------+
|  ListaEstoque (AED)   |     | PostgreSQL Real       |
|  Nós manuais NoBolsa  |     | Spring JDBC + Flyway  |
+-----------------------+     +-----------------------+
```

#### Contrato da Interface `EstoqueStorage`
```java
package com.rotavital.storage;

import com.rotavital.domain.Bolsa;
import java.util.Optional;

public interface EstoqueStorage {
    void salvar(Bolsa bolsa);
    Optional<Bolsa> buscarPorId(String id);
    boolean removerPorId(String id);
    Bolsa[] listarTodas();
    int tamanho();
}
```

#### 5.1.1. Especificação dos Adaptadores Concretos e Mapeamento de Assinaturas

A equivalência e aderência estrita aos contratos originais da U1 é assegurada pelo mapeamento direto entre as operações de `EstoqueStorage` e os componentes subjacentes:

1. **Adaptador em Memória (`ListaEstoqueStorage` — Perfis `{"u1", "default"}`):**
   Encapsula diretamente a instância de `ListaEstoque` (AED U1), preservando os nós dinâmicos `NoBolsa` e sua complexidade algorítmica:
   ```java
   package com.rotavital.storage;

   import com.rotavital.aed.u1.ListaEstoque;
   import com.rotavital.domain.Bolsa;
   import org.springframework.context.annotation.Profile;
   import org.springframework.stereotype.Component;

   import java.util.Optional;

   @Component
   @Profile({"u1", "default"})
   public class ListaEstoqueStorage implements EstoqueStorage {

       private final ListaEstoque listaEstoque = new ListaEstoque();

       @Override
       public void salvar(Bolsa bolsa) {
           listaEstoque.inserir(bolsa); // Inserção na cabeça O(1)
       }

       @Override
       public Optional<Bolsa> buscarPorId(String id) {
           return listaEstoque.consultar(id); // Busca linear O(n) retornando a mais recente
       }

       @Override
       public boolean removerPorId(String id) {
           return listaEstoque.remover(id); // Remoção O(n) da ocorrência mais recente
       }

       @Override
       public Bolsa[] listarTodas() {
           return listaEstoque.toArray(); // Array LIFO (cabeça -> cauda)
       }

       @Override
       public int tamanho() {
           return listaEstoque.getTamanho(); // Tamanho exato em memória
       }
   }
   ```

2. **Adaptador Relacional (`JdbcEstoqueStorage` — Perfil `"infra"`):**
   Opera sobre o PostgreSQL via Spring JDBC (`JdbcTemplate`), com demarcação transacional `@Transactional` isolada e mapeamento explícito via `RowMapper<Bolsa>`:
   ```java
   package com.rotavital.storage;

   import com.rotavital.domain.Bolsa;
   import org.springframework.context.annotation.Profile;
   import org.springframework.jdbc.core.JdbcTemplate;
   import org.springframework.jdbc.core.RowMapper;
   import org.springframework.stereotype.Component;
   import org.springframework.transaction.annotation.Transactional;

   import java.sql.Date;
   import java.util.List;
   import java.util.Optional;

   @Component
   @Profile("infra")
   public class JdbcEstoqueStorage implements EstoqueStorage {

       private final JdbcTemplate jdbcTemplate;

       public JdbcEstoqueStorage(JdbcTemplate jdbcTemplate) {
           this.jdbcTemplate = jdbcTemplate;
       }

       @Override
       @Transactional
       public void salvar(Bolsa bolsa) {
           String sql = """
               INSERT INTO bolsas (id, tipo_sanguineo, volume_ml, data_coleta, validade, doador)
               VALUES (?, ?, ?, ?, ?, ?)
               """;
           jdbcTemplate.update(sql,
               bolsa.getId(),
               bolsa.getTipoSanguineo(),
               bolsa.getVolumeMl(),
               Date.valueOf(bolsa.getDataColeta()),
               Date.valueOf(bolsa.getValidade()),
               bolsa.getDoador()
           );
       }

       @Override
       @Transactional(readOnly = true)
       public Optional<Bolsa> buscarPorId(String id) {
           String sql = """
               SELECT id, tipo_sanguineo, volume_ml, data_coleta, validade, doador
               FROM bolsas
               WHERE id = ?
               ORDER BY id_seq DESC
               LIMIT 1
               """;
           List<Bolsa> resultados = jdbcTemplate.query(sql, bolsaRowMapper, id);
           return resultados.isEmpty() ? Optional.empty() : Optional.of(resultados.get(0));
       }

       @Override
       @Transactional
       public boolean removerPorId(String id) {
           String sql = """
               DELETE FROM bolsas 
               WHERE id_seq = (
                   SELECT id_seq 
                   FROM bolsas 
                   WHERE id = ? 
                   ORDER BY id_seq DESC 
                   LIMIT 1
               )
               """;
           return jdbcTemplate.update(sql, id) > 0;
       }

       @Override
       @Transactional(readOnly = true)
       public Bolsa[] listarTodas() {
           String sql = """
               SELECT id, tipo_sanguineo, volume_ml, data_coleta, validade, doador
               FROM bolsas
               ORDER BY id_seq DESC
               """;
           List<Bolsa> bolsas = jdbcTemplate.query(sql, bolsaRowMapper);
           return bolsas.toArray(new Bolsa[0]);
       }

       @Override
       @Transactional(readOnly = true)
       public int tamanho() {
           Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bolsas", Integer.class);
           return total != null ? total : 0;
       }

       private final RowMapper<Bolsa> bolsaRowMapper = (rs, rowNum) -> new Bolsa(
           rs.getString("id"),
           rs.getString("tipo_sanguineo"),
           rs.getDouble("volume_ml"),
           rs.getDate("data_coleta").toLocalDate(),
           rs.getDate("validade").toLocalDate(),
           rs.getString("doador")
       );
   }
   ```

3. **Injeção Transparente em `EstoqueService`:**
   `EstoqueService` delega 100% de suas operações públicas para `storage`, sem qualquer acoplamento a banco ou memória:
   - `service.adicionar(bolsa)` $\rightarrow$ `storage.salvar(bolsa)`
   - `service.consultar(id)` $\rightarrow$ `storage.buscarPorId(id)`
   - `service.remover(id)` $\rightarrow$ `storage.removerPorId(id)`
   - `service.listarTodas()` $\rightarrow$ `storage.listarTodas()`
   - `service.tamanho()` $\rightarrow$ `storage.tamanho()`

---

### 5.2. Estratégia de Perfis Spring (`u1` vs `infra`)

A aplicação suportará dois modos operacionais mutualmente exclusivos e sem ambiguidade:

| Propriedade | Perfil Spring `u1` (Padrão) | Perfil Spring `infra` (Produção / Compose) |
|---|---|---|
| **Ativação** | Perfil padrão (`spring.profiles.default=u1`) ou explícito `u1`, com `@Profile({"u1", "default"})` no adaptador | Ativado explicitamente (`SPRING_PROFILES_ACTIVE=infra`), com `@Profile("infra")` |
| **Adaptador Ativo** | `ListaEstoqueStorage` (anotado com `@Component` e `@Profile({"u1", "default"})`) | `JdbcEstoqueStorage` (anotado com `@Component` e `@Profile("infra")`) |
| **Mecanismo de Dados** | `ListaEstoque` (lista simplesmente encadeada manual com nós `NoBolsa`) | PostgreSQL 16 via Spring `JdbcTemplate` e migrações Flyway |
| **Dependência de Banco** | Zero. Aplicação sobe sem JDBC, sem DataSource e sem contêiner | Obrigatória. Exige PostgreSQL saudável na rede |
| **Comportamento em Falha** | N/A (memória pura da JVM) | **Sem fallback silencioso**. Falha imediata (`DataAccessException`) e Actuator reporta `DOWN` |
| **Objetivo Acadêmico** | Preservar integralmente as evidências e contratos de AED da U1 | Atender os requisitos de infraestrutura corporativa e persistência da U2 |

#### Blindagem contra Fallback Silencioso
No perfil `infra`, qualquer falha de comunicação com o banco PostgreSQL deve lançar exceção de tempo de execução (`DataAccessException`) e interromper a requisição HTTP com código de erro correspondente (HTTP 500 / 503). Em nenhuma circunstância o sistema desviará requisições para a estrutura em memória caso o banco falhe, evitando corrupção de estado e divergência de dados.

### 5.2.1. Arquitetura de Isolamento de Auto-configuração (Zero-Database no perfil u1)
Um dos maiores riscos identificados na introdução de `spring-boot-starter-jdbc` e `flyway-core` no `pom.xml` é o comportamento padrão da auto-configuração do Spring Boot (`DataSourceAutoConfiguration`), que tenta instanciar um `DataSource` mesmo quando nenhum banco foi configurado, causando falha fatal de inicialização (`Failed to configure a DataSource: 'url' attribute is not specified`).

Para garantir que o perfil `u1` (padrão) inicie 100% livre de banco, estabelece-se a seguinte estratégia de configuração:
1. **Perfil Padrão Declarado (`application.properties`):**
   ```properties
   spring.profiles.default=u1
   ```
2. **Desativação Explícita de Auto-configuração no Perfil `u1` (`application-u1.properties`):**
   ```properties
   # Desativa tentativa de auto-configuração de DataSource e Flyway no perfil u1
   spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
     org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,\
     org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration,\
     org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
   spring.flyway.enabled=false
   ```
3. **Configuração Dedicada do Perfil `infra` (`application-infra.properties`):**
   ```properties
   # Habilita e configura PostgreSQL e Flyway exclusivamente sob o perfil infra
   spring.flyway.enabled=true
   spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/rotavital}
   spring.datasource.username=${SPRING_DATASOURCE_USERNAME:rotavital}
   spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:rotavital}
   spring.datasource.driver-class-name=org.postgresql.Driver
   management.health.db.enabled=true
   ```
4. **Comportamento do Actuator Health:**
   - **No perfil `infra`:** O `DataSourceHealthIndicator` é registrado automaticamente pelo Actuator. Se o PostgreSQL estiver offline, com pool esgotado ou inacessível, `/actuator/health` reporta imediatamente `{"status": "DOWN"}`.
   - **No perfil `u1`:** Como não há beans de `DataSource`, o `DataSourceHealthIndicator` não é registrado, e o Actuator reporta `{"status": "UP"}` baseado na saúde de disco e JVM, garantindo execução autônoma sem contêiner.

### 5.2.2. Gestão Transacional e Blindagem de Concorrência

1. **Delimitação de `@Transactional` Exclusivamente em `JdbcEstoqueStorage` (NÃO em `EstoqueService`):**
   - **Justificativa Arquitetural Crítica:** No perfil `u1`, para assegurar o funcionamento 100% autônomo sem banco de dados, `DataSourceTransactionManagerAutoConfiguration` é explicitamente desativado (`spring.autoconfigure.exclude`). Se a anotação `@Transactional` fosse declarada na classe de serviço `EstoqueService` (componente comum instanciado em ambos os perfis), o Spring tentaria interceptar as chamadas aos métodos gerenciados buscando um bean do tipo `PlatformTransactionManager` ou `TransactionManager`. Na ausência desse bean sob o perfil `u1`, qualquer invocação a `EstoqueService` falharia fatalmente com:
     ```text
     org.springframework.beans.factory.NoSuchBeanDefinitionException: No qualifying bean of type 'org.springframework.transaction.TransactionManager' available
     ```
   - **Solução Adotada:** A anotação `@Transactional` é demarcada estritamente dentro da implementação de infraestrutura (`JdbcEstoqueStorage`), onde o DataSource e o `DataSourceTransactionManager` estão ativamente disponíveis no contexto Spring sob `@Profile("infra")`. As operações de escrita (`salvar`, `removerPorId`) são anotadas com `@Transactional`, e as operações de leitura (`buscarPorId`, `listarTodas`, `tamanho`) com `@Transactional(readOnly = true)`. `EstoqueService` e `ListaEstoqueStorage` permanecem completamente desacoplados de qualquer dependência transacional.

2. **Isolamento de Concorrência na Remoção de Ocorrências Mais Recentes:**
   - Em cenários com identificadores repetidos (ex: duas bolsas com `id = "BOLSA-001"`), a remoção via `DELETE /api/estoque/{id}` deve ser atômica e determinística.
   - **Subquery Atômica Padrão:**
     ```sql
     DELETE FROM bolsas 
     WHERE id_seq = (
         SELECT id_seq FROM bolsas WHERE id = ? ORDER BY id_seq DESC LIMIT 1
     );
     ```
     Como o PostgreSQL executa a subquery dentro do contexto atômico da instrução DML, a linha identificada com o maior `id_seq` é travada e removida em uma única transação atômica.
   - **Padrão Avançado para Alta Vazão Concorrente (CTE com `FOR UPDATE SKIP LOCKED`):**
     Caso duas requisições concorrentes de exclusão para o mesmo ID ocorram no mesmo instante, para evitar esperas de lock (*lock contention*) ou leituras inconsistentes no nível *Read Committed*, modela-se alternativamente a exclusão com CTE:
     ```sql
     WITH target AS (
         SELECT id_seq FROM bolsas
         WHERE id = ?
         ORDER BY id_seq DESC
         LIMIT 1
         FOR UPDATE SKIP LOCKED
     )
     DELETE FROM bolsas b
     USING target
     WHERE b.id_seq = target.id_seq;
     ```
     O qualificador `SKIP LOCKED` assegura que a primeira transação trave e delete a ocorrência $k$ mais recente, enquanto a transação concorrente paralela pule imediatamente o registro bloqueado e selecione a ocorrência $k-1$, permitindo que ambas as exclusões ocorram com sucesso sem bloqueio ou erros 404 espúrios.

---

### 5.3. Modelagem Relacional e Resolução de IDs Repetidos ($LIFO$)

O banco de dados relacional deve respeitar fielmente a semântica de lista encadeada da U1:
1. Uma bolsa recém-inserida fica na cabeça (ordem $LIFO$).
2. Identificadores de bolsas (`id`) podem se repetir no estoque.
3. Consultas e remoções por ID afetam estritamente a ocorrência mais recente daquele ID.

#### 5.3.1. DDL da Migração Flyway (`V1__criar_tabela_bolsas.sql`)
```sql
-- Migração V1: Criação da tabela de bolsas de sangue com chave técnica LIFO
CREATE TABLE IF NOT EXISTS bolsas (
    id_seq          BIGSERIAL PRIMARY KEY,
    id              VARCHAR(50) NOT NULL,
    tipo_sanguineo  VARCHAR(10) NOT NULL,
    volume_ml       DOUBLE PRECISION NOT NULL,
    data_coleta     DATE NOT NULL,
    validade        DATE NOT NULL,
    doador          VARCHAR(255) NOT NULL,
    criado_em       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Índice composto cobrindo busca pontual e ordenação LIFO por ocorrência mais recente
CREATE INDEX IF NOT EXISTS idx_bolsas_id_seq ON bolsas(id, id_seq DESC);
```

#### 5.3.2. Fundamentação da Chave Técnica `id_seq` e Otimização de Queries
- O campo `id` da bolsa **não possui restrição UNIQUE**, permitindo IDs repetidos conforme exigido pela regra da U1.
- A chave primária `id_seq BIGSERIAL` é estritamente crescente e reflete com exatidão a ordem cronológica de inserção.
  - *Contraste com `criado_em TIMESTAMP`:* Em cenários de inserções em lote ou alta frequência dentro do mesmo milissegundo, timestamps podem sofrer colisões idênticas, gerando empates e ordenação LIFO não-determinística. Em contrapartida, `BIGSERIAL` é alimentado por uma sequência atômica nativa do PostgreSQL (`nextval`), garantindo monotonicidade estrita ($id\_seq_1 < id\_seq_2 < \dots < id\_seq_n$) e eliminando qualquer risco de inconsistência temporal sob concorrência.
- O índice composto `idx_bolsas_id_seq ON bolsas(id, id_seq DESC)` permite que consultas pontuais de duplicatas e a subquery de remoção executem `Index Only Scan` diretamente nos nós folha do índice sem necessidade de ordenação em memória (`Sort`) nem leitura sequencial no heap, operando em tempo $O(1)$.
- **Listagem Completa LIFO:**
  ```sql
  SELECT id, tipo_sanguineo, volume_ml, data_coleta, validade, doador 
  FROM bolsas 
  ORDER BY id_seq DESC;
  ```
  Garante que a última bolsa inserida é a primeira retornada, replicando o percurso `cabeca -> proximo` da lista encadeada.
- **Consulta da Ocorrência Mais Recente:**
  ```sql
  SELECT id, tipo_sanguineo, volume_ml, data_coleta, validade, doador 
  FROM bolsas 
  WHERE id = ? 
  ORDER BY id_seq DESC 
  LIMIT 1;
  ```
  Garante a recuperação determinística da bolsa mais recente em caso de duplicatas.
- **Remoção Exclusiva da Ocorrência Mais Recente:**
  - *Abordagem por Subquery Atômica (padrão):*
    ```sql
    DELETE FROM bolsas 
    WHERE id_seq = (
        SELECT id_seq 
        FROM bolsas 
        WHERE id = ? 
        ORDER BY id_seq DESC 
        LIMIT 1
    );
    ```
  - *Abordagem Avançada por CTE com Bloqueio de Linha (alta concorrência):*
    ```sql
    WITH target AS (
        SELECT id_seq 
        FROM bolsas 
        WHERE id = ? 
        ORDER BY id_seq DESC 
        LIMIT 1 
        FOR UPDATE SKIP LOCKED
    )
    DELETE FROM bolsas b
    USING target
    WHERE b.id_seq = target.id_seq;
    ```
  Exclui uma única linha do banco: se existirem 3 bolsas com ID `"BOLSA-001"`, apenas a mais recente é deletada; as 2 anteriores permanecem armazenadas. O `UPDATE/DELETE` avalia o retorno de linhas afetadas: `1` indica sucesso (`true`), `0` indica inexistência (`false`, propagado como HTTP 404).

---

### 5.4. Resiliência de Tipagem na Deserialização REST

#### Diagnóstico do Código Atual
Em `EstoqueController`, o método `adicionar` atualmente consome `@RequestBody Map<String, String> body`. Nas fixtures oficiais (`fixtures/bolsas_u1.json`), o campo `volumeMl` é serializado como número de ponto flutuante (`450.0` sem aspas):
```json
{
  "id": "BOLSA-001",
  "tipoSanguineo": "O+",
  "volumeMl": 450.0,
  "dataColeta": "2026-09-01",
  "validade": "2026-11-01",
  "doador": "João Silva"
}
```
Se um cliente HTTP REST estrito enviar esse JSON com `Content-Type: application/json`, o Jackson tenta mapear o número literal para uma String dentro de `Map<String, String>`. Em certas configurações estritas do Jackson, isso gera `MismatchedInputException` ou exige coerção forçada.

#### Solução Arquitetural
Modelar um DTO dedicado (`BolsaRequestDTO`) com tolerância a literais numéricos e strings, preservando o desacoplamento e a robustez contra variações nas fixtures:
```java
package com.rotavital.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rotavital.domain.Bolsa;

import java.time.LocalDate;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BolsaRequestDTO(
    String id,
    String tipoSanguineo,
    Double volumeMl,
    String dataColeta,
    String validade,
    String doador
) {
    public Bolsa toDomain() {
        return new Bolsa(
            Objects.requireNonNull(id, "id não pode ser nulo"),
            Objects.requireNonNull(tipoSanguineo, "tipoSanguineo não pode ser nulo"),
            volumeMl != null ? volumeMl : 450.0,
            LocalDate.parse(Objects.requireNonNull(dataColeta, "dataColeta não pode ser nula")),
            LocalDate.parse(Objects.requireNonNull(validade, "validade não pode ser nula")),
            Objects.requireNonNull(doador, "doador não pode ser nulo")
        );
    }
}
```
Essa abordagem garante que tanto requisições legadas enviando Strings quanto requisições modernas enviando números literais (`volumeMl: 450.0`) sejam aceitas transparentemente pelo Jackson, preservando 100% da integridade com as fixtures e testes da U1. Além disso, anotações de validação e tolerância a propriedades extras evitam quebras de contrato.

---

### 5.5. Estratégia de Testes de Caracterização da U1

Como constatado no diagnóstico da Etapa 0, **não existem testes automatizados legados para `EstoqueController` e `EstoqueService`** na base original (os testes da U1 cobriam apenas a estrutura de dados `ListaEstoque`).

Para assegurar que a refatoração e a introdução da interface `EstoqueStorage` não quebrem inadvertidamente os contratos originais da U1:
1. **Antes de modificar `EstoqueService` na Etapa 3**, serão criadas duas suítes dedicadas de caracterização sob o perfil padrão `@ActiveProfiles("u1")`:
   - `EstoqueServiceCaracterizacaoTest`:
     - `deveInserirEConsultarBolsa()`
     - `deveRetornarArrayEmOrdemLIFO()`
     - `deveConsultarOcorrenciaMaisRecenteEmDuplicatas()`
     - `deveRemoverApenasOcorrenciaMaisRecenteEmDuplicatas()`
     - `deveRetornarFalseAoRemoverInexistente()`
     - `deveRetornarTamanhoCorreto()`
   - `EstoqueControllerCaracterizacaoTest`:
     - `deveAdicionarBolsaERetornar200ComTamanho()`
     - `deveConsultarBolsaExistenteERetornar200()`
     - `deveRetornar404ParaBolsaInexistente()`
     - `deveRemoverBolsaERetornar200ComTamanho()`
     - `deveRetornar404AoRemoverInexistente()`
     - `deveListarBolsasComEnvelopeERespeitarLIFO()`
     - `deveRetornarEnvelopeVazioQuandoSemBolsas()`
2. Esses testes de caracterização servem como **rede de segurança inquebrável**: qualquer alteração estrutural futura que ameace a compatibilidade da U1 fará esses testes falharem imediatamente, blindando os contratos pedagógicos.

---

## 6. Governança, Rastreabilidade e Critérios de Conclusão da Etapa 1

### 6.1. Rastreabilidade com as Próximas Etapas
- **Etapa 2 (Base de Containers):** Consumirá as definições de DDL, PostgreSQL 16, perfis Spring (`u1` vs `infra`) e segregação de plugins de teste (`surefire` vs `failsafe`).
- **Etapa 3 (H1 — TDD):** Implementará a migração Flyway, `BolsaRequestDTO`, `JdbcEstoqueStorage.salvar`/`buscarPorId` e a comprovação pós-reinício seguindo estritamente a matriz TDD desta especificação.
- **Etapa 4 (H2 — TDD):** Implementará a listagem LIFO, envelope de resposta e resolução de duplicatas no PostgreSQL seguindo a matriz TDD.
- **Etapa 5 (H3 — TDD):** Implementará a remoção pontual da ocorrência mais recente via subquery SQL e tratamento de 404 seguindo a matriz TDD.

### 6.2. Declaração de Integridade
Em estrito cumprimento ao protocolo obrigatório:
- Nenhum arquivo em `src/main/` ou `aed-c/src/` foi modificado ou excluído durante a Etapa 1.
- Nenhuma linha de código de produção ou teste foi implementada antecipadamente.
- O story map, os critérios de aceite e os desenhos arquiteturais foram integralmente registrados neste documento (`docs/infra-u2/story-map.md`).

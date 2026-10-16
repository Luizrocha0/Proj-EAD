# Prompt de execução por etapas — Rota Vital — Infraestrutura U2, Parte 1

## Objetivo

Implemente no repositório https://github.com/Luizrocha0/Proj-EAD a entrega “Unidade 2 — Desafios PI — Parte 1” de Infraestrutura de Software, com prazo de 20/10/2026.

Execute as mudanças, os testes e as verificações. Não entregue apenas recomendações. Preserve a entrega da U1 e comprove cada requisito com código, execução ou evidência verificável.

Este documento contém o escopo completo, mas a execução deve ocorrer em partes, uma etapa por vez.

## 0. Protocolo obrigatório de execução por partes

### Uma etapa por resposta

1. Execute somente a etapa atual. Não implemente o projeto inteiro em uma única rodada.
2. Dentro da etapa, trabalhe em incrementos pequenos: teste, implemente, verifique e registre as evidências.
3. Conclua o critério de saída da etapa antes de considerá-la encerrada. Corrija as falhas encontradas nela.
4. Ao concluir, apresente o resumo padronizado abaixo e aguarde o usuário enviar “continuar”, “próxima etapa” ou uma instrução equivalente.
5. Ao receber esse comando, retome da etapa seguinte. Não reinicie o diagnóstico nem refaça o que já foi validado sem evidência de mudança.

Não interrompa uma etapa para pedir confirmação de escolhas rotineiras já cobertas por este documento. Só peça informação quando ela for indispensável e não puder ser obtida do repositório ou do ambiente.

Não avance por uma dependência bloqueada. Conclua o trabalho independente dentro da etapa e informe o bloqueio real. Não marque como atendida uma configuração externa que não conseguiu executar ou verificar.

### Registro para retomada

Mantenha `docs/infra-u2/progresso.md` com a etapa atual, as concluídas, decisões, arquivos alterados, comandos executados, resultados, commits e pendências. Registre somente informações verificadas e nunca credenciais.

Ao encerrar cada etapa, responda com no máximo cinco itens:

- **Etapa e estado:** concluída, parcial ou bloqueada.
- **Mudanças:** arquivos e decisões relevantes.
- **Verificação:** comandos, resultados reais e evidências.
- **Pendências:** somente problemas concretos, com a ação necessária.
- **Próxima etapa:** o que será executado quando o usuário mandar continuar.

Não apresente a entrega inteira como pronta ao concluir uma etapa intermediária.

### Sequência obrigatória

| Etapa | Trabalho delimitado | Critério de saída |
|---|---|---|
| 0 — Diagnóstico | Ler o repositório atual, instruções locais, enunciado e configurações; executar a linha de base. | Inventário real, SHA, testes existentes, lacunas e plano registrados. Sem implementação funcional. |
| 1 — Story map e critérios | Definir backbone, Release 1, H1/H2/H3, tarefas, critérios, testes e arquitetura. | Matriz história → tarefa → teste → evidência; contratos da U1 identificados. |
| 2 — E1: base de containers | Preparar Dockerfile, Compose, PostgreSQL, perfis, configuração externa e saúde. Preparar a execução de testes em branches para registrar o TDD seguinte, sem antecipar o deploy novo. | API e banco sobem pelo comando documentado; build executa testes; logs e saúde verificados. Persistência funcional do estoque fica explicitamente pendente das etapas 3–5. |
| 3 — E2/H1 | Implementar cadastro e recuperação persistentes pelo ciclo Red → Green → Refactor. | Testes unitários, integração e aceitação de H1 aprovados; recuperação após reinício comprovada. |
| 4 — E2/H2 | Implementar listagem persistente, vazios, ordem e IDs repetidos por TDD. | Testes de H2 aprovados contra PostgreSQL real; ordem e envelope preservados. |
| 5 — E2/H3 | Implementar remoção persistente e seus casos de ausência e repetição por TDD. | Testes de H3 aprovados; remoção da ocorrência correta confirmada após reinício. |
| 6 — E2: integração e gates | Consolidar testes por nível e regressões na esteira; concluir a demonstração de persistência do E1. | Testes descobertos e executados; evidências Red/Green rastreáveis; nenhum gate obrigatório contornado. |
| 7 — E3: branches e deploy | Aplicar fluxo por PR, proteção de main, deploy do SHA aprovado, verificação pública e rollback documentado. | Configurações e execução remotas verificadas ou pendências externas explicitadas, sem alegar conclusão indevida. |
| 8 — Documentação e apresentação | Completar README, decisões, uso de IA, evidências e roteiro da apresentação. | Documentos coerentes com código, comandos e execuções reais. |
| 9 — Auditoria final | Reexecutar a validação proporcional e confrontar cada requisito com uma evidência. | Matriz final completa; conclusão condicionada à ausência de pendências obrigatórias. |

A ordem de construção é E1 → E2 → E3. Durante E1, prepare apenas a infraestrutura necessária ao TDD. Não implemente antecipadamente as funcionalidades das três histórias.

Documentação, configuração e evidências devem ser atualizadas ao longo das etapas. A etapa 8 consolida esses registros; não deve inventar retrospectivamente a jornada.

**Comece executando somente a Etapa 0. Ao terminá-la, apresente os resultados e aguarde o próximo comando do usuário.**

## 1. Contexto e estado inicial verificado

A base foi consultada em 16/10/2026 no commit `b410dfcf8a6caa39c1fde36fbaa3e52c39863bef`. Confirme o HEAD atual. Considere alterações posteriores e preserve o trabalho existente.

A base consultada contém:

- Java 21, Spring Boot 3.3.3, Maven Wrapper, JUnit e Actuator, com `pom.xml` e `src/main/resources/application.properties`.
- AED U1: estruturas manuais em `aed-c/` e `src/main/java/com/rotavital/aed/u1/`, testes e tradução comentada em `docs/equivalencia-u1.md`.
- API: estoque em `/api/estoque`, fila em `/api/requisicoes` e detecção de duplicatas em `/api/duplicates/detect`.
- Infraestrutura: Dockerfile multi-stage, `.github/workflows/ci.yml`, `render.yaml` e URL documentada `https://rota-vital.onrender.com`.
- Lacunas confirmadas na consulta: ausência de Compose, configuração de banco, `.env.example` e story map; Dockerfile com `package -DskipTests`; main sem proteção. Histórico TDD e evidências da nova atividade precisam ser verificados e produzidos.

Leia o código real. Esta descrição não substitui a inspeção do estado atual. O endereço documentado e um pipeline antigo verde não comprovam o deploy desta nova entrega.

## 2. Escopo e regras obrigatórias

Esta tarefa é a U2 de Infraestrutura, Parte 1: E1 — container, E2 — TDD e E3 — deploy automatizado.

- Preserve os códigos C e Java da U1, seus testes, documentos, medições e gráficos. Não substitua as estruturas manuais por coleções prontas.
- Mantenha a fila FIFO e os algoritmos de threads existentes funcionando. Não implemente roteirização, FEFO, índice de estoque por hash ou compatibilidade ABO/Rh. Exemplos do PDF não autorizam acrescentar esses algoritmos.
- Faça mudanças delimitadas. Não crie frontend, microsserviços ou migração de stack. Atualizações de dependências exigem necessidade concreta e compatibilidade verificada.
- Não fabrique testes executados, prints, commits, resultados, URLs, configurações remotas ou histórico Red → Green. Não reescreva o passado para alegar TDD.
- Execute ações externas somente dentro da autorização e das permissões disponíveis. Se faltar acesso administrativo, conclua o possível e informe a ação externa exata, mantendo o requisito pendente.

Não acrescente `Generated with Codex`, assinaturas promocionais ou coautoria automática aos commits. Declare o uso de IA na documentação exigida pelo professor.

Preserve alterações existentes do usuário. Não apague trabalho nem use reset ou force push para impor o planejamento. Se delegar, atribua um responsável por arquivo e verifique o resultado recebido.

## 3. Arquitetura para persistir sem perder a U1

### Stack e funcionalidade

Use o estoque de bolsas, já existente no domínio e na API, como funcionalidade persistida. Não crie um CRUD desconectado apenas para demonstrar banco.

Mantenha Java 21, Spring Boot, Maven e GitHub Actions. Acrescente Spring JDBC, PostgreSQL e Flyway, com versões compatíveis com a base. Use JUnit 5 e Testcontainers com PostgreSQL real para integração.

Não migre para JPA ou outro framework sem necessidade concreta. Se as skills indicadas estiverem disponíveis, aplique as pertinentes à stack, TDD e investigação de falhas. A ausência de uma skill não autoriza alegar verificação nem impede usar as ferramentas disponíveis.

### Modos de execução

Extraia um contrato mínimo de armazenamento usado por `EstoqueService`, preservando suas operações públicas.

| Modo | Comportamento |
|---|---|
| Perfil Spring `u1` | Usa efetivamente `ListaEstoque`, com nós próprios. Demonstra equivalência C ↔ Java sem banco. |
| Perfil Spring `infra` | Usa PostgreSQL como fonte dos dados do estoque. Ativado explicitamente no Compose e no Render. |

Defina `u1` como perfil padrão. Nesse modo, a aplicação deve iniciar sem tentar configurar conexão ou migração de banco.

No perfil `infra`, não permita fallback silencioso para memória quando o banco falhar. Não mantenha dois estoques independentes que possam divergir.

A fila de requisições continua com a implementação atual. Documente que sua persistência está fora desta alteração.

A transição dos adaptadores deve ser incremental, guiada pelos testes das histórias. Código de produção novo ou alterado exige Red → Green → Refactor. Preparação declarativa de ambiente não autoriza antecipar regras de negócio.

### Contratos do estoque

Leia `EstoqueController`, `EstoqueService`, `Bolsa` e `ListaEstoque`.

- Preserve rotas, campos JSON e respostas: POST retorna 200 com mensagem e tamanho; consulta e remoção inexistentes retornam 404; listagem mantém `bolsas` e `tamanho`.
- Preserve `id`, `tipoSanguineo`, `volumeMl`, `dataColeta`, `validade` e `doador`.
- Preserve a ordem: a lista insere na cabeça; registros mais recentes aparecem primeiro.
- Preserve IDs repetidos: consulta e remoção afetam a ocorrência mais recente; remoção exclui somente essa ocorrência. Use chave técnica interna e não imponha `UNIQUE` ao identificador da bolsa.
- Falha ao persistir não pode produzir resposta de sucesso. Teste e documente o erro, sem expor senha ou detalhes internos.

Use migrações versionadas em `src/main/resources/db/migration/`. A primeira execução deve criar o esquema automaticamente. Não dependa de SQL aplicado manualmente.

Caracterize os contratos existentes com testes quando necessário. Diferencie testes de caracterização do código antigo de testes escritos antes do comportamento novo.

## 4. Story map e três histórias da Release 1

Crie backbone, Release 1 e pelo menos três histórias quebradas em tarefas.

| História | Resultado obrigatório |
|---|---|
| H1 — Cadastrar e recuperar bolsa persistida | Cadastrar pela API no perfil infra, confirmar gravação no PostgreSQL e recuperar todos os campos após reiniciar a aplicação/contexto. |
| H2 — Consultar o estoque persistido | Listar com envelope e ordem compatíveis com a U1, incluindo vazio e IDs repetidos. Confirmar persistência após reinício. |
| H3 — Remover bolsa de forma persistente | Remover somente a ocorrência mais recente, preservar as demais e comprovar exclusão após reinício. Manter 404 quando não houver ocorrência. |

Para cada história, documente:

- Tarefas e regras envolvidas.
- Critérios de aceite observáveis.
- Testes unitários de cada tarefa de regra.
- Pelo menos um teste de integração com PostgreSQL e um teste de aceitação.
- Commits e execuções que comprovem Red → Green.

Repository mockado não comprova integração. Consultar o mesmo objeto em memória não comprova persistência.

## 5. E1 — Container automatizado

### Dockerfile

Corrija o Dockerfile existente:

- O estágio de build compila e executa testes; teste vermelho faz o build falhar. Remova o `-DskipTests` desse estágio.
- Separe testes unitários dos que exigem Docker/PostgreSQL. O build da imagem executa os testes adequados sem Docker dentro do Docker.
- O estágio final contém apenas artefato e dependências de execução, com usuário sem privilégios.
- Porta e parâmetros da JVM são configuráveis e as variáveis documentadas são realmente consumidas.
- Healthcheck funciona; segredos e arquivos `.env` reais não entram na imagem.

Não monte socket Docker nem use modo privilegiado para contornar a separação dos testes.

### Compose

Crie `compose.yaml` na raiz com:

- Serviço API construído pelo Dockerfile e usando perfil `infra`.
- PostgreSQL em versão explícita e compatível, com volume nomeado.
- Healthchecks do banco e da API.
- `depends_on` aguardando banco saudável e conexão usando o nome do serviço na rede do Compose.
- Variáveis externas e inicialização com o comando abaixo.

```bash
docker compose up -d --build
```

`depends_on` sem condição de saúde não comprova prontidão do banco.

Crie `.env.example` somente com valores de exemplo. Atualize `.gitignore` e `.dockerignore` para excluir segredos e preservar o arquivo de exemplo.

### Persistência e evidências

Depois de H1/H2/H3 implementadas, conclua a demonstração completa:

1. Suba API e banco a partir de ambiente limpo.
2. Cadastre bolsa pela API e confirme a gravação no PostgreSQL.
3. Reinicie a API e confirme recuperação.
4. Execute `docker compose down`, suba novamente e confirme preservação pelo volume.
5. Capture `docker compose ps`, API respondendo e persistência efetiva.

Não use `down -v` nessa demonstração: remove o volume. Não apague dados preexistentes do usuário para simular ambiente limpo; use projeto Compose ou volume de teste separado.

Verifique que a API no perfil infra não informa saúde normal quando sua dependência de banco estiver indisponível. Na Etapa 2, distinga saúde estrutural de persistência funcional ainda não implementada.

## 6. E2 — TDD comprovável

### Processo por tarefa

1. Escreva teste pequeno, executável e ligado ao critério de aceite.
2. Execute e confirme Red pelo motivo correto: comportamento ausente.
3. Registre o teste e a evidência antes de implementar a funcionalidade.
4. Implemente o mínimo e execute até Green.
5. Refatore se necessário e execute novamente os testes afetados.

Credencial incorreta, Docker indisponível ou projeto sem compilar não comprovam falha funcional.

Não implemente tudo antes de organizar commits `test:` e `feat:`. Os commits devem retratar a sequência real. Não apague código para fabricar Red. Pode preparar a infraestrutura mínima necessária à execução dos testes.

Registre Red e Green dentro de cada história; não espere implementar as três histórias para testar.

### Testes por nível

Configure comandos claros para:

- Unitários sem banco externo.
- Integração com PostgreSQL real, preferencialmente Testcontainers.
- Aceitação das três histórias pelo comportamento da API.
- Regressões U1 em Java e C.
- Validação das medições e agregações existentes.

Use Surefire/Failsafe ou separação equivalente com descoberta comprovada. Se adotar perfil Maven `integration`, diferencie-o dos perfis Spring `u1` e `infra`.

Integração e aceitação executam na esteira e não são ignoradas silenciosamente se Docker ou banco estiverem indisponíveis. Banco em memória e mocks não substituem a evidência de PostgreSQL real.

Se usar Gherkin, vincule cenários a testes executáveis. Arquivos `.feature` sem execução não comprovam comportamento.

Registre história/tarefa, teste, comando, falha esperada, resultado Red, commit de implementação e resultado Green. Diferencie testes anteriores da U1 dos novos testes conduzidos por TDD.

## 7. E3 — Pipeline e deploy automatizado

### Pipeline

Evolua `.github/workflows/ci.yml`, preservando as verificações existentes. Execute em pushes das branches de trabalho e PRs para main:

- Testes Java e regressões existentes.
- Integração e aceitação com PostgreSQL real.
- Testes C com sanitizers existentes.
- Validação do benchmark.
- Build da imagem e smoke test da aplicação containerizada com banco.

O deploy ocorre após integração na main, somente depois de todos os gates obrigatórios aprovados.

Configure dependências entre jobs. Não esconda falhas com `continue-on-error`, `|| true` ou condições permissivas. Se criar gate agregador, ele falha quando uma verificação obrigatória falhar ou for indevidamente ignorada.

O healthcheck atual que inicia somente o JAR não substitui a demonstração de API e banco pelo Compose.

Publique relatórios e logs necessários, protegendo dados sensíveis. Prepare os gatilhos de testes antes das histórias quando houver autorização para publicar suas etapas Red/Green; finalize o encadeamento de deploy somente na etapa E3.

### GitHub Flow

Aplique de verdade:

- Branch de trabalho.
- Commits rastreáveis.
- Pull request para main.
- Checks obrigatórios configurados na proteção remota da main.
- Merge impedido enquanto os checks estiverem vermelhos.

README dizendo que main é protegida não configura proteção. Verifique o estado remoto. Sem permissão administrativa, informe o ajuste exato para o proprietário e mantenha a pendência.

Não exija no PR um check que só executa depois do merge, como deploy de produção. Não faça push direto na main para contornar o fluxo.

### Render

Mantenha o provedor existente e verifique a configuração real além de `render.yaml`:

- Perfil infra e conexão PostgreSQL no ambiente publicado.
- Credenciais e deploy hook no cofre apropriado, sem valores nos logs.
- Nenhum autodeploy paralelo publicando antes dos testes.
- Publicação do commit efetivamente validado e acompanhamento do deploy.
- Saúde e operação funcional da API pública usando banco.

O Render permite o SHA no parâmetro `ref` do deploy hook. Use o mecanismo documentado para evitar publicar outro commit.

Sucesso ao disparar hook comprova apenas início. Uma versão antiga responder 200 não comprova publicação nova. Relacione pipeline, SHA e identificação do deploy.

Não trate `JAVA_OPTS` como funcional sem verificar consumo: o entrypoint consultado chama diretamente `java -jar`.

### Execuções vermelha e verde

Produza evidências reais de:

- Execução vermelha de uma etapa legítima do TDD.
- Bloqueio de integração/publicação pelos gates.
- Execução verde após implementação.
- Publicação do commit aprovado.
- URL pública e procedimento de rollback.

Distinga job ignorado por condição de branch de bloqueio por teste vermelho. Não provoque falha artificial na main para gerar print.

Documente rollback com versão anterior identificável, procedimento do Render e cuidados com migrações. Não execute rollback de produção apenas para demonstrar.

## 8. README e documentação de entrega

O README deve conter: **Integrantes · Como rodar · Testes · Pipeline e deploy · Decisões · Uso de IA**.

### Integrantes

Eduardo Borges; Luiz Henrique Rocha; Eliziane Mota; Pedro Iranildo; Ricardo Severiano de Souza Filho.

### Conteúdo

- Clone, preparação, inicialização e encerramento; requisitos, variáveis e diferenças entre u1 e infra.
- Story map, histórias, tarefas, critérios, testes e commits.
- Comandos de testes por nível, relatórios reais e limitações.
- Fluxo de branches, gates, nomes dos secrets sem valores, deploy, URL e rollback.
- Decisões justificadas e uso de IA, com principais prompts, validação humana e partes revisadas.

Na tabela de decisões, registre escolha, alternativas e justificativa concreta para imagem base, multi-stage, PostgreSQL, JDBC, migrações, volume, rede, testes, branches, CI/CD, Render e segredos.

Preserve a documentação histórica U1. Indique como demonstrá-la pelo perfil u1. Não apresente JDBC como tradução equivalente da lista em C.

Organize os registros, por exemplo:

- `docs/infra-u2/story-map.md` e `docs/infra-u2/progresso.md`.
- `docs/infra-u2/tdd.md`.
- `docs/infra-u2/checklist.md`.
- `docs/evidencias/`.
- `docs/infra-u2/apresentacao.md`.

O roteiro de apresentação explica jornada, Red → Green, container, pipeline, deploy e lições aprendidas. Cada grupo deve apresentar na próxima aula, conforme a postagem do professor.

A entrega no Classroom é por grupo e inclui link do repositório acessível ao professor, URL pública, README e evidências. Publicar o código não equivale a efetuar a entrega no Classroom.

## 9. Comandos e critérios de validação

Use o wrapper adequado ao sistema, inclusive `mvnw.cmd` no Windows. Referência de comandos, a adaptar somente se a organização de perfis escolhida exigir:

```bash
./mvnw clean verify
./mvnw -Pintegration verify
make -C aed-c test
python -m unittest discover -s bench -p 'test_*.py'
python bench/plot_speedup.py --check
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

Se ajustar os comandos, documente os definitivos e prove descoberta e execução dos testes de integração e aceitação.

| Verificação | Critério observável |
|---|---|
| Modo U1 | Inicia sem banco e usa estruturas manuais reais. |
| Modo infra | Usa PostgreSQL, sem fallback silencioso para memória. |
| Cadastro | Persiste e recupera dados após reinício. |
| Listagem | Preserva envelope, tamanho, ordem e IDs repetidos. |
| Remoção | Exclui a ocorrência correta e mantém exclusão após reinício. |
| Banco indisponível | Não retorna sucesso por operação não persistida. |
| Dockerfile | Executa testes no build e mantém runtime separado. |
| Compose | API e banco sobem pelo comando documentado, com saúde e volume. |
| TDD | Sequência Red → Green real para as histórias. |
| CI | Testes obrigatórios executados; falha bloqueia progressão. |
| Branches | PR real e proteção remota verificada. |
| Deploy | Commit aprovado publicado e operação pública confirmada. |
| Documentação | README, decisões, IA, evidências e apresentação completos. |

Não altere CSVs históricos nem regenere medições para fazer uma checagem passar. Investigue qualquer divergência.

## 10. Conclusão e resposta final da auditoria

Somente na Etapa 9, conclua sobre a entrega completa. Não declare “pronto” enquanto houver requisito obrigatório sem comprovação.

Entregue:

1. Resumo das mudanças, arquivos e decisões.
2. Matriz requisito → implementação → evidência → estado, com `ATENDIDO`, `PENDENTE` ou `NÃO VERIFICADO`.
3. Resultados reais dos comandos, testes, falhas corrigidas e regressões.
4. Referências verificadas a commits Red/Green, PR, pipeline vermelho, pipeline verde e publicação, quando disponíveis.
5. Pendências objetivas: ação exata e responsável necessário.

Arquivo criado não significa requisito comprovado. Deploy configurado não significa deploy executado. Teste escrito não significa teste aprovado. Cada afirmação de conclusão deve apontar para evidência verificável.

Se faltar permissão, credencial ou serviço externo, identifique o limite e conclua as demais verificações permitidas. Não invente conclusão remota.

## 11. Referências

Enunciado: “Desafio do PI — Container, TDD e Deploy automatizados — 2026.2”, fornecido pelo professor. Postagem: “Unidade 2 Desafios PI — Parte 1 (Em Grupo)”, com prazo de 20 de outubro de 2026. O conteúdo acadêmico obrigatório está transcrito nas seções anteriores; a arquitetura e o modo de execução por etapas são orientações deste prompt.

| Fonte | Endereço |
|---|---|
| Repositório | https://github.com/Luizrocha0/Proj-EAD |
| Commit usado na inspeção | https://github.com/Luizrocha0/Proj-EAD/commit/b410dfcf8a6caa39c1fde36fbaa3e52c39863bef |
| Ordem e saúde no Compose | https://docs.docker.com/compose/how-tos/startup-order/ |
| Testcontainers com Spring Boot 3.3 | https://docs.spring.io/spring-boot/3.3/reference/testing/testcontainers.html |
| Deploy hooks do Render | https://render.com/docs/deploy-hooks |

Consulte documentação oficial ao ajustar APIs ou configurações. Não copie cegamente exemplos de outras versões.

---

**Instrução de início: execute agora apenas a Etapa 0 — Diagnóstico. Registre o resultado em `docs/infra-u2/progresso.md`, apresente as evidências e aguarde o usuário mandar continuar.**

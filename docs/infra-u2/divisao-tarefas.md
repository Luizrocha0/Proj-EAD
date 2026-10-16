# Funções da equipe — Infraestrutura U2, Parte 1

**Atualizado em:** 16/10/2026. **Prazo da atividade:** 20/10/2026; confirmar o horário no Classroom.

A divisão define cargos e responsabilidades para a próxima entrega. Cada integrante tem uma função principal e participa da implementação, revisão ou validação conforme seu papel. As atribuições são planejamento; conclusão exige evidência real.

## Cargos e responsabilidades

| Integrante | Função | Responsabilidades principais | Entregável previsto | Revisão e apoio |
| --- | --- | --- | --- | --- |
| Luiz Henrique Rocha | **Tech Leader** | Conduzir decisões técnicas, organizar integração, revisar PRs e orientar a preservação dos contratos da U1 | Arquitetura validada, integração Java e revisões técnicas; persistência e perfis coerentes | Eduardo implementa o backend; Eliziane valida os critérios |
| Eduardo Borges | **Desenvolvedor Backend** | Implementar domínio persistido, migrações, adaptadores e regras da API em ciclos de TDD | Cadastro, consulta, listagem e remoção persistentes, com PostgreSQL e compatibilidade com a U1 | Luiz orienta e revisa; Eliziane escreve e executa os testes com Eduardo |
| Eliziane Mota | **QA Engineer** | Organizar critérios de aceite, conduzir verificação Red → Green e executar testes unitários, de integração e aceitação | Histórias testadas, evidências de TDD e regressões verificadas | Eduardo implementa junto aos testes; Luiz revisa os contratos |
| Pedro Iranildo | **DevOps Engineer** | Preparar Docker/Compose, ambientes, CI/CD, segredos, deploy e procedimento de rollback | API + banco reproduzíveis, gates e publicação do commit aprovado | Luiz revisa a integração; Eliziane valida os gates |
| Ricardo Severiano de Souza Filho | **Product Owner** | Priorizar backlog, organizar escopo e prazos, consolidar documentação e evidências e conferir a entrega acadêmica | Story map, README, checklist, roteiro e pacote de entrega coerentes com o que foi executado | Grupo fornece evidências; Luiz confirma viabilidade técnica e Eliziane verifica aceite |

O Product Owner consolida a entrega; não pode substituir os critérios de avaliação do professor. Cada responsável documenta o que realmente executou e explica sua parte na apresentação.

## Responsáveis por arquivos

| Responsável | Arquivos ou áreas principais | Regra de colaboração |
| --- | --- | --- |
| Luiz — Tech Leader | Integração em `EstoqueService`, configurações de perfis `application*.properties` e `pom.xml` | Coordena dependências de teste e revisa o contrato de armazenamento antes de integrar |
| Eduardo — Backend | Novos contratos/adaptadores de persistência, migrações em `src/main/resources/db/migration/` e documento de dados | Trabalha com testes de Eliziane e revisão de Luiz; preserva os módulos manuais U1 |
| Eliziane — QA | Novos testes em `src/test/` e `docs/infra-u2/tdd.md` | Registra Red antes da implementação e fornece evidências dos testes executados |
| Pedro — DevOps | `Dockerfile`, `compose.yaml`, `.env.example`, `.gitignore`, `.dockerignore`, `.github/workflows/ci.yml`, `render.yaml` e guia de ambiente | Combina configurações Spring com Luiz, sem editar os mesmos arquivos em paralelo |
| Ricardo — Product Owner | `README.md`, `docs/infra-u2/story-map.md`, checklist, índice de evidências e roteiro de apresentação | Consolida informações verificadas dos responsáveis; não marca tarefa concluída por existir apenas planejamento |

Há um único responsável por arquivo durante a edição. Mudanças compartilhadas devem ser combinadas com o responsável para evitar sobrescritas.

## Histórias e trabalho conjunto

| História | Critério principal | Implementação | Testes e aceite | Liderança técnica |
| --- | --- | --- | --- | --- |
| H1 — Cadastrar e recuperar | Gravar no PostgreSQL e recuperar todos os campos após reinício | Eduardo | Eliziane conduz Red/Green; Ricardo confere o critério documentado | Luiz revisa integração e perfis |
| H2 — Listar | Preservar envelope, tamanho, vazio, ordem e IDs repetidos | Eduardo | Eliziane valida banco real e resposta da API | Luiz revisa o contrato existente |
| H3 — Remover | Excluir só a ocorrência mais recente e manter exclusão após reinício; ausência retorna 404 | Eduardo | Eliziane cobre duplicatas, ausência, reinício e falha de persistência | Luiz revisa integridade e comportamento |

Backend e QA trabalham em ciclos curtos por tarefa. Não implemente tudo antes de criar os testes. Identifique teste, comando, commit Red e commit Green reais.

## Entregas de infraestrutura

| Entrega | Responsável principal | Apoio | Evidência |
| --- | --- | --- | --- |
| E1 — Container automatizado | Pedro — DevOps | Eduardo fornece esquema; Luiz integra configuração | Build com testes; API + banco saudáveis; volume e reinício comprovados |
| E2 — TDD | Eliziane — QA | Eduardo implementa; Luiz revisa; Ricardo mantém histórias e critérios | Testes por nível, três histórias, Red/Green real e execução na esteira |
| E3 — Deploy automatizado | Pedro — DevOps | Luiz orienta fluxo e revisão; Eliziane verifica gates | PR, proteção remota, CI vermelha bloqueando progressão, CI verde, SHA publicado e URL funcional |
| Documentação e apresentação | Ricardo — Product Owner | Todos documentam e apresentam suas contribuições | README, decisões, prompts, evidências e roteiro completos |

O acesso consultado nesta sessão permite push, mas não administração. Quem tiver permissão correspondente deve configurar a proteção da main; a equipe precisa verificar o resultado antes de concluir E3.

## Marcos internos propostos

| Data | Marco | Responsáveis |
| --- | --- | --- |
| 16/10 | Backlog, critérios, arquitetura e divisão definidos | Ricardo e Luiz, com o grupo |
| 17/10 | E1: container, banco, configuração externa e saúde | Pedro e Eduardo, com integração de Luiz |
| 18/10 | E2: H1/H2/H3, persistência, TDD e regressões | Eduardo e Eliziane, com revisão de Luiz |
| 19/10 | E3: gates, proteção, publicação e evidências | Pedro e Luiz, com validação de Eliziane |
| 20/10 | Auditoria, ensaio e envio antes do horário oficial | Ricardo consolida; todos conferem e apresentam |

Os marcos são planejamento. Registre a consequência de uma falha antes de avançar por uma dependência.

## Estado inicial

| Função | Responsável | Frente | Estado |
| --- | --- | --- | --- |
| Tech Leader | Luiz | Arquitetura, integração e revisão | Planejado |
| Backend | Eduardo | Domínio e persistência | Planejado |
| QA | Eliziane | TDD, integração e aceitação | Planejado |
| DevOps | Pedro | Containers, CI/CD e deploy | Planejado |
| Product Owner | Ricardo | Backlog, documentação e entrega | Planejado |

## Apresentação

Luiz explica as decisões técnicas e a integração; Eduardo demonstra backend e persistência; Eliziane apresenta testes e Red/Green; Pedro demonstra containers e pipeline; Ricardo contextualiza requisitos, organização, evidências e entrega. Todos devem compreender o fluxo completo e as limitações.

O [prompt por etapas](PROMPT_EXECUCAO_POR_ETAPAS.md) detalha a execução. Ele exige uma etapa por resposta, validação antes do avanço e retomada quando o usuário mandar continuar.

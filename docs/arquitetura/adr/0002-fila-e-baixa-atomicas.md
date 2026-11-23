# ADR 0002 · Fila compartilhada e baixa atômica

**Status** · Adotado na refatoração local da Parte 2

## Contexto

A fila em memória era exclusiva de cada processo.
O encerramento do processo descartava os pedidos pendentes.
A baixa JDBC podia confirmar antes do consumo da fila.

## Decisão

Persistir a fila no PostgreSQL no perfil `infra`.
Serializar suas escritas e consumir o pedido na transação da baixa.
Preservar as estruturas encadeadas no perfil pedagógico `u1`.

## Consequências

Réplicas enxergam os mesmos pedidos pendentes.
Falhas revertem as alterações do atendimento em conjunto.
O bloqueio limita a vazão de mutações da fila.
A fila em memória foi descartada para réplicas do perfil `infra`.
A migração adiciona uma tabela. Pedidos em memória exigem drenagem antes da troca.

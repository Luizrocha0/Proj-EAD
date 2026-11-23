# ADR 0001 · Decomposição do Rota Vital

**Status** · Adotado na refatoração local da Parte 2

## Contexto

O atendimento reúne fila FIFO, baixa FEFO e roteirização.
A equipe mantém uma API e uma esteira compartilhada.
Baixa e consumo do pedido precisam confirmar juntos.

## Decisão

Adotar um monólito modular com quatro contextos.
Expedição integra Requisições e Estoque por contratos internos.
Comparação Textual permanece independente dos fluxos hospitalares.

## Consequências

Um deploy e uma transação simplificam a operação inicial.
Todos os contextos compartilham o processo e seus recursos.
Testes de fronteira verificam as importações do negócio.
Microsserviços exigiriam mensagens, compensações e observabilidade distribuída.
Reavaliar a extração após medir contenção e escala independente.

# language: pt
Funcionalidade: Confirmar baixa integral para o próximo pedido

  Cenário: Estoque insuficiente permanece intacto até receber outra ocorrência válida
    Dado que a data de referência é 23/11/2026
    E que REQ-BDD-01 está na cabeça da fila e pede duas bolsas O-
    E que o estoque possui uma bolsa O- válida até 23/11/2026
    E que existe uma bolsa O- vencida e uma O+ válida
    Quando a pessoa operadora solicita atendimento com destino ao hospital do pedido
    Então a API informa conflito de estoque insuficiente
    E mantém REQ-BDD-01 na cabeça da fila
    E mantém as três ocorrências no estoque
    Quando outra ocorrência O- válida até 25/11/2026 é registrada
    E a pessoa operadora tenta atender REQ-BDD-01 novamente
    Então a API confirma exatamente duas bolsas em ordem FEFO
    E retira REQ-BDD-01 da fila
    E preserva a ocorrência vencida e a incompatível

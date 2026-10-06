# Análise

Média HTTP: cinco repetições; 1, 2, 4 e 8 threads de plataforma.<br>
Com oito threads: 3,31x em 8.000 registros e 2,37x em 16.000.<br>
O ganho é não linear e fica abaixo do ideal de 8x.<br>
Em 16.000 e duas threads: 72.741–102.051 ms; a causa não foi isolada.<br>
Cada índice i faz n-i-1 comparações; fatias iguais ficam desbalanceadas.<br>
Criar o pool, agendar e agregar tarefas acrescenta overhead.<br>
Contadores locais são somados via Future.get(), sem contador compartilhado.<br>
Os mesmos n(n-1)/2 pares são comparados nas duas versões.<br>
Levenshtein leva o trabalho a O(n² · L²), com textos limitados por L.<br>
Para L fixo, o custo é O(n²); dobrar n elevou o tempo sequencial 4,10x.<br>
Threads reduzem o tempo de parede e mantêm a ordem de complexidade.<br>
Na Mesa DJ, concorrência intercala eventos de teclado, MIDI e áudio.<br>
Aqui, paralelismo executa fatias juntas quando há núcleos disponíveis.<br>
Uma proposta futura é balancear tarefas pela quantidade de pares.<br>
Selecionar candidatos reduz comparações, com risco de falso negativo.<br>
Execução assíncrona e distribuição entre máquinas são propostas para U2.<br>
Essas propostas ainda não foram implementadas nem medidas.<br>
100 mil e 1 milhão têm apenas [estimativas](medicoes.md) de escala.

# ─── Makefile — Estruturas AED em C ───
# Compilação com AddressSanitizer e UndefinedBehaviorSanitizer
# para detectar vazamentos de memória e comportamento indefinido.
#
# Uso:
#   make test       — compila e executa todos os testes
#   make clean      — remove binários
#   make test_lista — compila e executa apenas testes da lista
#   make test_fila  — compila e executa apenas testes da fila

CC      = gcc
CFLAGS  ?= -Wall -Wextra -std=c11 -g -fsanitize=address,undefined
LDFLAGS ?= -fsanitize=address,undefined
INCLUDE = -Iinclude

# ── Alvos ──

.PHONY: test clean test_lista test_fila

test: tests/test_lista tests/test_fila
	@echo ""
	@echo "╔═══════════════════════════════════════════╗"
	@echo "║  Executando testes C com sanitizers...    ║"
	@echo "╚═══════════════════════════════════════════╝"
	@echo ""
	./tests/test_lista
	@echo ""
	./tests/test_fila
	@echo ""
	@echo "✅ Todos os testes C passaram!"

test_lista: tests/test_lista
	./tests/test_lista

test_fila: tests/test_fila
	./tests/test_fila

# ── Compilação ──

tests/test_lista: src/u1/lista.c tests/test_lista.c include/lista.h include/bolsa.h
	$(CC) $(CFLAGS) $(INCLUDE) src/u1/lista.c tests/test_lista.c -o tests/test_lista $(LDFLAGS)

tests/test_fila: src/u1/fila.c tests/test_fila.c include/fila.h include/requisicao.h
	$(CC) $(CFLAGS) $(INCLUDE) src/u1/fila.c tests/test_fila.c -o tests/test_fila $(LDFLAGS)

ifeq ($(OS),Windows_NT)
    CLEAN_CMD = cmd /c "del /f /q tests\test_lista.exe tests\test_fila.exe tests\test_lista tests\test_fila 2>nul || exit 0"
else
    CLEAN_CMD = rm -f tests/test_lista tests/test_fila tests/*.exe
endif

clean:
	@$(CLEAN_CMD)

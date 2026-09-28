package com.rotavital.aed.u1;

import com.rotavital.domain.Requisicao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários da FilaRequisicoes.
 *
 * Utiliza as mesmas fixtures de fixtures/requisicoes_u1.json e os mesmos
 * resultados esperados de fixtures/resultados_esperados_u1.json.
 *
 * Cenários:
 * - Fila vazia
 * - Enfileiramento FIFO
 * - Desenfileiramento preserva ordem
 * - Espiar sem remover
 * - Fila que esvazia e volta a ser preenchida
 */
@DisplayName("FilaRequisicoes — Fila Encadeada FIFO (AED U1)")
class FilaRequisicaoTest {

    private FilaRequisicoes fila;
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 28, 10, 0);

    // ── Fixtures (mesmos dados de fixtures/requisicoes_u1.json) ──
    private static final Requisicao REQ_001 = new Requisicao("REQ-001", "HOSP-CENTRAL", "O+",
            2, Requisicao.Prioridade.EMERGENCIA, AGORA);
    private static final Requisicao REQ_002 = new Requisicao("REQ-002", "HOSP-NORTE", "A-",
            1, Requisicao.Prioridade.URGENTE, AGORA.plusMinutes(5));
    private static final Requisicao REQ_003 = new Requisicao("REQ-003", "HOSP-SUL", "B+",
            3, Requisicao.Prioridade.NORMAL, AGORA.plusMinutes(10));
    private static final Requisicao REQ_004 = new Requisicao("REQ-004", "HOSP-LESTE", "AB+",
            1, Requisicao.Prioridade.URGENTE, AGORA.plusMinutes(15));
    private static final Requisicao REQ_005 = new Requisicao("REQ-005", "HOSP-OESTE", "O-",
            4, Requisicao.Prioridade.EMERGENCIA, AGORA.plusMinutes(20));

    @BeforeEach
    void setUp() {
        fila = new FilaRequisicoes();
    }

    @Nested
    @DisplayName("Fila vazia")
    class FilaVazia {

        @Test
        @DisplayName("Fila recém-criada deve ter tamanho 0")
        void deveEstarVazia() {
            assertEquals(0, fila.getTamanho());
            assertTrue(fila.isVazia());
        }

        @Test
        @DisplayName("Desenfileirar fila vazia retorna empty")
        void desenfileirarVaziaRetornaEmpty() {
            Optional<Requisicao> resultado = fila.desenfileirar();
            assertTrue(resultado.isEmpty());
        }

        @Test
        @DisplayName("Espiar fila vazia retorna empty")
        void espiarVaziaRetornaEmpty() {
            Optional<Requisicao> resultado = fila.espiar();
            assertTrue(resultado.isEmpty());
        }

        @Test
        @DisplayName("toArray em fila vazia retorna array vazio")
        void toArrayVazioRetornaArrayVazio() {
            Requisicao[] resultado = fila.toArray();
            assertEquals(0, resultado.length);
        }
    }

    @Nested
    @DisplayName("Enfileiramento")
    class Enfileiramento {

        @Test
        @DisplayName("Enfileirar primeiro elemento muda tamanho para 1")
        void enfileirarPrimeiroElemento() {
            fila.enfileirar(REQ_001);
            assertEquals(1, fila.getTamanho());
            assertFalse(fila.isVazia());
        }

        @Test
        @DisplayName("Após enfileirar 5 requisições, tamanho deve ser 5")
        void enfileirarCincoRequisicoes() {
            enfileirarTodasFixtures();
            assertEquals(5, fila.getTamanho());
        }

        @Test
        @DisplayName("Ordem FIFO — primeira enfileirada aparece primeiro no array")
        void ordemFifo() {
            enfileirarTodasFixtures();
            Requisicao[] array = fila.toArray();
            assertEquals("REQ-001", array[0].getId());
            assertEquals("REQ-002", array[1].getId());
            assertEquals("REQ-003", array[2].getId());
            assertEquals("REQ-004", array[3].getId());
            assertEquals("REQ-005", array[4].getId());
        }
    }

    @Nested
    @DisplayName("Desenfileiramento")
    class Desenfileiramento {

        @BeforeEach
        void inserirFixtures() {
            enfileirarTodasFixtures();
        }

        @Test
        @DisplayName("Primeiro desenfileiramento retorna REQ-001 (HOSP-CENTRAL)")
        void primeiroDesenfileiramento() {
            Optional<Requisicao> resultado = fila.desenfileirar();
            assertTrue(resultado.isPresent());
            assertEquals("REQ-001", resultado.get().getId());
            assertEquals("HOSP-CENTRAL", resultado.get().getHospitalId());
            assertEquals(4, fila.getTamanho());
        }

        @Test
        @DisplayName("Segundo desenfileiramento retorna REQ-002 (HOSP-NORTE)")
        void segundoDesenfileiramento() {
            fila.desenfileirar(); // Remove REQ-001
            Optional<Requisicao> resultado = fila.desenfileirar();
            assertTrue(resultado.isPresent());
            assertEquals("REQ-002", resultado.get().getId());
            assertEquals("HOSP-NORTE", resultado.get().getHospitalId());
            assertEquals(3, fila.getTamanho());
        }

        @Test
        @DisplayName("Desenfileirar todos e depois tentar mais — retorna empty")
        void desenfileirarTodosEMais() {
            for (int i = 0; i < 5; i++) {
                assertTrue(fila.desenfileirar().isPresent());
            }
            assertEquals(0, fila.getTamanho());
            assertTrue(fila.isVazia());

            // Tentar desenfileirar fila já vazia
            assertTrue(fila.desenfileirar().isEmpty());
        }
    }

    @Nested
    @DisplayName("Espiar (peek)")
    class Espiar {

        @Test
        @DisplayName("Espiar não altera o tamanho da fila")
        void espiarNaoAlteraTamanho() {
            enfileirarTodasFixtures();
            fila.desenfileirar(); // Remove REQ-001
            fila.desenfileirar(); // Remove REQ-002

            Optional<Requisicao> espiada = fila.espiar();
            assertTrue(espiada.isPresent());
            assertEquals("REQ-003", espiada.get().getId());
            assertEquals(3, fila.getTamanho()); // Tamanho inalterado
        }
    }

    @Nested
    @DisplayName("Reuso da fila")
    class Reuso {

        @Test
        @DisplayName("Esvaziar e preencher novamente funciona corretamente")
        void esvaziarEPreencher() {
            fila.enfileirar(REQ_001);
            fila.desenfileirar();
            assertTrue(fila.isVazia());

            // Preencher novamente
            fila.enfileirar(REQ_005);
            fila.enfileirar(REQ_003);
            assertEquals(2, fila.getTamanho());

            Optional<Requisicao> resultado = fila.desenfileirar();
            assertTrue(resultado.isPresent());
            assertEquals("REQ-005", resultado.get().getId());
        }
    }

    /**
     * Enfileira as 5 requisições das fixtures na ordem: 001, 002, 003, 004, 005.
     * FIFO: primeiro a entrar (REQ-001) é o primeiro a sair.
     */
    private void enfileirarTodasFixtures() {
        fila.enfileirar(REQ_001);
        fila.enfileirar(REQ_002);
        fila.enfileirar(REQ_003);
        fila.enfileirar(REQ_004);
        fila.enfileirar(REQ_005);
    }
}

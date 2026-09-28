package com.rotavital.aed.u1;

import com.rotavital.domain.Bolsa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários da ListaEstoque.
 *
 * Utiliza as mesmas fixtures de fixtures/bolsas_u1.json e os mesmos
 * resultados esperados de fixtures/resultados_esperados_u1.json.
 *
 * Cenários:
 * - Lista vazia
 * - Inserção de elementos
 * - Consulta existente e inexistente
 * - Remoção da cabeça, do meio, da cauda e inexistente
 * - Operações em sequência
 */
@DisplayName("ListaEstoque — Lista Encadeada de Bolsas (AED U1)")
class ListaEstoqueTest {

    private ListaEstoque lista;

    // ── Fixtures (mesmos dados de fixtures/bolsas_u1.json) ──
    private static final Bolsa BOLSA_001 = new Bolsa("BOLSA-001", "O+", 450.0,
            LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 1), "João Silva");
    private static final Bolsa BOLSA_002 = new Bolsa("BOLSA-002", "A-", 450.0,
            LocalDate.of(2026, 9, 5), LocalDate.of(2026, 11, 5), "Maria Santos");
    private static final Bolsa BOLSA_003 = new Bolsa("BOLSA-003", "B+", 400.0,
            LocalDate.of(2026, 9, 10), LocalDate.of(2026, 11, 10), "Carlos Oliveira");
    private static final Bolsa BOLSA_004 = new Bolsa("BOLSA-004", "AB+", 450.0,
            LocalDate.of(2026, 8, 15), LocalDate.of(2026, 10, 15), "Ana Costa");
    private static final Bolsa BOLSA_005 = new Bolsa("BOLSA-005", "O-", 500.0,
            LocalDate.of(2026, 9, 20), LocalDate.of(2026, 11, 20), "Pedro Almeida");

    @BeforeEach
    void setUp() {
        lista = new ListaEstoque();
    }

    @Nested
    @DisplayName("Lista vazia")
    class ListaVazia {

        @Test
        @DisplayName("Lista recém-criada deve ter tamanho 0")
        void deveEstarVazia() {
            assertEquals(0, lista.getTamanho());
            assertTrue(lista.isVazia());
        }

        @Test
        @DisplayName("Consulta em lista vazia retorna empty")
        void consultaEmListaVaziaRetornaEmpty() {
            Optional<Bolsa> resultado = lista.consultar("BOLSA-001");
            assertTrue(resultado.isEmpty());
        }

        @Test
        @DisplayName("Remoção em lista vazia retorna false")
        void remocaoEmListaVaziaRetornaFalse() {
            assertFalse(lista.remover("BOLSA-001"));
        }

        @Test
        @DisplayName("toArray em lista vazia retorna array vazio")
        void toArrayVazioRetornaArrayVazio() {
            Bolsa[] resultado = lista.toArray();
            assertEquals(0, resultado.length);
        }
    }

    @Nested
    @DisplayName("Inserção")
    class Insercao {

        @Test
        @DisplayName("Inserir primeiro elemento muda tamanho para 1")
        void inserirPrimeiroElemento() {
            lista.inserir(BOLSA_001);
            assertEquals(1, lista.getTamanho());
            assertFalse(lista.isVazia());
        }

        @Test
        @DisplayName("Após inserir 5 bolsas, tamanho deve ser 5")
        void inserirCincoBolsas() {
            inserirTodasFixtures();
            assertEquals(5, lista.getTamanho());
        }

        @Test
        @DisplayName("Inserção no início — última inserida fica na cabeça (conforme resultados_esperados_u1.json)")
        void inserirNoInicio() {
            inserirTodasFixtures();
            Bolsa[] array = lista.toArray();
            // Ordem da cabeça para cauda: BOLSA-005, BOLSA-004, BOLSA-003, BOLSA-002, BOLSA-001
            assertEquals("BOLSA-005", array[0].getId());
            assertEquals("BOLSA-004", array[1].getId());
            assertEquals("BOLSA-003", array[2].getId());
            assertEquals("BOLSA-002", array[3].getId());
            assertEquals("BOLSA-001", array[4].getId());
        }
    }

    @Nested
    @DisplayName("Consulta")
    class Consulta {

        @BeforeEach
        void inserirFixtures() {
            inserirTodasFixtures();
        }

        @Test
        @DisplayName("Consultar bolsa existente retorna dados corretos (BOLSA-003)")
        void consultarExistente() {
            Optional<Bolsa> resultado = lista.consultar("BOLSA-003");
            assertTrue(resultado.isPresent());
            assertEquals("B+", resultado.get().getTipoSanguineo());
            assertEquals("Carlos Oliveira", resultado.get().getDoador());
        }

        @Test
        @DisplayName("Consultar bolsa inexistente retorna empty (BOLSA-999)")
        void consultarInexistente() {
            Optional<Bolsa> resultado = lista.consultar("BOLSA-999");
            assertTrue(resultado.isEmpty());
        }
    }

    @Nested
    @DisplayName("Remoção")
    class Remocao {

        @BeforeEach
        void inserirFixtures() {
            inserirTodasFixtures();
        }

        @Test
        @DisplayName("Remover cabeça (BOLSA-005) — tamanho cai para 4")
        void removerCabeca() {
            assertTrue(lista.remover("BOLSA-005"));
            assertEquals(4, lista.getTamanho());
            assertTrue(lista.consultar("BOLSA-005").isEmpty());
        }

        @Test
        @DisplayName("Remover do meio (BOLSA-003) — tamanho cai para 4")
        void removerMeio() {
            assertTrue(lista.remover("BOLSA-003"));
            assertEquals(4, lista.getTamanho());
            assertTrue(lista.consultar("BOLSA-003").isEmpty());
        }

        @Test
        @DisplayName("Remover cauda (BOLSA-001) — tamanho cai para 4")
        void removerCauda() {
            assertTrue(lista.remover("BOLSA-001"));
            assertEquals(4, lista.getTamanho());
            assertTrue(lista.consultar("BOLSA-001").isEmpty());
        }

        @Test
        @DisplayName("Remover inexistente (BOLSA-999) — retorna false e tamanho inalterado")
        void removerInexistente() {
            assertFalse(lista.remover("BOLSA-999"));
            assertEquals(5, lista.getTamanho());
        }

        @Test
        @DisplayName("Remoções em sequência — cabeça, meio e cauda")
        void remocoesEmSequencia() {
            // Remove cabeça
            assertTrue(lista.remover("BOLSA-005"));
            assertEquals(4, lista.getTamanho());

            // Remove meio (BOLSA-003)
            assertTrue(lista.remover("BOLSA-003"));
            assertEquals(3, lista.getTamanho());

            // Remove cauda (BOLSA-001)
            assertTrue(lista.remover("BOLSA-001"));
            assertEquals(2, lista.getTamanho());

            // Tentativa de remover inexistente
            assertFalse(lista.remover("BOLSA-999"));
            assertEquals(2, lista.getTamanho());

            // Verifica que restam apenas BOLSA-004 e BOLSA-002
            assertTrue(lista.consultar("BOLSA-004").isPresent());
            assertTrue(lista.consultar("BOLSA-002").isPresent());
        }
    }

    /**
     * Insere as 5 bolsas das fixtures na ordem: 001, 002, 003, 004, 005.
     * Resultado na lista (cabeça→cauda): 005, 004, 003, 002, 001.
     */
    private void inserirTodasFixtures() {
        lista.inserir(BOLSA_001);
        lista.inserir(BOLSA_002);
        lista.inserir(BOLSA_003);
        lista.inserir(BOLSA_004);
        lista.inserir(BOLSA_005);
    }
}

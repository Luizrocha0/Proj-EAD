package com.rotavital.storage;

import com.rotavital.domain.Bolsa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários - ListaEstoqueStorage (Perfil u1/default)")
class ListaEstoqueStorageTest {

    private ListaEstoqueStorage storage;

    @BeforeEach
    void setUp() {
        storage = new ListaEstoqueStorage();
    }

    private Bolsa criarBolsa(String id, String tipo, double volume, String doador) {
        return new Bolsa(id, tipo, volume, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 1), doador);
    }

    @Test
    @DisplayName("T1.1: Deve salvar e consultar bolsa na estrutura em memória")
    void deveSalvarEConsultarBolsa() {
        Bolsa bolsa = criarBolsa("BOLSA-001", "O+", 450.0, "João Silva");
        storage.salvar(bolsa);

        Optional<Bolsa> resultado = storage.buscarPorId("BOLSA-001");
        assertTrue(resultado.isPresent());
        assertEquals("BOLSA-001", resultado.get().getId());
        assertEquals("O+", resultado.get().getTipoSanguineo());
    }

    @Test
    @DisplayName("T1.1: Deve retornar Optional vazio para ID inexistente")
    void deveRetornarVazioAoConsultarIdInexistente() {
        Optional<Bolsa> resultado = storage.buscarPorId("BOLSA-INEXISTENTE");
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("T1.1: Deve retornar tamanho correto conforme inserções e remoções")
    void deveRetornarTamanhoCorreto() {
        assertEquals(0, storage.tamanho());
        storage.salvar(criarBolsa("B-1", "O+", 450.0, "D1"));
        assertEquals(1, storage.tamanho());
        storage.salvar(criarBolsa("B-2", "A+", 450.0, "D2"));
        assertEquals(2, storage.tamanho());
        storage.removerPorId("B-1");
        assertEquals(1, storage.tamanho());
    }

    @Test
    @DisplayName("T1.1: Deve delegar remoção e retornar booleano de sucesso")
    void deveRemoverBolsaERetornarBooleano() {
        Bolsa bolsa = criarBolsa("BOLSA-001", "O+", 450.0, "João Silva");
        storage.salvar(bolsa);

        boolean removido = storage.removerPorId("BOLSA-001");
        assertTrue(removido);
        assertFalse(storage.removerPorId("BOLSA-001"));
    }

    @Test
    @DisplayName("T1.1: Deve listar todas as bolsas em ordem LIFO")
    void deveListarTodasBolsasEmOrdemLIFO() {
        storage.salvar(criarBolsa("B-1", "O+", 450.0, "D1"));
        storage.salvar(criarBolsa("B-2", "A+", 450.0, "D2"));

        Bolsa[] bolsas = storage.listarTodas();
        assertEquals(2, bolsas.length);
        assertEquals("B-2", bolsas[0].getId());
        assertEquals("B-1", bolsas[1].getId());
    }

    @Test
    @DisplayName("Adversarial: Deve rejeitar salvar bolsa nula com fail-fast")
    void deveRejeitarSalvarBolsaNula() {
        assertThrows(NullPointerException.class, () -> storage.salvar(null));
    }

    @Test
    @DisplayName("Adversarial: Deve retornar Optional vazio para busca com id nulo ou em branco")
    void deveRetornarVazioAoConsultarIdNuloOuEmBranco() {
        assertTrue(storage.buscarPorId(null).isEmpty());
        assertTrue(storage.buscarPorId("   ").isEmpty());
    }

    @Test
    @DisplayName("Adversarial: Deve retornar false para remoção com id nulo ou em branco")
    void deveRetornarFalseAoRemoverIdNuloOuEmBranco() {
        assertFalse(storage.removerPorId(null));
        assertFalse(storage.removerPorId("   "));
    }
}

package com.rotavital.storage;

import com.rotavital.domain.Bolsa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
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
    @DisplayName("Adversarial: Deve retornar Optional vazio para busca com id nulo, em branco ou excedendo 50 caracteres")
    void deveRetornarVazioAoConsultarIdNuloOuEmBranco() {
        assertTrue(storage.buscarPorId(null).isEmpty());
        assertTrue(storage.buscarPorId("   ").isEmpty());
        assertTrue(storage.buscarPorId("A".repeat(51)).isEmpty());
    }

    @Test
    @DisplayName("Adversarial: Deve retornar false para remoção com id nulo, em branco ou excedendo 50 caracteres")
    void deveRetornarFalseAoRemoverIdNuloOuEmBranco() {
        assertFalse(storage.removerPorId(null));
        assertFalse(storage.removerPorId("   "));
        assertFalse(storage.removerPorId("A".repeat(51)));
    }

    @Test
    @DisplayName("H2 / T2.1: Deve listar bolsas em ordem LIFO retornando List<Bolsa>")
    void deveListarBolsasEmOrdemLIFOComLista() {
        storage.salvar(criarBolsa("B-1", "O+", 450.0, "D1"));
        storage.salvar(criarBolsa("B-2", "A+", 450.0, "D2"));

        List<Bolsa> bolsas = storage.listar();
        assertEquals(2, bolsas.size());
        assertEquals("B-2", bolsas.get(0).getId());
        assertEquals("B-1", bolsas.get(1).getId());
    }

    @Test
    @DisplayName("H2 / T2.1: Deve retornar lista vazia sem erros quando estoque estiver vazio")
    void deveRetornarListaVaziaQuandoEstoqueVazio() {
        List<Bolsa> bolsas = storage.listar();
        assertNotNull(bolsas);
        assertTrue(bolsas.isEmpty());
        assertEquals(0, bolsas.size());
    }

    @Test
    @DisplayName("H2 / T2.1: Deve preservar todas as ocorrências de IDs duplicados em ordem temporal LIFO")
    void devePreservarTodasAsOcorrenciasDeIdsDuplicadosEmOrdemLIFO() {
        storage.salvar(criarBolsa("B-1", "O+", 400.0, "Doador A"));
        storage.salvar(criarBolsa("B-2", "A+", 420.0, "Doador B"));
        storage.salvar(criarBolsa("B-1", "O+", 450.0, "Doador C"));

        List<Bolsa> bolsas = storage.listar();
        assertEquals(3, bolsas.size());
        assertEquals("B-1", bolsas.get(0).getId());
        assertEquals("Doador C", bolsas.get(0).getDoador());
        assertEquals("B-2", bolsas.get(1).getId());
        assertEquals("Doador B", bolsas.get(1).getDoador());
        assertEquals("B-1", bolsas.get(2).getId());
        assertEquals("Doador A", bolsas.get(2).getDoador());
    }

    @Test
    @DisplayName("H2 / Defensivo: Deve retornar cópia desacoplada em listar() não afetando armazenamento interno")
    void deveRetornarCopiaDesacopladaEmListarNaoAfetandoArmazenamentoInterno() {
        storage.salvar(criarBolsa("B-1", "O+", 450.0, "D1"));
        storage.salvar(criarBolsa("B-2", "A+", 420.0, "D2"));

        List<Bolsa> lista = storage.listar();
        assertEquals(2, lista.size());

        lista.clear();
        assertEquals(0, lista.size());
        assertEquals(2, storage.tamanho(), "Tamanho interno do storage não deve ser afetado pela modificação da lista");
        assertEquals(2, storage.listar().size(), "Nova chamada a listar() deve recuperar todas as bolsas intactas");
    }

    @Test
    @DisplayName("H3 / T3.1: Deve remover apenas a ocorrência mais recente sob IDs duplicados em memória")
    void deveRemoverApenasOcorrenciaMaisRecenteSobIdsDuplicadosEmMemoria() {
        storage.salvar(criarBolsa("BOLSA-DUP", "O+", 400.0, "Doador Antigo"));
        storage.salvar(criarBolsa("BOLSA-DUP", "O+", 460.0, "Doador Recente"));
        assertEquals(2, storage.tamanho());

        boolean removido = storage.removerPorId("BOLSA-DUP");
        assertTrue(removido, "Primeira remoção deve retornar true");
        assertEquals(1, storage.tamanho(), "Tamanho deve decrementar para 1");

        Optional<Bolsa> restante = storage.buscarPorId("BOLSA-DUP");
        assertTrue(restante.isPresent());
        assertEquals("Doador Antigo", restante.get().getDoador(), "A ocorrência mais antiga deve permanecer intacta");

        boolean removidoSegundo = storage.removerPorId("BOLSA-DUP");
        assertTrue(removidoSegundo, "Segunda remoção deve remover a ocorrência restante");
        assertEquals(0, storage.tamanho());
        assertTrue(storage.buscarPorId("BOLSA-DUP").isEmpty());

        assertFalse(storage.removerPorId("BOLSA-DUP"), "Terceira remoção em lista vazia deve retornar false");
    }

    @Test
    @DisplayName("H3 / T3.1: Deve retornar false ao tentar remover bolsa inexistente com estoque vazio ou populado")
    void deveRetornarFalseAoTentarRemoverBolsaInexistenteEmMemoria() {
        assertFalse(storage.removerPorId("BOLSA-INEXISTENTE"), "Remoção em estoque vazio deve retornar false");

        storage.salvar(criarBolsa("BOLSA-001", "A+", 450.0, "Doador"));
        assertFalse(storage.removerPorId("BOLSA-OUTRA"), "Remoção de ID inexistente com estoque populado deve retornar false");
        assertEquals(1, storage.tamanho(), "Tamanho não deve ser alterado");
    }

    @Test
    @DisplayName("H3 / T3.1 / Defensivo: Deve retornar false para ID nulo, em branco ou excedendo 50 caracteres em memória")
    void deveRetornarFalseParaIdInvalidoEmMemoria() {
        assertFalse(storage.removerPorId(null));
        assertFalse(storage.removerPorId("   "));
        assertFalse(storage.removerPorId("A".repeat(51)));
    }

    @Test
    @DisplayName("H3 / T3.1: Deve realizar trim no ID ao remover em memória")
    void deveFazerTrimNoIdAoRemoverEmMemoria() {
        storage.salvar(criarBolsa("BOLSA-TRIM", "O+", 450.0, "Doador"));
        assertEquals(1, storage.tamanho());

        assertTrue(storage.removerPorId("   BOLSA-TRIM   "));
        assertEquals(0, storage.tamanho());
    }
}

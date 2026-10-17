package com.rotavital.service;

import com.rotavital.domain.Bolsa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Caracterização da U1 - EstoqueService")
class EstoqueServiceCaracterizacaoTest {

    private EstoqueService service;

    @BeforeEach
    void setUp() {
        service = new EstoqueService();
    }

    private Bolsa criarBolsa(String id, String tipo, double volume, String doador) {
        return new Bolsa(id, tipo, volume, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 1), doador);
    }

    @Test
    @DisplayName("Deve inserir e consultar bolsa existente")
    void deveInserirEConsultarBolsa() {
        Bolsa bolsa = criarBolsa("BOLSA-001", "O+", 450.0, "João Silva");
        service.adicionar(bolsa);

        Optional<Bolsa> resultado = service.consultar("BOLSA-001");
        assertTrue(resultado.isPresent());
        assertEquals("BOLSA-001", resultado.get().getId());
        assertEquals("O+", resultado.get().getTipoSanguineo());
        assertEquals(450.0, resultado.get().getVolumeMl());
        assertEquals("João Silva", resultado.get().getDoador());
    }

    @Test
    @DisplayName("Deve retornar array em ordem LIFO (mais recente primeiro)")
    void deveRetornarArrayEmOrdemLIFO() {
        Bolsa b1 = criarBolsa("BOLSA-001", "O+", 450.0, "Doador 1");
        Bolsa b2 = criarBolsa("BOLSA-002", "A-", 400.0, "Doador 2");
        Bolsa b3 = criarBolsa("BOLSA-003", "B+", 420.0, "Doador 3");

        service.adicionar(b1);
        service.adicionar(b2);
        service.adicionar(b3);

        Bolsa[] bolsas = service.listarTodas();
        assertEquals(3, bolsas.length);
        assertEquals("BOLSA-003", bolsas[0].getId());
        assertEquals("BOLSA-002", bolsas[1].getId());
        assertEquals("BOLSA-001", bolsas[2].getId());
    }

    @Test
    @DisplayName("H2 / Caracterização: Deve listar todas as bolsas em ordem LIFO retornando List<Bolsa>")
    void deveListarEstoqueEmOrdemLIFORetornandoList() {
        Bolsa b1 = criarBolsa("BOLSA-001", "O+", 450.0, "Doador 1");
        Bolsa b2 = criarBolsa("BOLSA-002", "A-", 400.0, "Doador 2");
        Bolsa b3 = criarBolsa("BOLSA-003", "B+", 420.0, "Doador 3");

        service.adicionar(b1);
        service.adicionar(b2);
        service.adicionar(b3);

        java.util.List<Bolsa> bolsas = service.listarEstoque();
        assertEquals(3, bolsas.size());
        assertEquals("BOLSA-003", bolsas.get(0).getId());
        assertEquals("BOLSA-002", bolsas.get(1).getId());
        assertEquals("BOLSA-001", bolsas.get(2).getId());
    }

    @Test
    @DisplayName("Deve consultar a ocorrência mais recente em caso de duplicatas")
    void deveConsultarOcorrenciaMaisRecenteEmDuplicatas() {
        Bolsa antiga = criarBolsa("BOLSA-DUP", "O+", 450.0, "Doador Antigo");
        Bolsa recente = criarBolsa("BOLSA-DUP", "O+", 460.0, "Doador Recente");

        service.adicionar(antiga);
        service.adicionar(recente);

        Optional<Bolsa> resultado = service.consultar("BOLSA-DUP");
        assertTrue(resultado.isPresent());
        assertEquals("Doador Recente", resultado.get().getDoador());
        assertEquals(460.0, resultado.get().getVolumeMl());
    }

    @Test
    @DisplayName("Deve remover apenas a ocorrência mais recente em caso de duplicatas")
    void deveRemoverApenasOcorrenciaMaisRecenteEmDuplicatas() {
        Bolsa antiga = criarBolsa("BOLSA-DUP", "O+", 450.0, "Doador Antigo");
        Bolsa recente = criarBolsa("BOLSA-DUP", "O+", 460.0, "Doador Recente");

        service.adicionar(antiga);
        service.adicionar(recente);
        assertEquals(2, service.tamanho());

        boolean removido = service.remover("BOLSA-DUP");
        assertTrue(removido);
        assertEquals(1, service.tamanho());

        Optional<Bolsa> restante = service.consultar("BOLSA-DUP");
        assertTrue(restante.isPresent());
        assertEquals("Doador Antigo", restante.get().getDoador());
    }

    @Test
    @DisplayName("Deve retornar false ao remover bolsa inexistente")
    void deveRetornarFalseAoRemoverInexistente() {
        boolean removido = service.remover("BOLSA-INEXISTENTE");
        assertFalse(removido);
    }

    @Test
    @DisplayName("Deve retornar tamanho correto do estoque")
    void deveRetornarTamanhoCorreto() {
        assertEquals(0, service.tamanho());
        service.adicionar(criarBolsa("B-1", "O+", 450.0, "D1"));
        assertEquals(1, service.tamanho());
        service.adicionar(criarBolsa("B-2", "A+", 450.0, "D2"));
        assertEquals(2, service.tamanho());
        service.remover("B-1");
        assertEquals(1, service.tamanho());
    }

    @Test
    @DisplayName("Adversarial: Deve rejeitar adicionar bolsa nula com fail-fast")
    void deveRejeitarAdicionarBolsaNula() {
        assertThrows(NullPointerException.class, () -> service.adicionar(null));
    }

    @Test
    @DisplayName("Adversarial: Deve retornar Optional vazio ao consultar id nulo ou em branco")
    void deveRetornarVazioAoConsultarIdNuloOuEmBranco() {
        assertTrue(service.consultar(null).isEmpty());
        assertTrue(service.consultar("   ").isEmpty());
    }

    @Test
    @DisplayName("Adversarial: Deve retornar false ao remover id nulo ou em branco")
    void deveRetornarFalseAoRemoverIdNuloOuEmBranco() {
        assertFalse(service.remover(null));
        assertFalse(service.remover("   "));
        assertFalse(service.removerBolsa(null));
        assertFalse(service.removerBolsa("   "));
    }

    @Test
    @DisplayName("H3 / Caracterização: removerBolsa deve remover apenas a ocorrência mais recente em caso de duplicatas")
    void deveRemoverBolsaApenasOcorrenciaMaisRecenteEmDuplicatas() {
        Bolsa antiga = criarBolsa("BOLSA-DUP-RB", "O+", 450.0, "Doador Antigo");
        Bolsa recente = criarBolsa("BOLSA-DUP-RB", "O+", 460.0, "Doador Recente");

        service.adicionar(antiga);
        service.adicionar(recente);
        assertEquals(2, service.tamanho());

        boolean removido = service.removerBolsa("BOLSA-DUP-RB");
        assertTrue(removido);
        assertEquals(1, service.tamanho());

        Optional<Bolsa> restante = service.consultar("BOLSA-DUP-RB");
        assertTrue(restante.isPresent());
        assertEquals("Doador Antigo", restante.get().getDoador());

        assertTrue(service.removerBolsa("BOLSA-DUP-RB"));
        assertEquals(0, service.tamanho());
        assertFalse(service.removerBolsa("BOLSA-DUP-RB"));
    }
}

package com.rotavital.service;

import com.rotavital.domain.Bolsa;
import com.rotavital.storage.EstoqueStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Testes Unitários - EstoqueService com EstoqueStorage (T1.3)")
class EstoqueServiceTest {

    private EstoqueStorage storage;
    private EstoqueService service;

    @BeforeEach
    void setUp() {
        storage = mock(EstoqueStorage.class);
        service = new EstoqueService(storage);
    }

    private Bolsa criarBolsa(String id) {
        return new Bolsa(id, "O+", 450.0, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 1), "Doador");
    }

    @Test
    @DisplayName("T1.3: Deve delegar adicionar para storage.salvar()")
    void deveSalvarBolsaDelegandoParaStorage() {
        Bolsa bolsa = criarBolsa("BOLSA-001");
        service.adicionar(bolsa);

        verify(storage, times(1)).salvar(bolsa);
    }

    @Test
    @DisplayName("T1.3: Deve delegar consultar para storage.buscarPorId()")
    void deveConsultarBolsaDelegandoParaStorage() {
        Bolsa bolsa = criarBolsa("BOLSA-001");
        when(storage.buscarPorId("BOLSA-001")).thenReturn(Optional.of(bolsa));

        Optional<Bolsa> resultado = service.consultar("BOLSA-001");

        assertTrue(resultado.isPresent());
        assertEquals("BOLSA-001", resultado.get().getId());
        verify(storage, times(1)).buscarPorId("BOLSA-001");
    }

    @Test
    @DisplayName("T1.3: Deve delegar tamanho para storage.tamanho()")
    void deveDelegarTamanhoParaStorage() {
        when(storage.tamanho()).thenReturn(42);

        int tamanho = service.tamanho();

        assertEquals(42, tamanho);
        verify(storage, times(1)).tamanho();
    }

    @Test
    @DisplayName("T1.3: Deve delegar remover para storage.removerPorId()")
    void deveDelegarRemoverParaStorage() {
        when(storage.removerPorId("BOLSA-001")).thenReturn(true);

        boolean removido = service.remover("BOLSA-001");

        assertTrue(removido);
        verify(storage, times(1)).removerPorId("BOLSA-001");
    }

    @Test
    @DisplayName("T1.3: Deve delegar listarTodas para storage.listarTodas()")
    void deveDelegarListarTodasParaStorage() {
        Bolsa[] bolsas = new Bolsa[]{criarBolsa("B-1"), criarBolsa("B-2")};
        when(storage.listarTodas()).thenReturn(bolsas);

        Bolsa[] resultado = service.listarTodas();

        assertSame(bolsas, resultado);
        verify(storage, times(1)).listarTodas();
    }

    @Test
    @DisplayName("T1.3: Deve propagar exceção de persistência imediatamente sem fallback para memória (fail-fast)")
    void devePropagarExcecaoSemFallbackQuandoStorageFalhar() {
        Bolsa bolsa = criarBolsa("BOLSA-001");
        doThrow(new DataAccessException("Falha de I/O de banco de dados") {})
                .when(storage).salvar(bolsa);

        assertThrows(DataAccessException.class, () -> service.adicionar(bolsa));
    }

    @Test
    @DisplayName("Adversarial: Deve rejeitar adicionar bolsa nula com fail-fast")
    void deveRejeitarAdicionarBolsaNula() {
        assertThrows(NullPointerException.class, () -> service.adicionar(null));
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("Adversarial: Deve retornar Optional vazio para id nulo ou em branco sem delegar ao storage")
    void deveRetornarVazioAoConsultarIdNuloOuEmBrancoSemDelegar() {
        assertTrue(service.consultar(null).isEmpty());
        assertTrue(service.consultar("   ").isEmpty());
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("Adversarial: Deve retornar false para remoção com id nulo ou em branco sem delegar ao storage")
    void deveRetornarFalseAoRemoverIdNuloOuEmBrancoSemDelegar() {
        assertFalse(service.remover(null));
        assertFalse(service.remover("   "));
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("H2 / T2.3: Deve delegar listarEstoque para storage.listar()")
    void deveDelegarListarEstoqueParaStorageListar() {
        List<Bolsa> bolsas = List.of(criarBolsa("B-1"), criarBolsa("B-2"));
        when(storage.listar()).thenReturn(bolsas);

        List<Bolsa> resultado = service.listarEstoque();

        assertSame(bolsas, resultado);
        verify(storage, times(1)).listar();
    }

    @Test
    @DisplayName("H2 / T2.3: Deve propagar exceção de persistência em listarEstoque sem fallback")
    void devePropagarExcecaoQuandoListarEstoqueFalhar() {
        when(storage.listar()).thenThrow(new DataAccessException("Falha de I/O na consulta") {});

        assertThrows(DataAccessException.class, () -> service.listarEstoque());
        verify(storage, times(1)).listar();
    }
}

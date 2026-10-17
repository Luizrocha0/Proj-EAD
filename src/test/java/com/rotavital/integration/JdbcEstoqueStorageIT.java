package com.rotavital.integration;

import com.rotavital.domain.Bolsa;
import com.rotavital.storage.JdbcEstoqueStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Testes de Integração - JdbcEstoqueStorage contra PostgreSQL Real")
class JdbcEstoqueStorageIT extends AbstractPostgresIT {

    @Autowired
    private JdbcEstoqueStorage storage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM bolsas");
    }

    private Bolsa criarBolsa(String id, String tipo, double volume, LocalDate dataColeta, LocalDate validade, String doador) {
        return new Bolsa(id, tipo, volume, dataColeta, validade, doador);
    }

    @Test
    @DisplayName("H1 / T1.2: Deve inserir e recuperar bolsa comprovando todos os campos no PostgreSQL")
    void deveInserirERecuperarBolsaComTodosOsCampos() {
        Bolsa bolsa = criarBolsa(
                "BOLSA-001",
                "O+",
                450.0,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 11, 1),
                "João Silva"
        );

        storage.salvar(bolsa);

        Optional<Bolsa> recuperada = storage.buscarPorId("BOLSA-001");
        assertTrue(recuperada.isPresent(), "A bolsa deve ser recuperada do PostgreSQL");

        Bolsa b = recuperada.get();
        assertEquals("BOLSA-001", b.getId());
        assertEquals("O+", b.getTipoSanguineo());
        assertEquals(450.0, b.getVolumeMl(), 0.001);
        assertEquals(LocalDate.of(2026, 9, 1), b.getDataColeta());
        assertEquals(LocalDate.of(2026, 11, 1), b.getValidade());
        assertEquals("João Silva", b.getDoador());
    }

    @Test
    @DisplayName("H1 / T1.2: Deve retornar a ocorrência mais recente sob IDs duplicados (LIFO via id_seq DESC)")
    void deveRecuperarOcorrenciaMaisRecenteSobIdsDuplicados() {
        Bolsa bolsa1 = criarBolsa(
                "BOLSA-DUP",
                "O+",
                400.0,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 11, 1),
                "Doador Antigo"
        );
        Bolsa bolsa2 = criarBolsa(
                "BOLSA-DUP",
                "O+",
                460.0,
                LocalDate.of(2026, 9, 2),
                LocalDate.of(2026, 11, 2),
                "Doador Recente"
        );

        storage.salvar(bolsa1);
        storage.salvar(bolsa2);

        assertEquals(2, storage.tamanho(), "Banco deve conter ambos os registros persistidos");

        Optional<Bolsa> resultado = storage.buscarPorId("BOLSA-DUP");
        assertTrue(resultado.isPresent());
        assertEquals("Doador Recente", resultado.get().getDoador());
        assertEquals(460.0, resultado.get().getVolumeMl(), 0.001);
        assertEquals(LocalDate.of(2026, 9, 2), resultado.get().getDataColeta());
    }

    @Test
    @DisplayName("H1 / T1.2: Deve retornar Optional vazio para ID inexistente no banco")
    void deveRetornarVazioParaIdInexistente() {
        Optional<Bolsa> resultado = storage.buscarPorId("BOLSA-INEXISTENTE");
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Adversarial: Deve retornar Optional vazio para ID nulo ou em branco sem erro SQL")
    void deveRetornarVazioParaIdNuloOuEmBranco() {
        assertTrue(storage.buscarPorId(null).isEmpty());
        assertTrue(storage.buscarPorId("   ").isEmpty());
    }

    @Test
    @DisplayName("Adversarial: Deve rejeitar salvar bolsa nula no PostgreSQL com fail-fast")
    void deveRejeitarSalvarBolsaNula() {
        assertThrows(NullPointerException.class, () -> storage.salvar(null));
    }
}

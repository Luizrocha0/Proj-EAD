package com.rotavital.storage;

import com.rotavital.domain.Bolsa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Testes Unitários - JdbcEstoqueStorage (Perfil infra)")
class JdbcEstoqueStorageTest {

    private JdbcTemplate jdbcTemplate;
    private JdbcEstoqueStorage storage;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        storage = new JdbcEstoqueStorage(jdbcTemplate);
    }

    private Bolsa criarBolsa(String id, String tipo, double volume, String doador) {
        return new Bolsa(id, tipo, volume, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 1), doador);
    }

    @Test
    @DisplayName("T1.2: Deve salvar bolsa executando INSERT com todos os campos via JdbcTemplate")
    void deveSalvarBolsaExecutandoInsert() {
        Bolsa bolsa = criarBolsa("BOLSA-001", "O+", 450.0, "João Silva");

        storage.salvar(bolsa);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(
                sqlCaptor.capture(),
                eq("BOLSA-001"),
                eq("O+"),
                eq(450.0),
                eq(Date.valueOf(LocalDate.of(2026, 9, 1))),
                eq(Date.valueOf(LocalDate.of(2026, 11, 1))),
                eq("João Silva")
        );

        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("INSERT INTO bolsas"));
        assertTrue(sql.contains("id, tipo_sanguineo, volume_ml, data_coleta, validade, doador"));
    }

    @Test
    @DisplayName("T1.2: Deve buscar bolsa por ID com ordenação LIFO (id_seq DESC LIMIT 1)")
    void deveBuscarPorIdComOrdenacaoLIFO() {
        Bolsa bolsaEsperada = criarBolsa("BOLSA-001", "O+", 450.0, "João Silva");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("BOLSA-001")))
                .thenReturn(List.of(bolsaEsperada));

        Optional<Bolsa> resultado = storage.buscarPorId("BOLSA-001");

        assertTrue(resultado.isPresent());
        assertEquals("BOLSA-001", resultado.get().getId());
        assertEquals(450.0, resultado.get().getVolumeMl());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), eq("BOLSA-001"));
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("FROM bolsas"));
        assertTrue(sql.contains("ORDER BY id_seq DESC"));
        assertTrue(sql.contains("LIMIT 1"));
    }

    @Test
    @DisplayName("T1.2: Deve retornar Optional vazio quando bolsa não for encontrada")
    void deveRetornarVazioQuandoNaoEncontrada() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("BOLSA-INEXISTENTE")))
                .thenReturn(Collections.emptyList());

        Optional<Bolsa> resultado = storage.buscarPorId("BOLSA-INEXISTENTE");
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("T1.2: Deve consultar tamanho total via COUNT(*)")
    void deveConsultarTamanhoTotal() {
        when(jdbcTemplate.queryForObject(contains("COUNT(*)"), eq(Integer.class)))
                .thenReturn(5);

        int tamanho = storage.tamanho();
        assertEquals(5, tamanho);
    }

    @Test
    @DisplayName("T1.2: Deve falhar imediatamente sem fallback para memória se JdbcTemplate lançar exceção")
    void deveFalharImediatamenteSemFallbackQuandoBancoFalhar() {
        Bolsa bolsa = criarBolsa("BOLSA-001", "O+", 450.0, "João Silva");
        doThrow(new DataAccessException("Simulando falha de conectividade PostgreSQL") {})
                .when(jdbcTemplate).update(anyString(), any(), any(), any(), any(), any(), any());

        assertThrows(DataAccessException.class, () -> storage.salvar(bolsa));
    }
}

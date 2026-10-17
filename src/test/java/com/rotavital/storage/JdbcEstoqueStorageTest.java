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

    @Test
    @DisplayName("Adversarial: Deve rejeitar salvar bolsa nula com fail-fast")
    void deveRejeitarSalvarBolsaNula() {
        assertThrows(NullPointerException.class, () -> storage.salvar(null));
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("Adversarial: Deve retornar Optional vazio para busca com id nulo ou em branco sem consultar JDBC")
    void deveRetornarVazioAoConsultarIdNuloOuEmBrancoSemChamarJdbc() {
        assertTrue(storage.buscarPorId(null).isEmpty());
        assertTrue(storage.buscarPorId("   ").isEmpty());
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("Adversarial: Deve rejeitar instanciação com JdbcTemplate nulo")
    void deveRejeitarInstanciacaoComJdbcTemplateNulo() {
        assertThrows(NullPointerException.class, () -> new JdbcEstoqueStorage(null));
    }

    @Test
    @DisplayName("H2 / T2.2: Deve listar bolsas executando SELECT com ORDER BY id_seq DESC via JdbcTemplate")
    void deveListarBolsasExecutandoSelectComOrderBySeqDesc() {
        Bolsa b1 = criarBolsa("BOLSA-001", "O+", 450.0, "João Silva");
        Bolsa b2 = criarBolsa("BOLSA-002", "A-", 400.0, "Maria Souza");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(b2, b1));

        List<Bolsa> resultado = storage.listar();

        assertEquals(2, resultado.size());
        assertEquals("BOLSA-002", resultado.get(0).getId());
        assertEquals("BOLSA-001", resultado.get(1).getId());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class));
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM bolsas"));
        assertTrue(sql.contains("ORDER BY id_seq DESC"), "Consulta SQL deve conter ORDER BY id_seq DESC para garantir LIFO");
    }

    @Test
    @DisplayName("H2 / T2.2: Deve retornar lista vazia sem erros quando a tabela estiver vazia")
    void deveRetornarListaVaziaQuandoTabelaEstiverVazia() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(Collections.emptyList());

        List<Bolsa> resultado = storage.listar();

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
        assertEquals(0, resultado.size());
    }

    @Test
    @DisplayName("H2 / T2.2: Deve preservar todas as ocorrências de IDs repetidos em suas posições temporais")
    void devePreservarTodasAsOcorrenciasDeIdsDuplicadosEmOrdemTemporal() {
        Bolsa b1 = criarBolsa("BOLSA-DUP", "O+", 400.0, "Doador Antigo");
        Bolsa b2 = criarBolsa("BOLSA-OTHER", "B+", 420.0, "Doador Intermediario");
        Bolsa b3 = criarBolsa("BOLSA-DUP", "O+", 450.0, "Doador Recente");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(b3, b2, b1));

        List<Bolsa> resultado = storage.listar();

        assertEquals(3, resultado.size());
        assertEquals("Doador Recente", resultado.get(0).getDoador());
        assertEquals("Doador Intermediario", resultado.get(1).getDoador());
        assertEquals("Doador Antigo", resultado.get(2).getDoador());
    }

    @Test
    @DisplayName("H2 / T2.2: Deve falhar imediatamente sem fallback para memória se SELECT falhar no banco")
    void deveFalharImediatamenteSemFallbackQuandoSelectFalhar() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenThrow(new DataAccessException("Simulando falha de I/O no PostgreSQL") {});

        assertThrows(DataAccessException.class, () -> storage.listar());
    }

    @Test
    @DisplayName("H2 / T2.2: listarTodas() deve retornar array equivalente ao listar()")
    void deveRetornarArrayEquivalenteEmListarTodas() {
        Bolsa b1 = criarBolsa("BOLSA-001", "O+", 450.0, "João Silva");
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(b1));

        Bolsa[] array = storage.listarTodas();

        assertNotNull(array);
        assertEquals(1, array.length);
        assertEquals("BOLSA-001", array[0].getId());
    }

    @Test
    @DisplayName("Segregação H3: Deve lançar UnsupportedOperationException ao chamar removerPorId pois H3 está pendente")
    void deveLancarUnsupportedOperationExceptionAoRemoverPorIdPoisH3EstaPendente() {
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> storage.removerPorId("BOLSA-001")
        );
        assertTrue(ex.getMessage().contains("Pendente para Etapa 5 (H3)"));
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("Adversarial: Deve preservar encoding UTF-8 com acentuação e caracteres especiais em doador e ID")
    void devePreservarEncodingUtf8ECaracteresEspeciais() {
        String idComSimbolos = "BOLSA_001-A/2026";
        String doadorAcentuado = "José da Conceição Araújo & Filhos";
        Bolsa bolsa = criarBolsa(idComSimbolos, "AB+", 450.0, doadorAcentuado);

        storage.salvar(bolsa);

        verify(jdbcTemplate).update(
                anyString(),
                eq(idComSimbolos),
                eq("AB+"),
                eq(450.0),
                any(Date.class),
                any(Date.class),
                eq(doadorAcentuado)
        );

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(idComSimbolos)))
                .thenReturn(List.of(bolsa));

        Optional<Bolsa> resultado = storage.buscarPorId(idComSimbolos);
        assertTrue(resultado.isPresent());
        assertEquals(idComSimbolos, resultado.get().getId());
        assertEquals(doadorAcentuado, resultado.get().getDoador());
    }

    @Test
    @DisplayName("Adversarial: Deve ser resiliente a tentativas de SQL Injection em buscarPorId e salvar")
    void deveSerResilienteAInjecaoSql() {
        String idSqlInjection = "' OR '1'='1";
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(idSqlInjection)))
                .thenReturn(Collections.emptyList());

        Optional<Bolsa> resultado = storage.buscarPorId(idSqlInjection);
        assertTrue(resultado.isEmpty());
        verify(jdbcTemplate).query(contains("WHERE id = ?"), any(RowMapper.class), eq(idSqlInjection));

        String idMalicioso = "'; DROP TABLE bolsas; --";
        String doadorMalicioso = "Robert'); DROP TABLE bolsas;--";
        Bolsa bolsaMaliciosa = criarBolsa(idMalicioso, "O+", 450.0, doadorMalicioso);

        storage.salvar(bolsaMaliciosa);
        verify(jdbcTemplate).update(
                contains("INSERT INTO bolsas"),
                eq(idMalicioso),
                eq("O+"),
                eq(450.0),
                any(Date.class),
                any(Date.class),
                eq(doadorMalicioso)
        );
    }

    @Test
    @DisplayName("Adversarial: Deve retornar Optional vazio para ID com mais de 50 caracteres sem consultar JDBC")
    void deveRetornarVazioParaIdExcedendo50CaracteresSemChamarJdbc() {
        String idGigante = "A".repeat(51);
        Optional<Bolsa> resultado = storage.buscarPorId(idGigante);
        assertTrue(resultado.isEmpty());
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("Adversarial: Deve garantir consulta LIFO atômica com ORDER BY id_seq DESC LIMIT 1 mesmo para chamadas repetidas")
    void deveGarantirConsultaLifoComClausulaSqlExplicita() {
        String idChave = "BOLSA-REPETIDA";
        Bolsa bolsaMaisRecente = criarBolsa(idChave, "AB+", 480.0, "Doador Recente");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(idChave)))
                .thenReturn(List.of(bolsaMaisRecente));

        Optional<Bolsa> r1 = storage.buscarPorId(idChave);
        Optional<Bolsa> r2 = storage.buscarPorId(idChave);

        assertTrue(r1.isPresent());
        assertTrue(r2.isPresent());
        assertEquals("Doador Recente", r1.get().getDoador());
        assertEquals("Doador Recente", r2.get().getDoador());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(2)).query(sqlCaptor.capture(), any(RowMapper.class), eq(idChave));
        for (String sql : sqlCaptor.getAllValues()) {
            assertTrue(sql.contains("ORDER BY id_seq DESC"), "A query deve conter ORDER BY id_seq DESC");
            assertTrue(sql.contains("LIMIT 1"), "A query deve conter LIMIT 1");
        }
    }
}

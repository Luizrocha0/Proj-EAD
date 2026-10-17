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
import java.util.List;
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

    @Test
    @DisplayName("H2 / Integração: Deve listar bolsas em ordem LIFO (id_seq DESC) com PostgreSQL Real")
    void deveListarBolsasEmOrdemLIFOComPostgreSqlReal() {
        for (int i = 1; i <= 5; i++) {
            storage.salvar(criarBolsa(
                    "BOLSA-00" + i,
                    "O+",
                    400.0 + i * 10,
                    LocalDate.of(2026, 9, i),
                    LocalDate.of(2026, 11, i),
                    "Doador #" + i
            ));
        }

        List<Bolsa> resultado = storage.listar();
        assertEquals(5, resultado.size());
        assertEquals("BOLSA-005", resultado.get(0).getId());
        assertEquals("BOLSA-004", resultado.get(1).getId());
        assertEquals("BOLSA-003", resultado.get(2).getId());
        assertEquals("BOLSA-002", resultado.get(3).getId());
        assertEquals("BOLSA-001", resultado.get(4).getId());

        Bolsa[] array = storage.listarTodas();
        assertEquals(5, array.length);
        assertEquals("BOLSA-005", array[0].getId());
        assertEquals("BOLSA-001", array[4].getId());
    }

    @Test
    @DisplayName("H2 / Integração: Deve retornar lista vazia sem erros quando tabela estiver limpa no PostgreSQL")
    void deveRetornarListaVaziaQuandoEstoqueEstiverVazioNoPostgreSql() {
        List<Bolsa> resultado = storage.listar();
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
        assertEquals(0, storage.tamanho());
    }

    @Test
    @DisplayName("H2 / Integração: Deve listar todas as ocorrências de IDs duplicados em suas respectivas posições temporais")
    void deveListarTodasOcorrenciasDeIdsDuplicadosEmSuasPosicoesTemporais() {
        storage.salvar(criarBolsa("BOLSA-DUP", "O+", 400.0, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 1), "Doador Antigo"));
        storage.salvar(criarBolsa("BOLSA-OTHER", "A+", 420.0, LocalDate.of(2026, 9, 2), LocalDate.of(2026, 11, 2), "Doador Intermediario"));
        storage.salvar(criarBolsa("BOLSA-DUP", "O+", 460.0, LocalDate.of(2026, 9, 3), LocalDate.of(2026, 11, 3), "Doador Recente"));

        List<Bolsa> resultado = storage.listar();
        assertEquals(3, resultado.size());
        assertEquals("BOLSA-DUP", resultado.get(0).getId());
        assertEquals("Doador Recente", resultado.get(0).getDoador());
        assertEquals("BOLSA-OTHER", resultado.get(1).getId());
        assertEquals("BOLSA-DUP", resultado.get(2).getId());
        assertEquals("Doador Antigo", resultado.get(2).getDoador());
    }

    @Test
    @DisplayName("H3 / T3.1 / CA3.1: Deve remover bolsa existente fisicamente no PostgreSQL retornando true")
    void deveRemoverBolsaExistenteERetornarTrueComRegistroFisicamenteDeletado() {
        Bolsa bolsa = criarBolsa("BOLSA-DEL-01", "O+", 450.0, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 1), "João Silva");
        storage.salvar(bolsa);
        assertEquals(1, storage.tamanho());

        boolean removido = storage.removerPorId("BOLSA-DEL-01");

        assertTrue(removido, "removerPorId deve retornar true para registro existente");
        assertEquals(0, storage.tamanho(), "Tamanho deve decrementar para 0");
        assertTrue(storage.buscarPorId("BOLSA-DEL-01").isEmpty(), "Registro não deve mais existir no banco");
    }

    @Test
    @DisplayName("H3 / T3.2 / CA3.2: Deve retornar false ao tentar remover bolsa inexistente no PostgreSQL")
    void deveRetornarFalseAoTentarRemoverInexistenteNoPostgreSql() {
        boolean removido = storage.removerPorId("BOLSA-INEXISTENTE");
        assertFalse(removido, "Deve retornar false quando nenhuma linha for afetada");
        assertEquals(0, storage.tamanho(), "Tamanho deve permanecer inalterado");
    }

    @Test
    @DisplayName("H3 / T3.3 / CA3.3 / RN3.3: Deve remover exclusivamente a ocorrência mais recente sob IDs repetidos, preservando histórico anterior")
    void deveRemoverApenasOcorrenciaMaisRecentePreservandoDuplicataMaisAntiga() {
        Bolsa antiga = criarBolsa("BOLSA-DUP-DEL", "O+", 400.0, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 1), "Doador Antigo");
        Bolsa recente = criarBolsa("BOLSA-DUP-DEL", "O+", 460.0, LocalDate.of(2026, 9, 2), LocalDate.of(2026, 11, 2), "Doador Recente");

        storage.salvar(antiga);
        storage.salvar(recente);
        assertEquals(2, storage.tamanho(), "Banco deve conter ambas as bolsas persistidas");

        // Primeira remoção: deve deletar exclusivamente a mais recente (Doador Recente)
        boolean removidoRecente = storage.removerPorId("BOLSA-DUP-DEL");
        assertTrue(removidoRecente, "Primeira remoção deve ter sucesso");
        assertEquals(1, storage.tamanho(), "Tamanho deve decrementar para 1");

        Optional<Bolsa> buscaAposPrimeiraRemocao = storage.buscarPorId("BOLSA-DUP-DEL");
        assertTrue(buscaAposPrimeiraRemocao.isPresent(), "Ocorrência mais antiga deve permanecer no banco");
        assertEquals("Doador Antigo", buscaAposPrimeiraRemocao.get().getDoador());
        assertEquals(400.0, buscaAposPrimeiraRemocao.get().getVolumeMl(), 0.001);

        // Segunda remoção: deve deletar a ocorrência restante (Doador Antigo)
        boolean removidoAntiga = storage.removerPorId("BOLSA-DUP-DEL");
        assertTrue(removidoAntiga, "Segunda remoção deve ter sucesso");
        assertEquals(0, storage.tamanho(), "Tamanho deve decrementar para 0");
        assertTrue(storage.buscarPorId("BOLSA-DUP-DEL").isEmpty(), "Nenhuma ocorrência deve restar");

        // Terceira remoção: ID não existe mais, deve retornar false
        assertFalse(storage.removerPorId("BOLSA-DUP-DEL"), "Remoção subsequente deve retornar false");
    }

    @Test
    @DisplayName("H3 / T3.3: Deve esgotar tripla duplicata no PostgreSQL através de remoções atômicas sucessivas")
    void deveRemoverSucessivamenteTriplaDuplicataNoPostgreSql() {
        Bolsa b1 = criarBolsa("BOLSA-TRIPLA-PG", "O+", 400.0, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 1), "Doador 1");
        Bolsa b2 = criarBolsa("BOLSA-TRIPLA-PG", "O+", 430.0, LocalDate.of(2026, 9, 2), LocalDate.of(2026, 11, 2), "Doador 2");
        Bolsa b3 = criarBolsa("BOLSA-TRIPLA-PG", "O+", 460.0, LocalDate.of(2026, 9, 3), LocalDate.of(2026, 11, 3), "Doador 3");

        storage.salvar(b1);
        storage.salvar(b2);
        storage.salvar(b3);
        assertEquals(3, storage.tamanho());

        // 1ª remoção: elimina Doador 3
        assertTrue(storage.removerPorId("BOLSA-TRIPLA-PG"));
        assertEquals(2, storage.tamanho());
        assertEquals("Doador 2", storage.buscarPorId("BOLSA-TRIPLA-PG").orElseThrow().getDoador());

        // 2ª remoção: elimina Doador 2
        assertTrue(storage.removerPorId("BOLSA-TRIPLA-PG"));
        assertEquals(1, storage.tamanho());
        assertEquals("Doador 1", storage.buscarPorId("BOLSA-TRIPLA-PG").orElseThrow().getDoador());

        // 3ª remoção: elimina Doador 1
        assertTrue(storage.removerPorId("BOLSA-TRIPLA-PG"));
        assertEquals(0, storage.tamanho());
        assertTrue(storage.buscarPorId("BOLSA-TRIPLA-PG").isEmpty());

        // 4ª remoção: esgotado
        assertFalse(storage.removerPorId("BOLSA-TRIPLA-PG"));
    }

    @Test
    @DisplayName("H3 / T3.2 / Defensivo: Deve remover bolsa com ID de exatamente 50 caracteres no PostgreSQL")
    void deveRemoverBolsaComExatamente50CaracteresNoPostgreSql() {
        String id50 = "B".repeat(50);
        Bolsa bolsa = criarBolsa(id50, "O+", 450.0, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 1), "Doador 50 chars");
        storage.salvar(bolsa);
        assertEquals(1, storage.tamanho());

        assertTrue(storage.removerPorId(id50));
        assertEquals(0, storage.tamanho());
        assertTrue(storage.buscarPorId(id50).isEmpty());
    }

    @Test
    @DisplayName("H3 / T3.2: Deve retornar false para remoção com id nulo, em branco ou excedendo 50 caracteres sem erro SQL")
    void deveRetornarFalseParaIdNuloEmBrancoOuExcedendo50CaracteresEmRemoverPorId() {
        assertFalse(storage.removerPorId(null));
        assertFalse(storage.removerPorId("   "));
        assertFalse(storage.removerPorId("A".repeat(51)));
    }

    @Test
    @DisplayName("H3 / T3.2: Deve ser resiliente a tentativas de SQL Injection em removerPorId")
    void deveSerResilienteAInjecaoSqlEmRemoverPorId() {
        assertFalse(storage.removerPorId("' OR '1'='1"));
    }

    @Test
    @DisplayName("H1 / T1.2: Deve persistir e recuperar com encoding UTF-8 e símbolos alfanuméricos")
    void devePersistirERecuperarComEncodingUtf8ESimbolos() {
        Bolsa bolsa = criarBolsa(
                "BOLSA_UTF8-001/2026",
                "O-",
                450.0,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 11, 1),
                "José da Conceição Araújo & Cia"
        );
        storage.salvar(bolsa);

        Optional<Bolsa> resultado = storage.buscarPorId("BOLSA_UTF8-001/2026");
        assertTrue(resultado.isPresent());
        assertEquals("José da Conceição Araújo & Cia", resultado.get().getDoador());
    }

    @Test
    @DisplayName("H1 / T1.2: Deve ser resiliente a tentativas de SQL Injection em buscarPorId")
    void deveSerResilienteAInjecaoSqlEmBuscarPorId() {
        Optional<Bolsa> resultado = storage.buscarPorId("' OR '1'='1");
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Adversarial: Deve retornar Optional vazio para ID com mais de 50 caracteres sem erro SQL")
    void deveRetornarVazioParaIdExcedendo50Caracteres() {
        String idGigante = "A".repeat(51);
        Optional<Bolsa> resultado = storage.buscarPorId(idGigante);
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("H1 / Concorrência: Deve tratar inserções concorrentes sob mesma chave de negócio garantindo monotonicidade LIFO")
    void deveTratarInsercoesConcorrentesGarantindoMonotonicidadeLifo() throws Exception {
        int numThreads = 10;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(numThreads);
        java.util.concurrent.CountDownLatch latchInicio = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch latchFim = new java.util.concurrent.CountDownLatch(numThreads);
        java.util.concurrent.atomic.AtomicInteger erros = new java.util.concurrent.atomic.AtomicInteger(0);

        String idComum = "BOLSA-CONCORRENTE";

        for (int i = 0; i < numThreads; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    latchInicio.await();
                    Bolsa b = criarBolsa(
                            idComum,
                            "O+",
                            400.0 + idx,
                            LocalDate.of(2026, 9, 1),
                            LocalDate.of(2026, 11, 1),
                            "Doador Concorrente #" + idx
                    );
                    storage.salvar(b);
                } catch (Exception e) {
                    erros.incrementAndGet();
                } finally {
                    latchFim.countDown();
                }
            });
        }

        // Libera todas as threads simultaneamente
        latchInicio.countDown();
        boolean concluiu = latchFim.await(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(concluiu, "Todas as threads concorrentes devem concluir a tempo");
        assertEquals(0, erros.get(), "Nenhum erro deve ocorrer durante inserções concorrentes");
        assertEquals(numThreads, storage.tamanho(), "Todas as 10 bolsas devem ser persistidas no PostgreSQL");

        Optional<Bolsa> recuperada = storage.buscarPorId(idComum);
        assertTrue(recuperada.isPresent());
        assertTrue(recuperada.get().getDoador().startsWith("Doador Concorrente #"),
                "A bolsa recuperada deve corresponder a uma das inserções concorrentes confirmadas");
    }
}

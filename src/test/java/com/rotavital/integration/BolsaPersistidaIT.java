package com.rotavital.integration;

import com.rotavital.domain.Bolsa;
import com.rotavital.storage.EstoqueStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Testes de Integração - Comprovação de Persistência Pós-Reinício de Contexto")
class BolsaPersistidaIT extends AbstractPostgresIT {

    @Autowired
    private EstoqueStorage storage;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM bolsas");
    }

    @Test
    @DirtiesContext
    @DisplayName("H1 / T1.4: Deve persistir bolsa no PostgreSQL e recuperar todos os campos após reinicialização do contexto Spring")
    void devePersistirBolsaAposReinicioContexto() {
        Bolsa bolsaOriginal = new Bolsa(
                "BOLSA-RESTART-001",
                "AB+",
                475.5,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 11, 10),
                "Doador Resiliente"
        );

        // 1. Grava no PostgreSQL real via contexto atual
        storage.salvar(bolsaOriginal);

        // Confirma gravacao no contexto atual
        Optional<Bolsa> antesDoReinicio = storage.buscarPorId("BOLSA-RESTART-001");
        assertTrue(antesDoReinicio.isPresent());

        // 2. Inicializa um novo contexto Spring independente a frio apontando para o mesmo banco
        try (ConfigurableApplicationContext novoContexto = criarNovoContexto()) {
            assertTrue(novoContexto.isActive(), "Novo contexto Spring deve estar ativo");

            // 4. Recupera o storage a partir do novo contexto
            EstoqueStorage novoStorage = novoContexto.getBean(EstoqueStorage.class);

            // 5. Busca a bolsa pelo ID no novo contexto e valida todos os campos
            Optional<Bolsa> recuperada = novoStorage.buscarPorId("BOLSA-RESTART-001");
            assertTrue(recuperada.isPresent(), "A bolsa deve sobreviver ao encerramento e reinicio do processo/contexto");

            Bolsa bolsaAposReinicio = recuperada.get();
            assertEquals("BOLSA-RESTART-001", bolsaAposReinicio.getId());
            assertEquals("AB+", bolsaAposReinicio.getTipoSanguineo());
            assertEquals(475.5, bolsaAposReinicio.getVolumeMl(), 0.001);
            assertEquals(LocalDate.of(2026, 9, 10), bolsaAposReinicio.getDataColeta());
            assertEquals(LocalDate.of(2026, 11, 10), bolsaAposReinicio.getValidade());
            assertEquals("Doador Resiliente", bolsaAposReinicio.getDoador());
        }
    }

    @Test
    @DirtiesContext
    @DisplayName("H2 / T2.5: Deve preservar listagem completa, IDs repetidos e ordem LIFO após reinicialização do contexto Spring")
    void devePreservarListagemEOrdemLIFOAposReinicioContexto() {
        Bolsa b1 = new Bolsa("BOLSA-LIFO-01", "O+", 450.0, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 1), "Doador Inicial");
        Bolsa b2 = new Bolsa("BOLSA-LIFO-02", "A-", 420.0, LocalDate.of(2026, 9, 2), LocalDate.of(2026, 11, 2), "Doador Intermediario");
        Bolsa b3 = new Bolsa("BOLSA-LIFO-01", "O+", 460.0, LocalDate.of(2026, 9, 3), LocalDate.of(2026, 11, 3), "Doador Recente (Duplicata)");

        storage.salvar(b1);
        storage.salvar(b2);
        storage.salvar(b3);

        assertEquals(3, storage.tamanho());

        // Inicializa um novo contexto Spring independente a frio apontando para o mesmo banco
        try (ConfigurableApplicationContext novoContexto = criarNovoContexto()) {
            assertTrue(novoContexto.isActive());

            EstoqueStorage novoStorage = novoContexto.getBean(EstoqueStorage.class);
            List<Bolsa> bolsasAposReinicio = novoStorage.listar();

            assertNotNull(bolsasAposReinicio);
            assertEquals(3, bolsasAposReinicio.size());

            // Ordem LIFO estrita preservada
            assertEquals("BOLSA-LIFO-01", bolsasAposReinicio.get(0).getId());
            assertEquals("Doador Recente (Duplicata)", bolsasAposReinicio.get(0).getDoador());

            assertEquals("BOLSA-LIFO-02", bolsasAposReinicio.get(1).getId());
            assertEquals("Doador Intermediario", bolsasAposReinicio.get(1).getDoador());

            assertEquals("BOLSA-LIFO-01", bolsasAposReinicio.get(2).getId());
            assertEquals("Doador Inicial", bolsasAposReinicio.get(2).getDoador());

            assertEquals(3, novoStorage.tamanho());

            Optional<Bolsa> buscaDuplicata = novoStorage.buscarPorId("BOLSA-LIFO-01");
            assertTrue(buscaDuplicata.isPresent(), "Busca pontual por ID duplicado deve funcionar pós-reinício");
            assertEquals("Doador Recente (Duplicata)", buscaDuplicata.get().getDoador(), "Deve recuperar a ocorrência mais recente");

            Bolsa[] arrayAposReinicio = novoStorage.listarTodas();
            assertNotNull(arrayAposReinicio);
            assertEquals(3, arrayAposReinicio.length);
            assertEquals("BOLSA-LIFO-01", arrayAposReinicio[0].getId());
            assertEquals("Doador Recente (Duplicata)", arrayAposReinicio[0].getDoador());
            assertEquals("BOLSA-LIFO-02", arrayAposReinicio[1].getId());
            assertEquals("BOLSA-LIFO-01", arrayAposReinicio[2].getId());
            assertEquals("Doador Inicial", arrayAposReinicio[2].getDoador());
        }
    }

    @Test
    @DirtiesContext
    @DisplayName("H3 / T3.4 / CA3.4 / CA3.5 / RN3.5: Deve manter remoção persistente de duplicata e ausência durável após reinicialização do contexto Spring")
    void deveManterRemocaoAposReinicioContexto() {
        Bolsa b1Antiga = new Bolsa("BOLSA-RESTART-DEL", "O+", 400.0, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 1), "Doador Antigo");
        Bolsa b1Recente = new Bolsa("BOLSA-RESTART-DEL", "O+", 460.0, LocalDate.of(2026, 9, 2), LocalDate.of(2026, 11, 2), "Doador Recente");
        Bolsa b2 = new Bolsa("BOLSA-RESTART-OUTRA", "A+", 450.0, LocalDate.of(2026, 9, 3), LocalDate.of(2026, 11, 3), "Outro Doador");

        storage.salvar(b1Antiga);
        storage.salvar(b1Recente);
        storage.salvar(b2);

        assertEquals(3, storage.tamanho());

        // 1. Remove a ocorrência mais recente da duplicata
        boolean removido = storage.removerPorId("BOLSA-RESTART-DEL");
        assertTrue(removido, "Primeira remoção deve ter sucesso no contexto original");
        assertEquals(2, storage.tamanho());

        // 2. Inicializa um novo contexto Spring independente a frio conectando ao mesmo PostgreSQL
        try (ConfigurableApplicationContext novoContexto = criarNovoContexto()) {
            assertTrue(novoContexto.isActive());

            EstoqueStorage novoStorage = novoContexto.getBean(EstoqueStorage.class);
            assertEquals(2, novoStorage.tamanho(), "Tamanho pós-reinício deve permanecer 2");

            // A ocorrência mais recente foi deletada duravelmente; a mais antiga sobreviveu
            Optional<Bolsa> buscaDuplicata = novoStorage.buscarPorId("BOLSA-RESTART-DEL");
            assertTrue(buscaDuplicata.isPresent(), "Ocorrência mais antiga deve sobreviver ao reinício");
            assertEquals("Doador Antigo", buscaDuplicata.get().getDoador());
            assertEquals(400.0, buscaDuplicata.get().getVolumeMl(), 0.001);

            // Remove a ocorrência restante no segundo contexto
            boolean removidoSegunda = novoStorage.removerPorId("BOLSA-RESTART-DEL");
            assertTrue(removidoSegunda, "Segunda remoção deve ter sucesso no novo contexto");
            assertEquals(1, novoStorage.tamanho());
            assertTrue(novoStorage.buscarPorId("BOLSA-RESTART-DEL").isEmpty());
        }

        // 4. Recria um terceiro contexto Spring para atestar que a exclusão total é definitiva e durável
        try (ConfigurableApplicationContext terceiroContexto = criarNovoContexto()) {
            assertTrue(terceiroContexto.isActive());

            EstoqueStorage terceiroStorage = terceiroContexto.getBean(EstoqueStorage.class);
            assertEquals(1, terceiroStorage.tamanho(), "Apenas a outra bolsa deve restar no terceiro contexto");
            assertTrue(terceiroStorage.buscarPorId("BOLSA-RESTART-DEL").isEmpty(),
                    "Bolsa totalmente removida jamais deve reaparecer pós-reinício");
            assertTrue(terceiroStorage.buscarPorId("BOLSA-RESTART-OUTRA").isPresent(),
                    "Bolsa não removida deve permanecer intacta");
        }
    }

    private ConfigurableApplicationContext criarNovoContexto() {
        return new org.springframework.boot.builder.SpringApplicationBuilder(com.rotavital.RotaVitalApplication.class)
                .profiles("infra")
                .run(
                        "--spring.datasource.url=" + postgres.getJdbcUrl(),
                        "--spring.datasource.username=" + postgres.getUsername(),
                        "--spring.datasource.password=" + postgres.getPassword(),
                        "--spring.flyway.enabled=true",
                        "--spring.flyway.clean-disabled=false"
                );
    }
}

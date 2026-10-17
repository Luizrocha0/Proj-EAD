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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Testes de Integração - Comprovação de Persistência Pós-Reinício de Contexto")
class BolsaPersistidaIT extends AbstractPostgresIT {

    @Autowired
    private EstoqueStorage storage;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

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

        // 2. Simula o encerramento da aplicacao fechando o contexto Spring
        applicationContext.close();
        assertFalse(applicationContext.isActive(), "Contexto Spring original deve ter sido encerrado");

        // 3. Recria o contexto Spring do zero, apontando para o mesmo banco PostgreSQL
        org.springframework.boot.builder.SpringApplicationBuilder builder =
                new org.springframework.boot.builder.SpringApplicationBuilder(com.rotavital.RotaVitalApplication.class)
                        .profiles("infra")
                        .properties(
                                "spring.datasource.url=" + postgres.getJdbcUrl(),
                                "spring.datasource.username=" + postgres.getUsername(),
                                "spring.datasource.password=" + postgres.getPassword(),
                                "spring.flyway.enabled=true"
                        );

        try (ConfigurableApplicationContext novoContexto = builder.run()) {
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
}

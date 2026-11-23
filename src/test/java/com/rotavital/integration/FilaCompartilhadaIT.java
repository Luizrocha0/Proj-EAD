package com.rotavital.integration;

import com.rotavital.RotaVitalApplication;
import com.rotavital.estoque.negocio.dominio.Bolsa;
import com.rotavital.estoque.negocio.aplicacao.EstoqueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FilaCompartilhadaIT extends AbstractPostgresIT {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private EstoqueService estoque;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void dadoPedidoPendenteEmUmaInstancia_quandoDuasAtendem_entaoUmaConfirmaABaixa() throws Exception {
        var hoje = LocalDate.now();
        estoque.adicionar(new Bolsa("FEFO", "O-", 450, hoje.minusDays(4), hoje, "Sintético"));
        estoque.adicionar(new Bolsa("RESERVA", "O-", 450, hoje.minusDays(4), hoje.plusDays(1), "Sintético"));
        enfileirar(mvc, "REQ-1").andExpect(status().isOk());
        enfileirar(mvc, "REQ-2").andExpect(status().isOk());

        try (var contexto = new SpringApplicationBuilder(RotaVitalApplication.class).profiles("infra").run(
                "--server.port=0", "--spring.datasource.url=" + postgres.getJdbcUrl(),
                "--spring.datasource.username=" + postgres.getUsername(),
                "--spring.datasource.password=" + postgres.getPassword())) {
            var outraInstancia = MockMvcBuilders.webAppContextSetup((WebApplicationContext) contexto).build();
            outraInstancia.perform(get("/api/requisicoes/proxima"))
                    .andExpect(jsonPath("$.id").value("REQ-1"));
            enfileirar(outraInstancia, "REQ-1").andExpect(status().isConflict());
            outraInstancia.perform(get("/api/requisicoes"))
                    .andExpect(jsonPath("$.requisicoes[0].id").value("REQ-1"))
                    .andExpect(jsonPath("$.requisicoes[1].id").value("REQ-2"))
                    .andExpect(jsonPath("$.tamanho").value(2));

            var inicio = new CyclicBarrier(2);
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var primeira = executor.submit(() -> atender(mvc, inicio));
                var segunda = executor.submit(() -> atender(outraInstancia, inicio));
                assertThat(new int[]{primeira.get(10, TimeUnit.SECONDS), segunda.get(10, TimeUnit.SECONDS)})
                        .containsExactlyInAnyOrder(200, 409);
            }
        }

        mvc.perform(get("/api/requisicoes/proxima")).andExpect(jsonPath("$.id").value("REQ-2"));
        assertThat(estoque.listarEstoque()).extracting(Bolsa::getId).containsExactly("RESERVA");

        jdbc.execute("""
                CREATE FUNCTION bloquear_consumo_de_teste() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN RETURN NULL; END; $$
                """);
        jdbc.execute("""
                CREATE TRIGGER bloquear_consumo_de_teste BEFORE DELETE ON requisicoes
                FOR EACH ROW EXECUTE FUNCTION bloquear_consumo_de_teste()
                """);
        try {
            solicitarAtendimento(mvc, "REQ-2").andExpect(status().isConflict());
            mvc.perform(get("/api/requisicoes/proxima")).andExpect(jsonPath("$.id").value("REQ-2"));
            assertThat(estoque.listarEstoque()).extracting(Bolsa::getId).containsExactly("RESERVA");
        } finally {
            jdbc.execute("DROP TRIGGER bloquear_consumo_de_teste ON requisicoes");
            jdbc.execute("DROP FUNCTION bloquear_consumo_de_teste()");
        }

        solicitarAtendimento(mvc, "REQ-2").andExpect(status().isOk())
                .andExpect(jsonPath("$.bolsas[0].id").value("RESERVA"));
        assertThat(estoque.listarEstoque()).isEmpty();
    }

    private ResultActions enfileirar(MockMvc instancia, String id) throws Exception {
        return instancia.perform(post("/api/requisicoes").contentType(MediaType.APPLICATION_JSON).content("""
                {"id":"%s","hospitalId":"HOSP","tipoSanguineo":"O-","quantidadeBolsas":1}
                """.formatted(id)));
    }

    private int atender(MockMvc instancia, CyclicBarrier inicio) throws Exception {
        inicio.await(10, TimeUnit.SECONDS);
        return solicitarAtendimento(instancia, "REQ-1").andReturn().getResponse().getStatus();
    }

    private ResultActions solicitarAtendimento(MockMvc instancia, String id) throws Exception {
        return instancia.perform(post("/api/requisicoes/atender").contentType(MediaType.APPLICATION_JSON).content("""
                {"requisicaoId":"%s","origem":{"id":"CENTRO","xKm":0,"yKm":0},
                 "destino":{"id":"HOSP","xKm":3,"yKm":4}}
                """.formatted(id)));
    }
}

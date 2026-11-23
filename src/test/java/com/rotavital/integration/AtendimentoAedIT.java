package com.rotavital.integration;

import com.rotavital.domain.Bolsa;
import com.rotavital.service.EstoqueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AtendimentoAedIT extends AbstractPostgresIT {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private EstoqueService estoque;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void baixaOcorrenciasFefoEmTransacaoEReverteBaixaParcial() throws Exception {
        var hoje = LocalDate.now();
        estoque.adicionar(new Bolsa("DUP", "O-", 400, hoje.minusDays(5), hoje, "Sintético"));
        estoque.adicionar(new Bolsa("BLOQUEADA", "A-", 410, hoje.minusDays(5), hoje.plusDays(1), "Sintético"));
        estoque.adicionar(new Bolsa("DUP", "O-", 420, hoje.minusDays(5), hoje.plusDays(2), "Sintético"));

        mvc.perform(post("/api/requisicoes").contentType(MediaType.APPLICATION_JSON).content("""
                {"id": "REQ-PG", "hospitalId": "HOSP-PG", "tipoSanguineo": "A-", "quantidadeBolsas": 2}
                """)).andExpect(status().isOk());

        String rota = """
                {"requisicaoId": "REQ-PG", "origem": {"id": "CENTRO", "xKm": 0, "yKm": 0},
                 "destino": {"id": "HOSP-PG", "xKm": 3, "yKm": 4}}
                """;

        jdbc.execute("""
                CREATE FUNCTION impedir_baixa_de_teste() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF OLD.id = 'BLOQUEADA' THEN RETURN NULL; END IF;
                    RETURN OLD;
                END;
                $$
                """);
        jdbc.execute("""
                CREATE TRIGGER impedir_baixa_de_teste BEFORE DELETE ON bolsas
                FOR EACH ROW EXECUTE FUNCTION impedir_baixa_de_teste()
                """);
        try {
            mvc.perform(post("/api/requisicoes/atender").contentType(MediaType.APPLICATION_JSON).content(rota))
                    .andExpect(status().isConflict());
            assertEquals(3, estoque.tamanho());
            mvc.perform(get("/api/requisicoes/proxima")).andExpect(jsonPath("$.id").value("REQ-PG"));
        } finally {
            jdbc.execute("DROP TRIGGER impedir_baixa_de_teste ON bolsas");
            jdbc.execute("DROP FUNCTION impedir_baixa_de_teste()");
        }

        mvc.perform(post("/api/requisicoes/atender").contentType(MediaType.APPLICATION_JSON).content(rota))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bolsas[0].volumeMl").value(400.0))
                .andExpect(jsonPath("$.bolsas[1].id").value("BLOQUEADA"))
                .andExpect(jsonPath("$.rota.distanciaKm").value(5.0))
                .andExpect(jsonPath("$.restantes").value(0));
        assertEquals(1, estoque.tamanho());
        assertEquals(420, estoque.consultar("DUP").orElseThrow().getVolumeMl());
    }
}

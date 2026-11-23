package com.rotavital.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("u1")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AtendimentoAedTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void atendeAFilaComEstoqueCompativelFefoRotaEBaixaDaOcorrenciaExata() throws Exception {
        cadastrarBolsa("DUP", "O-", 0);
        cadastrarBolsa("COMPATIVEL", "A-", 2);
        cadastrarBolsa("DUP", "O-", 8);
        cadastrarBolsa("RH-INCOMPATIVEL", "A+", 0);
        cadastrarBolsa("VENCIDA", "O-", -1);
        enfileirar("REQ-1", " a- ", 2).andExpect(status().isOk());
        enfileirar("REQ-2", "A-", 1).andExpect(status().isOk());

        atender("REQ-2", "HOSP").andExpect(status().isConflict());
        atender("REQ-1", "HOSP").andExpect(status().isOk())
                .andExpect(jsonPath("$.requisicao.id").value("REQ-1"))
                .andExpect(jsonPath("$.requisicao.tipoSanguineo").value("A-"))
                .andExpect(jsonPath("$.bolsas.length()").value(2))
                .andExpect(jsonPath("$.bolsas[0].id").value("DUP"))
                .andExpect(jsonPath("$.bolsas[0].validade").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.bolsas[0].doador").doesNotExist())
                .andExpect(jsonPath("$.bolsas[1].id").value("COMPATIVEL"))
                .andExpect(jsonPath("$.rota.pontos[1].id").value("PERTO"))
                .andExpect(jsonPath("$.rota.pontos[2].id").value("LONGE"))
                .andExpect(jsonPath("$.rota.pontos[3].id").value("HOSP"))
                .andExpect(jsonPath("$.rota.distanciaKm").value(closeTo(8, 1e-9)))
                .andExpect(jsonPath("$.restantes").value(1));

        mvc.perform(get("/api/estoque")).andExpect(jsonPath("$.tamanho").value(3));
        mvc.perform(get("/api/estoque/DUP"))
                .andExpect(jsonPath("$.validade").value(LocalDate.now().plusDays(8).toString()));
        atender("REQ-1", "HOSP").andExpect(status().isConflict());
        mvc.perform(get("/api/requisicoes/proxima")).andExpect(jsonPath("$.id").value("REQ-2"));
    }

    @Test
    void mantemPedidoEEstoqueQuandoFaltaBolsaOuDestinoNaoCorresponde() throws Exception {
        cadastrarBolsa("O-VALIDA", "O-", 3);
        cadastrarBolsa("O-INCOMPATIVEL", "O+", 3);
        enfileirar("REQ-1", "O-", 2).andExpect(status().isOk());

        atender("REQ-1", "OUTRO").andExpect(status().isBadRequest());
        atender("REQ-1", "HOSP").andExpect(status().isConflict());
        mvc.perform(get("/api/estoque")).andExpect(jsonPath("$.tamanho").value(2));
        mvc.perform(get("/api/requisicoes/proxima")).andExpect(jsonPath("$.id").value("REQ-1"));

        cadastrarBolsa("O-NOVA", "O-", 4);
        atender("REQ-1", "HOSP").andExpect(status().isOk())
                .andExpect(jsonPath("$.bolsas.length()").value(2))
                .andExpect(jsonPath("$.restantes").value(0));
        atender("REQ-1", "HOSP").andExpect(status().isOk())
                .andExpect(jsonPath("$.bolsas").doesNotExist());
    }

    @Test
    void rejeitaEntradasInvalidasAntesDeAlterarEstado() throws Exception {
        enfileirar("REQ-1", "Z+", 1).andExpect(status().isBadRequest());
        enfileirar("REQ-1", "O-", 0).andExpect(status().isBadRequest());
        enfileirar("REQ-1", "O-", 1).andExpect(status().isOk());
        enfileirar("REQ-1", "O-", 1).andExpect(status().isConflict());
        mvc.perform(post("/api/requisicoes/atender").contentType(MediaType.APPLICATION_JSON).content("""
                {"requisicaoId": "REQ-1", "origem": {"id": "CENTRO", "xKm": 0},
                 "destino": {"id": "HOSP", "xKm": 8, "yKm": 0}}
                """)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/requisicoes/atender")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/requisicoes")).andExpect(jsonPath("$.tamanho").value(1));
    }

    private void cadastrarBolsa(String id, String tipo, int diasAteValidade) throws Exception {
        var hoje = LocalDate.now();
        mvc.perform(post("/api/estoque").contentType(MediaType.APPLICATION_JSON).content("""
                {"id": "%s", "tipoSanguineo": "%s", "dataColeta": "%s",
                 "validade": "%s", "doador": "Sintético"}
                """.formatted(id, tipo, hoje.minusDays(14), hoje.plusDays(diasAteValidade))))
                .andExpect(status().isOk());
    }

    private ResultActions enfileirar(String id, String tipo, int quantidade) throws Exception {
        return mvc.perform(post("/api/requisicoes").contentType(MediaType.APPLICATION_JSON).content("""
                {"id": "%s", "hospitalId": "HOSP", "tipoSanguineo": "%s", "quantidadeBolsas": %d}
                """.formatted(id, tipo, quantidade)));
    }

    private ResultActions atender(String requisicaoId, String hospitalId) throws Exception {
        return mvc.perform(post("/api/requisicoes/atender").contentType(MediaType.APPLICATION_JSON).content("""
                {"requisicaoId": "%s",
                 "origem": {"id": "CENTRO", "xKm": 0, "yKm": 0},
                 "paradas": [{"id": "LONGE", "xKm": 5, "yKm": 0},
                             {"id": "PERTO", "xKm": 2, "yKm": 0}],
                 "destino": {"id": "%s", "xKm": 8, "yKm": 0}}
                """.formatted(requisicaoId, hospitalId)));
    }
}

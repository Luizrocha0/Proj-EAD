package com.rotavital.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("u1")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("Testes de Caracterização da U1 - EstoqueController sob perfil u1")
class EstoqueControllerCaracterizacaoTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String JSON_BOLSA_1 = """
        {
            "id": "BOLSA-001",
            "tipoSanguineo": "O+",
            "volumeMl": "450.0",
            "dataColeta": "2026-09-01",
            "validade": "2026-11-01",
            "doador": "João Silva"
        }
        """;

    private static final String JSON_BOLSA_2 = """
        {
            "id": "BOLSA-002",
            "tipoSanguineo": "A-",
            "volumeMl": "400.0",
            "dataColeta": "2026-09-02",
            "validade": "2026-11-02",
            "doador": "Maria Souza"
        }
        """;

    @Test
    @DisplayName("POST /api/estoque deve cadastrar bolsa e retornar 200 com tamanho 1")
    void deveAdicionarBolsaERetornar200ComTamanho() throws Exception {
        mockMvc.perform(post("/api/estoque")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_BOLSA_1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem", is("Bolsa adicionada ao estoque")))
                .andExpect(jsonPath("$.tamanho", is(1)));
    }

    @Test
    @DisplayName("GET /api/estoque/{id} deve retornar 200 e dados completos da bolsa existente")
    void deveConsultarBolsaExistenteERetornar200() throws Exception {
        mockMvc.perform(post("/api/estoque")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_BOLSA_1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/estoque/BOLSA-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("BOLSA-001")))
                .andExpect(jsonPath("$.tipoSanguineo", is("O+")))
                .andExpect(jsonPath("$.volumeMl", is(450.0)))
                .andExpect(jsonPath("$.dataColeta", is("2026-09-01")))
                .andExpect(jsonPath("$.validade", is("2026-11-01")))
                .andExpect(jsonPath("$.doador", is("João Silva")));
    }

    @Test
    @DisplayName("GET /api/estoque/{id} deve retornar 404 para bolsa inexistente")
    void deveRetornar404ParaBolsaInexistente() throws Exception {
        mockMvc.perform(get("/api/estoque/BOLSA-INEXISTENTE"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/estoque/{id} deve remover bolsa existente e decrementar tamanho")
    void deveRemoverBolsaERetornar200ComTamanho() throws Exception {
        mockMvc.perform(post("/api/estoque")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_BOLSA_1))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/estoque/BOLSA-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem", is("Bolsa removida")))
                .andExpect(jsonPath("$.tamanho", is(0)));

        mockMvc.perform(get("/api/estoque/BOLSA-001"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/estoque/{id} deve retornar 404 ao tentar remover bolsa inexistente")
    void deveRetornar404AoRemoverInexistente() throws Exception {
        mockMvc.perform(delete("/api/estoque/BOLSA-INEXISTENTE"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/estoque deve retornar envelope com bolsas em ordem LIFO e tamanho")
    void deveListarBolsasComEnvelopeERespeitarLIFO() throws Exception {
        mockMvc.perform(post("/api/estoque")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_BOLSA_1))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/estoque")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_BOLSA_2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/estoque"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanho", is(2)))
                .andExpect(jsonPath("$.bolsas", hasSize(2)))
                .andExpect(jsonPath("$.bolsas[0].id", is("BOLSA-002")))
                .andExpect(jsonPath("$.bolsas[1].id", is("BOLSA-001")));
    }

    @Test
    @DisplayName("GET /api/estoque deve retornar envelope vazio quando estoque sem bolsas")
    void deveRetornarEnvelopeVazioQuandoSemBolsas() throws Exception {
        mockMvc.perform(get("/api/estoque"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanho", is(0)))
                .andExpect(jsonPath("$.bolsas", hasSize(0)));
    }

    @Test
    @DisplayName("H2 / Caracterização: GET /api/estoque deve listar múltiplas ocorrências de IDs duplicados em ordem LIFO sob perfil u1")
    void deveListarMultiplasOcorrenciasDeIdsDuplicadosNoEnvelopeSobPerfilU1() throws Exception {
        String jsonDup1 = """
            {
                "id": "BOLSA-DUP-U1",
                "tipoSanguineo": "O+",
                "volumeMl": 400.0,
                "dataColeta": "2026-09-01",
                "validade": "2026-11-01",
                "doador": "Doador Antigo"
            }
            """;
        String jsonDup2 = """
            {
                "id": "BOLSA-DUP-U1",
                "tipoSanguineo": "O+",
                "volumeMl": 460.0,
                "dataColeta": "2026-09-02",
                "validade": "2026-11-02",
                "doador": "Doador Recente"
            }
            """;

        mockMvc.perform(post("/api/estoque").contentType(MediaType.APPLICATION_JSON).content(jsonDup1))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/estoque").contentType(MediaType.APPLICATION_JSON).content(jsonDup2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/estoque"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanho", is(2)))
                .andExpect(jsonPath("$.bolsas", hasSize(2)))
                .andExpect(jsonPath("$.bolsas[0].id", is("BOLSA-DUP-U1")))
                .andExpect(jsonPath("$.bolsas[0].doador", is("Doador Recente")))
                .andExpect(jsonPath("$.bolsas[1].id", is("BOLSA-DUP-U1")))
                .andExpect(jsonPath("$.bolsas[1].doador", is("Doador Antigo")));
    }

    @Test
    @DisplayName("Adversarial: POST /api/estoque deve retornar 400 ao receber id ou doador vazio")
    void deveRetornar400AoAdicionarBolsaComCamposVazios() throws Exception {
        String jsonIdVazio = """
            {
                "id": "   ",
                "tipoSanguineo": "O+",
                "volumeMl": 450.0,
                "dataColeta": "2026-09-01",
                "validade": "2026-11-01",
                "doador": "João Silva"
            }
            """;

        mockMvc.perform(post("/api/estoque")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonIdVazio))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem", containsString("Dados inválidos")));
    }

    @Test
    @DisplayName("Adversarial: POST /api/estoque deve retornar 400 ao receber datas inválidas ou inconsistentes")
    void deveRetornar400AoAdicionarBolsaComDatasInvalidas() throws Exception {
        String jsonInvalido = """
            {
                "id": "BOLSA-INV",
                "tipoSanguineo": "O+",
                "volumeMl": 450.0,
                "dataColeta": "2026-99-99",
                "validade": "2026-11-01",
                "doador": "João Silva"
            }
            """;

        mockMvc.perform(post("/api/estoque")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem", containsString("Dados inválidos")));

        String jsonValidadeAnterior = """
            {
                "id": "BOLSA-INV-2",
                "tipoSanguineo": "O+",
                "volumeMl": 450.0,
                "dataColeta": "2026-10-01",
                "validade": "2026-09-01",
                "doador": "João Silva"
            }
            """;

        mockMvc.perform(post("/api/estoque")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonValidadeAnterior))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem", containsString("Dados inválidos")));
    }

    @Test
    @DisplayName("Adversarial: POST /api/estoque deve retornar 400 para volume negativo ou zero")
    void deveRetornar400AoAdicionarBolsaComVolumeInvalido() throws Exception {
        String jsonVolumeNegativo = """
            {
                "id": "BOLSA-VOL",
                "tipoSanguineo": "O+",
                "volumeMl": -10.0,
                "dataColeta": "2026-09-01",
                "validade": "2026-11-01",
                "doador": "João Silva"
            }
            """;

        mockMvc.perform(post("/api/estoque")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonVolumeNegativo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem", containsString("Dados inválidos")));
    }

    @Test
    @DisplayName("Adversarial: POST /api/estoque deve aceitar tipo sanguíneo com espaços/minúsculas e normalizar para canonical")
    void deveAceitarTipoSanguineoComEspacosEMinusculoENormalizar() throws Exception {
        String jsonNormalizar = """
            {
                "id": "BOLSA-NORM",
                "tipoSanguineo": "  ab+  ",
                "volumeMl": 450.0,
                "dataColeta": "2026-09-01",
                "validade": "2026-11-01",
                "doador": "João Silva"
            }
            """;

        mockMvc.perform(post("/api/estoque")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonNormalizar))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/estoque/BOLSA-NORM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoSanguineo", is("AB+")));
    }

    @Test
    @DisplayName("Adversarial: POST e GET /api/estoque devem preservar acentuação UTF-8 em doador e símbolos no ID")
    void devePreservarAcentuacaoUtf8ESimbolosEmIdViaApi() throws Exception {
        String jsonUtf8 = """
            {
                "id": "BOLSA_UTF8-01",
                "tipoSanguineo": "O-",
                "volumeMl": 450.0,
                "dataColeta": "2026-09-01",
                "validade": "2026-11-01",
                "doador": "José da Conceição Araújo & Cia"
            }
            """;

        mockMvc.perform(post("/api/estoque")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonUtf8))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/estoque/BOLSA_UTF8-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("BOLSA_UTF8-01")))
                .andExpect(jsonPath("$.doador", is("José da Conceição Araújo & Cia")));
    }

    @Test
    @DisplayName("Adversarial: POST /api/estoque deve retornar 400 se id exceder 50 caracteres")
    void deveRetornar400SeIdExceder50Caracteres() throws Exception {
        String jsonIdLongo = """
            {
                "id": "%s",
                "tipoSanguineo": "O+",
                "volumeMl": 450.0,
                "dataColeta": "2026-09-01",
                "validade": "2026-11-01",
                "doador": "João Silva"
            }
            """.formatted("B".repeat(51));

        mockMvc.perform(post("/api/estoque")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonIdLongo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem", containsString("id não pode exceder 50 caracteres")));
    }

    @Test
    @DisplayName("Adversarial: POST /api/estoque deve retornar 400 se doador exceder 255 caracteres")
    void deveRetornar400SeDoadorExceder255Caracteres() throws Exception {
        String jsonDoadorLongo = """
            {
                "id": "BOLSA-VALIDA",
                "tipoSanguineo": "O+",
                "volumeMl": 450.0,
                "dataColeta": "2026-09-01",
                "validade": "2026-11-01",
                "doador": "%s"
            }
            """.formatted("D".repeat(256));

        mockMvc.perform(post("/api/estoque")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonDoadorLongo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem", containsString("doador não pode exceder 255 caracteres")));
    }
}

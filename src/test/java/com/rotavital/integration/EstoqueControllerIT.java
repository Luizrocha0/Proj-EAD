package com.rotavital.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Testes de Aceitação da API REST sob perfil infra (PostgreSQL Real)")
class EstoqueControllerIT extends AbstractPostgresIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM bolsas");
    }

    @Test
    @DisplayName("H1 / Aceitação: POST deve cadastrar bolsa com volumeMl literal e GET deve recuperar todos os campos")
    void deveCadastrarBolsaViaPostERecuperarViaGet() throws Exception {
        String jsonPayload = """
            {
                "id": "BOLSA-001",
                "tipoSanguineo": "O+",
                "volumeMl": 450.0,
                "dataColeta": "2026-09-01",
                "validade": "2026-11-01",
                "doador": "João Silva"
            }
            """;

        // 1. Cadastra bolsa via POST /api/estoque
        mockMvc.perform(post("/api/estoque")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem", is("Bolsa adicionada ao estoque")))
                .andExpect(jsonPath("$.tamanho", is(1)));

        // 2. Recupera a bolsa cadastrada via GET /api/estoque/{id}
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
    @DisplayName("H1 / Aceitação: GET em bolsa inexistente sob perfil infra deve retornar 404 Not Found")
    void deveRetornar404ParaBolsaInexistente() throws Exception {
        mockMvc.perform(get("/api/estoque/BOLSA-INEXISTENTE"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("H1 / Aceitação: POST com dados inválidos sob perfil infra deve retornar 400 Bad Request")
    void deveRetornar400ParaPayloadInvalidoSobPerfilInfra() throws Exception {
        String jsonInvalido = """
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
                .content(jsonInvalido))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("H1 / Aceitação: POST deve normalizar tipo sanguíneo com espaços/minúsculas no PostgreSQL")
    void deveCadastrarComTipoSanguineoNormalizadoSobPerfilInfra() throws Exception {
        String jsonNormalizar = """
            {
                "id": "BOLSA-NORM-INFRA",
                "tipoSanguineo": "  b+  ",
                "volumeMl": 450.0,
                "dataColeta": "2026-09-01",
                "validade": "2026-11-01",
                "doador": "Carlos"
            }
            """;

        mockMvc.perform(post("/api/estoque")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonNormalizar))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/estoque/BOLSA-NORM-INFRA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoSanguineo", is("B+")));
    }

    @Test
    @DisplayName("H1 / Aceitação: POST e GET sob perfil infra devem preservar acentuação UTF-8 e símbolos no ID")
    void devePreservarUtf8ESimbolosEmIdSobPerfilInfra() throws Exception {
        String jsonUtf8 = """
            {
                "id": "BOLSA_UTF8-02/2026",
                "tipoSanguineo": "AB+",
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

        mockMvc.perform(get("/api/estoque/BOLSA_UTF8-02/2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("BOLSA_UTF8-02/2026")))
                .andExpect(jsonPath("$.doador", is("José da Conceição Araújo & Cia")));
    }
}

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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @Test
    @DisplayName("H1 / Aceitação: POST com id excedendo 50 caracteres sob perfil infra deve retornar 400 Bad Request prevenindo erro 500 do PostgreSQL")
    void deveRetornar400ParaIdExcedendo50SobPerfilInfra() throws Exception {
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
                .andExpect(jsonPath("$.mensagem", org.hamcrest.Matchers.containsString("id não pode exceder 50 caracteres")));
    }

    @Test
    @DisplayName("H2 / Aceitação: GET /api/estoque sob perfil infra deve retornar envelope com bolsas em ordem LIFO e tamanho")
    void deveListarEstoqueComEnvelopeSobPerfilInfra() throws Exception {
        String json1 = """
            {
                "id": "BOLSA-INFRA-01",
                "tipoSanguineo": "O+",
                "volumeMl": 450.0,
                "dataColeta": "2026-09-01",
                "validade": "2026-11-01",
                "doador": "João Silva"
            }
            """;
        String json2 = """
            {
                "id": "BOLSA-INFRA-02",
                "tipoSanguineo": "A-",
                "volumeMl": 420.0,
                "dataColeta": "2026-09-02",
                "validade": "2026-11-02",
                "doador": "Maria Souza"
            }
            """;

        mockMvc.perform(post("/api/estoque").contentType(MediaType.APPLICATION_JSON).content(json1))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/estoque").contentType(MediaType.APPLICATION_JSON).content(json2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/estoque"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanho", is(2)))
                .andExpect(jsonPath("$.bolsas", hasSize(2)))
                .andExpect(jsonPath("$.bolsas[0].id", is("BOLSA-INFRA-02")))
                .andExpect(jsonPath("$.bolsas[0].doador", is("Maria Souza")))
                .andExpect(jsonPath("$.bolsas[1].id", is("BOLSA-INFRA-01")))
                .andExpect(jsonPath("$.bolsas[1].doador", is("João Silva")));
    }

    @Test
    @DisplayName("H2 / T2.4 / CA2.2 / Aceitação: GET /api/estoque após inserir 5 fixtures sob perfil infra deve retornar tamanho 5 e ordem LIFO reversa")
    void deveListarTodasBolsasComEnvelope() throws Exception {
        String[] fixtures = new String[] {
            """
            {
                "id": "BOLSA-001",
                "tipoSanguineo": "O+",
                "volumeMl": 450.0,
                "dataColeta": "2026-09-01",
                "validade": "2026-11-01",
                "doador": "João Silva"
            }
            """,
            """
            {
                "id": "BOLSA-002",
                "tipoSanguineo": "A-",
                "volumeMl": 450.0,
                "dataColeta": "2026-09-05",
                "validade": "2026-11-05",
                "doador": "Maria Santos"
            }
            """,
            """
            {
                "id": "BOLSA-003",
                "tipoSanguineo": "B+",
                "volumeMl": 400.0,
                "dataColeta": "2026-09-10",
                "validade": "2026-11-10",
                "doador": "Carlos Oliveira"
            }
            """,
            """
            {
                "id": "BOLSA-004",
                "tipoSanguineo": "AB+",
                "volumeMl": 450.0,
                "dataColeta": "2026-08-15",
                "validade": "2026-10-15",
                "doador": "Ana Costa"
            }
            """,
            """
            {
                "id": "BOLSA-005",
                "tipoSanguineo": "O-",
                "volumeMl": 500.0,
                "dataColeta": "2026-09-20",
                "validade": "2026-11-20",
                "doador": "Pedro Almeida"
            }
            """
        };

        for (String f : fixtures) {
            mockMvc.perform(post("/api/estoque").contentType(MediaType.APPLICATION_JSON).content(f))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/estoque"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanho", is(5)))
                .andExpect(jsonPath("$.bolsas", hasSize(5)))
                .andExpect(jsonPath("$.bolsas[0].id", is("BOLSA-005")))
                .andExpect(jsonPath("$.bolsas[0].doador", is("Pedro Almeida")))
                .andExpect(jsonPath("$.bolsas[1].id", is("BOLSA-004")))
                .andExpect(jsonPath("$.bolsas[1].doador", is("Ana Costa")))
                .andExpect(jsonPath("$.bolsas[2].id", is("BOLSA-003")))
                .andExpect(jsonPath("$.bolsas[2].doador", is("Carlos Oliveira")))
                .andExpect(jsonPath("$.bolsas[3].id", is("BOLSA-002")))
                .andExpect(jsonPath("$.bolsas[3].doador", is("Maria Santos")))
                .andExpect(jsonPath("$.bolsas[4].id", is("BOLSA-001")))
                .andExpect(jsonPath("$.bolsas[4].doador", is("João Silva")));
    }

    @Test
    @DisplayName("H2 / Aceitação: GET /api/estoque sob perfil infra deve retornar envelope vazio quando banco limpo")
    void deveRetornarEnvelopeVazioSobPerfilInfraQuandoEstoqueVazio() throws Exception {
        mockMvc.perform(get("/api/estoque"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanho", is(0)))
                .andExpect(jsonPath("$.bolsas", hasSize(0)));
    }

    @Test
    @DisplayName("H2 / Aceitação: GET /api/estoque sob perfil infra deve listar todas as ocorrências de IDs duplicados")
    void deveListarMultiplasOcorrenciasDeIdsDuplicadosComEnvelope() throws Exception {
        String jsonDup1 = """
            {
                "id": "BOLSA-DUP-REST",
                "tipoSanguineo": "O+",
                "volumeMl": 400.0,
                "dataColeta": "2026-09-01",
                "validade": "2026-11-01",
                "doador": "Doador Antigo"
            }
            """;
        String jsonDup2 = """
            {
                "id": "BOLSA-DUP-REST",
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
                .andExpect(jsonPath("$.bolsas[0].id", is("BOLSA-DUP-REST")))
                .andExpect(jsonPath("$.bolsas[0].doador", is("Doador Recente")))
                .andExpect(jsonPath("$.bolsas[1].id", is("BOLSA-DUP-REST")))
                .andExpect(jsonPath("$.bolsas[1].doador", is("Doador Antigo")));
    }

    @Test
    @DisplayName("H2 / T2.3 / Aceitação: GET /api/estoque/{id} com IDs duplicados sob perfil infra deve retornar a ocorrência mais recente")
    void deveConsultarBolsaExistenteRetornandoMaisRecente() throws Exception {
        String jsonDup1 = """
            {
                "id": "BOLSA-DUP-PONTUAL-INFRA",
                "tipoSanguineo": "O+",
                "volumeMl": 400.0,
                "dataColeta": "2026-09-01",
                "validade": "2026-11-01",
                "doador": "Doador Antigo"
            }
            """;
        String jsonDup2 = """
            {
                "id": "BOLSA-DUP-PONTUAL-INFRA",
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

        mockMvc.perform(get("/api/estoque/BOLSA-DUP-PONTUAL-INFRA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("BOLSA-DUP-PONTUAL-INFRA")))
                .andExpect(jsonPath("$.doador", is("Doador Recente")))
                .andExpect(jsonPath("$.volumeMl", is(460.0)));
    }

    @Test
    @DisplayName("H3 / T3.3 / CA3.1 / Aceitação: DELETE /api/estoque/{id} deve remover bolsa e retornar 200 com envelope")
    void deveRemoverBolsaComSucessoViaDelete() throws Exception {
        String jsonPayload = """
            {
                "id": "BOLSA-DEL-REST",
                "tipoSanguineo": "O+",
                "volumeMl": 450.0,
                "dataColeta": "2026-09-01",
                "validade": "2026-11-01",
                "doador": "Carlos Silva"
            }
            """;

        mockMvc.perform(post("/api/estoque").contentType(MediaType.APPLICATION_JSON).content(jsonPayload))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/estoque/BOLSA-DEL-REST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem", is("Bolsa removida")))
                .andExpect(jsonPath("$.tamanho", is(0)));

        mockMvc.perform(get("/api/estoque/BOLSA-DEL-REST"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("H3 / T3.2 / CA3.2 / Aceitação: DELETE /api/estoque/{id} em bolsa inexistente deve retornar 404")
    void deveRetornar404AoRemoverInexistente() throws Exception {
        mockMvc.perform(delete("/api/estoque/BOLSA-INEXISTENTE"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("H3 / T3.3 / CA3.3 / Aceitação: DELETE /api/estoque/{id} com duplicatas deve remover mais recente e manter anterior")
    void deveRemoverMaisRecenteEManterDuplicataAnterior() throws Exception {
        String jsonDup1 = """
            {
                "id": "BOLSA-DUP-DEL-API",
                "tipoSanguineo": "O+",
                "volumeMl": 400.0,
                "dataColeta": "2026-09-01",
                "validade": "2026-11-01",
                "doador": "Doador Antigo"
            }
            """;
        String jsonDup2 = """
            {
                "id": "BOLSA-DUP-DEL-API",
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

        // 1. DELETE remove ocorrência mais recente (Doador Recente)
        mockMvc.perform(delete("/api/estoque/BOLSA-DUP-DEL-API"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem", is("Bolsa removida")))
                .andExpect(jsonPath("$.tamanho", is(1)));

        // 2. GET subsequente deve retornar a ocorrência anterior (Doador Antigo)
        mockMvc.perform(get("/api/estoque/BOLSA-DUP-DEL-API"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("BOLSA-DUP-DEL-API")))
                .andExpect(jsonPath("$.doador", is("Doador Antigo")))
                .andExpect(jsonPath("$.volumeMl", is(400.0)));

        // 3. Segundo DELETE remove a ocorrência restante (Doador Antigo)
        mockMvc.perform(delete("/api/estoque/BOLSA-DUP-DEL-API"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem", is("Bolsa removida")))
                .andExpect(jsonPath("$.tamanho", is(0)));

        // 4. Terceiro DELETE: esgotadas as duplicatas, deve retornar 404
        mockMvc.perform(delete("/api/estoque/BOLSA-DUP-DEL-API"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("H3 / Aceitação: DELETE /api/estoque/{id} com ID excedendo 50 caracteres deve retornar 404")
    void deveRetornar404AoRemoverComIdExcedendo50Caracteres() throws Exception {
        mockMvc.perform(delete("/api/estoque/" + "B".repeat(51)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("H3 / Aceitação: DELETE /api/estoque/{id} deve remover bolsa preservando acentuação UTF-8 sob perfil infra")
    void deveRemoverBolsaComUtf8ESimbolosSobPerfilInfra() throws Exception {
        String jsonUtf8 = """
            {
                "id": "BOLSA_UTF8-DEL",
                "tipoSanguineo": "AB+",
                "volumeMl": 450.0,
                "dataColeta": "2026-09-01",
                "validade": "2026-11-01",
                "doador": "José da Conceição Araújo"
            }
            """;

        mockMvc.perform(post("/api/estoque").contentType(MediaType.APPLICATION_JSON).content(jsonUtf8))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/estoque/BOLSA_UTF8-DEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem", is("Bolsa removida")))
                .andExpect(jsonPath("$.tamanho", is(0)));

        mockMvc.perform(get("/api/estoque/BOLSA_UTF8-DEL"))
                .andExpect(status().isNotFound());
    }
}


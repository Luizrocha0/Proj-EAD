package com.rotavital.controller;

import com.rotavital.domain.Bolsa;
import com.rotavital.dto.BolsaRequestDTO;
import com.rotavital.service.EstoqueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Testes Unitários Puros - EstoqueController (Nível 1 sem contexto Spring)")
class EstoqueControllerTest {

    private EstoqueService estoqueService;
    private EstoqueController controller;

    @BeforeEach
    void setUp() {
        estoqueService = mock(EstoqueService.class);
        controller = new EstoqueController(estoqueService);
    }

    private Bolsa criarBolsa(String id, String tipo, double volume, String doador) {
        return new Bolsa(id, tipo, volume, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 1), doador);
    }

    @Test
    @DisplayName("H2 / Nível 1: GET /api/estoque deve retornar envelope com bolsas e tamanho derivados da lista sem chamar estoqueService.tamanho()")
    void deveListarEstoqueComEnvelopeEPadraoDeTamanho() {
        Bolsa b1 = criarBolsa("BOLSA-001", "O+", 450.0, "Doador 1");
        Bolsa b2 = criarBolsa("BOLSA-002", "A-", 400.0, "Doador 2");
        when(estoqueService.listarEstoque()).thenReturn(List.of(b1, b2));

        ResponseEntity<Map<String, Object>> response = controller.listar();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().get("tamanho"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bolsas = (List<Map<String, Object>>) response.getBody().get("bolsas");
        assertEquals(2, bolsas.size());
        assertEquals("BOLSA-001", bolsas.get(0).get("id"));
        assertEquals("BOLSA-002", bolsas.get(1).get("id"));

        // Garante que o controller não faz roundtrip redundante de COUNT(*) ao storage
        verify(estoqueService, never()).tamanho();
    }

    @Test
    @DisplayName("H2 / Nível 1: GET /api/estoque deve retornar envelope vazio quando estoque sem registros")
    void deveListarEstoqueVazioComEnvelopeZerado() {
        when(estoqueService.listarEstoque()).thenReturn(Collections.emptyList());

        ResponseEntity<Map<String, Object>> response = controller.listar();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().get("tamanho"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bolsas = (List<Map<String, Object>>) response.getBody().get("bolsas");
        assertNotNull(bolsas);
        assertTrue(bolsas.isEmpty());
        verify(estoqueService, never()).tamanho();
    }

    @Test
    @DisplayName("Adversarial / Nível 1: GET /api/estoque deve tratar retorno nulo de listarEstoque() sem lançar NullPointerException")
    void deveTratarListaNulaDeEstoqueRetornandoEnvelopeVazio() {
        when(estoqueService.listarEstoque()).thenReturn(null);

        ResponseEntity<Map<String, Object>> response = controller.listar();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().get("tamanho"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bolsas = (List<Map<String, Object>>) response.getBody().get("bolsas");
        assertNotNull(bolsas);
        assertTrue(bolsas.isEmpty());
    }

    @Test
    @DisplayName("Adversarial / Nível 1: GET /api/estoque deve ignorar elementos nulos na lista preservando consistência de tamanho")
    void deveIgnorarElementosNulosNaListaDeEstoquePreservandoConsistenciaDeTamanho() {
        Bolsa b1 = criarBolsa("BOLSA-001", "O+", 450.0, "Doador 1");
        java.util.List<Bolsa> listaComNulo = java.util.Arrays.asList(b1, null);
        when(estoqueService.listarEstoque()).thenReturn(listaComNulo);

        ResponseEntity<Map<String, Object>> response = controller.listar();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().get("tamanho"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bolsas = (List<Map<String, Object>>) response.getBody().get("bolsas");
        assertEquals(1, bolsas.size());
        assertEquals("BOLSA-001", bolsas.get(0).get("id"));
    }

    @Test
    @DisplayName("H2 / Fail-Fast: GET /api/estoque deve propagar DataAccessException imediatamente sem fallback")
    void devePropagarDataAccessExceptionSemFallbackEmListar() {
        when(estoqueService.listarEstoque()).thenThrow(new DataAccessException("Simulando falha de conectividade") {});

        assertThrows(DataAccessException.class, () -> controller.listar());
    }

    @Test
    @DisplayName("Concorrência / Envelope: Deve garantir atomicidade entre tamanho e bolsas no envelope")
    void deveGarantirConsistenciaDeTamanhoNoEnvelope() {
        Bolsa b1 = criarBolsa("BOLSA-001", "O+", 450.0, "Doador 1");
        when(estoqueService.listarEstoque()).thenReturn(List.of(b1));
        // Mesmo se o serviço reportar outro valor em tamanho() por causa de inserção concorrente
        when(estoqueService.tamanho()).thenReturn(999);

        ResponseEntity<Map<String, Object>> response = controller.listar();

        assertEquals(1, response.getBody().get("tamanho"), "O campo tamanho deve ser exatamente igual ao tamanho da lista retornada");
    }

    @Test
    @DisplayName("H1 / Nível 1: GET /api/estoque/{id} deve retornar 200 e dados da bolsa existente")
    void deveConsultarBolsaExistente() {
        Bolsa b = criarBolsa("BOLSA-001", "AB+", 460.0, "Carlos");
        when(estoqueService.consultar("BOLSA-001")).thenReturn(Optional.of(b));

        ResponseEntity<?> response = controller.consultar("BOLSA-001");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("BOLSA-001", body.get("id"));
        assertEquals("AB+", body.get("tipoSanguineo"));
        assertEquals(460.0, body.get("volumeMl"));
        assertEquals("2026-09-01", body.get("dataColeta"));
        assertEquals("2026-11-01", body.get("validade"));
        assertEquals("Carlos", body.get("doador"));
    }

    @Test
    @DisplayName("H1 / Nível 1: GET /api/estoque/{id} deve retornar 404 para bolsa inexistente, id nulo ou em branco")
    void deveRetornar404AoConsultarIdInexistenteOuVazio() {
        when(estoqueService.consultar("BOLSA-INEXISTENTE")).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, controller.consultar("BOLSA-INEXISTENTE").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.consultar(null).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.consultar("   ").getStatusCode());
    }

    @Test
    @DisplayName("H2 / Fail-Fast: GET /api/estoque/{id} deve propagar DataAccessException imediatamente sem fallback")
    void devePropagarDataAccessExceptionSemFallbackEmConsultar() {
        when(estoqueService.consultar("BOLSA-FAIL")).thenThrow(new DataAccessException("Simulando falha de banco") {});

        assertThrows(DataAccessException.class, () -> controller.consultar("BOLSA-FAIL"));
    }

    @Test
    @DisplayName("H1 / Nível 1: POST /api/estoque deve adicionar bolsa e retornar 200 com mensagem e tamanho")
    void deveAdicionarBolsaComSucesso() {
        BolsaRequestDTO dto = new BolsaRequestDTO(
                "BOLSA-001", "O+", 450.0, "2026-09-01", "2026-11-01", "Doador A"
        );
        when(estoqueService.tamanho()).thenReturn(1);

        ResponseEntity<?> response = controller.adicionar(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("Bolsa adicionada ao estoque", body.get("mensagem"));
        assertEquals(1, body.get("tamanho"));
        verify(estoqueService, times(1)).adicionar(any(Bolsa.class));
    }

    @Test
    @DisplayName("Adversarial / Nível 1: POST /api/estoque deve retornar 400 para body nulo")
    void deveRetornar400AoAdicionarBodyNulo() {
        ResponseEntity<?> response = controller.adicionar(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(estoqueService);
    }

    @Test
    @DisplayName("Adversarial / Nível 1: POST /api/estoque deve retornar 400 para dados inválidos")
    void deveRetornar400AoAdicionarDadosInvalidos() {
        BolsaRequestDTO dtoInvalido = new BolsaRequestDTO(
                "   ", "O+", 450.0, "2026-09-01", "2026-11-01", "Doador A"
        );

        ResponseEntity<?> response = controller.adicionar(dtoInvalido);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(estoqueService);
    }

    @Test
    @DisplayName("H3 / Nível 1: DELETE /api/estoque/{id} deve remover bolsa existente e retornar 200")
    void deveRemoverBolsaComSucesso() {
        when(estoqueService.remover("BOLSA-001")).thenReturn(true);
        when(estoqueService.tamanho()).thenReturn(0);

        ResponseEntity<Map<String, Object>> response = controller.remover("BOLSA-001");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Bolsa removida", response.getBody().get("mensagem"));
        assertEquals(0, response.getBody().get("tamanho"));
    }

    @Test
    @DisplayName("H3 / Nível 1: DELETE /api/estoque/{id} deve retornar 404 para id inexistente, nulo ou em branco")
    void deveRetornar404AoRemoverIdInexistenteOuVazio() {
        when(estoqueService.remover("BOLSA-INEXISTENTE")).thenReturn(false);

        assertEquals(HttpStatus.NOT_FOUND, controller.remover("BOLSA-INEXISTENTE").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.remover(null).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.remover("   ").getStatusCode());
    }
}

package com.rotavital.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rotavital.domain.Bolsa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários - BolsaRequestDTO Resiliente (T1.4)")
class BolsaRequestDTOTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("T1.4: Deve deserializar JSON com volumeMl numérico literal e converter para entidade Bolsa")
    void deveDesserializarNumeroLiteralEConverterParaDominio() throws Exception {
        String json = """
            {
                "id": "BOLSA-001",
                "tipoSanguineo": "O+",
                "volumeMl": 450.0,
                "dataColeta": "2026-09-01",
                "validade": "2026-11-01",
                "doador": "João Silva"
            }
            """;

        BolsaRequestDTO dto = objectMapper.readValue(json, BolsaRequestDTO.class);
        Bolsa bolsa = dto.toDomain();

        assertEquals("BOLSA-001", bolsa.getId());
        assertEquals("O+", bolsa.getTipoSanguineo());
        assertEquals(450.0, bolsa.getVolumeMl());
        assertEquals(LocalDate.of(2026, 9, 1), bolsa.getDataColeta());
        assertEquals(LocalDate.of(2026, 11, 1), bolsa.getValidade());
        assertEquals("João Silva", bolsa.getDoador());
    }

    @Test
    @DisplayName("T1.4: Deve deserializar JSON com volumeMl em String numérica e converter para entidade Bolsa")
    void deveDesserializarStringNumericaEConverterParaDominio() throws Exception {
        String json = """
            {
                "id": "BOLSA-002",
                "tipoSanguineo": "A-",
                "volumeMl": "380.5",
                "dataColeta": "2026-09-02",
                "validade": "2026-11-02",
                "doador": "Maria Souza"
            }
            """;

        BolsaRequestDTO dto = objectMapper.readValue(json, BolsaRequestDTO.class);
        Bolsa bolsa = dto.toDomain();

        assertEquals("BOLSA-002", bolsa.getId());
        assertEquals(380.5, bolsa.getVolumeMl());
    }

    @Test
    @DisplayName("T1.4: Deve aplicar volume padrão de 450.0 quando volumeMl estiver ausente ou nulo")
    void deveAplicarVolumePadraoQuandoAusente() throws Exception {
        String json = """
            {
                "id": "BOLSA-003",
                "tipoSanguineo": "B+",
                "dataColeta": "2026-09-03",
                "validade": "2026-11-03",
                "doador": "Carlos Lima"
            }
            """;

        BolsaRequestDTO dto = objectMapper.readValue(json, BolsaRequestDTO.class);
        Bolsa bolsa = dto.toDomain();

        assertEquals(450.0, bolsa.getVolumeMl());
    }

    @Test
    @DisplayName("T1.4: Deve rejeitar campos obrigatórios nulos ao converter para domínio")
    void deveRejeitarCamposObrigatoriosNulos() {
        BolsaRequestDTO dtoInvalido = new BolsaRequestDTO(null, "O+", 450.0, "2026-09-01", "2026-11-01", "Doador");
        assertThrows(NullPointerException.class, dtoInvalido::toDomain);
    }
}

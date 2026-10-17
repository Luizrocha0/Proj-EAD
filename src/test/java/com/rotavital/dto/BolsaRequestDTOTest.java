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

        BolsaRequestDTO dtoSemTipo = new BolsaRequestDTO("B-1", null, 450.0, "2026-09-01", "2026-11-01", "Doador");
        assertThrows(NullPointerException.class, dtoSemTipo::toDomain);

        BolsaRequestDTO dtoSemData = new BolsaRequestDTO("B-1", "O+", 450.0, null, "2026-11-01", "Doador");
        assertThrows(NullPointerException.class, dtoSemData::toDomain);

        BolsaRequestDTO dtoSemValidade = new BolsaRequestDTO("B-1", "O+", 450.0, "2026-09-01", null, "Doador");
        assertThrows(NullPointerException.class, dtoSemValidade::toDomain);

        BolsaRequestDTO dtoSemDoador = new BolsaRequestDTO("B-1", "O+", 450.0, "2026-09-01", "2026-11-01", null);
        assertThrows(NullPointerException.class, dtoSemDoador::toDomain);
    }

    @Test
    @DisplayName("Adversarial: Deve normalizar tipos sanguíneos com espaços ou letras minúsculas")
    void deveNormalizarTipoSanguineoComEspacosEMinusculas() {
        BolsaRequestDTO dto1 = new BolsaRequestDTO("B-1", "  o+  ", 450.0, "2026-09-01", "2026-11-01", "Doador");
        assertEquals("O+", dto1.toDomain().getTipoSanguineo());

        BolsaRequestDTO dto2 = new BolsaRequestDTO("B-2", "a-", 450.0, "2026-09-01", "2026-11-01", "Doador");
        assertEquals("A-", dto2.toDomain().getTipoSanguineo());

        BolsaRequestDTO dto3 = new BolsaRequestDTO("B-3", " ab+ ", 450.0, "2026-09-01", "2026-11-01", "Doador");
        assertEquals("AB+", dto3.toDomain().getTipoSanguineo());

        BolsaRequestDTO dto4 = new BolsaRequestDTO("B-4", "O +", 450.0, "2026-09-01", "2026-11-01", "Doador");
        assertEquals("O+", dto4.toDomain().getTipoSanguineo());
    }

    @Test
    @DisplayName("Adversarial: Deve rejeitar id vazio ou em branco")
    void deveRejeitarIdVazioOuEmBranco() {
        BolsaRequestDTO dtoVazio = new BolsaRequestDTO("", "O+", 450.0, "2026-09-01", "2026-11-01", "Doador");
        assertThrows(IllegalArgumentException.class, dtoVazio::toDomain);

        BolsaRequestDTO dtoBranco = new BolsaRequestDTO("   ", "O+", 450.0, "2026-09-01", "2026-11-01", "Doador");
        assertThrows(IllegalArgumentException.class, dtoBranco::toDomain);
    }

    @Test
    @DisplayName("Adversarial: Deve rejeitar tipo sanguíneo vazio ou em branco")
    void deveRejeitarTipoSanguineoVazioOuEmBranco() {
        BolsaRequestDTO dtoVazio = new BolsaRequestDTO("B-1", "", 450.0, "2026-09-01", "2026-11-01", "Doador");
        assertThrows(IllegalArgumentException.class, dtoVazio::toDomain);

        BolsaRequestDTO dtoBranco = new BolsaRequestDTO("B-1", "   ", 450.0, "2026-09-01", "2026-11-01", "Doador");
        assertThrows(IllegalArgumentException.class, dtoBranco::toDomain);
    }

    @Test
    @DisplayName("Adversarial: Deve rejeitar doador vazio ou em branco")
    void deveRejeitarDoadorVazioOuEmBranco() {
        BolsaRequestDTO dtoVazio = new BolsaRequestDTO("B-1", "O+", 450.0, "2026-09-01", "2026-11-01", "");
        assertThrows(IllegalArgumentException.class, dtoVazio::toDomain);

        BolsaRequestDTO dtoBranco = new BolsaRequestDTO("B-1", "O+", 450.0, "2026-09-01", "2026-11-01", "   ");
        assertThrows(IllegalArgumentException.class, dtoBranco::toDomain);
    }

    @Test
    @DisplayName("Adversarial: Deve rejeitar volumeMl negativo ou zero")
    void deveRejeitarVolumeMlNegativoOuZero() {
        BolsaRequestDTO dtoZero = new BolsaRequestDTO("B-1", "O+", 0.0, "2026-09-01", "2026-11-01", "Doador");
        assertThrows(IllegalArgumentException.class, dtoZero::toDomain);

        BolsaRequestDTO dtoNegativo = new BolsaRequestDTO("B-1", "O+", -450.0, "2026-09-01", "2026-11-01", "Doador");
        assertThrows(IllegalArgumentException.class, dtoNegativo::toDomain);
    }

    @Test
    @DisplayName("Adversarial: Deve rejeitar volumeMl em string não-numérica")
    void deveRejeitarVolumeMlNaoNumerico() {
        BolsaRequestDTO dtoInvalido = new BolsaRequestDTO("B-1", "O+", "abc", "2026-09-01", "2026-11-01", "Doador");
        assertThrows(IllegalArgumentException.class, dtoInvalido::toDomain);
    }

    @Test
    @DisplayName("Adversarial: Deve rejeitar validade anterior à data de coleta")
    void deveRejeitarValidadeAnteriorADataColeta() {
        BolsaRequestDTO dtoInconsistente = new BolsaRequestDTO(
                "B-1", "O+", 450.0, "2026-10-01", "2026-09-01", "Doador"
        );
        assertThrows(IllegalArgumentException.class, dtoInconsistente::toDomain);
    }

    @Test
    @DisplayName("Adversarial: Deve realizar parsing tolerante a espaços periféricos em datas")
    void deveTratarDatasComEspacosPerifericos() {
        BolsaRequestDTO dto = new BolsaRequestDTO(
                "B-1", "O+", 450.0, " 2026-09-01 ", " 2026-11-01 ", "Doador"
        );
        Bolsa bolsa = dto.toDomain();
        assertEquals(LocalDate.of(2026, 9, 1), bolsa.getDataColeta());
        assertEquals(LocalDate.of(2026, 11, 1), bolsa.getValidade());
    }

    @Test
    @DisplayName("Adversarial: Deve preservar acentuação UTF-8 em doador e símbolos alfanuméricos em ID")
    void devePreservarAcentuacaoUtf8ESimbolosEmId() {
        BolsaRequestDTO dto = new BolsaRequestDTO(
                "  BOLSA_001-A/2026  ",
                "  o+  ",
                450.0,
                "2026-09-01",
                "2026-11-01",
                "  José da Conceição Araújo & Cia  "
        );
        Bolsa bolsa = dto.toDomain();
        assertEquals("BOLSA_001-A/2026", bolsa.getId());
        assertEquals("O+", bolsa.getTipoSanguineo());
        assertEquals("José da Conceição Araújo & Cia", bolsa.getDoador());
    }
}

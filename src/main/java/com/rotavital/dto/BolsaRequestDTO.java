package com.rotavital.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rotavital.domain.Bolsa;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BolsaRequestDTO(
    String id,
    String tipoSanguineo,
    Object volumeMl,
    String dataColeta,
    String validade,
    String doador
) {
    public Bolsa toDomain() {
        throw new UnsupportedOperationException("Stub para ciclo TDD Red T1.4");
    }
}

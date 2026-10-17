package com.rotavital.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rotavital.domain.Bolsa;

import java.time.LocalDate;
import java.util.Objects;

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
        double volume = 450.0;
        if (volumeMl instanceof Number n) {
            volume = n.doubleValue();
        } else if (volumeMl instanceof String s && !s.isBlank()) {
            volume = Double.parseDouble(s);
        }
        return new Bolsa(
            Objects.requireNonNull(id, "id não pode ser nulo"),
            Objects.requireNonNull(tipoSanguineo, "tipoSanguineo não pode ser nulo"),
            volume,
            LocalDate.parse(Objects.requireNonNull(dataColeta, "dataColeta não pode ser nula")),
            LocalDate.parse(Objects.requireNonNull(validade, "validade não pode ser nula")),
            Objects.requireNonNull(doador, "doador não pode ser nulo")
        );
    }
}

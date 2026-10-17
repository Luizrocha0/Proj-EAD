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
        Objects.requireNonNull(id, "id não pode ser nulo");
        String idLimpo = id.trim();
        if (idLimpo.isEmpty()) {
            throw new IllegalArgumentException("id não pode ser vazio");
        }

        Objects.requireNonNull(tipoSanguineo, "tipoSanguineo não pode ser nulo");
        String tipoNormalizado = tipoSanguineo.replaceAll("\\s+", "").toUpperCase();
        if (tipoNormalizado.isEmpty()) {
            throw new IllegalArgumentException("tipoSanguineo não pode ser vazio");
        }

        double volume = 450.0;
        if (volumeMl instanceof Number n) {
            volume = n.doubleValue();
        } else if (volumeMl instanceof String s && !s.isBlank()) {
            try {
                volume = Double.parseDouble(s.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("volumeMl inválido: " + s);
            }
        }
        if (volume <= 0.0) {
            throw new IllegalArgumentException("volumeMl deve ser positivo");
        }

        Objects.requireNonNull(dataColeta, "dataColeta não pode ser nula");
        String dataColetaLimpa = dataColeta.trim();
        if (dataColetaLimpa.isEmpty()) {
            throw new IllegalArgumentException("dataColeta não pode ser vazia");
        }
        LocalDate dataColetaParsed = LocalDate.parse(dataColetaLimpa);

        Objects.requireNonNull(validade, "validade não pode ser nula");
        String validadeLimpa = validade.trim();
        if (validadeLimpa.isEmpty()) {
            throw new IllegalArgumentException("validade não pode ser vazia");
        }
        LocalDate validadeParsed = LocalDate.parse(validadeLimpa);

        if (validadeParsed.isBefore(dataColetaParsed)) {
            throw new IllegalArgumentException("validade não pode ser anterior à data de coleta");
        }

        Objects.requireNonNull(doador, "doador não pode ser nulo");
        String doadorLimpo = doador.trim();
        if (doadorLimpo.isEmpty()) {
            throw new IllegalArgumentException("doador não pode ser vazio");
        }

        return new Bolsa(
            idLimpo,
            tipoNormalizado,
            volume,
            dataColetaParsed,
            validadeParsed,
            doadorLimpo
        );
    }
}

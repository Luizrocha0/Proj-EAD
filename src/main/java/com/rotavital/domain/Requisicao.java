package com.rotavital.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Representa uma requisição emergencial de sangue feita por um hospital.
 */
public class Requisicao {

    /** Prioridade da requisição emergencial */
    public enum Prioridade {
        NORMAL, URGENTE, EMERGENCIA
    }

    private final String id;
    private final String hospitalId;
    private final String tipoSanguineo;
    private final int quantidadeBolsas;
    private final Prioridade prioridade;
    private final LocalDateTime dataHora;

    public Requisicao(String id, String hospitalId, String tipoSanguineo,
                      int quantidadeBolsas, Prioridade prioridade, LocalDateTime dataHora) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.hospitalId = Objects.requireNonNull(hospitalId, "hospitalId não pode ser nulo");
        this.tipoSanguineo = Objects.requireNonNull(tipoSanguineo, "tipoSanguineo não pode ser nulo");
        this.quantidadeBolsas = quantidadeBolsas;
        this.prioridade = Objects.requireNonNull(prioridade, "prioridade não pode ser nula");
        this.dataHora = Objects.requireNonNull(dataHora, "dataHora não pode ser nula");
    }

    public String getId() { return id; }
    public String getHospitalId() { return hospitalId; }
    public String getTipoSanguineo() { return tipoSanguineo; }
    public int getQuantidadeBolsas() { return quantidadeBolsas; }
    public Prioridade getPrioridade() { return prioridade; }
    public LocalDateTime getDataHora() { return dataHora; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Requisicao that = (Requisicao) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Requisicao{id='%s', hospital='%s', tipo='%s', qtd=%d, prioridade=%s}"
                .formatted(id, hospitalId, tipoSanguineo, quantidadeBolsas, prioridade);
    }
}

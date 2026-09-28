package com.rotavital.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Representa uma bolsa de sangue no estoque do banco de sangue.
 * Entidade central do domínio Rota Vital.
 */
public class Bolsa {

    private final String id;
    private final String tipoSanguineo;  // ex: "O+", "A-", "AB+"
    private final double volumeMl;
    private final LocalDate dataColeta;
    private final LocalDate validade;
    private final String doador;

    public Bolsa(String id, String tipoSanguineo, double volumeMl,
                 LocalDate dataColeta, LocalDate validade, String doador) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tipoSanguineo = Objects.requireNonNull(tipoSanguineo, "tipoSanguineo não pode ser nulo");
        this.volumeMl = volumeMl;
        this.dataColeta = Objects.requireNonNull(dataColeta, "dataColeta não pode ser nula");
        this.validade = Objects.requireNonNull(validade, "validade não pode ser nula");
        this.doador = Objects.requireNonNull(doador, "doador não pode ser nulo");
    }

    public String getId() { return id; }
    public String getTipoSanguineo() { return tipoSanguineo; }
    public double getVolumeMl() { return volumeMl; }
    public LocalDate getDataColeta() { return dataColeta; }
    public LocalDate getValidade() { return validade; }
    public String getDoador() { return doador; }

    /**
     * Verifica se a bolsa está vencida em relação a uma data de referência.
     */
    public boolean isVencida(LocalDate referencia) {
        return validade.isBefore(referencia);
    }

    /**
     * Verifica se a bolsa está vencida hoje.
     */
    public boolean isVencida() {
        return isVencida(LocalDate.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bolsa bolsa = (Bolsa) o;
        return id.equals(bolsa.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Bolsa{id='%s', tipo='%s', volume=%.1fml, validade=%s, doador='%s'}"
                .formatted(id, tipoSanguineo, volumeMl, validade, doador);
    }
}

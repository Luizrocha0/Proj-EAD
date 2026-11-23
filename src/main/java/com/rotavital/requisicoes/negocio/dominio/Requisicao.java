package com.rotavital.requisicoes.negocio.dominio;

import com.rotavital.comum.dominio.ConflitoAtendimentoException;
import com.rotavital.comum.dominio.TipoSanguineo;

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
    private final TipoSanguineo tipoSanguineo;
    private final int quantidadeBolsas;
    private final Prioridade prioridade;
    private final LocalDateTime dataHora;

    public Requisicao(String id, String hospitalId, String tipoSanguineo,
                      int quantidadeBolsas, Prioridade prioridade, LocalDateTime dataHora) {
        this.id = exigirIdentificador(id, "id");
        this.hospitalId = exigirIdentificador(hospitalId, "hospitalId");
        this.tipoSanguineo = TipoSanguineo.exigir(tipoSanguineo);
        if (quantidadeBolsas <= 0) {
            throw new IllegalArgumentException("quantidadeBolsas deve ser um inteiro positivo");
        }
        this.quantidadeBolsas = quantidadeBolsas;
        this.prioridade = Objects.requireNonNull(prioridade, "prioridade não pode ser nula");
        this.dataHora = Objects.requireNonNull(dataHora, "dataHora não pode ser nula");
    }

    public String getId() { return id; }
    public String getHospitalId() { return hospitalId; }
    public String getTipoSanguineo() { return tipoSanguineo.rotulo(); }
    public int getQuantidadeBolsas() { return quantidadeBolsas; }
    public Prioridade getPrioridade() { return prioridade; }
    public LocalDateTime getDataHora() { return dataHora; }

    public TipoSanguineo tipoSolicitado() {
        return tipoSanguineo;
    }

    public void conferirAtendimento(String requisicaoId, String destinoId) {
        if (!id.equals(requisicaoId)) {
            throw new ConflitoAtendimentoException("A requisição informada não está no início da fila");
        }
        if (!hospitalId.equals(destinoId)) {
            throw new IllegalArgumentException("O destino deve corresponder ao hospital da requisição");
        }
    }

    public static String exigirIdentificador(String valor, String campo) {
        String id = valor == null ? "" : valor.strip();
        if (id.isEmpty() || id.length() > 50) {
            throw new IllegalArgumentException(campo + " deve ter de 1 a 50 caracteres");
        }
        return id;
    }

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
                .formatted(id, hospitalId, tipoSanguineo.rotulo(), quantidadeBolsas, prioridade);
    }
}

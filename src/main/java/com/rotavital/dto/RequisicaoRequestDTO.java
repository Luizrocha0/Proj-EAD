package com.rotavital.dto;

import com.rotavital.domain.Requisicao;
import com.rotavital.domain.TipoSanguineo;

import java.time.LocalDateTime;
import java.util.Locale;

public record RequisicaoRequestDTO(String id, String hospitalId, String tipoSanguineo,
                                  String quantidadeBolsas, String prioridade) {
    private static final String QUANTIDADE_INVALIDA = "quantidadeBolsas deve ser um inteiro positivo";

    public Requisicao toDomain() {
        String rotulo = tipoSanguineo == null ? null : tipoSanguineo.strip().toUpperCase(Locale.ROOT);
        return new Requisicao(
                identificador(id, "id"),
                identificador(hospitalId, "hospitalId"),
                TipoSanguineo.exigir(rotulo).rotulo(),
                quantidade(),
                prioridadeRequisicao(),
                LocalDateTime.now()
        );
    }

    static String identificador(String valor, String campo) {
        String identificador = valor == null ? "" : valor.strip();
        if (identificador.isEmpty() || identificador.length() > 50) {
            throw new IllegalArgumentException(campo + " deve ter de 1 a 50 caracteres");
        }
        return identificador;
    }

    private int quantidade() {
        try {
            int valor = quantidadeBolsas == null ? 1 : Integer.parseInt(quantidadeBolsas.strip());
            if (valor > 0) {
                return valor;
            }
        } catch (NumberFormatException ignorada) {
            throw new IllegalArgumentException(QUANTIDADE_INVALIDA);
        }
        throw new IllegalArgumentException(QUANTIDADE_INVALIDA);
    }

    private Requisicao.Prioridade prioridadeRequisicao() {
        try {
            return prioridade == null ? Requisicao.Prioridade.NORMAL : Requisicao.Prioridade.valueOf(prioridade);
        } catch (IllegalArgumentException ignorada) {
            throw new IllegalArgumentException("prioridade inválida");
        }
    }
}

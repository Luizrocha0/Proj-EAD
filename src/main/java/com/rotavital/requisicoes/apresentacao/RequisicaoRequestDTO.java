package com.rotavital.requisicoes.apresentacao;

import com.rotavital.requisicoes.negocio.dominio.Requisicao;

import java.time.LocalDateTime;
import java.util.Locale;

public record RequisicaoRequestDTO(String id, String hospitalId, String tipoSanguineo,
                                  String quantidadeBolsas, String prioridade) {
    private static final String QUANTIDADE_INVALIDA = "quantidadeBolsas deve ser um inteiro positivo";

    public Requisicao toDomain() {
        String rotulo = tipoSanguineo == null ? null : tipoSanguineo.strip().toUpperCase(Locale.ROOT);
        return new Requisicao(
                id,
                hospitalId,
                rotulo,
                quantidade(),
                prioridadeRequisicao(),
                LocalDateTime.now()
        );
    }

    private int quantidade() {
        try {
            return quantidadeBolsas == null ? 1 : Integer.parseInt(quantidadeBolsas.strip());
        } catch (NumberFormatException ignorada) {
            throw new IllegalArgumentException(QUANTIDADE_INVALIDA);
        }
    }

    private Requisicao.Prioridade prioridadeRequisicao() {
        try {
            return prioridade == null ? Requisicao.Prioridade.NORMAL : Requisicao.Prioridade.valueOf(prioridade);
        } catch (IllegalArgumentException ignorada) {
            throw new IllegalArgumentException("prioridade inválida");
        }
    }
}

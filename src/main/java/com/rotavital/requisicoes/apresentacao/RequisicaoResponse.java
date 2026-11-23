package com.rotavital.requisicoes.apresentacao;

import com.rotavital.requisicoes.negocio.dominio.Requisicao;

public record RequisicaoResponse(String id, String hospitalId, String tipoSanguineo,
                                 int quantidadeBolsas, String prioridade) {
    public static RequisicaoResponse de(Requisicao requisicao) {
        return new RequisicaoResponse(requisicao.getId(), requisicao.getHospitalId(), requisicao.getTipoSanguineo(),
                requisicao.getQuantidadeBolsas(), requisicao.getPrioridade().name());
    }
}

package com.rotavital.dto;

import com.rotavital.aed.u2.Roteirizador;

import java.util.List;

public record AtendimentoRequestDTO(String requisicaoId, PontoDTO origem,
                                    List<PontoDTO> paradas, PontoDTO destino) {
    public String requisicaoIdValidado() {
        return RequisicaoRequestDTO.identificador(requisicaoId, "requisicaoId");
    }

    public List<Roteirizador.Ponto> paradasValidas() {
        if (paradas == null) {
            return List.of();
        }
        if (paradas.size() > Roteirizador.MAX_PARADAS) {
            throw new IllegalArgumentException("A rota aceita até " + Roteirizador.MAX_PARADAS + " paradas");
        }
        return paradas.stream().map(PontoDTO::converter).toList();
    }

    public record PontoDTO(String id, Double xKm, Double yKm) {
        public static Roteirizador.Ponto converter(PontoDTO dados) {
            if (dados == null || dados.xKm == null || dados.yKm == null) {
                throw new IllegalArgumentException("Cada ponto precisa de xKm e yKm");
            }
            return new Roteirizador.Ponto(dados.id, dados.xKm, dados.yKm);
        }
    }
}

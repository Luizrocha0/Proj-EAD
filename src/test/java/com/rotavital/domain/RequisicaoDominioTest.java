package com.rotavital.domain;

import com.rotavital.requisicoes.negocio.dominio.Requisicao;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RequisicaoDominioTest {
    @Test
    void rejeitaDemandaVaziaMesmoForaDoHttp() {
        assertThrows(IllegalArgumentException.class, () -> criar("O-", 0));
    }

    @Test
    void rejeitaTipoDesconhecidoMesmoForaDoHttp() {
        assertThrows(IllegalArgumentException.class, () -> criar("Z+", 2));
    }

    private Requisicao criar(String tipo, int quantidade) {
        return new Requisicao("REQ-TESTE", "HOSP-TESTE", tipo, quantidade,
                Requisicao.Prioridade.NORMAL, LocalDateTime.of(2026, 11, 23, 10, 0));
    }
}

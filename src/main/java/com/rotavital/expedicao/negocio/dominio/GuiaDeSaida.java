package com.rotavital.expedicao.negocio.dominio;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

public final class GuiaDeSaida {
    private final String requisicaoId;
    private final String hospitalId;
    private final List<Item> itens;

    private GuiaDeSaida(String requisicaoId, String hospitalId, List<Item> itens) {
        this.requisicaoId = requisicaoId;
        this.hospitalId = hospitalId;
        this.itens = itens;
    }

    public static GuiaDeSaida emitir(String requisicaoId, String hospitalId, int quantidade, List<Item> itens) {
        var confirmados = List.copyOf(itens);
        if (quantidade <= 0 || confirmados.size() != quantidade) {
            throw new IllegalArgumentException("A guia exige a quantidade integral do pedido");
        }
        var chaves = new HashSet<Long>();
        for (Item item : confirmados) {
            if (!chaves.add(item.ocorrencia())) {
                throw new IllegalArgumentException("Cada ocorrência deve aparecer uma vez na guia");
            }
        }
        return new GuiaDeSaida(requisicaoId, hospitalId, confirmados);
    }

    public String requisicaoId() {
        return requisicaoId;
    }

    public String hospitalId() {
        return hospitalId;
    }

    public List<Item> itens() {
        return itens;
    }

    public record Item(long ocorrencia, String id, String tipoSanguineo, double volumeMl, LocalDate validade) {
    }
}

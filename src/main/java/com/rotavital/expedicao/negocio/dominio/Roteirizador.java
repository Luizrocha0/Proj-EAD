package com.rotavital.expedicao.negocio.dominio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class Roteirizador {
    public static final int MAX_PARADAS = 256;
    private static final double MAX_COORDENADA_KM = 50_000;

    private Roteirizador() {
    }

    public record Ponto(String id, double xKm, double yKm) {
        public Ponto {
            id = id == null ? "" : id.strip();
            if (id.isEmpty() || id.length() > 50) {
                throw new IllegalArgumentException("Cada ponto deve ter um ID de até 50 caracteres");
            }
            if (!Double.isFinite(xKm) || !Double.isFinite(yKm)
                    || Math.abs(xKm) > MAX_COORDENADA_KM || Math.abs(yKm) > MAX_COORDENADA_KM) {
                throw new IllegalArgumentException("Coordenadas devem ser finitas e estar entre -50000 e 50000 km");
            }
        }
    }

    public record Rota(List<Ponto> pontos, double distanciaKm) {
        public Rota {
            pontos = List.copyOf(pontos);
        }
    }

    public static Rota calcular(Ponto origem, List<Ponto> paradas, Ponto destino) {
        validarPontos(origem, paradas, destino);
        Ponto[] intermediarias = paradas.toArray(Ponto[]::new);
        boolean[] visitadas = new boolean[intermediarias.length];
        var trajeto = new ArrayList<Ponto>(intermediarias.length + 2);
        trajeto.add(origem);

        Ponto atual = origem;
        double totalKm = 0;
        for (int etapa = 0; etapa < intermediarias.length; etapa++) {
            int maisProxima = -1;
            double menorDistancia = Double.POSITIVE_INFINITY;
            for (int i = 0; i < intermediarias.length; i++) {
                if (!visitadas[i]) {
                    double distancia = distancia(atual, intermediarias[i]);
                    // Em C e Java, '<' mantém a primeira parada da entrada quando há empate.
                    if (distancia < menorDistancia) {
                        maisProxima = i;
                        menorDistancia = distancia;
                    }
                }
            }
            visitadas[maisProxima] = true;
            atual = intermediarias[maisProxima];
            trajeto.add(atual);
            totalKm += menorDistancia;
        }
        trajeto.add(destino);
        return new Rota(trajeto, totalKm + distancia(atual, destino));
    }

    private static void validarPontos(Ponto origem, List<Ponto> paradas, Ponto destino) {
        if (origem == null || destino == null) {
            throw new IllegalArgumentException("Origem e destino são obrigatórios");
        }
        if (paradas == null || paradas.size() > MAX_PARADAS) {
            throw new IllegalArgumentException("A rota aceita uma lista de até " + MAX_PARADAS + " paradas");
        }

        var ids = new HashSet<String>();
        ids.add(origem.id());
        if (!ids.add(destino.id())) {
            throw new IllegalArgumentException("Origem e destino devem ter IDs diferentes");
        }
        for (Ponto parada : paradas) {
            if (parada == null || !ids.add(parada.id())) {
                throw new IllegalArgumentException("Cada parada deve ter um ID distinto");
            }
        }
    }

    private static double distancia(Ponto origem, Ponto destino) {
        return Math.hypot(origem.xKm() - destino.xKm(), origem.yKm() - destino.yKm());
    }
}

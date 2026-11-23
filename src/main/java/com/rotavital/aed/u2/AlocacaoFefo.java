package com.rotavital.aed.u2;

import com.rotavital.estoque.negocio.dominio.Bolsa;
import com.rotavital.comum.dominio.TipoSanguineo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class AlocacaoFefo {
    private AlocacaoFefo() {
    }

    public static List<Integer> selecionar(Bolsa[] estoque, TipoSanguineo receptor,
                                          int quantidade, LocalDate referencia) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("quantidadeBolsas deve ser positiva");
        }
        if (quantidade > estoque.length) {
            return List.of();
        }

        var indice = new IndiceEstoque();
        for (int posicao = 0; posicao < estoque.length; posicao++) {
            Bolsa bolsa = estoque[posicao];
            if (!bolsa.isVencida(referencia)) {
                var tipo = bolsa.tipoReconhecido();
                if (tipo.isPresent()) {
                    indice.inserir(tipo.get(), posicao);
                }
            }
        }

        List<Integer> ordenadas = new ArrayList<>();
        for (TipoSanguineo doador : TipoSanguineo.values()) {
            if (CompatibilidadeABORh.compativel(doador, receptor)) {
                for (int posicao : indice.buscar(doador)) {
                    inserirOrdenado(estoque, ordenadas, posicao, quantidade);
                }
            }
        }
        return ordenadas.size() == quantidade ? List.copyOf(ordenadas) : List.of();
    }

    private static void inserirOrdenado(Bolsa[] estoque, List<Integer> ordenadas,
                                       int candidata, int quantidade) {
        int posicao = Buscas.limiteInferior(ordenadas.size(),
                i -> precede(estoque, ordenadas.get(i), candidata));
        // A lista mantém apenas o prefixo necessário. ArrayList desloca o mesmo sufixo do memmove em C.
        if (posicao == quantidade) {
            return;
        }
        if (ordenadas.size() == quantidade) {
            ordenadas.removeLast();
        }
        ordenadas.add(posicao, candidata);
    }

    private static boolean precede(Bolsa[] estoque, int primeira, int segunda) {
        int comparacao = estoque[primeira].getValidade().compareTo(estoque[segunda].getValidade());
        return comparacao < 0 || (comparacao == 0 && primeira < segunda);
    }
}

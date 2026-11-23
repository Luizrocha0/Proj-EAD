package com.rotavital.aed.u2;

import com.rotavital.domain.Bolsa;
import com.rotavital.domain.TipoSanguineo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.rotavital.domain.TipoSanguineo.*;
import static org.junit.jupiter.api.Assertions.*;

class AlgoritmosAedTest {
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 23);

    @Test
    void confereOs64ParesDeDoadorEReceptor() {
        String[] tipos = {"O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+"};
        int[] mascarasPorReceptor = {1, 3, 5, 15, 17, 51, 85, 255};
        for (int receptor = 0; receptor < tipos.length; receptor++) {
            for (int doador = 0; doador < tipos.length; doador++) {
                assertEquals((mascarasPorReceptor[receptor] & (1 << doador)) != 0,
                        CompatibilidadeABORh.compativel(exigir(tipos[doador]), exigir(tipos[receptor])),
                        tipos[doador] + " para " + tipos[receptor]);
            }
        }
    }

    @Test
    void resolveColisoesSemPerderAsOcorrenciasDaMesmaChave() {
        var indice = new IndiceEstoque();
        indice.inserir(O_NEG, 0);
        indice.inserir(B_POS, 1);
        indice.inserir(O_NEG, 3);

        assertEquals(List.of(0, 3), indice.buscar(O_NEG));
        assertEquals(List.of(1), indice.buscar(B_POS));
        assertEquals(List.of(), indice.buscar(AB_NEG));
    }

    @Test
    void fefoOrdenaEntreTiposCompativeisEUsaPosicaoLifoNoEmpate() {
        Bolsa[] estoque = {
                bolsa("DUP", "A-", 5),
                bolsa("VENCIDA", "O-", -1),
                bolsa("EMPATE-RECENTE", "A-", 2),
                bolsa("DUP", "O-", 0),
                bolsa("INCOMPATIVEL", "A+", 0),
                bolsa("EMPATE-ANTIGO", "O-", 2),
                bolsa("TIPO-LEGADO", "X?", 0)
        };

        assertEquals(List.of(3, 2, 5, 0), AlocacaoFefo.selecionar(estoque, A_NEG, 4, HOJE));
        assertEquals(List.of(3, 2), AlocacaoFefo.selecionar(estoque, A_NEG, 2, HOJE));
        assertTrue(AlocacaoFefo.selecionar(estoque, A_NEG, 5, HOJE).isEmpty());
        assertTrue(AlocacaoFefo.selecionar(new Bolsa[0], A_NEG, 1, HOJE).isEmpty());
    }

    @Test
    void rotaRecalculaVizinhoAPartirDaUltimaParada() {
        var origem = new Roteirizador.Ponto("CENTRO", 0, 0);
        var destino = new Roteirizador.Ponto("HOSP", 8, 1);
        var distantes = new Roteirizador.Ponto("A", 0, 4);
        var primeira = new Roteirizador.Ponto("B", 2, 0);
        var segunda = new Roteirizador.Ponto("C", 5, 0);

        var rota = Roteirizador.calcular(origem, List.of(distantes, primeira, segunda), destino);

        assertEquals(List.of(origem, primeira, segunda, distantes, destino), rota.pontos());
        assertEquals(5 + Math.sqrt(41) + Math.sqrt(73), rota.distanciaKm(), 1e-10);
    }

    @Test
    void rotaDesempataPelaOrdemDeEntradaEAceitaTrajetoDireto() {
        var origem = new Roteirizador.Ponto("CENTRO", 0, 0);
        var esquerda = new Roteirizador.Ponto("ESQUERDA", -1, 0);
        var direita = new Roteirizador.Ponto("DIREITA", 1, 0);
        var destino = new Roteirizador.Ponto("HOSP", 3, 4);

        assertEquals(List.of(origem, esquerda, direita, destino),
                Roteirizador.calcular(origem, List.of(esquerda, direita), destino).pontos());
        assertEquals(5, Roteirizador.calcular(origem, List.of(), destino).distanciaKm());
        assertThrows(IllegalArgumentException.class,
                () -> Roteirizador.calcular(origem, List.of(esquerda, esquerda), destino));
        assertThrows(IllegalArgumentException.class, () -> new Roteirizador.Ponto("NAN", Double.NaN, 0));
    }

    @Test
    void buscasRetornamAPrimeiraOcorrenciaOuMenosUm() {
        assertEquals(1, Buscas.sequencial(new int[]{9, 4, 4, -1}, 4));
        assertEquals(1, Buscas.binaria(new int[]{-4, 2, 2, 2, 9}, 2));
        assertEquals(0, Buscas.binaria(new int[]{-4, 2, 2, 9}, -4));
        assertEquals(3, Buscas.binaria(new int[]{-4, 2, 2, 9}, 9));
        assertEquals(-1, Buscas.binaria(new int[]{-4, 2, 9}, 5));
        assertEquals(-1, Buscas.sequencial(new int[]{9, 4}, 8));
        assertEquals(-1, Buscas.sequencial(new int[0], 1));
        assertEquals(-1, Buscas.binaria(new int[0], 1));
    }

    @ParameterizedTest
    @MethodSource("ordenacoes")
    void ordenaVazioSingletonDuplicatasExtremosEEntradasComOrdensVariadas(Consumer<int[]> ordenar) {
        int[][] casos = {
                {}, {4}, {1, 2, 3, 4}, {4, 3, 2, 1}, {2, 2, 2, 2},
                {4, -2, 4, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, 1, -2, 19},
                new java.util.Random(26).ints(512, -31, 32).toArray()
        };
        for (int[] entrada : casos) {
            int[] esperado = entrada.clone();
            Arrays.sort(esperado);
            ordenar.accept(entrada);
            assertArrayEquals(esperado, entrada);
        }
    }

    static Stream<Consumer<int[]>> ordenacoes() {
        return Stream.of(Ordenacoes::bubbleSort, Ordenacoes::selectionSort, Ordenacoes::insertionSort,
                Ordenacoes::mergeSort, Ordenacoes::quickSort, Ordenacoes::heapSort);
    }

    private Bolsa bolsa(String id, String tipo, int diasAteValidade) {
        return new Bolsa(id, tipo, 450, HOJE.minusDays(14), HOJE.plusDays(diasAteValidade), "Sintético");
    }
}

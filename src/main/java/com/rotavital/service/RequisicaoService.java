package com.rotavital.service;

import com.rotavital.aed.u1.FilaRequisicoes;
import com.rotavital.aed.u2.Roteirizador;
import com.rotavital.domain.Bolsa;
import com.rotavital.domain.ConflitoAtendimentoException;
import com.rotavital.domain.Requisicao;
import com.rotavital.domain.TipoSanguineo;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Serviço de aplicação para gestão da fila de requisições emergenciais.
 * Delega operações para a estrutura AED real ({@link FilaRequisicoes}).
 */
@Service
public class RequisicaoService {

    private final FilaRequisicoes fila = new FilaRequisicoes();
    private final Set<String> pendentes = new HashSet<>();
    private final EstoqueService estoque;

    public RequisicaoService(EstoqueService estoque) {
        this.estoque = estoque;
    }

    /**
     * Enfileira uma nova requisição.
     */
    public synchronized int enfileirar(Requisicao requisicao) {
        if (!pendentes.add(requisicao.getId())) {
            throw new ConflitoAtendimentoException("Já existe uma requisição pendente com esse ID");
        }
        fila.enfileirar(requisicao);
        return fila.getTamanho();
    }

    public synchronized Optional<Atendimento> atenderProxima(String requisicaoId, Roteirizador.Ponto origem,
                                                              List<Roteirizador.Ponto> paradas,
                                                              Roteirizador.Ponto destino) {
        Optional<Requisicao> proxima = fila.espiar();
        if (proxima.isEmpty()) {
            return Optional.empty();
        }
        Requisicao requisicao = proxima.get();
        if (!requisicao.getId().equals(requisicaoId)) {
            throw new ConflitoAtendimentoException("A requisição informada não está no início da fila");
        }
        if (!requisicao.getHospitalId().equals(destino.id())) {
            throw new IllegalArgumentException("O destino deve corresponder ao hospital da requisição");
        }

        Roteirizador.Rota rota = Roteirizador.calcular(origem, paradas, destino);
        TipoSanguineo receptor = TipoSanguineo.exigir(requisicao.getTipoSanguineo());
        List<Bolsa> alocadas = estoque.alocar(receptor, requisicao.getQuantidadeBolsas(), LocalDate.now());
        if (alocadas.isEmpty()) {
            throw new ConflitoAtendimentoException("Estoque compatível e válido insuficiente para o pedido");
        }

        fila.desenfileirar();
        pendentes.remove(requisicaoId);
        var itens = alocadas.stream()
                .map(bolsa -> new BolsaAlocada(bolsa.getId(), bolsa.getTipoSanguineo(),
                        bolsa.getVolumeMl(), bolsa.getValidade()))
                .toList();
        return Optional.of(new Atendimento(requisicao, itens, rota, fila.getTamanho()));
    }

    public record Atendimento(Requisicao requisicao, List<BolsaAlocada> bolsas,
                              Roteirizador.Rota rota, int restantes) {
    }

    public record BolsaAlocada(String id, String tipoSanguineo, double volumeMl, LocalDate validade) {
    }

    /**
     * Consulta a próxima requisição sem removê-la.
     */
    public synchronized Optional<Requisicao> espiarProxima() {
        return fila.espiar();
    }

    /**
     * Retorna a quantidade de requisições na fila.
     */
    public synchronized int tamanho() {
        return fila.getTamanho();
    }

    /**
     * Lista todas as requisições na fila.
     */
    public synchronized Requisicao[] listarTodas() {
        return fila.toArray();
    }
}

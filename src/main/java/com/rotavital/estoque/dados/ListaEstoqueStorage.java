package com.rotavital.estoque.dados;

import com.rotavital.aed.u1.ListaEstoque;
import com.rotavital.estoque.negocio.aplicacao.EstoqueStorage;
import com.rotavital.estoque.negocio.aplicacao.SelecionarLoteFefo;
import com.rotavital.estoque.negocio.dominio.Bolsa;
import com.rotavital.estoque.negocio.dominio.LoteDeBaixa;
import com.rotavital.estoque.negocio.dominio.OcorrenciaBolsa;
import com.rotavital.estoque.negocio.dominio.PedidoDeBaixa;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Profile({"u1", "default"})
public class ListaEstoqueStorage implements EstoqueStorage {

    private final ListaEstoque listaEstoque = new ListaEstoque();

    @Override
    public synchronized void salvar(Bolsa bolsa) {
        java.util.Objects.requireNonNull(bolsa, "bolsa não pode ser nula");
        listaEstoque.inserir(bolsa);
    }

    @Override
    public synchronized Optional<Bolsa> buscarPorId(String id) {
        if (id == null || id.isBlank() || id.trim().length() > 50) {
            return Optional.empty();
        }
        return listaEstoque.consultar(id.trim());
    }

    @Override
    public synchronized boolean removerPorId(String id) {
        if (id == null || id.isBlank() || id.trim().length() > 50) {
            return false;
        }
        return listaEstoque.removerPorId(id.trim());
    }

    @Override
    public synchronized Optional<LoteDeBaixa> alocar(PedidoDeBaixa pedido) {
        var lote = SelecionarLoteFefo.selecionar(pedido, Arrays.asList(listaEstoque.ocorrencias()));
        lote.ifPresent(confirmado -> {
            var chaves = confirmado.ocorrencias().stream().map(OcorrenciaBolsa::chave).collect(Collectors.toSet());
            listaEstoque.removerOcorrencias(chaves::contains);
        });
        return lote;
    }

    @Override
    public synchronized List<Bolsa> listar() {
        return new ArrayList<>(Arrays.asList(listaEstoque.toArray()));
    }

    @Override
    public synchronized Bolsa[] listarTodas() {
        return listaEstoque.toArray();
    }

    @Override
    public synchronized int tamanho() {
        return listaEstoque.getTamanho();
    }
}

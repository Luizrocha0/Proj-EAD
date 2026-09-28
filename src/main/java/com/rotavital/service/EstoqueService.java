package com.rotavital.service;

import com.rotavital.aed.u1.ListaEstoque;
import com.rotavital.domain.Bolsa;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Serviço de aplicação para gestão do estoque de bolsas de sangue.
 * Delega operações para a estrutura AED real ({@link ListaEstoque}).
 *
 * Controllers apenas chamam este serviço — toda lógica reside aqui.
 */
@Service
public class EstoqueService {

    private final ListaEstoque estoque = new ListaEstoque();

    /**
     * Adiciona uma bolsa ao estoque.
     */
    public void adicionar(Bolsa bolsa) {
        estoque.inserir(bolsa);
    }

    /**
     * Consulta uma bolsa pelo ID.
     */
    public Optional<Bolsa> consultar(String id) {
        return estoque.consultar(id);
    }

    /**
     * Remove uma bolsa do estoque pelo ID.
     *
     * @return true se encontrada e removida
     */
    public boolean remover(String id) {
        return estoque.remover(id);
    }

    /**
     * Retorna o tamanho atual do estoque.
     */
    public int tamanho() {
        return estoque.getTamanho();
    }

    /**
     * Lista todas as bolsas no estoque.
     */
    public Bolsa[] listarTodas() {
        return estoque.toArray();
    }
}

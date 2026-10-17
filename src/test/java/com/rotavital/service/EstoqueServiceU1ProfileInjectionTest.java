package com.rotavital.service;

import com.rotavital.storage.EstoqueStorage;
import com.rotavital.storage.ListaEstoqueStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

@SpringBootTest
@ActiveProfiles("u1")
@DisplayName("Verificação de Injeção de Dependência do Spring - Perfil u1")
class EstoqueServiceU1ProfileInjectionTest {

    @Autowired
    private EstoqueService service;

    @Autowired
    private EstoqueStorage storage;

    @Test
    @DisplayName("Sob perfil u1, Spring deve injetar ListaEstoqueStorage em EstoqueService")
    void deveInjetarListaEstoqueStorageSobPerfilU1() {
        assertInstanceOf(ListaEstoqueStorage.class, storage, "Storage ativo deve ser ListaEstoqueStorage");
        assertSame(storage, service.getStorage(), "EstoqueService deve ter recebido o bean ListaEstoqueStorage do Spring");
    }
}

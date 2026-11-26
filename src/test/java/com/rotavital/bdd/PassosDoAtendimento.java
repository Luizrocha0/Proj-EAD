package com.rotavital.bdd;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PassosDoAtendimento {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final String HOSPITAL = "HOSP-BDD";

    private final MockMvc api;
    private final ObjectMapper json;
    private final Clock relogio;
    private LocalDate referencia;
    private String pedido;
    private ResultActions resposta;

    public PassosDoAtendimento(MockMvc api, ObjectMapper json, Clock relogio) {
        this.api = api;
        this.json = json;
        this.relogio = relogio;
    }

    @Dado("que a data de referência é {word}")
    public void fixarData(String data) {
        referencia = LocalDate.parse(data, DATA);
        given(relogio.getZone()).willReturn(ZoneOffset.UTC);
        given(relogio.instant()).willReturn(referencia.atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    @Dado("que {word} está na cabeça da fila e pede duas bolsas {word}")
    public void enfileirar(String id, String tipo) throws Exception {
        pedido = id;
        enviar("/api/requisicoes", Map.of("id", id, "hospitalId", HOSPITAL, "tipoSanguineo", tipo,
                "quantidadeBolsas", 2)).andExpect(status().isOk());
        conferirCabeca(id);
    }

    @Dado("que o estoque possui uma bolsa {word} válida até {word}")
    public void registrarPrimeira(String tipo, String validade) throws Exception {
        cadastrar("PRIMEIRA", tipo, LocalDate.parse(validade, DATA));
    }

    @Dado("que existe uma bolsa {word} vencida e uma {word} válida")
    public void registrarRestantes(String tipoVencido, String tipoIncompativel) throws Exception {
        cadastrar("VENCIDA", tipoVencido, referencia.minusDays(1));
        cadastrar("INCOMPATIVEL", tipoIncompativel, referencia.plusDays(1));
    }

    @Quando("a pessoa operadora solicita atendimento com destino ao hospital do pedido")
    public void solicitarAtendimento() throws Exception {
        atender(pedido);
    }

    @Então("a API informa conflito de estoque insuficiente")
    public void conferirInsuficiencia() throws Exception {
        resposta.andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Estoque compatível e válido insuficiente para o pedido"));
    }

    @Então("mantém {word} na cabeça da fila")
    public void conferirCabeca(String id) throws Exception {
        api.perform(get("/api/requisicoes/proxima")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Então("mantém as três ocorrências no estoque")
    public void conferirEstoqueIntacto() throws Exception {
        api.perform(get("/api/estoque")).andExpect(status().isOk())
                .andExpect(jsonPath("$.bolsas[*].id", containsInAnyOrder("PRIMEIRA", "VENCIDA", "INCOMPATIVEL")));
    }

    @Quando("outra ocorrência {word} válida até {word} é registrada")
    public void repor(String tipo, String validade) throws Exception {
        cadastrar("REPOSICAO", tipo, LocalDate.parse(validade, DATA));
    }

    @Quando("a pessoa operadora tenta atender {word} novamente")
    public void atender(String id) throws Exception {
        resposta = enviar("/api/requisicoes/atender", Map.of("requisicaoId", id,
                "origem", Map.of("id", "CENTRO", "xKm", 0, "yKm", 0),
                "destino", Map.of("id", HOSPITAL, "xKm", 3, "yKm", 4)));
    }

    @Então("a API confirma exatamente duas bolsas em ordem FEFO")
    public void conferirGuia() throws Exception {
        resposta.andExpect(status().isOk())
                .andExpect(jsonPath("$.requisicao.id").value(pedido))
                .andExpect(jsonPath("$.requisicao.hospitalId").value(HOSPITAL))
                .andExpect(jsonPath("$.bolsas[*].id", contains("PRIMEIRA", "REPOSICAO")));
    }

    @Então("retira {word} da fila")
    public void conferirRetirada(String id) throws Exception {
        resposta.andExpect(jsonPath("$.requisicao.id").value(id));
        api.perform(get("/api/requisicoes")).andExpect(status().isOk())
                .andExpect(jsonPath("$.requisicoes").isEmpty());
    }

    @Então("preserva a ocorrência vencida e a incompatível")
    public void conferirSaldoFinal() throws Exception {
        api.perform(get("/api/estoque")).andExpect(status().isOk())
                .andExpect(jsonPath("$.bolsas[*].id", containsInAnyOrder("VENCIDA", "INCOMPATIVEL")));
    }

    private void cadastrar(String id, String tipo, LocalDate validade) throws Exception {
        enviar("/api/estoque", Map.of("id", id, "tipoSanguineo", tipo, "dataColeta", referencia.minusDays(5),
                "validade", validade, "doador", "Sintético")).andExpect(status().isOk());
    }

    private ResultActions enviar(String caminho, Map<String, Object> corpo) throws Exception {
        return api.perform(post(caminho).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(corpo)));
    }
}

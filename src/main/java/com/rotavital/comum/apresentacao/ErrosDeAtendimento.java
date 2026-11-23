package com.rotavital.comum.apresentacao;

import com.rotavital.comum.dominio.ConflitoAtendimentoException;
import com.rotavital.expedicao.apresentacao.AtendimentoController;
import com.rotavital.requisicoes.apresentacao.RequisicaoController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.Objects;

@RestControllerAdvice(assignableTypes = {RequisicaoController.class, AtendimentoController.class})
public class ErrosDeAtendimento {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> requisicaoInvalida(IllegalArgumentException erro) {
        String mensagem = Objects.requireNonNullElse(erro.getMessage(), "Requisição inválida");
        return ResponseEntity.badRequest().body(Map.of("mensagem", mensagem));
    }

    @ExceptionHandler(ConflitoAtendimentoException.class)
    public ResponseEntity<Map<String, String>> conflitoDeAtendimento(ConflitoAtendimentoException erro) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensagem", erro.getMessage()));
    }
}

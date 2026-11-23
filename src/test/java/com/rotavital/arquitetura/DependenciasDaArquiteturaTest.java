package com.rotavital.arquitetura;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class DependenciasDaArquiteturaTest {
    private static final Pattern IMPORTACAO = Pattern.compile("(?m)^import (?:static )?([\\w.]+);");
    private static final Pattern DOMINIO = Pattern.compile("^com\\.rotavital\\.\\w+\\.(negocio\\.)?dominio\\.");
    private static final Pattern APLICACAO = Pattern.compile("^com\\.rotavital\\.\\w+\\.negocio\\.aplicacao\\.");

    @Test
    void negocioIndependeDeFrameworkEAdaptadores() throws IOException {
        var violacoes = new ArrayList<String>();
        int arquivosDeNegocio = 0;
        try (var fontes = Files.walk(Path.of("src/main/java/com/rotavital"))) {
            for (Path arquivo : fontes.filter(caminho -> caminho.toString().endsWith(".java")).toList()) {
                String classe = arquivo.toString().replace('\\', '/')
                        .substring("src/main/java/".length()).replace('/', '.');
                boolean dominio = DOMINIO.matcher(classe).find();
                boolean aplicacao = APLICACAO.matcher(classe).find();
                boolean apresentacao = classe.contains(".apresentacao.");
                if (dominio || aplicacao) {
                    arquivosDeNegocio++;
                }
                var imports = IMPORTACAO.matcher(Files.readString(arquivo));
                while (imports.find()) {
                    String dependencia = imports.group(1);
                    if (dominio && !dependencia.startsWith("java.") && !DOMINIO.matcher(dependencia).find()
                            || aplicacao && !dependencia.startsWith("java.") && !dependencia.startsWith("com.rotavital.aed.")
                            && !DOMINIO.matcher(dependencia).find() && !APLICACAO.matcher(dependencia).find()
                            || apresentacao && dependencia.contains(".dados.")) {
                        violacoes.add(classe + " -> " + dependencia);
                    }
                }
            }
        }
        assertThat(arquivosDeNegocio).isPositive();
        assertThat(violacoes).isEmpty();
    }
}

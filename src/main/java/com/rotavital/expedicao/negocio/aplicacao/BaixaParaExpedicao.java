package com.rotavital.expedicao.negocio.aplicacao;

import com.rotavital.comum.dominio.TipoSanguineo;
import com.rotavital.expedicao.negocio.dominio.GuiaDeSaida;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BaixaParaExpedicao {
    Optional<List<GuiaDeSaida.Item>> baixar(TipoSanguineo receptor, int quantidade, LocalDate data);
}

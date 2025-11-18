package com.ajmarcos.multiprobador;

import com.ajmarcos.multiprobador.ValidadorResultado.ResultadoCompleto;

public interface PruebaResultadoListener {
    void onResultadoFinalizado(ResultadoCompleto completo);
}
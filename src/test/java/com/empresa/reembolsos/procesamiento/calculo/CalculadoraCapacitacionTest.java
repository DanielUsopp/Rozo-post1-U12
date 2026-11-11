package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static com.empresa.reembolsos.DatosDePrueba.conDocumentos;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraCapacitacionTest {

    private final CalculadoraCapacitacion calculadora = new CalculadoraCapacitacion();

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "2500000, 2000000",   // 80 %
        "4000000, 3000000"    // 80 % = 3.200.000, limitado al tope anual
    })
    void calcular_reembolsaOchentaPorCientoHastaElTopeAnual(String monto, String esperado) {
        assertEquals(0, new BigDecimal(esperado)
                .compareTo(calculadora.calcular(conDocumentos(CategoriaGasto.CAPACITACION, monto, 0))));
    }
}

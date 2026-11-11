package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static com.empresa.reembolsos.DatosDePrueba.conDocumentos;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraRepresentacionTest {

    private final CalculadoraRepresentacion calculadora = new CalculadoraRepresentacion();

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "1800000, 1800000",   // monto completo
        "2500000, 2000000"    // limitado al tope mensual
    })
    void calcular_reembolsaElMontoCompletoHastaElTopeMensual(String monto, String esperado) {
        assertEquals(0, new BigDecimal(esperado)
                .compareTo(calculadora.calcular(conDocumentos(CategoriaGasto.REPRESENTACION, monto, 0))));
    }
}

package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static com.empresa.reembolsos.DatosDePrueba.conDocumentos;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraViajeTest {

    private final CalculadoraViaje calculadora = new CalculadoraViaje();

    @ParameterizedTest(name = "{0} en {1} dias -> {2}")
    @CsvSource({
        "400000, 3, 400000",   // bajo el tope de 3 dias (450.000)
        "500000, 3, 450000",   // limitado a 150.000 x 3
        "150000, 1, 150000"
    })
    void calcular_limitaAlTopeDiarioPorLosDiasDelViaje(String monto, int dias, String esperado) {
        assertEquals(0, new BigDecimal(esperado)
                .compareTo(calculadora.calcular(conDocumentos(CategoriaGasto.VIAJE, monto, dias))));
    }
}

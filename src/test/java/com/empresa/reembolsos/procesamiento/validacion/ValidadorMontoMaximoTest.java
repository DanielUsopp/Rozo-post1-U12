package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static com.empresa.reembolsos.DatosDePrueba.conDocumentos;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidadorMontoMaximoTest {

    private final ValidadorMontoMaximo validador = new ValidadorMontoMaximo();

    @ParameterizedTest(name = "{0} por {1} con {2} dias -> valida={3}")
    @CsvSource({
        "REPRESENTACION, 2000000, 0, true",    // justo en el tope
        "REPRESENTACION, 2000001, 0, false",
        "CAPACITACION,   3000000, 0, true",
        "CAPACITACION,   3000001, 0, false",
        "VIAJE,           450000, 3, true",     // 150.000 por dia x 3 dias
        "VIAJE,           450001, 3, false",
        "VIAJE,           150001, 1, false"
    })
    void validar_comparaContraElTopeDeLaCategoria(CategoriaGasto categoria, String monto, int dias,
                                                  boolean esperado) {
        ResultadoValidacion resultado = validador.validar(conDocumentos(categoria, monto, dias), List.of());

        assertEquals(esperado, resultado.valida());
        if (!esperado) {
            assertEquals("Monto excede el tope de la categoria " + categoria, resultado.motivoRechazo());
        }
    }
}

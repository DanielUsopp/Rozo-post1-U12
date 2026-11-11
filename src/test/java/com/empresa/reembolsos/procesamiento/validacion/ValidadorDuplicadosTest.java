package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.empresa.reembolsos.DatosDePrueba.conDocumentos;
import static com.empresa.reembolsos.DatosDePrueba.pendiente;
import static org.junit.jupiter.api.Assertions.*;

class ValidadorDuplicadosTest {

    private final ValidadorDuplicados validador = new ValidadorDuplicados();

    @Test
    void validar_pendienteMismaCategoriaYMes_rechaza() {
        var pendientes = List.of(pendiente(CategoriaGasto.VIAJE, LocalDate.of(2026, 11, 20)));

        ResultadoValidacion resultado = validador.validar(conDocumentos(CategoriaGasto.VIAJE, "60000", 1), pendientes);

        assertFalse(resultado.valida());
        assertEquals("Ya existe una solicitud pendiente de la misma categoria en el periodo",
                resultado.motivoRechazo());
    }

    @Test
    void validar_pendienteDeOtraCategoria_noBloquea() {
        var pendientes = List.of(pendiente(CategoriaGasto.CAPACITACION, LocalDate.of(2026, 11, 20)));

        assertTrue(validador.validar(conDocumentos(CategoriaGasto.VIAJE, "60000", 1), pendientes).valida());
    }

    @Test
    void validar_pendienteDelMismoMesDeOtroAno_noBloquea() {
        var pendientes = List.of(pendiente(CategoriaGasto.VIAJE, LocalDate.of(2025, 11, 20)));

        assertTrue(validador.validar(conDocumentos(CategoriaGasto.VIAJE, "60000", 1), pendientes).valida());
    }
}

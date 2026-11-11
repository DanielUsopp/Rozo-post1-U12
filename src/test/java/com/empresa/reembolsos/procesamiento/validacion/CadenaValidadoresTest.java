package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.empresa.reembolsos.DatosDePrueba.conDocumentos;
import static com.empresa.reembolsos.DatosDePrueba.pendiente;
import static org.junit.jupiter.api.Assertions.*;

class CadenaValidadoresTest {

    private final CadenaValidadores cadena = new CadenaValidadores(
            new ValidadorDocumentosSoporte(), new ValidadorMontoMaximo(), new ValidadorDuplicados());

    @Test
    void validar_solicitudCorrecta_recorreLosTresEslabonesYAprueba() {
        assertTrue(cadena.validar(conDocumentos(CategoriaGasto.CAPACITACION, "900000", 0), List.of()).valida());
    }

    @Test
    void validar_variosProblemasALaVez_reportaElPrimeroDeLaCadena() {
        // Sin documentos, sobre el tope y duplicada: corta en el primer eslabon
        SolicitudReembolso conTodo = conDocumentos(CategoriaGasto.VIAJE, "900000", 1);
        conTodo.setDocumentosSoporte(false);
        var pendientes = List.of(pendiente(CategoriaGasto.VIAJE, LocalDate.of(2026, 11, 1)));

        assertEquals("Faltan documentos soporte", cadena.validar(conTodo, pendientes).motivoRechazo());
    }

    @Test
    void validar_soloEsDuplicada_llegaHastaElUltimoEslabon() {
        var pendientes = List.of(pendiente(CategoriaGasto.VIAJE, LocalDate.of(2026, 11, 1)));

        ResultadoValidacion resultado = cadena.validar(conDocumentos(CategoriaGasto.VIAJE, "60000", 1), pendientes);

        assertEquals("Ya existe una solicitud pendiente de la misma categoria en el periodo",
                resultado.motivoRechazo());
    }
}

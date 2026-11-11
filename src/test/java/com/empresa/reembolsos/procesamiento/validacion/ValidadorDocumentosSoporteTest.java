package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.empresa.reembolsos.DatosDePrueba.conDocumentos;
import static org.junit.jupiter.api.Assertions.*;

class ValidadorDocumentosSoporteTest {

    private final ValidadorDocumentosSoporte validador = new ValidadorDocumentosSoporte();

    @Test
    void validar_conDocumentos_apruebaLaEtapa() {
        assertTrue(validador.validar(conDocumentos(CategoriaGasto.VIAJE, "80000", 1), List.of()).valida());
    }

    @Test
    void validar_sinDocumentos_rechazaConMotivo() {
        SolicitudReembolso sinSoporte = conDocumentos(CategoriaGasto.VIAJE, "80000", 1);
        sinSoporte.setDocumentosSoporte(false);

        ResultadoValidacion resultado = validador.validar(sinSoporte, List.of());

        assertFalse(resultado.valida());
        assertEquals("Faltan documentos soporte", resultado.motivoRechazo());
    }
}

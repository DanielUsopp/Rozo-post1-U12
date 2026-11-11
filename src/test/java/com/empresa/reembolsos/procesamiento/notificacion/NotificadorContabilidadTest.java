package com.empresa.reembolsos.procesamiento.notificacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;

import static com.empresa.reembolsos.DatosDePrueba.conDocumentos;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
class NotificadorContabilidadTest {

    private final NotificadorContabilidad contabilidad = new NotificadorContabilidad();

    @Test
    void onCambioEstado_aprobada_programaElPago(CapturedOutput salida) {
        SolicitudReembolso s = conDocumentos(CategoriaGasto.REPRESENTACION, "700000", 0);
        s.setMontoAprobado(new BigDecimal("700000"));
        s.setEstado(EstadoSolicitud.APROBADA);

        contabilidad.onCambioEstado(s, EstadoSolicitud.PENDIENTE);

        assertTrue(salida.getOut().contains("Contabilidad: programar pago de 700000 para E-210"));
    }

    @Test
    void onCambioEstado_rechazadaOCorreccion_noHaceNada(CapturedOutput salida) {
        SolicitudReembolso s = conDocumentos(CategoriaGasto.REPRESENTACION, "700000", 0);
        s.setEstado(EstadoSolicitud.RECHAZADA);
        contabilidad.onCambioEstado(s, EstadoSolicitud.PENDIENTE);
        s.setEstado(EstadoSolicitud.REQUIERE_CORRECCION);
        contabilidad.onCambioEstado(s, EstadoSolicitud.PENDIENTE);

        assertFalse(salida.getOut().contains("Contabilidad"));
    }
}

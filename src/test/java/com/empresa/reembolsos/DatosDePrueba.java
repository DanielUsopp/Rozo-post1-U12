package com.empresa.reembolsos;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fabrica de solicitudes para las pruebas. */
public final class DatosDePrueba {

    private DatosDePrueba() { }

    public static SolicitudReembolso solicitud(String empleadoId, CategoriaGasto categoria, String monto,
                                               LocalDate fechaGasto, int diasViaje, boolean documentos) {
        SolicitudReembolso s = new SolicitudReembolso();
        s.setEmpleadoId(empleadoId);
        s.setCategoria(categoria);
        s.setMontoSolicitado(new BigDecimal(monto));
        s.setFechaGasto(fechaGasto);
        s.setDiasViaje(diasViaje);
        s.setDocumentosSoporte(documentos);
        return s;
    }

    public static SolicitudReembolso conDocumentos(CategoriaGasto categoria, String monto, int diasViaje) {
        return solicitud("E-210", categoria, monto, LocalDate.of(2026, 11, 3), diasViaje, true);
    }

    public static SolicitudReembolso pendiente(CategoriaGasto categoria, LocalDate fechaGasto) {
        SolicitudReembolso s = solicitud("E-210", categoria, "50000", fechaGasto, 1, true);
        s.setEstado(EstadoSolicitud.PENDIENTE);
        return s;
    }
}

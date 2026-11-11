package com.empresa.reembolsos.aplicacion;

public class SolicitudNoEncontradaException extends IllegalArgumentException {
    public SolicitudNoEncontradaException(Long id) {
        super("Solicitud no encontrada: " + id);
    }
}

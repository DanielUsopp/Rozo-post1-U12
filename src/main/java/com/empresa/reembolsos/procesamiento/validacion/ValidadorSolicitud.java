package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.SolicitudReembolso;

import java.util.List;

/**
 * Eslabón de la cadena de validación (Chain of Responsibility, RF1).
 * Cada validador revisa una sola regla; si la solicitud la cumple, la pasa al
 * siguiente eslabón, y si no, corta la cadena con el motivo del rechazo.
 * Ningún eslabón sabe cuántos validadores vienen después de él.
 */
public abstract class ValidadorSolicitud {
    private ValidadorSolicitud siguiente;

    public ValidadorSolicitud siguiente(ValidadorSolicitud siguiente) {
        this.siguiente = siguiente;
        return siguiente;
    }

    public ResultadoValidacion validar(SolicitudReembolso solicitud, List<SolicitudReembolso> pendientesDelEmpleado) {
        ResultadoValidacion propio = validarPropio(solicitud, pendientesDelEmpleado);
        if (!propio.valida() || siguiente == null) {
            return propio;
        }
        return siguiente.validar(solicitud, pendientesDelEmpleado);
    }

    protected abstract ResultadoValidacion validarPropio(
        SolicitudReembolso solicitud, List<SolicitudReembolso> pendientesDelEmpleado);
}

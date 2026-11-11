package com.empresa.reembolsos.procesamiento.notificacion;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

/**
 * Interesado en los cambios de estado de una solicitud (Observer, RF3).
 * El servicio de aplicación avisa a todos los observadores registrados y
 * cada uno decide qué hacer; el servicio no conoce a ninguno en particular.
 */
public interface ObservadorCambioEstado {
    void onCambioEstado(SolicitudReembolso solicitud, EstadoSolicitud estadoAnterior);
}

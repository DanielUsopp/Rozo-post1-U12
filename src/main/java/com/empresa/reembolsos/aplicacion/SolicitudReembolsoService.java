package com.empresa.reembolsos.aplicacion;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import com.empresa.reembolsos.infraestructura.RepositorioSolicitudesJpa;
import com.empresa.reembolsos.procesamiento.calculo.SelectorCalculadora;
import com.empresa.reembolsos.procesamiento.notificacion.ObservadorCambioEstado;
import com.empresa.reembolsos.procesamiento.validacion.CadenaValidadores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SolicitudReembolsoService {
    private static final Logger log = LoggerFactory.getLogger(SolicitudReembolsoService.class);

    private final CadenaValidadores validadores;
    private final SelectorCalculadora calculadoras;
    private final List<ObservadorCambioEstado> observadores;
    private final RepositorioSolicitudesJpa repositorio;

    public SolicitudReembolsoService(CadenaValidadores validadores, SelectorCalculadora calculadoras,
                                      List<ObservadorCambioEstado> observadores, RepositorioSolicitudesJpa repositorio) {
        this.validadores = validadores;
        this.calculadoras = calculadoras;
        this.observadores = observadores;
        this.repositorio = repositorio;
    }

    public SolicitudReembolso crear(SolicitudReembolso solicitud) {
        var pendientes = repositorio.findByEmpleadoIdAndEstado(solicitud.getEmpleadoId(), EstadoSolicitud.PENDIENTE);
        var resultado = validadores.validar(solicitud, pendientes);
        if (!resultado.valida()) {
            throw new SolicitudInvalidaException(resultado.motivoRechazo());
        }
        solicitud.setMontoAprobado(calculadoras.obtener(solicitud.getCategoria()).calcular(solicitud));
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        return repositorio.save(solicitud);
    }

    public SolicitudReembolso cambiarEstado(Long id, EstadoSolicitud nuevoEstado) {
        var solicitud = repositorio.findById(id)
            .orElseThrow(() -> new SolicitudNoEncontradaException(id));
        var estadoAnterior = solicitud.getEstado();
        solicitud.setEstado(nuevoEstado);
        var guardada = repositorio.save(solicitud);
        observadores.forEach(o -> notificarSinInterrumpir(o, guardada, estadoAnterior));
        return guardada;
    }

    // Un observador que falla no debe impedir que los demas se enteren (ADR-001)
    private void notificarSinInterrumpir(ObservadorCambioEstado observador, SolicitudReembolso solicitud,
                                         EstadoSolicitud estadoAnterior) {
        try {
            observador.onCambioEstado(solicitud, estadoAnterior);
        } catch (RuntimeException e) {
            log.error("{} fallo al procesar el cambio de estado de la solicitud {}",
                observador.getClass().getSimpleName(), solicitud.getId(), e);
        }
    }
}

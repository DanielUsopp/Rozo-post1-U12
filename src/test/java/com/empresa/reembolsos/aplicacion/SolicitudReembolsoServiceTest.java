package com.empresa.reembolsos.aplicacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import com.empresa.reembolsos.infraestructura.RepositorioSolicitudesJpa;
import com.empresa.reembolsos.procesamiento.calculo.CalculadoraCapacitacion;
import com.empresa.reembolsos.procesamiento.calculo.SelectorCalculadora;
import com.empresa.reembolsos.procesamiento.notificacion.ObservadorCambioEstado;
import com.empresa.reembolsos.procesamiento.validacion.CadenaValidadores;
import com.empresa.reembolsos.procesamiento.validacion.ValidadorDocumentosSoporte;
import com.empresa.reembolsos.procesamiento.validacion.ValidadorDuplicados;
import com.empresa.reembolsos.procesamiento.validacion.ValidadorMontoMaximo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.empresa.reembolsos.DatosDePrueba.conDocumentos;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SolicitudReembolsoServiceTest {

    private RepositorioSolicitudesJpa repositorio;
    private ObservadorCambioEstado observadorA;
    private ObservadorCambioEstado observadorB;
    private SolicitudReembolsoService servicio;

    @BeforeEach
    void setUp() {
        repositorio = mock(RepositorioSolicitudesJpa.class);
        observadorA = mock(ObservadorCambioEstado.class);
        observadorB = mock(ObservadorCambioEstado.class);
        when(repositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));
        servicio = new SolicitudReembolsoService(
                new CadenaValidadores(new ValidadorDocumentosSoporte(), new ValidadorMontoMaximo(),
                        new ValidadorDuplicados()),
                new SelectorCalculadora(List.of(new CalculadoraCapacitacion())),
                List.of(observadorA, observadorB),
                repositorio);
    }

    @Test
    void crear_solicitudValida_calculaMontoYQuedaPendiente() {
        SolicitudReembolso creada = servicio.crear(conDocumentos(CategoriaGasto.CAPACITACION, "1000000", 0));

        assertEquals(0, new BigDecimal("800000").compareTo(creada.getMontoAprobado()));
        assertEquals(EstadoSolicitud.PENDIENTE, creada.getEstado());
        verify(repositorio).save(creada);
    }

    @Test
    void crear_solicitudInvalida_lanzaExcepcionYNoGuarda() {
        SolicitudReembolso sinSoporte = conDocumentos(CategoriaGasto.CAPACITACION, "1000000", 0);
        sinSoporte.setDocumentosSoporte(false);

        var error = assertThrows(SolicitudInvalidaException.class, () -> servicio.crear(sinSoporte));

        assertEquals("Faltan documentos soporte", error.getMessage());
        verify(repositorio, never()).save(any());
    }

    @Test
    void cambiarEstado_avisaATodosLosObservadoresConElEstadoAnterior() {
        SolicitudReembolso existente = conDocumentos(CategoriaGasto.CAPACITACION, "1000000", 0);
        existente.setEstado(EstadoSolicitud.PENDIENTE);
        when(repositorio.findById(4L)).thenReturn(Optional.of(existente));

        servicio.cambiarEstado(4L, EstadoSolicitud.RECHAZADA);

        verify(observadorA).onCambioEstado(existente, EstadoSolicitud.PENDIENTE);
        verify(observadorB).onCambioEstado(existente, EstadoSolicitud.PENDIENTE);
        assertEquals(EstadoSolicitud.RECHAZADA, existente.getEstado());
    }

    @Test
    void cambiarEstado_solicitudInexistente_lanzaNoEncontrada() {
        when(repositorio.findById(77L)).thenReturn(Optional.empty());

        assertThrows(SolicitudNoEncontradaException.class,
                () -> servicio.cambiarEstado(77L, EstadoSolicitud.APROBADA));
        verifyNoInteractions(observadorA, observadorB);
    }
}

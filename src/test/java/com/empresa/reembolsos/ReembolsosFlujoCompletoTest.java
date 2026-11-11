package com.empresa.reembolsos;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import com.empresa.reembolsos.infraestructura.RepositorioSolicitudesJpa;
import com.empresa.reembolsos.procesamiento.notificacion.NotificadorAuditoria;
import com.empresa.reembolsos.procesamiento.notificacion.NotificadorContabilidad;
import com.empresa.reembolsos.procesamiento.notificacion.NotificadorEmpleado;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReembolsosFlujoCompletoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepositorioSolicitudesJpa repositorio;

    @Autowired
    private ObjectMapper json;

    @SpyBean
    private NotificadorEmpleado notificadorEmpleado;

    @SpyBean
    private NotificadorContabilidad notificadorContabilidad;

    @SpyBean
    private NotificadorAuditoria notificadorAuditoria;

    @BeforeEach
    void vaciarBase() {
        repositorio.deleteAll();
    }

    @ParameterizedTest(name = "{0} por {1} ({2} dias) -> aprobado {3}")
    @CsvSource({
        "VIAJE,          400000,  3, 400000",
        "REPRESENTACION, 1800000, 0, 1800000",
        "CAPACITACION,   2500000, 0, 2000000"
    })
    void crear_cadaCategoria_quedaPendienteConSuFormula(String categoria, String monto, int dias,
                                                        double aprobado) throws Exception {
        mockMvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON)
                        .content(solicitudJson("E-301", categoria, monto, dias, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.montoAprobado").value(aprobado));
    }

    @Test
    void crear_sinDocumentos_respondeBadRequestConElMotivo() throws Exception {
        mockMvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON)
                        .content(solicitudJson("E-302", "VIAJE", "90000", 1, false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.motivo").value("Faltan documentos soporte"));
    }

    @Test
    void cambiarEstado_aAprobada_avisaALosTresInteresados() throws Exception {
        String creada = mockMvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON)
                        .content(solicitudJson("E-303", "REPRESENTACION", "700000", 0, true)))
                .andReturn().getResponse().getContentAsString();
        Long id = json.readValue(creada, SolicitudReembolso.class).getId();

        mockMvc.perform(put("/api/solicitudes/" + id + "/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("\"APROBADA\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));

        verify(notificadorEmpleado).onCambioEstado(any(), eq(EstadoSolicitud.PENDIENTE));
        verify(notificadorContabilidad).onCambioEstado(any(), eq(EstadoSolicitud.PENDIENTE));
        verify(notificadorAuditoria).onCambioEstado(any(), eq(EstadoSolicitud.PENDIENTE));
    }

    @Test
    void cambiarEstado_idInexistente_respondeNotFound() throws Exception {
        mockMvc.perform(put("/api/solicitudes/999/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("\"APROBADA\""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.motivo").value("Solicitud no encontrada: 999"));
    }

    private String solicitudJson(String empleado, String categoria, String monto, int dias, boolean documentos) {
        return """
            {"empleadoId":"%s","categoria":"%s","montoSolicitado":%s,
             "fechaGasto":"2026-11-03","diasViaje":%d,"documentosSoporte":%b}
            """.formatted(empleado, categoria, monto, dias, documentos);
    }
}

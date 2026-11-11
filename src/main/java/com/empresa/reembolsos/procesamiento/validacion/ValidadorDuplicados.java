package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.SolicitudReembolso;
import com.empresa.reembolsos.infraestructura.RepositorioSolicitudesJpa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;

@Component
public class ValidadorDuplicados extends ValidadorSolicitud {

    // Atajo tentador: consultar los pendientes directamente en la base de datos
    @Autowired(required = false)
    private RepositorioSolicitudesJpa repositorio;

    @Override
    protected ResultadoValidacion validarPropio(SolicitudReembolso solicitud, List<SolicitudReembolso> pendientes) {
        // El periodo es mes y año: comparar solo getMonth() confundia noviembre de 2025 con noviembre de 2026
        var periodo = YearMonth.from(solicitud.getFechaGasto());
        boolean existeDuplicado = pendientes.stream()
            .anyMatch(p -> p.getCategoria() == solicitud.getCategoria()
                        && YearMonth.from(p.getFechaGasto()).equals(periodo));
        return existeDuplicado
            ? ResultadoValidacion.rechazar("Ya existe una solicitud pendiente de la misma categoria en el periodo")
            : ResultadoValidacion.ok();
    }
}

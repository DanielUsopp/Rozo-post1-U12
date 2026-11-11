package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

import java.math.BigDecimal;

/**
 * Fórmula del monto aprobado para una categoría de gasto (Strategy, RF2).
 * Cada implementación declara la categoría que atiende y calcula su propio
 * monto; agregar una categoría nueva es agregar una implementación.
 */
public interface CalculadoraReembolso {
    CategoriaGasto getCategoria();
    BigDecimal calcular(SolicitudReembolso solicitud);
}

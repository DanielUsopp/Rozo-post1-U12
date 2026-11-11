package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SelectorCalculadoraTest {

    @Test
    void obtener_devuelveLaCalculadoraDeCadaCategoria() {
        var viaje = new CalculadoraViaje();
        var representacion = new CalculadoraRepresentacion();
        var capacitacion = new CalculadoraCapacitacion();
        var selector = new SelectorCalculadora(List.of(viaje, representacion, capacitacion));

        assertSame(viaje, selector.obtener(CategoriaGasto.VIAJE));
        assertSame(representacion, selector.obtener(CategoriaGasto.REPRESENTACION));
        assertSame(capacitacion, selector.obtener(CategoriaGasto.CAPACITACION));
    }

    @Test
    void obtener_categoriaSinCalculadora_lanzaExcepcion() {
        var selector = new SelectorCalculadora(List.of(new CalculadoraViaje()));

        var error = assertThrows(IllegalArgumentException.class,
                () -> selector.obtener(CategoriaGasto.CAPACITACION));
        assertEquals("Categoria no soportada: CAPACITACION", error.getMessage());
    }

    @Test
    void constructor_dosCalculadorasParaLaMismaCategoria_fallaAlConstruir() {
        assertThrows(IllegalStateException.class,
                () -> new SelectorCalculadora(List.of(new CalculadoraViaje(), new CalculadoraViaje())));
    }
}

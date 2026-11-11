package com.empresa.reembolsos;

import com.empresa.reembolsos.procesamiento.calculo.CalculadoraReembolso;
import com.empresa.reembolsos.procesamiento.notificacion.ObservadorCambioEstado;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

// Las reglas son sobre el codigo de produccion: las clases de prueba viven en
// los mismos paquetes (p. ej. CalculadoraViajeTest) y no deben contarse.
@AnalyzeClasses(packages = "com.empresa.reembolsos", importOptions = ImportOption.DoNotIncludeTests.class)
public class ReglasArquitectura {

    // Regla 1: procesamiento no depende de infraestructura, adaptadores ni JPA
    @ArchTest
    static final ArchRule procesamientoSinPersistenciaNiWeb = noClasses()
        .that().resideInAPackage("..procesamiento..")
        .should().dependOnClassesThat()
        .resideInAnyPackage(
            "..infraestructura..", "..adaptadores..",
            "org.springframework.data..", "jakarta.persistence..");

    // Regla 2: el controlador solo accede al servicio de aplicacion y al dominio
    // (y a su propio paquete: leer su campo "servicio" tambien cuenta como acceso)
    @ArchTest
    static final ArchRule controladorSoloUsaServicio = classes()
        .that().resideInAPackage("..adaptadores.rest..")
        .should().onlyAccessClassesThat()
        .resideInAnyPackage(
            "..adaptadores.rest..", "..aplicacion..", "..dominio..",
            "org.springframework.web..", "org.springframework.http..", "java..");

    // Regla 3: las calculadoras implementan CalculadoraReembolso
    // SelectorCalculadora es el Factory que elige entre ellas y vive a proposito en el mismo paquete
    @ArchTest
    static final ArchRule calculadorasImplementanInterfaz = classes()
        .that().resideInAPackage("..procesamiento.calculo..")
        .and().areNotInterfaces()
        .and().doNotHaveSimpleName("SelectorCalculadora")
        .should().implement(CalculadoraReembolso.class);

    // Regla 4: los notificadores implementan ObservadorCambioEstado
    @ArchTest
    static final ArchRule notificadoresImplementanInterfaz = classes()
        .that().resideInAPackage("..procesamiento.notificacion..")
        .and().areNotInterfaces()
        .should().implement(ObservadorCambioEstado.class);
}

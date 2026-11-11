# Reembolsos Integrado — Sistema de Solicitudes de Reembolso

## Descripción
Repositorio del post-contenido de la Unidad 12 de Patrones de Diseño de Software, proyecto integrador de cierre de la materia. Sistema Spring Boot (`reembolsos-integrado`) que reemplaza la hoja de cálculo compartida del área de Finanzas para las solicitudes de reembolso de gastos: los empleados registran una solicitud con la categoría, el monto, la fecha del gasto y sus documentos soporte; el sistema la valida, calcula el monto aprobado y avisa a los interesados cuando cambia de estado.

## Diseño Propuesto
Esta sección se escribió a partir de los tres requisitos funcionales, antes de implementar cualquier patrón. Para cada requisito se identificó qué es lo que varía y se eligió el patrón que resuelve exactamente esa variación.

| Requisito | Punto de variabilidad | Patrón propuesto | Justificación en una oración |
|-----------|----------------------|------------------|------------------------------|
| RF1 — Validación en varias etapas | El **conjunto de verificaciones** por las que pasa una solicitud: hoy son tres (tope por categoría, documentos soporte, duplicado pendiente) y Finanzas ya anunció una cuarta (límite anual acumulado). | **Chain of Responsibility** | Cada verificación es un eslabón independiente que aprueba o corta, de modo que agregar el límite anual es agregar un eslabón sin abrir los tres existentes. |
| RF2 — Cálculo distinto por categoría | La **fórmula del monto aprobado**, que depende de la categoría: tope diario × días (viáticos), monto completo hasta un tope mensual (representación), 80 % hasta un tope anual (capacitación), y vendrán más categorías. | **Strategy** | Las tres categorías calculan el mismo resultado (un monto) con fórmulas distintas elegidas por un solo valor, la categoría, y una categoría nueva como "bienestar" es una clase nueva. |
| RF3 — Notificación a varios interesados | **Quiénes** se enteran de un cambio de estado y **qué hace cada uno** con él: el empleado recibe el resultado, Contabilidad solo reacciona a aprobaciones, auditoría registra todo, y ya se anunció un panel de métricas. | **Observer** | El servicio solo anuncia "esta solicitud cambió de estado" y cada interesado decide cómo reaccionar, así que el panel de métricas será un suscriptor más sin tocar el servicio. |

**Por qué no otros patrones.**
- **RF1: Strategy no sirve.** No se elige una sola verificación entre varias: todas deben ejecutarse en orden y cualquiera puede cortar el proceso. Esa combinación de "secuencia + corte anticipado" es la que resuelve Chain of Responsibility.
- **RF2: Chain of Responsibility tampoco aplica.** Para una solicitud aplica exactamente una fórmula, decidida por la categoría, y no hay un orden de evaluación entre fórmulas.
- **RF3: una lista de llamadas directas dentro del servicio no alcanza.** Funcionaría hoy, pero el servicio tendría que conocer a cada interesado y cambiaría cada vez que llegue uno nuevo, que es exactamente lo que Finanzas anunció.

**Cómo encajan los tres.** Cada patrón resuelve un problema que los otros dos no resuelven, y se conectan sin adaptadores: la cadena entrega un `ResultadoValidacion`; si es válido, la estrategia de la categoría produce el monto aprobado; y cuando la solicitud cambia de estado, el `SolicitudReembolsoService` anuncia el cambio a los observadores. El servicio de aplicación es el único punto que conoce a los tres y al repositorio.

## Decisiones de diseño — Parte 1

### Decisión de diseño 1 — Orden de la cadena: documentos → monto → duplicados
Los validadores se encadenaron del más barato al más costoso. `ValidadorDocumentosSoporte` revisa un booleano y `ValidadorMontoMaximo` hace una comparación contra un tope fijo; los dos responden en tiempo constante. `ValidadorDuplicados` recorre la lista de solicitudes pendientes del empleado, y esa lista además exige una consulta a la base de datos antes de entrar a la cadena. Al ponerlo al final, una solicitud sin soporte o con un monto fuera de rango se rechaza sin llegar a recorrer esa lista.

- **Lo que se gana.** Las solicitudes con errores evidentes se resuelven con el trabajo mínimo, y el eslabón caro solo trabaja para solicitudes que ya pasaron las dos verificaciones baratas.
- **Lo que se pierde.** El empleado recibe un motivo a la vez: si le faltan documentos y además está duplicada, primero se entera de los documentos y, al corregirlos, recién se entera del duplicado (`CadenaValidadoresTest.validar_variosProblemasALaVez_reportaElPrimeroDeLaCadena` lo demuestra). Con el orden inverso pasaría lo mismo, pero con el costo de consultar los duplicados en cada solicitud.
- **Matiz honesto.** En la implementación actual el servicio carga los pendientes antes de llamar a la cadena, así que la consulta a la base de datos se hace siempre; lo que el orden ahorra es el recorrido de la lista. Si el volumen lo justificara, el siguiente paso sería cargar los pendientes solo cuando la cadena llegue al último eslabón.

### Decisión de diseño 2 — Selección por un `Map` construido desde la `List<CalculadoraReembolso>`
Se eligió el mapa que arma `SelectorCalculadora` a partir de todas las `CalculadoraReembolso` que Spring encuentra, y no un `switch` sobre `CategoriaGasto`. El RF2 dice que Finanzas agregará categorías "sin modificar el cálculo de las categorías existentes". Con el mapa, agregar "bienestar" es crear `CalculadoraBienestar` con `@Component` y agregar la constante al enum; ninguna línea de `SelectorCalculadora` ni de las calculadoras existentes cambia. Con un `switch`, cada categoría nueva obligaría a editar el método de selección: el mismo `switch` que se eliminó en la Unidad 11.

Además, `Collectors.toMap` lanza `IllegalStateException` si dos calculadoras declaran la misma categoría, así que ese error de configuración aparece al arrancar la aplicación y no en medio de una solicitud (`SelectorCalculadoraTest.constructor_dosCalculadorasParaLaMismaCategoria_fallaAlConstruir`).

## Hallazgos al probar el flujo
Al ejercitar el flujo completo con solicitudes reales aparecieron dos defectos en el código de partida. Se corrigieron en un commit aparte (`fix: calcular el tope de viaticos por dia y comparar duplicados por mes y año`):

1. **El tope de viáticos no consideraba los días.** `ValidadorMontoMaximo` comparaba toda solicitud de VIAJE contra 150.000, que en el RF2 es el tope *diario*. Un viaje de 3 días por 400.000, que `CalculadoraViaje` aprobaría completo (tope 450.000), se rechazaba en la validación con "Monto excede el tope de la categoria VIAJE". Ahora el validador usa 150.000 × días para VIAJE.
2. **El período de duplicados ignoraba el año.** `ValidadorDuplicados` comparaba `getMonth()`, así que una solicitud pendiente de noviembre de 2025 bloqueaba una nueva de noviembre de 2026. Ahora compara `YearMonth` (mes y año).

Queda una observación sin cambiar porque no rompe nada: para REPRESENTACION y CAPACITACION, el validador rechaza todo monto por encima del tope, así que el `min(tope)` de esas calculadoras nunca llega a recortar un monto. Funciona como una segunda barrera.

## Evidencia de funcionamiento
Respuestas reales de la aplicación (`java -jar`, H2 en memoria):

```
POST /api/solicitudes  REPRESENTACION 1.800.000          -> 200  montoAprobado 1800000, estado PENDIENTE
POST /api/solicitudes  CAPACITACION 2.500.000            -> 200  montoAprobado 2000000.0 (80 %)
POST /api/solicitudes  VIAJE 90.000 sin documentos       -> 400  {"motivo":"Faltan documentos soporte"}
PUT  /api/solicitudes/1/estado "APROBADA"                -> 200  estado APROBADA
PUT  /api/solicitudes/2/estado "REQUIERE_CORRECCION"     -> 200  estado REQUIERE_CORRECCION
PUT  /api/solicitudes/99/estado "APROBADA"               -> 404  {"motivo":"Solicitud no encontrada: 99"}
```
Salida de los observadores para esos dos cambios de estado (Contabilidad solo reacciona a la aprobación):
```
Notificando a E-201: su solicitud paso de PENDIENTE a APROBADA
Contabilidad: programar pago de 1800000.00 para E-201
Auditoria: solicitud 1 cambio de PENDIENTE a APROBADA
Notificando a E-201: su solicitud paso de PENDIENTE a REQUIERE_CORRECCION
Auditoria: solicitud 2 cambio de PENDIENTE a REQUIERE_CORRECCION
```

## Pruebas
`mvn verify`: 37 pruebas, 0 fallos.
- **Unitarias (31):**
  - Validadores: 2 + 7 + 3, más 3 de la cadena completa.
  - Calculadoras: 3 + 2 + 2, más 3 del selector.
  - `NotificadorContabilidad`: 2.
  - `SolicitudReembolsoService` con repositorio y observadores simulados: 4.
- **Integración (6), `ReembolsosFlujoCompletoTest` (`@SpringBootTest` + MockMvc):** creación de las tres categorías, rechazo sin documentos, cambio a APROBADA verificando con `@SpyBean` que los tres notificadores recibieron el aviso, y 404 para un id inexistente.

## Parte 2 — Decisión arquitectónica y análisis de impacto

### Decisión de diseño 3 — Resumen de ADR-001
El documento completo está en [`docs/adr/ADR-001.md`](docs/adr/ADR-001.md). Se evaluó por separado cada dependencia externa contra el criterio de la Guía (sección 6.2): Hexagonal vale la pena en sistemas que sobrevivirán 3 años o más con cambios de infraestructura.

- **Persistencia: capas simples.** `SolicitudReembolsoService` sigue usando `RepositorioSolicitudesJpa` directamente. No hay ningún plan de cambiar de base de datos, el servicio usa solo tres operaciones del repositorio y el dominio ya está libre de JPA gracias a `orm.xml`. Un puerto hoy sería una interfaz con una sola implementación y sin caso de uso (YAGNI). La deuda aceptada es tener que introducir el puerto después si algún día cambia el motor.
- **Notificación: sin interfaces nuevas, con un cambio de comportamiento.** `ObservadorCambioEstado` ya es el punto de extensión para la integración externa de Contabilidad. Pero esa integración introduce algo que hoy no existe: un observador que puede fallar. Como el servicio recorría los observadores con un `forEach`, una excepción de Contabilidad habría impedido que auditoría registrara el cambio, y el RF3 exige que auditoría registre todo cambio "sin excepción". Por eso el servicio ahora notifica a cada observador por separado y registra en el log el que falle (`cambiarEstado_unObservadorFalla_losDemasIgualSeEnteran`). No se diseñó desde ahora una interfaz para la API de Contabilidad, porque su contrato todavía no existe.

### Decisión de diseño 4 — La Regla 1 distingue estereotipos de Spring de acceso a datos y web
La Regla 1 prohíbe que `procesamiento` dependa de `org.springframework.data..` y `jakarta.persistence..`, pero no de todo `org.springframework`. La razón es que las dos clases de dependencia son de naturaleza distinta:

- **`@Component` es una etiqueta para el contenedor.** No cambia lo que hace un validador ni qué necesita para funcionar: `new ValidadorDuplicados()` se usa sin Spring en `ValidadorDuplicadosTest`, y si mañana se cambiara de contenedor, bastaría con quitar la anotación sin tocar ninguna regla de negocio.
- **Spring Data, JPA o las clases web sí cambian el diseño.** Si un validador importara `RepositorioSolicitudesJpa` o `jakarta.persistence`, consultaría la base de datos por su cuenta y ya no se podría probar con una lista en memoria. Si importara clases web, conocería HTTP. Son justamente los acoplamientos que separan las capas.

Prohibir todo `org.springframework` obligaría a registrar los validadores, calculadoras y notificadores a mano en una clase de configuración, sin ganar ningún aislamiento real. La regla prohíbe lo que de verdad acopla, no lo que solo etiqueta.

### Ajustes a las reglas del enunciado
Con las reglas exactamente como en el enunciado, `mvn test -Dtest=ReglasArquitectura` dio 3 de 4 reglas en rojo, aunque el código de producción las cumple. Las causas fueron:

1. **ArchUnit también importaba las clases de prueba.** `CalculadoraViajeTest` vive en `procesamiento.calculo` y no implementa `CalculadoraReembolso`, lo mismo con `NotificadorContabilidadTest` en la Regla 4. Se agregó `importOptions = ImportOption.DoNotIncludeTests.class`, porque las reglas son sobre el código de producción.
2. **Regla 2: el controlador accede a su propio campo.** Leer `this.servicio` cuenta como acceso a una clase de `adaptadores.rest`, que no estaba en la lista permitida. Se agregó `..adaptadores.rest..`; la intención de la regla (que el controlador no llegue a infraestructura ni a procesamiento) se mantiene.
3. **Regla 3: `SelectorCalculadora` vive junto a las calculadoras.** Es el Factory que elige la estrategia y no es una calculadora. Se excluyó por nombre con `doNotHaveSimpleName("SelectorCalculadora")` en lugar de moverlo de paquete, porque su lugar natural es junto a las estrategias que selecciona.

Con esos ajustes, las 4 reglas pasan. Las reglas se ejecutan en su propio paso del pipeline (`mvn test -Dtest=ReglasArquitectura`); `mvn verify` corre las otras 38 pruebas, porque el nombre `ReglasArquitectura` no sigue el patrón `*Test` que Surefire incluye por defecto.

## Qué se descartó y por qué
Para cada elemento se aplicó el criterio de la Guía (sección 4.2): ¿la variabilidad que justificaría la alternativa es real y está documentada hoy, o es especulación?

**1. `ApplicationEventPublisher` en lugar de una lista de observadores inyectada (referencia del enunciado).**
- **Qué se consideró:** publicar un evento de dominio y reaccionar con `@EventListener`.
- **Por qué se descartó:** con tres observadores fijos y conocidos, el bus de eventos no agrega desacoplamiento, porque `ObservadorCambioEstado` ya lo da, y sí hace más difícil seguir en el código quién reacciona a qué.
- **Señal para reconsiderar:** que los interesados vivan en módulos que no deben conocerse entre sí, o que se necesite procesamiento asíncrono.

**2. Visitor o Abstract Factory para exportar reportes en varios formatos (referencia del enunciado).**
- **Qué se consideró:** anticipar exportaciones a PDF y Excel además de CSV.
- **Por qué se descartó:** solo existe el requisito de CSV, así que no se cumple la Rule of Three.
- **Señal para reconsiderar:** una segunda solicitud real de otro formato.

**3. Template Method para las tres calculadoras (propio).**
- **Qué se consideró:** al implementar el RF2, las tres calculadoras terminaban con `.min(tope)`, y la Rule of Three dice que a la tercera repetición se abstrae. La tentación fue una clase base `CalculadoraConTope` con un método `calcularSinTope()` abstracto y el `min` en la base.
- **Por qué se descartó:** la repetición es de una sola llamada y cada tope tiene un significado distinto: diario por días, mensual y anual sobre el 80 %. La clase base agregaría herencia y un nivel más de indirección para ahorrar una línea por clase, y amarraría a todas las categorías futuras a "fórmula + tope", cuando nada indica que "bienestar" vaya a tener tope. La Rule of Three se cumple en el número de repeticiones, pero no en el contenido: lo repetido no es un algoritmo, es una llamada a `BigDecimal.min`. Tampoco pasa el test de explicabilidad: no se puede decir en una oración qué problema resolvería la clase base que hoy exista.
- **Señal para reconsiderar:** que las categorías empiecen a compartir pasos reales del cálculo, por ejemplo impuestos o redondeos comunes, y no solo el tope.

**4. Un puerto para la API externa de Contabilidad diseñado desde ahora (propio).**
- **Qué se consideró:** ya que Contabilidad anunció la integración, crear desde ya una interfaz `PasarelaPagosContabilidad` con métodos como `programarPago(...)` y un adaptador simulado.
- **Por qué se descartó:** el contrato de esa API no existe todavía. Cualquier interfaz escrita hoy sería una adivinanza sobre sus parámetros, su autenticación y su manejo de errores, y lo más probable es que haya que reescribirla cuando llegue el contrato real. `ObservadorCambioEstado` ya permite cambiar el interior de `NotificadorContabilidad` sin tocar nada más; lo único que sí se adelantó es lo que se sabe con certeza, que una API externa puede fallar (ver ADR-001).
- **Señal para reconsiderar:** la publicación de la especificación de la API, o que otro módulo además de la notificación necesite hablar con Contabilidad.

## Estructura de paquetes
```
src/main/java/com/empresa/reembolsos/
├── ReembolsosApplication.java
├── dominio/                 SolicitudReembolso, CategoriaGasto, EstadoSolicitud (sin Spring ni JPA)
├── procesamiento/
│   ├── validacion/          RF1 — Chain of Responsibility
│   ├── calculo/             RF2 — Strategy
│   └── notificacion/        RF3 — Observer
├── aplicacion/              SolicitudReembolsoService
├── infraestructura/         RepositorioSolicitudesJpa
└── adaptadores/rest/        SolicitudReembolsoController
```

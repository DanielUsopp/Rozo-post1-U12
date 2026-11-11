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

# Práctica 2 — Orquestación de servicios REST con BPMN 2.0 y Flowable

Máster en Desarrollo de Software · DSS · Curso 2026/27

## 1. Qué contiene este proyecto

Proyecto Maven de partida (**no es la solución**): Spring Boot 3.4.5, Flowable 7.2.0, Java 21 y H2 en memoria. Incluye API de reservas simuladas, endpoints REST de empleados y aerolíneas, tres delegados iniciales (`checkEmployee`, `rejectEmployee`, `quoteIberia`), un proceso BPMN inicial con **BPMN DI** (editable visualmente) y cuatro peticiones JSON de ejemplo.

## 2. Requisitos y puesta en marcha

- JDK 21, Maven 3.9+ y acceso a Internet para resolver dependencias.
- STS 5.x o IDE compatible; puerto 8080 disponible.
- Descomprimir el ZIP y abrir en STS: **File > Import > Maven > Existing Maven Projects**.
- Ejecutar `es.ugr.dss.flight.FlightApplication` o `mvn spring-boot:run` desde la raíz.

## 3. Comprobación del punto de partida

Enviar `POST http://localhost:8080/api/bookings`, cabecera `Content-Type: application/json` y cuerpo de `requests/authorized.json`:

```json
{"employeeId":"EMP001","destination":"AMS","departureDate":"2026-11-10","returnDate":"2026-11-14"}
```

El starter **solo consulta Iberia**: para este caso, su cotización simulada es **300 EUR**. No esperar todavía Vueling ni la selección de la oferta más barata. También se incluyen `rejected.json`, `no-offer.json` y `outage.json`.

## 4. Edición visual del BPMN

1. Abrir https://demo.bpmn.io/ y elegir **Open Diagram**.
2. Cargar `src/main/resources/processes/flight-booking.bpmn20.xml`. Ya incluye información gráfica BPMN DI; no hay que convertir el archivo.
3. Mantener `start -> checkEmployee -> auth`, con rechazo `reject -> end` por la rama predeterminada.
4. En la rama autorizada (`${authorized}`), añadir una pasarela paralela de división (`parallelSplit`), dos tareas de servicio (`quoteIberia` y `quoteVueling`), una pasarela paralela de unión (`parallelJoin`) y `chooseOffer -> end`.
5. Descargar el BPMN y sustituir el archivo del proyecto. **Comprobar el XML exportado antes de ejecutarlo.**

El editor bpmn.io es genérico y puede no permitir editar atributos propios de Flowable. Las tareas deben conservar/recibir los siguientes atributos:

```xml
flowable:delegateExpression="${checkEmployee}"
flowable:delegateExpression="${rejectEmployee}"
flowable:delegateExpression="${quoteIberia}"
flowable:delegateExpression="${quoteVueling}"
flowable:delegateExpression="${chooseOffer}"
```

Estos atributos se escriben **dentro de la etiqueta de cada `serviceTask` correspondiente**, no como elementos XML independientes. Mantener `xmlns:flowable`, `process id="flightBooking"`, la condición `${authorized}` y el flujo `default` de `auth`. Los IDs BPMN deben ser únicos. No unir la rama de rechazo con `parallelJoin`.

## 5. Desarrollo Java

El archivo `WorkflowDelegates.java` contiene las implementaciones iniciales. Se deben:

- Adaptar `QuoteIberia` para almacenar su oferta **sin fijar el resultado definitivo**.
- Implementar `QuoteVueling` como `JavaDelegate` con `@Component("quoteVueling")` y consulta mediante `HttpPartners.quote("VUELING", ...)`.
- Implementar `ChooseOffer` con `@Component("chooseOffer")`, comparar ofertas válidas y actualizar las variables `status`, `selectedAirline`, `totalPrice` y `detail`.
- Gestionar ausencia de ofertas y fallo HTTP de una aerolínea sin perder la oferta válida de la otra.
- Mantener `travelClass`, fijada en `CheckEmployee`.

**Advertencia sobre concurrencia:** las pasarelas paralelas BPMN representan dos ramas lógicas, pero en esta configuración síncrona no garantizan llamadas HTTP simultáneas.

## 6. Servicios simulados

- `GET /mock/employees/EMP001`: autorizado, ECONOMY.
- `GET /mock/employees/EMP002`: autorizado, BUSINESS.
- `GET /mock/employees/EMP999`: no autorizado.
- `GET /mock/airlines/{airline}/quote?destination=AMS&travelClass=ECONOMY&departureDate=2026-11-10&returnDate=2026-11-14` (IBERIA o VUELING).
- Destino `NONE`: ninguna aerolínea ofrece vuelo.
- Destino `FAIL`: VUELING devuelve HTTP 503; IBERIA sigue disponible.

## 7. Casos de aceptación

| Caso | Archivo | Resultado esperado tras completar la práctica |
| --- | --- | --- |
| Autorizado | `authorized.json` | QUOTED, VUELING, 275 EUR |
| Rechazado | `rejected.json` | REJECTED, sin cotizaciones |
| Sin ofertas | `no-offer.json` | NO_OFFER, sin aerolínea seleccionada |
| Fallo Vueling | `outage.json` | QUOTED, IBERIA, 170 EUR (destino FAIL) |
| Fechas inválidas | Modificar fechas | HTTP 400 |

El proyecto contiene una prueba JUnit inicial; ampliar la cobertura para las nuevas funcionalidades. Los valores son simulados y no representan precios reales.

## 8. Entrega

Seguir el guion PDF publicado en PRADO. Entregar código fuente Maven y BPMN exportado con BPMN DI, captura del diagrama final y evidencias de los casos de aceptación. No incluir `target/` ni archivos compilados.

**Uso didáctico exclusivamente:** no se realizan reservas reales; los servicios simulados comparten la misma JVM y H2 es temporal. No publicar esta aplicación en Internet.

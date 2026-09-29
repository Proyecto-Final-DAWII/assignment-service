# TODO — Assignment Service

Hoja de ruta para implementar asignaciones, transferencias y devoluciones respetando la arquitectura actual del proyecto, Java 25, OpenFeign y RabbitMQ.

## Convenciones

- `[x]`: terminado y disponible en el proyecto.
- `[ ]`: pendiente.
- `P0`: obligatorio para el MVP.
- `P1`: necesario para una entrega completa.
- `P2`: mejora posterior al MVP.

Documentación relacionada:

- [Estructura y responsabilidad de paquetes](HELP.md)
- [Guía de OpenFeign](src/main/java/pe/edu/cibertec/assignment/config/openfeign/HELP.md)
- [Guía de RabbitMQ](src/main/java/pe/edu/cibertec/assignment/config/rabbitmq/HELP.md)

---

## 1. Estado actual de la infraestructura

### Base del proyecto

- [x] Java 25 configurado en Maven.
- [x] Spring Boot 4.1.
- [x] Estructura de paquetes por capas.
- [x] Dockerfile con Java 25, Maven Wrapper, capas de Spring Boot y usuario no root.
- [ ] Configurar PostgreSQL mediante variables de entorno.
- [ ] Incorporar migraciones versionadas de base de datos.
- [ ] Incorporar validación Jakarta para los requests.

### OpenFeign

- [x] Dependencia `spring-cloud-starter-openfeign`.
- [x] Escaneo de clientes bajo `pe.edu.cibertec.assignment.client`.
- [x] Interceptor global que propaga el header `Authorization`.
- [x] El interceptor no crea headers vacíos ni sobrescribe una credencial explícita.
- [x] Pruebas unitarias del interceptor.
- [ ] Crear `IdentityClient`.
- [ ] Crear `AssetClient`.
- [ ] Definir timeouts por cliente.
- [ ] Traducir errores remotos a excepciones del dominio.

### RabbitMQ

- [x] Configuración dinámica de exchanges, queues, bindings y routes.
- [x] `RabbitMqDto<T>` como contrato base obligatorio.
- [x] `RabbitMqTemplate` con trazabilidad y validación.
- [x] Registro de rutas inyectables mediante `@Qualifier`.
- [x] Pruebas de configuración y publicación.
- [ ] Configurar las rutas de eventos de asignaciones.
- [ ] Crear eventos en `messaging.event`.
- [ ] Crear productor en `messaging.producer`.
- [ ] Implementar outbox para publicación confiable.

---

## 2. Responsabilidad del servicio

`assignment-service` es dueño de:

- entregas de activos;
- devoluciones;
- transferencias entre colaboradores;
- historial de posesión;
- condición de entrega y devolución;
- evidencia y documentos asociados;
- consulta de asignaciones actuales e históricas.

No es dueño de:

- identidad, estado o permisos del colaborador: pertenecen a `identity-service`;
- catálogo, disponibilidad o estado maestro del activo: pertenecen a `asset-service`;
- auditoría central: recibe eventos publicados por RabbitMQ.

Regla de integración:

```text
HTTP entrante
    ↓
controller → service.impl
                 ├──→ repository
                 ├──→ IdentityClient / AssetClient (OpenFeign)
                 └──→ outbox → RabbitMQ

RabbitMQ → messaging.consumer → service
```

- Un controller nunca llama directamente a un repository, cliente Feign o `RabbitTemplate`.
- Los clientes Feign no contienen reglas de negocio.
- Los productores deciden cómo publicar; el service decide cuándo debe ocurrir el evento.
- Los consumidores reciben y adaptan el mensaje, y luego delegan al service.

---

## 3. Modelo de datos — P0

### `assignment`

```text
id                    UUID PK
asset_id              UUID NOT NULL
employee_id           UUID NOT NULL
status                VARCHAR NOT NULL
assigned_at           TIMESTAMPTZ NOT NULL
expected_return_date  DATE NULL
returned_at           TIMESTAMPTZ NULL
assignment_condition  VARCHAR NOT NULL
return_condition      VARCHAR NULL
assignment_notes      TEXT NULL
return_notes          TEXT NULL
created_by            UUID NOT NULL
closed_by             UUID NULL
created_at            TIMESTAMPTZ NOT NULL
updated_at            TIMESTAMPTZ NOT NULL
version               BIGINT NOT NULL
```

Estados permitidos:

```text
ACTIVE
RETURNED
TRANSFERRED
CANCELLED
```

Condiciones permitidas:

```text
NEW
GOOD
FAIR
DAMAGED
```

Restricciones:

- [ ] Crear índice por `asset_id`.
- [ ] Crear índice por `employee_id`.
- [ ] Crear índice por `status`.
- [ ] Impedir dos asignaciones `ACTIVE` para el mismo `asset_id` mediante un índice único parcial de PostgreSQL.
- [ ] Usar `@Version` para evitar actualizaciones concurrentes perdidas.

### `assignment_transfer`

```text
id                UUID PK
assignment_id     UUID NOT NULL
from_employee_id  UUID NOT NULL
to_employee_id    UUID NOT NULL
transferred_at    TIMESTAMPTZ NOT NULL
condition         VARCHAR NOT NULL
notes             TEXT NULL
performed_by      UUID NOT NULL
```

Una transferencia agrega historia; nunca reemplaza ni elimina la asignación anterior.

### `assignment_file` — P1

```text
id             UUID PK
assignment_id  UUID NOT NULL
object_key     VARCHAR UNIQUE NOT NULL
file_type      VARCHAR NOT NULL
original_name  VARCHAR NULL
created_at     TIMESTAMPTZ NOT NULL
```

Tipos:

```text
DELIVERY_PHOTO
RETURN_PHOTO
DELIVERY_DOCUMENT
RETURN_DOCUMENT
```

### `outbox_event` — P0

```text
id             UUID PK
aggregate_type VARCHAR NOT NULL
aggregate_id   UUID NOT NULL
event_type     VARCHAR NOT NULL
routing_key    VARCHAR NOT NULL
payload        JSONB NOT NULL
trace_id       VARCHAR NOT NULL
status         VARCHAR NOT NULL
attempts       INTEGER NOT NULL DEFAULT 0
created_at     TIMESTAMPTZ NOT NULL
published_at   TIMESTAMPTZ NULL
last_error     TEXT NULL
```

Estados sugeridos:

```text
PENDING
PUBLISHED
FAILED
```

La fila de outbox debe guardarse en la misma transacción que el cambio de la asignación. Solamente el publicador de outbox envía eventos a RabbitMQ.

---

## 4. Clases por paquete — P0

### `entity`

- [ ] `Assignment`.
- [ ] `AssignmentTransfer`.
- [ ] `AssignmentFile` — P1.
- [ ] `OutboxEvent`.
- [ ] Enums `AssignmentStatus`, `AssetCondition`, `AssignmentFileType` y `OutboxStatus`.

### `repository`

- [ ] `AssignmentRepository`.
- [ ] `AssignmentTransferRepository`.
- [ ] `AssignmentFileRepository` — P1.
- [ ] `OutboxEventRepository`.
- [ ] Consulta de asignación activa por activo.
- [ ] Consultas paginadas por colaborador, activo y estado.
- [ ] Consulta bloqueable cuando devolución o transferencia necesiten evitar concurrencia.

### `dto.request`

- [ ] `CreateAssignmentRequest`.
- [ ] `ReturnAssignmentRequest`.
- [ ] `TransferAssignmentRequest`.
- [ ] `CancelAssignmentRequest`.
- [ ] Agregar validaciones con `@NotNull`, `@FutureOrPresent`, `@Size` y las que correspondan.

### `dto.response`

- [ ] `AssignmentResponse`.
- [ ] `AssignmentDetailResponse`.
- [ ] `AssignmentHistoryResponse`.
- [ ] No exponer entidades JPA directamente.

### `mapper`

- [ ] `AssignmentMapper`.
- [ ] `AssignmentTransferMapper`.
- [ ] Mantener los mappers sin acceso a repositories ni reglas de negocio.

### `service` y `service.impl`

- [ ] `AssignmentService`.
- [ ] `AssignmentServiceImpl`.
- [ ] `OutboxPublisherService`.
- [ ] Colocar `@Transactional` en operaciones que modifican estado.
- [ ] Mantener tipos HTTP como `ResponseEntity` fuera del service.

### `controller`

- [ ] `AssignmentController`.
- [ ] Recibir y validar DTOs.
- [ ] Delegar todos los casos de uso al service.
- [ ] No usar repositories, clientes Feign o RabbitMQ directamente.

### `exception`

- [ ] `AssignmentNotFoundException`.
- [ ] `ActiveAssignmentAlreadyExistsException`.
- [ ] `EmployeeNotActiveException`.
- [ ] `AssetNotAvailableException`.
- [ ] `InvalidAssignmentStateException`.
- [ ] `RemoteServiceException` o excepciones específicas por integración.
- [ ] `GlobalExceptionHandler` con respuestas consistentes y sin datos sensibles.

---

## 5. OpenFeign — P0

### Ubicación obligatoria

```text
client/IdentityClient.java
client/AssetClient.java
client/dto/EmployeeClientResponse.java
client/dto/AssetClientResponse.java
client/dto/AssetTransitionRequest.java
```

Los contratos externos permanecen en `client.dto`; no deben mezclarse con los DTOs públicos de `assignment-service`.

### Configuración

- [ ] Declarar las URLs mediante variables de entorno:

```properties
app.clients.identity-service.url=${IDENTITY_SERVICE_URL:http://localhost:8081}
app.clients.asset-service.url=${ASSET_SERVICE_URL:http://localhost:8082}
```

- [ ] Usar nombres estables en `@FeignClient`:

```java
@FeignClient(name = "identity-service", url = "${app.clients.identity-service.url}")
@FeignClient(name = "asset-service", url = "${app.clients.asset-service.url}")
```

- [ ] Configurar `connect-timeout` y `read-timeout` por cliente.
- [ ] Confirmar los endpoints y DTOs contra los contratos reales de `identity-service` y `asset-service` antes de implementarlos.
- [ ] Traducir `404`, `409`, `401`, `403` y timeouts a excepciones entendibles para el caso de uso.

### Autenticación

- [x] `AuthTokenRequestInterceptor` propaga automáticamente `Authorization` en llamadas realizadas dentro de la petición HTTP actual.
- [ ] No agregar `@RequestHeader("Authorization")` a cada método Feign.
- [ ] No pasar el token por controller, service o DTO.
- [ ] No registrar el token en logs, errores, base de datos ni eventos RabbitMQ.
- [ ] Mantener la llamada Feign en el mismo hilo HTTP cuando dependa del token del usuario.

Una llamada iniciada desde `@Scheduled`, un consumidor RabbitMQ, `@Async` o el publicador de outbox no tiene el token del usuario. Si alguno de esos procesos necesita llamar a un servicio protegido, debe usar una credencial técnica configurada exclusivamente para ese cliente.

### Responsabilidades de los clientes

`IdentityClient` debe permitir, según el contrato real:

- [ ] Consultar un colaborador por ID.
- [ ] Verificar que exista y esté `ACTIVE`.
- [ ] Obtener el colaborador actual para `/api/v1/me/assignments` sin recibir manualmente el token.

`AssetClient` debe permitir, según el contrato real:

- [ ] Consultar detalle y disponibilidad del activo.
- [ ] Cambiar `AVAILABLE → RESERVED`.
- [ ] Cambiar `RESERVED → ASSIGNED`.
- [ ] Cambiar `ASSIGNED → AVAILABLE`.
- [ ] Cambiar `ASSIGNED → MAINTENANCE` cuando la devolución reporte daño.
- [ ] Ejecutar las transiciones de compensación admitidas por `asset-service`.

Solo `service.impl` debe coordinar estos clientes. No deben invocarse desde controllers, repositories o mappers.

---

## 6. RabbitMQ — P0

RabbitMQ reemplaza cualquier referencia anterior a Kafka. Los nombres de eventos y routing keys se mantienen versionados.

### Eventos salientes

```text
assignment.created.v1
assignment.transferred.v1
assignment.returned.v1
assignment.cancelled.v1
```

### Rutas productoras

Configurar en `application.properties` una route por operación:

```properties
app.rabbitmq.routes.assignment-created.exchange=assignment.events
app.rabbitmq.routes.assignment-created.routing-key=assignment.created.v1

app.rabbitmq.routes.assignment-transferred.exchange=assignment.events
app.rabbitmq.routes.assignment-transferred.routing-key=assignment.transferred.v1

app.rabbitmq.routes.assignment-returned.exchange=assignment.events
app.rabbitmq.routes.assignment-returned.routing-key=assignment.returned.v1

app.rabbitmq.routes.assignment-cancelled.exchange=assignment.events
app.rabbitmq.routes.assignment-cancelled.routing-key=assignment.cancelled.v1
```

Estas son rutas exclusivamente productoras, por lo que `assignment-service` no necesita declarar una queue para consumir sus propios eventos. Cada servicio consumidor debe declarar su propia queue y binding.

Qualifiers resultantes:

```text
@Qualifier("assignmentCreatedRoute")
@Qualifier("assignmentTransferredRoute")
@Qualifier("assignmentReturnedRoute")
@Qualifier("assignmentCancelledRoute")
```

### Contratos de eventos

Ubicación:

```text
messaging/event/AssignmentCreatedEvent.java
messaging/event/AssignmentTransferredEvent.java
messaging/event/AssignmentReturnedEvent.java
messaging/event/AssignmentCancelledEvent.java
messaging/event/AssignmentEventPayload.java
```

Reglas obligatorias:

- [ ] Todo evento publicado hereda de `RabbitMqDto<T>`.
- [ ] El payload contiene únicamente datos necesarios para el consumidor.
- [ ] Incluir snapshots mínimos como `assetTag` o `employeeName` solamente cuando sean necesarios para auditoría histórica.
- [ ] No incluir `Authorization`, contraseñas ni información sensible.
- [ ] No modificar manualmente `timestamp`, `operation` o `type`; `RabbitMqTemplate` los prepara.
- [ ] Permitir que la configuración existente propague o genere `traceId`.

Ejemplo de payload:

```json
{
  "assignmentId": "uuid",
  "assetId": "uuid",
  "assetTag": "LAP-000120",
  "employeeId": "uuid",
  "employeeName": "Ana Torres",
  "occurredAt": "2026-09-29T15:30:00Z",
  "status": "ACTIVE"
}
```

### Productor

Ubicación:

```text
messaging/producer/AssignmentEventProducer.java
```

- [ ] Inyectar `RabbitTemplate` y las routes mediante `@Qualifier`.
- [ ] Publicar con `route.getExchange()` y `route.getRoutingKey()`.
- [ ] Publicar solamente instancias que hereden de `RabbitMqDto<?>`.
- [ ] No decidir reglas de negocio dentro del producer.
- [ ] No registrar el body completo si contiene información personal.

### Outbox

- [ ] Guardar el cambio de asignación y su `OutboxEvent` en una sola transacción local.
- [ ] Crear un publicador programado que lea eventos `PENDING` por lotes.
- [ ] Reconstruir el evento tipado y enviarlo mediante `AssignmentEventProducer`.
- [ ] Marcar `PUBLISHED` únicamente después de una publicación exitosa.
- [ ] Incrementar `attempts` y conservar un error sanitizado cuando falle.
- [ ] Reintentar con backoff y establecer un máximo antes de marcar `FAILED`.
- [ ] Evitar que dos instancias publiquen la misma fila usando bloqueo o `SKIP LOCKED`.
- [ ] Preservar el `traceId` original almacenado en outbox.

### Eventos entrantes — solo cuando exista un caso real

Si el servicio necesita consumir un evento externo, la ruta sí debe declarar queue:

```properties
app.rabbitmq.routes.asset-updated.exchange=asset.events
app.rabbitmq.routes.asset-updated.queue=assignment.asset-updated.queue
app.rabbitmq.routes.asset-updated.routing-key=asset.updated.v1
```

El listener se ubica en `messaging.consumer`:

```java
@RabbitListener(queues = "#{@assetUpdatedQueue.name}")
public void consume(AssetUpdatedEvent event) {
    assignmentService.handleAssetUpdated(event);
}
```

- [ ] Hacer consumidores idempotentes.
- [ ] No asumir que existe un token HTTP dentro del listener.
- [ ] Delegar el caso de uso al service.
- [ ] Definir estrategia de reintentos y dead-letter queue antes de activar consumidores productivos.

---

## 7. Casos de uso — P0

### Crear una asignación

```text
1. Controller valida CreateAssignmentRequest.
2. Service consulta al colaborador mediante IdentityClient.
3. Service exige que el colaborador exista y esté ACTIVE.
4. Service consulta el activo mediante AssetClient.
5. Service exige que el activo esté AVAILABLE.
6. Service solicita AVAILABLE → RESERVED.
7. Dentro de una transacción local, comprueba nuevamente que no exista otra asignación ACTIVE.
8. En esa transacción guarda la asignación ACTIVE.
9. Antes de confirmar la transacción solicita RESERVED → ASSIGNED.
10. En la misma transacción guarda assignment.created.v1 en outbox y confirma el commit.
11. Si falla la persistencia, la transición remota o el commit, revierte la transacción y ejecuta la compensación segura.
12. Después del commit, el publicador de outbox envía assignment.created.v1 por RabbitMQ.
13. Controller devuelve 201 con AssignmentResponse.
```

Este MVP acepta una llamada remota dentro de la transacción local para impedir que el outbox sea visible antes de que el activo quede `ASSIGNED`. La capa de orquestación debe capturar también los errores ocurridos al confirmar el commit. Como mejora P1, se debe incorporar reconciliación para recuperar una operación interrumpida entre la transición remota y la confirmación local.

Compensación mínima:

- [ ] Si falla la persistencia después de reservar, solicitar `RESERVED → AVAILABLE`.
- [ ] Si falla `RESERVED → ASSIGNED`, cancelar o revertir la asignación local y liberar la reserva.
- [ ] Si `RESERVED → ASSIGNED` tuvo éxito pero falla el commit local, solicitar la transición compensatoria definida por `asset-service`.
- [ ] Registrar `traceId`, operación e identificadores; nunca el token.
- [ ] Documentar qué hacer si también falla la compensación.
- [ ] Incorporar una tarea de reconciliación para compensaciones pendientes — P1.

### Devolver una asignación

```text
1. Cargar y bloquear la asignación.
2. Validar que esté ACTIVE.
3. Registrar condición, observaciones, actor y fecha.
4. Cambiar a RETURNED.
5. Solicitar ASSIGNED → AVAILABLE al asset-service.
6. Si está dañada y el caso lo requiere, solicitar ASSIGNED → MAINTENANCE.
7. Guardar assignment.returned.v1 en outbox.
```

### Transferir una asignación

```text
1. Cargar y bloquear la asignación actual.
2. Validar que esté ACTIVE.
3. Validar mediante IdentityClient que el nuevo colaborador esté ACTIVE.
4. Marcar la asignación anterior como TRANSFERRED.
5. Registrar AssignmentTransfer.
6. Crear una nueva asignación ACTIVE para el nuevo colaborador.
7. Mantener el activo en ASSIGNED.
8. Guardar assignment.transferred.v1 en outbox.
```

### Cancelar una asignación

- [ ] Definir exactamente desde qué estados se permite cancelar.
- [ ] Cambiar a `CANCELLED` sin borrar historia.
- [ ] Revertir el estado del activo cuando corresponda.
- [ ] Guardar `assignment.cancelled.v1` en outbox.

---

## 8. Endpoints — P0

```http
GET  /api/v1/assignments
POST /api/v1/assignments
GET  /api/v1/assignments/{id}
POST /api/v1/assignments/{id}/return
POST /api/v1/assignments/{id}/transfer
POST /api/v1/assignments/{id}/cancel
GET  /api/v1/assignments/by-asset/{assetId}
GET  /api/v1/assignments/by-employee/{employeeId}
GET  /api/v1/assignments/active/by-asset/{assetId}
GET  /api/v1/me/assignments
```

Reglas:

- [ ] Usar paginación en endpoints que devuelven colecciones.
- [ ] Retornar `201 Created` al crear.
- [ ] Retornar `404` cuando no exista la asignación.
- [ ] Retornar `409 Conflict` para estados incompatibles o asignación activa duplicada.
- [ ] Obtener la identidad de `/me` mediante el contexto autenticado o `IdentityClient`; no aceptar `employeeId` desde el frontend para ese endpoint.
- [ ] No aceptar `status`, `createdBy`, `closedBy`, timestamps ni `traceId` desde el request.

Request mínimo de creación:

```json
{
  "assetId": "uuid",
  "employeeId": "uuid",
  "expectedReturnDate": "2027-09-29",
  "condition": "GOOD",
  "notes": "Incluye cargador USB-C de 65W"
}
```

Validaciones:

- [ ] Activo y colaborador existentes.
- [ ] Colaborador activo.
- [ ] Activo disponible.
- [ ] Fecha esperada presente o futura, según la regla acordada.
- [ ] Notas con longitud máxima.
- [ ] Ausencia de otra asignación activa para el activo.

---

## 9. Idempotencia y concurrencia — P1

- [ ] Aceptar `Idempotency-Key` en `POST /api/v1/assignments`.
- [ ] Persistir la clave y el resultado durante un periodo definido.
- [ ] Devolver el mismo resultado para reintentos con la misma clave y payload.
- [ ] Rechazar la reutilización de una clave con un payload diferente.
- [ ] Mantener la restricción única de asignación activa como última defensa.
- [ ] Usar bloqueo optimista o pesimista en devolución, transferencia y cancelación.
- [ ] Hacer idempotente el publicador de outbox y cualquier consumer RabbitMQ.

---

## 10. Archivos y documento — P1

Endpoints:

```http
POST /api/v1/assignments/{id}/files
GET  /api/v1/assignments/{id}/files
GET  /api/v1/assignments/{id}/document
```

- [ ] Validar tipo, extensión y tamaño del archivo.
- [ ] Almacenar solo metadatos y `object_key` en PostgreSQL.
- [ ] No guardar binarios grandes dentro de la entidad `Assignment`.
- [ ] Generar un PDF con asignación, colaborador, activo, serial, condición, accesorios, fechas y actor.
- [ ] Generar el documento desde información persistida y snapshots necesarios.
- [ ] No implementar firma digital legal en el MVP.

---

## 11. Pruebas obligatorias

### Unitarias — P0

- [ ] Crear asignación exitosamente.
- [ ] Rechazar colaborador inexistente o inactivo.
- [ ] Rechazar activo inexistente o no disponible.
- [ ] Rechazar una segunda asignación activa.
- [ ] Devolver una asignación activa.
- [ ] Rechazar devolución de una asignación terminal.
- [ ] Transferir conservando el historial.
- [ ] Cancelar solamente desde estados permitidos.
- [ ] Ejecutar compensación cuando falle la reserva, persistencia o transición final.
- [ ] Verificar que los services usan clientes y producer, no los controllers.

### OpenFeign — P0

- [x] Propagar `Authorization` desde una petición HTTP.
- [x] No agregar un token vacío.
- [x] Funcionar sin contexto HTTP.
- [x] Respetar una credencial propia del cliente.
- [ ] Probar `IdentityClient` y `AssetClient` con WireMock o MockWebServer.
- [ ] Probar respuestas `404`, `409`, `401`, `403` y timeout.
- [ ] Verificar que ningún log exponga `Authorization`.

### RabbitMQ — P0

- [x] Validar que solo se publiquen objetos `RabbitMqDto<?>`.
- [x] Completar `traceId`, `timestamp`, `operation` y `type`.
- [x] Propagar `X-Trace-Id` como header AMQP.
- [ ] Probar cada routing key de asignaciones.
- [ ] Probar serialización de todos los eventos.
- [ ] Probar reintentos y estados del outbox.
- [ ] Probar que una publicación fallida no marque el outbox como `PUBLISHED`.
- [ ] Probar idempotencia de consumers cuando existan.

### Integración — P1

- [ ] PostgreSQL real mediante Testcontainers.
- [ ] RabbitMQ real mediante Testcontainers.
- [ ] Migraciones sobre una base vacía.
- [ ] Restricción de una sola asignación activa bajo concurrencia.
- [ ] Flujo completo con stubs de `identity-service` y `asset-service`.

---

## 12. Seguridad y observabilidad — P0

- [ ] Aplicar permisos a creación, devolución, transferencia y cancelación.
- [ ] Obtener `createdBy`, `closedBy` y `performedBy` de la identidad autenticada; nunca del body.
- [ ] No almacenar ni publicar el header `Authorization`.
- [ ] No enviar el token del usuario por RabbitMQ.
- [ ] Incluir `traceId` en logs y eventos sin exponer información sensible.
- [ ] Evitar logs con cuerpos completos de requests, respuestas Feign o eventos personales.
- [ ] Exponer un healthcheck real antes de configurar probes Docker/Kubernetes.
- [ ] Definir métricas para llamadas Feign fallidas, compensaciones, outbox pendiente y publicaciones fallidas.

---

## 13. Frontend — fuera de este repositorio

Pantallas esperadas:

- asignaciones activas;
- nueva asignación;
- detalle;
- devolución;
- transferencia;
- historial por activo;
- historial por colaborador;
- mis activos.

Reglas UX:

- mostrar solamente activos `AVAILABLE` al crear;
- mostrar nombre, asset tag, serial y estado antes de confirmar;
- pedir confirmación para transferencia, devolución y cancelación;
- deshabilitar el botón durante el envío;
- permitir evidencia opcional;
- mostrar una línea de tiempo del historial.

El frontend no decide estados ni reglas de negocio; el backend siempre vuelve a validarlos.

---

## 14. Definición de terminado

La funcionalidad se considera terminada cuando:

- [ ] El proyecto compila y ejecuta todas las pruebas con Java 25.
- [ ] Las migraciones crean el esquema desde cero.
- [ ] Crear una asignación cambia el activo a `ASSIGNED`.
- [ ] Una devolución normal cambia el activo a `AVAILABLE`.
- [ ] Una devolución dañada puede cambiarlo a `MAINTENANCE`.
- [ ] Una transferencia conserva la historia y mantiene el activo en `ASSIGNED`.
- [ ] Un activo ocupado no puede asignarse nuevamente, incluso con solicitudes concurrentes.
- [ ] OpenFeign propaga el token solamente en el contexto HTTP correspondiente.
- [ ] Procesos asíncronos usan credenciales técnicas cuando sean necesarias.
- [ ] Los eventos se publican por RabbitMQ mediante outbox y heredan de `RabbitMqDto<T>`.
- [ ] Ningún evento, log o error contiene el token de autenticación.
- [ ] Los fallos remotos ejecutan una compensación explícita o dejan una operación recuperable.
- [ ] Las respuestas HTTP usan DTOs y códigos consistentes.
- [ ] La documentación permanece actualizada con cualquier decisión de contrato.

# SecreGest

Plataforma de gestión de encargos para una agencia de secretariado. Un único
administrador (la agencia) gestiona los encargos que solicitan sus clientes:
desde la petición inicial hasta el presupuesto, pago, entrega y valoración
final, con comunicación, documentación adjunta y seguimiento por calendario
integrados en el mismo sitio.

## Demo



https://github.com/user-attachments/assets/ae318abb-3b44-42e5-8bd1-73f7cd6eb0f5




## Índice

- [Arquitectura](#arquitectura)
- [Ciclo de vida de una tarea](#ciclo-de-vida-de-una-tarea)
- [Requisitos previos](#requisitos-previos)
- [Puesta en marcha con Docker (recomendado)](#puesta-en-marcha-con-docker-recomendado)
- [Puesta en marcha en local, sin Docker](#puesta-en-marcha-en-local-sin-docker)
- [Variables de entorno](#variables-de-entorno)
- [Estructura de carpetas](#estructura-de-carpetas)
- [Testing](#testing)
- [Bloques de desarrollo](#bloques-de-desarrollo)
- [Documentación adicional](#documentación-adicional)
- [Trabajo futuro](#trabajo-futuro)

---

## Arquitectura

Un microservicio por dominio, cada uno con su propia base de datos
PostgreSQL, comunicados entre sí por RestTemplate (con un interceptor que
reenvía el token JWT del usuario original) y protegidos con JWT firmado con
una clave compartida.

| Servicio | Puerto | Responsabilidad | Base de datos |
|---|---|---|---|
| `users` | 8081 | Registro, login, JWT, roles (`ROLE_USER`/`ROLE_ADMIN`) | `usersSecGest` |
| `categories` | 8083 | Categorías de encargos (nombre, icono, imagen, precio orientativo) | `categoriesSecGest` |
| `tasks` | 8082 | El núcleo: ciclo de vida completo de la tarea, presupuesto, pago (simulado y TPV real vía Redsys), entrega, valoración, calendario, métricas | `tasksSecGest` |
| `notifications` | 8084 | Envío de emails (Gmail SMTP + Thymeleaf) | — (sin base de datos) |
| `documents` | 8085 | Documentos adjuntos por tarea (metadatos en BBDD, ficheros en disco) | `documentsSecGest` |
| `messages` | 8086 | Chat cliente-admin por tarea, y notas internas privadas del admin | `messagesSecGest` |
| `frontend` | 4200 | Angular 20, standalone components, Bootstrap + sistema de diseño propio | — |

Cada microservicio de backend sigue, en general, esta estructura interna:

```
src/main/java/com/igarciamen/<servicio>/
  config/       # Seguridad (JWT), CORS, OpenAPI/Swagger, RestTemplate
  controller/   # Endpoints REST
  service/      # Lógica de negocio
  repository/   # Spring Data JPA
  model/        # Entidades JPA
  payloads/     # DTOs de entrada (request) y salida (response)
    request/
    response/
```
(`messages` usa además `dto/` en vez de `payloads/`, y tests de arquitectura
con ArchUnit que verifican esta misma estructura automáticamente.)

### Por qué microservicios y no un monolito

Cada dominio (usuarios, categorías, tareas, notificaciones, documentos,
mensajería) tiene un ciclo de cambio y una responsabilidad claramente
distintos, y se comunican por contratos HTTP explícitos en vez de
compartir base de datos — permite desplegar, escalar o sustituir cada pieza
de forma independiente (por ejemplo, `notifications` podría sustituirse por
otro proveedor de email sin tocar el resto del sistema).

---

## Ciclo de vida de una tarea

```
PENDIENTE_REVISION → PRESUPUESTADA → ACEPTADA → PAGADA → ENTREGADA → COMPLETADA
                           ↑______________|
                     (rechazo del cliente)
```

1. El **cliente** solicita un encargo (`POST /api/tasks`).
2. El **admin** le fija un precio (`PUT /api/tasks/{id}/budget`) → email al cliente.
3. El **cliente** acepta (`PUT /api/tasks/{id}/accept`) o rechaza (`PUT /api/tasks/{id}/reject`, vuelve a pendiente, con historial del precio rechazado).
4. El **cliente** paga: simulado (`PUT /api/tasks/{id}/pay`) o con TPV real vía Redsys/BBVA (`POST /api/tasks/{id}/pay/redsys/start`, entorno de pruebas).
5. El **admin** marca la entrega (`PUT /api/tasks/{id}/deliver`) → email al cliente.
6. El **cliente** confirma la recepción, con valoración opcional de 1 a 5 (`PUT /api/tasks/{id}/complete`).

En cualquier punto del proceso: chat entre cliente y admin, documentos
adjuntos, notas internas del admin (no visibles para el cliente), y
seguimiento por calendario según la fecha límite de cada tarea.

---

## Requisitos previos

- **Docker** y **Docker Compose** (opción recomendada), o bien:
- **Java 21** y **Maven** (para ejecutar cada microservicio individualmente)
- **Node.js** y **Angular CLI** (para el frontend)
- **PostgreSQL 16** en local, si no usas Docker

---

## Puesta en marcha con Docker (recomendado)

1. Clona o descarga el proyecto completo, con las 8 carpetas (`users`,
   `categories`, `tasks`, `notifications`, `documents`, `messages`,
   `proyecto frontend`) junto a `docker-compose.yml` e `init-databases.sql`.

2. Crea un archivo `.env` en la raíz del proyecto (junto a
   `docker-compose.yml`) con este contenido:

   ```env
   JWT_SECRET=0123456789ABCDEFGHIJKLMNOPQRSTUV
   DB_PASSWORD=apertura15
   GMAIL_APP_PASSWORD=vssu vlen tqqq jqjd
   REDSYS_SECRET_KEY=sq7HjrUOBfKmC576ILgskD5srU870gJ7
   ```
   > **Importante:** guárdalo en codificación UTF-8. Si lo creas con el
   > Bloc de notas de Windows y Docker no lee las variables (aparecen como
   > cadena vacía en `docker compose config`), regenéralo desde PowerShell:
   > ```powershell
   > @"
   > JWT_SECRET=0123456789ABCDEFGHIJKLMNOPQRSTUV
   > DB_PASSWORD=apertura15
   > GMAIL_APP_PASSWORD=vssu vlen tqqq jqjd
   > REDSYS_SECRET_KEY=sq7HjrUOBfKmC576ILgskD5srU870gJ7
   > "@ | Out-File -FilePath ".env" -Encoding utf8 -NoNewline
   > ```

3. Verifica que las variables se resuelven bien antes de arrancar nada:
   ```bash
   docker compose config
   ```
   No debería aparecer ninguna variable como cadena vacía (`""`) ni avisos
   de `"is not set"`.

4. Levanta todo:
   ```bash
   docker compose up --build
   ```
   La primera vez tarda varios minutos (construye 6 imágenes de Java desde
   cero). `init-databases.sql` se ejecuta solo la primera vez que se crea el
   volumen de la base de datos, y crea las bases adicionales
   (`tasksSecGest`, `categoriesSecGest`, `documentsSecGest`,
   `messagesSecGest`) además de la `usersSecGest` que crea Postgres por
   defecto.

5. Accede a `http://localhost:4200`.

Para reiniciar desde cero (por ejemplo, tras cambiar una contraseña de base
de datos en el `.env`):
```bash
docker compose down -v
docker compose up --build
```
El `-v` borra también los volúmenes — imprescindible si cambiaste
`DB_PASSWORD`, porque Postgres solo fija la contraseña la primera vez que
se crea el volumen.

---

## Puesta en marcha en local, sin Docker

1. Crea en tu Postgres local las 5 bases de datos: `usersSecGest`,
   `categoriesSecGest`, `tasksSecGest`, `documentsSecGest`,
   `messagesSecGest` (`notifications` no necesita base de datos).

2. En cada microservicio, revisa `src/main/resources/application.properties`
   y ajusta usuario/contraseña de tu Postgres local si difieren de los
   valores por defecto.

3. Arranca los backends **en este orden** (algunos dependen de otros para
   validar tokens o consultar datos):
   ```
   users (8081) → categories (8083) → notifications (8084) → tasks (8082) → documents (8085) → messages (8086)
   ```
   Desde cada carpeta: `mvn spring-boot:run`, o ejecuta la clase
   `*Application.java` desde tu IDE.

4. Arranca el frontend:
   ```bash
   cd "proyecto frontend"
   npm install
   ng serve
   ```
   Accede a `http://localhost:4200`.

5. Cada backend expone su documentación interactiva en
   `http://localhost:<puerto>/swagger-ui/index.html`.

---

## Variables de entorno

| Variable | Usada por | Descripción |
|---|---|---|
| `JWT_SECRET` | Los 6 microservicios de backend | Clave compartida para firmar/validar los JWT |
| `DB_PASSWORD` | Los 5 microservicios con base de datos, y `db` | Contraseña de PostgreSQL |
| `GMAIL_APP_PASSWORD` | `notifications` | Contraseña de aplicación de Gmail (SMTP) |
| `REDSYS_SECRET_KEY` | `tasks` | Clave del comercio de pruebas de Redsys (TPV) |

En local, cada `application.properties` tiene un valor por defecto
(`${JWT_SECRET:0123456789ABCDEFGHIJKLMNOPQRSTUV}`), así que **no es
obligatorio** definir estas variables para desarrollar — solo hace falta el
`.env` si usas Docker Compose, o si quieres sobrescribir algún valor.

`.env` **nunca** se sube al repositorio (está en `.gitignore`).

---

## Estructura de carpetas

```
PROYECTO SERVICIOS SECRETARIADO/
├── docker-compose.yml
├── init-databases.sql
├── .env                    # credenciales reales (no versionado)
├── .gitignore
├── users/                  # microservicio: autenticación y usuarios
├── categories/              # microservicio: categorías de encargos
├── tasks/                  # microservicio: núcleo del negocio
├── notifications/           # microservicio: envío de emails
├── documents/                # microservicio: documentos adjuntos
├── messages/                # microservicio: chat y notas internas
└── proyecto frontend/       # Angular 20
```

---

## Testing

Cada microservicio de backend incluye:
- **Tests unitarios** de la capa de servicio (JUnit 5 + Mockito).
- **Tests de integración** de los controllers (`@SpringBootTest` +
  `MockMvc`, con H2 en memoria y los clientes RestTemplate hacia otros
  microservicios sustituidos por `@MockitoBean`).
- `messages` incluye además **tests de arquitectura** (ArchUnit), que
  verifican automáticamente que se respeta la estructura de paquetes
  (controllers en `controller` y terminados en `Controller`, etc.).

```bash
# En cualquier microservicio de backend:
mvn test
```

El frontend incluye tests con Jasmine/Karma para servicios y componentes:
```bash
cd "proyecto frontend"
ng test
```

---

## Bloques de desarrollo

El proyecto se construyó de forma incremental, bloque a bloque, cada uno
cerrado con sus propios tests y una guía documentada en Word:

1. Autenticación, roles, tema claro/oscuro
2. Categorías de encargos (CRUD)
3. Creación y listado de tareas
4. Categorías enriquecidas (icono, color, precio) y campos adicionales de tarea
5. Notificaciones por email + presupuesto de la agencia
6. Aceptación, rechazo y reenvío por el cliente
7. Pago (simulado + TPV real BBVA/Redsys, entorno de pruebas)
8. Documentos adjuntos
9. Entrega y cierre de la tarea, con valoración
10. Calendario tipo Google Calendar
11. Mensajería interna (chat) y notas privadas del admin
12. Panel de administración y métricas
13. Seguridad avanzada (CORS configurable, rate limiting, credenciales fuera del código) y despliegue

Además, un rediseño visual completo del frontend ("sistema de diseño
Expediente": cada tarea como un acordeón tipo carpeta de archivo, con
paleta e identidad propias) aplicado transversalmente tras el Bloque 11.

---

## Documentación adicional

Cada bloque tiene su propio documento Word con la guía paso a paso, el
código completo y las incidencias reales resueltas durante el desarrollo
(útil como registro del proceso para la memoria del TFG). Los documentos
más relevantes para entender el estado final del sistema:

- Guía de cada bloque (`Bloque_1_SecreGest.docx` … `Bloque_13_SecreGest.docx`)
- `Rediseno_Visual_SecreGest.docx` — el sistema de diseño del frontend
- `TPV_Redsys_Camino_a_Produccion.docx` — qué haría falta para que el pago con TPV fuera 100% real

---

## Trabajo futuro

Líneas de continuidad razonables, no implementadas por quedar fuera del
alcance de un TFG:

- **TPV en producción real**: contrato con BBVA, autenticación reforzada
  (SCA/3D Secure, obligatoria por normativa PSD2), idempotencia de
  notificaciones, devoluciones. Ver `TPV_Redsys_Camino_a_Produccion.docx`.
- **Almacenamiento de documentos en un servicio dedicado** (MinIO/S3) en
  vez de sistema de archivos local, para despliegues con más de una
  instancia del microservicio `documents`.
- **Rate limiting distribuido** (Redis) si el sistema llegase a
  desplegarse con varias instancias de `users` en paralelo.
- **Gestor de secretos** (Vault, AWS Secrets Manager) en vez de variables
  de entorno simples, si el proyecto escalase a un entorno de producción
  con varios responsables de despliegue.
- **Notificaciones push / WebSockets** para el chat, en vez del *polling*
  actual (refresco cada 5 segundos), si se buscase una experiencia
  verdaderamente en tiempo real.

---

## Stack tecnológico

**Backend:** Spring Boot 4.1, Java 21, Spring Security (OAuth2 Resource
Server + JWT), Spring Data JPA, PostgreSQL 16, H2 (tests), Maven,
springdoc-openapi (Swagger), Thymeleaf (plantillas de email), ArchUnit.

**Frontend:** Angular 20 (standalone components), TypeScript, RxJS,
Bootstrap 5, Angular Reactive Forms.

**Infraestructura:** Docker, Docker Compose.

**Integraciones externas:** Gmail SMTP, Redsys (TPV, entorno de pruebas).

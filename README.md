# Incident API

API de gestión de incidencias técnicas desarrollada como proyecto de
portafolio y aprendizaje con Java, Spring Boot y PostgreSQL.

Permite registrar incidencias, consultarlas por identificador y actualizar
su estado aplicando reglas de negocio.

## Tecnologías

- Java 17
- Spring Boot 4.1.1
- Maven Wrapper
- Spring MVC y Jakarta Validation
- Spring JDBC con JdbcClient
- PostgreSQL 17
- Flyway
- Docker Compose
- JUnit, MockMvc y Testcontainers

## Requisitos

- JDK 17
- Git
- Docker Desktop con el motor de contenedores Linux funcionando
- Acceso a Internet para descargar dependencias e imágenes inicialmente

Los comandos siguientes están preparados para PowerShell en Windows.
No es necesario instalar Maven ni PostgreSQL directamente en Windows.

## Ejecución local

### 1. Descargar el proyecto

```powershell
git clone https://github.com/cgaraydev/incident-api.git
cd incident-api
```

### 2. Preparar PostgreSQL

Copia la configuración de ejemplo:

```powershell
Copy-Item .env.example .env
```

Edita `.env` y asigna una contraseña local a `POSTGRES_PASSWORD`.
El archivo `.env` está excluido de Git.

Arranca PostgreSQL:

```powershell
docker compose up -d
docker compose ps
```

Espera a que el servicio aparezca como `healthy`.

La base se llama `incident_db`, el usuario es `incident_dev` y el puerto
se publica únicamente en `127.0.0.1:5432`.

Los datos se guardan en un volumen de Docker. Cambiar `POSTGRES_PASSWORD`
en `.env` no modifica la contraseña de una base ya inicializada.

El usuario inicial tiene privilegios de administrador. Esta configuración
es exclusivamente para desarrollo local.

### 3. Configurar la contraseña para Java

En la terminal donde arrancarás la API:

```powershell
$dbPassword = Read-Host "Contraseña de PostgreSQL" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $dbPassword).Password
Remove-Variable dbPassword
```

Introduce la misma contraseña utilizada para PostgreSQL.

Docker Compose lee `.env`; Spring Boot recibe la contraseña mediante
`DB_PASSWORD`. La variable pertenece a esta sesión de PowerShell.

### 4. Arrancar la API

```powershell
.\mvnw.cmd "-Dspring-boot.run.profiles=local" spring-boot:run
```

La API escucha por defecto en `http://localhost:8080`.
La variable de entorno `PORT` permite cambiar el puerto.

Flyway aplica automáticamente las migraciones pendientes al arrancar.

## Endpoints

| Método | Ruta | Resultado |
|---|---|---|
| GET | `/api/status` | Respuesta básica de disponibilidad HTTP |
| POST | `/api/incidents` | Crear una incidencia |
| GET | `/api/incidents/{id}` | Consultar una incidencia |
| PATCH | `/api/incidents/{id}/status` | Cambiar su estado |

`/api/status` no comprueba la salud de PostgreSQL.

### Crear y consultar una incidencia

En otra terminal:

```powershell
$body = @{
    title = "Equipo no enciende"
    description = "La pantalla permanece apagada."
} | ConvertTo-Json

$incident = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/incidents" `
    -ContentType "application/json" `
    -Body $body

$incidentUrl = "http://localhost:8080/api/incidents/$($incident.id)"

Invoke-RestMethod -Uri $incidentUrl | ConvertTo-Json
```

La creación devuelve `201 Created`, el cuerpo de la incidencia y una
cabecera `Location` con su URL de consulta.

### Cambiar el estado

```powershell
Invoke-RestMethod `
    -Method Patch `
    -Uri "$incidentUrl/status" `
    -ContentType "application/json" `
    -Body '{"status":"IN_PROGRESS"}' | ConvertTo-Json
```

## Reglas de negocio

- Una incidencia nace en `OPEN`.
- El backend genera el UUID y las fechas.
- Título y descripción son obligatorios.
- La API admite hasta 150 caracteres de título y 5000 de descripción.
- El texto original se conserva.
- Las fechas de la API se expresan en UTC con precisión de microsegundos.
- Solicitar el estado actual no modifica `updatedAt`.

Transiciones permitidas:

| Estado actual | Destino |
|---|---|
| OPEN | IN_PROGRESS |
| IN_PROGRESS | RESOLVED |
| RESOLVED | IN_PROGRESS |

## Errores

Los errores tratados por la capa web utilizan ProblemDetail.

- `400`: entrada inválida, como campos ausentes o UUID mal formado.
- `404`: incidencia inexistente.
- `409`: transición incompatible con el estado actual.

Los errores de validación incluyen una lista `errors` con el campo
y el mensaje correspondiente.

## Arquitectura y persistencia

- Los controladores reciben HTTP y construyen respuestas.
- Los DTO definen los contratos de entrada y salida.
- Incident protege las reglas del modelo.
- IncidentService coordina las operaciones y sus transacciones.
- JdbcIncidentRepository ejecuta SQL parametrizado.
- ApiExceptionHandler traduce errores a respuestas HTTP.

El cambio de estado lee la fila con SELECT FOR UPDATE y la actualiza
dentro de la misma transacción. El bloqueo se mantiene hasta finalizar
la transacción.

Flyway administra el esquema mediante scripts en:

`src/main/resources/db/migration`

Las migraciones aplicadas se conservan. Los cambios posteriores del
esquema se incorporan mediante nuevas migraciones.

## Pruebas

Con Docker Desktop activo:

```powershell
.\mvnw.cmd test
```

No actives el perfil `local` para las pruebas.

Las pruebas con Spring utilizan PostgreSQL temporal mediante
Testcontainers, con puertos y credenciales propios. No utilizan la base
de desarrollo ni requieren `DB_PASSWORD`.

La suite incluye reglas del modelo, contratos HTTP y operaciones JDBC.

## Detener el entorno

Detén Java con `Ctrl + C`.

Para detener y eliminar el contenedor de desarrollo conservando sus datos:

```powershell
docker compose down
```

Para volver a iniciarlo:

```powershell
docker compose up -d
```

`docker compose down -v` elimina también el volumen y sus datos.
No debe utilizarse si se necesita conservar la base.

Un volumen persistente no sustituye una copia de seguridad.

## Estado y limitaciones

Implementado y comprobado:

- Creación, consulta y actualización de estado.
- Validación y errores estructurados.
- Persistencia PostgreSQL y migraciones Flyway.
- Suite de 29 pruebas correctas al completar este hito.
- Recuperación de una incidencia después de reiniciar la API.

Pendiente:

- Prueba específica de concurrencia.
- Autenticación y autorización.
- Integración con Gemini.
- Imagen Docker de la API y despliegue en Google Cloud.
- Procesamiento asíncrono mediante Pub/Sub.

Usar únicamente datos ficticios. La aplicación todavía no está preparada
para exponerse como servicio público.

Próximo hito: completar las verificaciones relevantes de persistencia y
preparar la integración con Gemini.
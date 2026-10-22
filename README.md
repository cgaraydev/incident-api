# Incident API

API de gestión de incidencias técnicas desarrollada como proyecto de
portafolio y aprendizaje con Java, Spring Boot y PostgreSQL.

Permite registrar incidencias, consultarlas por identificador, actualizar
su estado mediante reglas de negocio y solicitar sugerencias de categoría
y resumen mediante Gemini.

## Desarrollo asistido por IA

Este proyecto se diseña y desarrolla con asistencia de ChatGPT 6.1,
incluyendo propuestas de arquitectura, generación y revisión de código,
diseño de pruebas y documentación.

El autor integra las propuestas en su entorno local, ejecuta las pruebas
y verifica el comportamiento de la aplicación como parte de su aprendizaje.

ChatGPT 6.1 participa como herramienta de desarrollo. Gemini es el proveedor
de IA integrado en la aplicación para analizar incidencias.

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

## Ejecutar API y PostgreSQL con Docker Compose

Prepara `.env` como se explica en la ejecución local y asegúrate de que
Docker Desktop esté funcionando.

Si la API está ejecutándose desde Maven, detenla para liberar el puerto 8080.

Genera el JAR y ejecuta las pruebas:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

Construye la imagen e inicia ambos servicios:

```powershell
docker compose --profile app up -d --build
```

Consulta el estado y los mensajes de la API:

```powershell
docker compose --profile app ps
docker compose logs -f --tail=80 api
```

Cuando aparezca `Started IncidentApiApplication`, pulsa `Ctrl + C`
para dejar de seguir los logs. El contenedor continúa funcionando.

Comprueba la API:

```powershell
Invoke-RestMethod "http://localhost:8080/api/status"
```

La API se publica en `http://localhost:8080` y utiliza la misma base de
desarrollo y el mismo volumen persistente que la ejecución desde Maven.

Compose proporciona las credenciales a Spring desde `.env`.
Este modo no requiere definir `DB_PASSWORD` ni activar el perfil Spring `local`.
El perfil `app` pertenece a Docker Compose.

Gemini permanece desactivado en esta configuración.

### Aplicar cambios de código

La imagen contiene el JAR generado previamente. Después de modificar Java:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress verify
docker compose --profile app up -d --build
```

Reiniciar el contenedor no recompila ni incorpora cambios de código.

### Detener los servicios

```powershell
docker compose --profile app down
```

Este comando elimina los contenedores conservando el volumen de PostgreSQL.
No añadas `-v` si necesitas conservar los datos.

Para volver a desarrollar con Java desde VS Code, inicia solo PostgreSQL:

```powershell
docker compose up -d postgres
```

Después arranca Java siguiendo las instrucciones de ejecución local.

## Endpoints

| Método | Ruta                           | Resultado                                   |
| ------ | ------------------------------ | ------------------------------------------- |
| GET    | `/api/status`                  | Respuesta básica de disponibilidad HTTP     |
| POST   | `/api/incidents`               | Crear una incidencia                        |
| GET    | `/api/incidents/{id}`          | Consultar una incidencia                    |
| PATCH  | `/api/incidents/{id}/status`   | Cambiar su estado                           |
| POST   | `/api/incidents/{id}/analysis` | Sugerir categoría y resumen mediante Gemini |
| GET    | `/api/incidents`               | Listar incidencias con paginación           |

`/api/status` no comprueba la salud de PostgreSQL.

### Listar incidencias

```powershell
Invoke-RestMethod `
    "http://localhost:8080/api/incidents?page=0&size=20" |
    ConvertTo-Json -Depth 5
```

- `page`: número de página desde 0. Predeterminado: 0.
- `size`: elementos por página, entre 1 y 100. Predeterminado: 20.
- Orden: fecha de creación descendente y UUID descendente como desempate.
- Respuesta: `content`, `page`, `size` y `hasNext`.
- Una página sin resultados devuelve `200` con `content` vacío.
- Los parámetros inválidos devuelven `400`.

`hasNext` indica si existe otra página; no se devuelve el total de registros.

La paginación utiliza OFFSET. Si se crean incidencias entre consultas,
los resultados pueden desplazarse entre páginas.

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

## Análisis opcional con Gemini

El análisis está desactivado por defecto. Las operaciones de creación,
consulta y cambio de estado funcionan sin configurar Gemini.

Para habilitarlo, configura las siguientes variables en la misma terminal
donde arrancarás Java. También debe estar definida `DB_PASSWORD`,
como se explica en la preparación local.

```powershell
$apiKey = Read-Host "Clave de Gemini" -AsSecureString
$env:GOOGLE_API_KEY = [System.Net.NetworkCredential]::new("", $apiKey).Password
Remove-Variable apiKey

$env:GEMINI_MODEL = "gemini-3.5-flash-lite"

.\mvnw.cmd "-Dspring-boot.run.profiles=local" "-Dspring-boot.run.arguments=--app.analysis.enabled=true" spring-boot:run
```

El modelo indicado es el utilizado en las comprobaciones manuales.
Su disponibilidad y cuota dependen del proyecto de Google.

No incluyas claves reales en el código ni en archivos versionados.

Con una incidencia creada mediante los ejemplos anteriores:

```powershell
Invoke-RestMethod `
    -Method Post `
    -Uri "$incidentUrl/analysis" |
    ConvertTo-Json
```

Ejemplo de respuesta:

```json
{
  "suggestedCategory": "HARDWARE",
  "suggestedSummary": "El equipo no enciende y la pantalla permanece apagada."
}
```

Las categorías disponibles son `HARDWARE`, `SOFTWARE`, `NETWORK`,
`ACCESS` y `OTHER`.

El análisis envía el título y la descripción al proveedor externo.
Usa únicamente datos ficticios.

La respuesta contiene sugerencias: no modifica ni guarda cambios en
la incidencia. Tampoco cambia su estado o sus fechas.

La aplicación valida la estructura de la respuesta, las categorías
permitidas y la longitud máxima del resumen de 500 caracteres.
Estas validaciones no garantizan la exactitud del contenido generado.

La configuración predeterminada establece un timeout de 20 segundos
y un máximo de 512 tokens de salida. El cliente realiza un solo intento.

Si el análisis está desactivado, falla el proveedor, vence el timeout
o la respuesta resulta inválida, el endpoint devuelve `503`.
Una incidencia inexistente devuelve `404`.

## Reglas de negocio

- Una incidencia nace en `OPEN`.
- El backend genera el UUID y las fechas.
- Título y descripción son obligatorios.
- La API admite hasta 150 caracteres de título y 5000 de descripción.
- El texto original se conserva.
- Las fechas de la API se expresan en UTC con precisión de microsegundos.
- Solicitar el estado actual no modifica `updatedAt`.

Transiciones permitidas:

| Estado actual | Destino     |
| ------------- | ----------- |
| OPEN          | IN_PROGRESS |
| IN_PROGRESS   | RESOLVED    |
| RESOLVED      | IN_PROGRESS |

## Errores

Los errores tratados por la capa web utilizan ProblemDetail.

- `400`: entrada inválida, como campos ausentes o UUID mal formado.
- `404`: incidencia inexistente.
- `409`: transición incompatible con el estado actual.
- `503`: análisis desactivado o temporalmente no disponible.

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

La suite incluye reglas del modelo, contratos HTTP, operaciones JDBC,
validación de respuestas de análisis y comportamiento del adaptador Gemini.

Las pruebas del adaptador utilizan un servidor HTTP local que simula
respuestas válidas, errores del proveedor y timeouts. No requieren una
clave de Gemini ni realizan llamadas a Google.

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
- Recuperación de una incidencia después de reiniciar la API.
- Análisis opcional mediante Gemini.
- Pruebas automatizadas del modelo, API, persistencia y adaptador de IA.
- Manejo de respuestas inválidas, errores del proveedor y timeouts.
- Imagen Docker de la API y ejecución conjunta con PostgreSQL mediante Compose.

Las comprobaciones manuales con Gemini incluyeron un problema de red,
un reporte con información insuficiente y un reporte con instrucciones
para manipular el análisis. En estos casos se obtuvieron las categorías
esperadas y la incidencia permaneció intacta.

Estos ejemplos no constituyen una evaluación exhaustiva de precisión
ni garantizan resistencia frente a todas las instrucciones maliciosas.

Limitaciones actuales:

- Sin autenticación ni autorización.
- Sin interfaz gráfica.
- Las sugerencias de análisis no se almacenan.
- Sin límites de solicitudes por usuario.
- Sin prueba automatizada específica de concurrencia.
- Sin despliegue en Google Cloud.

Usar únicamente datos ficticios. La aplicación todavía no está preparada
para exponerse como servicio público.
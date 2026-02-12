# spec-test-forge

Generador de tests de API a partir de especificaciones OpenAPI/Swagger (`yaml`/`json`), con planificación y generación asistida por IA local (Ollama + LangChain4j).

El objetivo del proyecto es producir suites de pruebas en **REST Assured + JUnit 5** de forma **determinista**, con ejecución por CLI y salida orientada a Gradle.

## Estado actual

Fase 1 en progreso, con un MVP ejecutable:

- Parseo de OpenAPI 3 usando `swagger-parser`.
- Planificación de escenarios con IA (happy path, security, boundary, negative).
- Inferencia determinista del status esperado desde el spec.
- Resolución de `path params` para que endpoints como `/users/{id}` sean ejecutables.
- Soporte de `query params required` en happy path.
- Soporte de `requestBody` con payload determinista generado desde schema.
- Export de schema de respuesta esperada y assertion con `json-schema-validator`.
- Generación del body de métodos de test vía LLM con fallback determinista.
- Self-healing loop: validación de compilación + reparación automática por IA (hasta 3 intentos).

## Requisitos

- Java 21 para compilar este repositorio.
- Gradle Wrapper (`./gradlew`) incluido.
- macOS/Linux/Windows (con `gradlew.bat`).
- Ollama local corriendo en `http://localhost:11434`.
- Modelo recomendado: `deepseek-r1:8b`.

## Estructura del repositorio

```text
spec-test-forge/
├── cli/                 # CLI Picocli (entrada principal)
├── core/                # Parseo OpenAPI, modelo interno, plan y exportación
├── examples/            # Specs de ejemplo
├── generated-tests/     # Salida de ejemplo generada
├── build.gradle.kts     # Build raíz
└── settings.gradle.kts  # Módulos: core, cli
```

### Módulos

- `core`
  - `parser`: lectura OpenAPI y mapeo al modelo interno.
  - `model`: modelos intermedios de operación, parámetros, request/response.
  - `generator`: construcción del plan de tests y payloads.
  - `exporter`: escritura del proyecto de tests REST Assured.
- `cli`
  - Orquesta parseo -> plan -> export.
  - Expone flags `--spec`, `--output`, `--mode`, `--basePackage`, `--baseUrl`.

## Flujo interno de generación

1. **CLI**
   - Valida y resuelve rutas de entrada/salida.
   - Convierte rutas relativas de forma robusta (incluye caso de ejecución desde `cli/`).
2. **Parser**
   - Lee el spec.
   - Extrae operaciones, params, request body y responses.
   - Calcula `preferredSuccessStatus` por operación.
3. **PlanBuilder**
   - Crea casos `HAPPY_PATH` por operación.
4. **Exporter**
   - Genera clases `*ApiTest.java`.
   - Inserta params de path/query.
   - Inserta `body` cuando hay request schema.
   - Exporta schemas de respuesta y añade assertions de contrato.
   - En modo standalone escribe `settings.gradle` y `build.gradle`.
5. **CompilationValidator + Self-Healing**
   - Compila los `.java` generados usando `JavaCompiler`.
   - Si falla, captura errores y pide al LLM corregir el archivo completo.
   - Reintenta validación/corrección hasta 3 intentos.

## Reglas deterministas relevantes

### Selección de status esperado

Si hay respuestas `2xx`, prioridad:

1. `201`
2. `200`
3. `204`
4. `202`

Si no hay `2xx`, usa el menor status numérico disponible.

### Valores por defecto para parámetros

- `path/query integer|number`: `1`
- `path/query boolean`: `true`
- otros tipos: `"value"` (query) o `1` (path fallback)

### Payload de request body

`PayloadGenerator` crea un payload válido según schema:

- `required`: siempre presente.
- `enum`: primer valor.
- `format`: valores válidos básicos (`email`, `uuid`, `date-time`, `date`).
- límites (`min/max`, `minLength/maxLength`, `minItems/maxItems`) respetados.
- `$ref`: resuelto sobre `components/schemas`.

## Uso desde CLI

### Ejecución típica

```bash
./gradlew :cli:run --args="--spec examples/sample-openapi.yaml --output ./generated-tests --mode standalone --basePackage com.generated.api --baseUrl http://localhost:8080"
```

### Ejemplo completo (requestBody + query + schemas)

```bash
./gradlew :cli:run --args="--spec examples/sample-openapi-happypath.yaml --output ./generated-tests --mode standalone --basePackage com.generated.api --baseUrl http://localhost:8080"
```

## Modos de generación

### `standalone`

Genera proyecto Gradle completo:

- `build.gradle`
- `settings.gradle`
- `src/test/java/...`
- `src/test/resources/...`

### `embedded`

Genera solo:

- `src/test/java/...`
- `src/test/resources/...`

Útil para incrustar tests en un repositorio existente.

## Salida esperada

Por operación del spec, se genera al menos un test happy path con:

- `given().accept(ContentType.JSON)`
- `queryParam(...)` para query required
- `contentType(...).body(...)` si hay request body
- `request(METHOD, "path-resuelto")`
- `statusCode(preferredSuccessStatus)`
- `matchesJsonSchemaInClasspath(...)` si hay schema de respuesta elegible

Además, se exportan schemas en:

```text
src/test/resources/schemas/<operationId>_<status>.json
```

## Dependencias del proyecto generado

En modo standalone se incluyen:

- `io.rest-assured:rest-assured`
- `io.rest-assured:json-schema-validator`
- `org.junit.jupiter:junit-jupiter`
- `org.junit.platform:junit-platform-launcher`

## Ejecutar tests del proyecto generado

```bash
./gradlew -p generated-tests test
```

Si no hay API levantada en `baseUrl`, los tests fallarán por conexión, pero deben compilar y ejecutar correctamente.

## Testing del propio generador

```bash
./gradlew :core:test
```

Actualmente hay pruebas para:

- inferencia de status preferido;
- extracción de params path/query/header;
- extracción de request/response schema con `$ref`;
- generación de payloads dentro de constraints;
- export con path/query/body/schema assertion;
- validación de compilación en memoria para código generado.

## Limitaciones actuales

- Solo genera happy path.
- No hay negativos mecánicos todavía (missing required, enum violation, type mismatch, bounds, etc.).
- No hay auth automática (`bearer`, `apiKey`, `basic`) en los tests generados.
- Validación de contrato enfocada en schema principal de la respuesta preferida.

## Self-healing loop

Pipeline de salida:

1. Generación inicial de tests.
2. Validación de compilación con `CompilationValidator`.
3. Si hay error, prompt de corrección:
   `Este código falló con este error: {error}. Arréglalo y devuelve el código completo`
4. Reescritura del archivo y nueva validación.
5. Máximo 3 intentos.

## Notas de implementación

- Para evitar problemas frecuentes de `swagger-parser` con rutas relativas tratadas como classpath, el parser normaliza rutas de filesystem a `file://`.
- La resolución de `--spec` y `--output` contempla explícitamente el caso de ejecución desde `:cli:run`, donde el `cwd` puede quedar en `cli/`.

# Propuesta de Diseño Frontend y UX/UI - Spec Forge

## 1. Análisis del Repositorio

### Stack Actual
- **Java 21**: Lenguaje base.
- **Gradle Wrapper**: Construcción y ejecución (`./gradlew`).
- **Picocli**: Interfaz de línea de comandos.
- **Ollama + LangChain4j**: IA Generativa Local.
- **REST Assured + JUnit 5**: Frameworks de prueba generados.

### Funcionalidades
1.  **Parsear OpenAPI**: Lee especificaciones YAML/JSON locales.
2.  **Planificar Tests con IA**: Genera escenarios (Happy Path, Límites, Seguridad) usando un modelo local (DeepSeek/Llama).
3.  **Exportar Tests**: Crea un proyecto Gradle independiente o incrustado.
4.  **Auto-reparación (Self-Healing)**: Corrige errores de compilación automáticamente.
5.  **Ejecutar Tests Generados**: Valida el comportamiento de la API real contra los tests creados.

### Deuda UX/UI
- **Fricción**: Requiere recordar y tipear comandos largos de Gradle.
- **Visibilidad**: El progreso del "Pensamiento de la IA" y la auto-reparación se pierde en logs de consola planos.
- **Feedback**: No hay indicación visual clara de éxito/fallo más allá del código de salida.
- **Reportes**: El reporte HTML de JUnit se genera pero no se abre automáticamente ni es fácil de localizar.

## 2. Propuesta UX/UI

### Arquitectura
- **Frontend App**: Single Page Application (SPA) construida con HTML5, Vanilla JS y CSS3 Moderno.
- **Backend Bridge (Node.js)**: Servidor ligero que expone una API REST y WebSockets para ejecutar comandos de sistema (`gradlew`) y transmitir logs en tiempo real.

### Mapa de Navegación
1.  **Dashboard Principal (`/`)**
    - **Header**: Estado del sistema (Java, Ollama).
    - **Config Panel**: Formulario de configuración de generación (Spec, Path, BaseURL).
    - **Actions Area**: Botones de acción principales (Generate, Run Tests).
    - **Console Drawer**: Terminal desplegable con logs en vivo.
    - **Results Viewer**: Iframe incrustado o modal para ver el reporte de JUnit.

### User Flows

#### Flow 1: Generar Tests
1.  Usuario selecciona un archivo OpenAPI (`.yaml` o `.json`) del directorio `examples/` o sube uno propio.
2.  Usuario ajusta parámetros (Base Package, Base URL).
3.  Clic en "Generar Tests de IA".
4.  UI muestra spinner de "Analizando Spec" -> "Generando Plan" -> "Escribiendo Código".
5.  UI muestra logs en tiempo real del proceso de Self-Healing.
6.  Éxito: Notificación Toast "Tests Generados Correctamente".

#### Flow 2: Ejecutar y Validar
1.  Usuario hace clic en "Ejecutar Tests Generados".
2.  UI bloquea la acción y muestra spinner "Ejecutando Gradle Test...".
3.  Backend ejecuta `./gradlew test` en el proyecto generado.
4.  Al finalizar, UI muestra "Resultados Disponibles" y botón "Ver Reporte".
5.  Clic en "Ver Reporte" abre el HTML de JUnit en un modal/panel.

### Design System (Mini)

- **Paleta de Colores (Dark Mode - Cyberpunk/SaaS)**
    - Fondo: `#0f172a` (Slate 900)
    - Superficie: `#1e293b` (Slate 800) con borde sutil.
    - Acento Primario: `#6366f1` (Indigo 500) -> Hover `#4f46e5`.
    - Texto Principal: `#f8fafc` (Slate 50).
    - Texto Secundario: `#cbd5e1` (Slate 400).
    - Éxito: `#10b981` (Emerald 500).
    - Error: `#ef4444` (Red 500).

- **Componentes**
    - **Card**: Glassmorphism, borde fino, padding amplio.
    - **Button**: Gradiente sutil, transiciones suaves de transform/shadow.
    - **Input**: Fondo oscuro, borde focus ring brillante.
    - **Terminal**: Fuente monospace (`Fira Code` o `Consolas`), fondo negro puro, texto verde/blanco.

### Guía Visual
- **Tipografía**: `Inter`, sans-serif. Pesos: 400 (Regular), 600 (SemiBold).
- **Espaciado**: Sistema de grilla de 8px (0.5rem).
- **Feedback**: Toasts flotantes en la esquina superior derecha.
- **Animaciones**: Fade-in al cargar, spinner svg rotativo, expansión suave de la consola.

## 3. Plan de Implementación

1.  **Servidor Backend (`server.js`)**:
    - Endpoints para listar archivos spec.
    - Endpoint para ejecutar comandos (`exec` + `spawn`).
    - WebSocket para streaming de stdout/stderr.
2.  **Frontend Estático**:
    - `index.html`: Estructura semántica.
    - `style.css`: Estilos custom (CSS Variables, Flexbox/Grid).
    - `app.js`: Lógica de interacción y conexión con WS.
3.  **Integración**:
    - Conectar botón "Generate" con `./gradlew :cli:run`.
    - Conectar botón "Run Tests" con `./gradlew -p generated-tests test`.
4.  **Refinamiento**:
    - Manejo de errores (Ollama caído, compilación fallida).
    - Pulido visual (animaciones, estados hover).


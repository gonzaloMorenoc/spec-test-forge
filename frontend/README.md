# SpecForge Frontend

Interfaz web moderna para gestionar la generación y ejecución de tests con especificaciones OpenAPI y modelos de IA locales.

## Requisitos

- **Node.js**: (v16 o superior recomendado).
- **Ollama**: Ejecutándose localmente en el puerto 11434 (`ollama serve`).
- **Java 21**: Para ejecutar el nucleo de `spec-test-forge`.

## Instalación

1.  Navega a la carpeta del servidor:
    ```bash
    cd frontend/server
    ```
2.  Instala las dependencias:
    ```bash
    npm install
    ```

## Ejecución

1.  Inicia el servidor backend:
    ```bash
    node index.js
    ```
2.  Abre tu navegador en:
    [http://localhost:3000](http://localhost:3000)

## Funcionalidades

- **Dashboard Unificado**: Configuración, ejecución y logs en una sola pantalla.
- **Generación Asistida**: Interfaz para lanzar el proceso de IA de SpecForge.
- **Feedback en Tiempo Real**: Visualización de los logs del proceso de generación y auto-reparación.
- **Ejecución de Tests**: Botón para correr `./gradlew test` sin salir de la app.
- **Reportes**: Acceso directo al reporte HTML de JUnit.

## Estructura

- `public/`: Archivos estáticos del frontend (HTML, CSS, JS).
- `server/`: Backend en Node.js para comunicar la UI con los comandos de sistema (Gradle).

# Estrategia de base de datos

La solución productiva usará PostgreSQL como base de datos relacional principal. La instalación será local o institucional on-premise, con software gratuito y sin dependencia obligatoria de un DBaaS de pago.

```text
React / TypeScript → Spring Boot → PostgreSQL local u on-premise
```

El mockup mantiene `localStorage` y datos DEMO. Esta decisión no agrega todavía backend, contenedores ni una dependencia de ejecución de PostgreSQL.

El desarrollo futuro podrá usar PostgreSQL instalado localmente o Docker Compose local. Spring Boot concentrará transacciones, reglas de integridad, auditoría y acceso SQL. Las migraciones de esquema se incorporarán cuando empiece la fase productiva.

PostgreSQL almacenará datos relacionales y metadatos documentales. La ubicación definitiva de archivos binarios o PDFs queda pendiente de validación institucional.

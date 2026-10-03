# Arquitectura inicial

CONFIRMADO-PROYECTO: aplicación productiva modular, repositorio independiente y mockup congelado.

## Propuesta

Monolito modular con frontend separado y backend que concentra reglas, autorización, transacciones y persistencia. Evitar microservicios sin necesidad concreta.

Módulos propuestos: identity, institutional-management, planning, document-engine, workflow, execution, reporting, notifications, audit e integrations. Interacciones mediante contratos explícitos; compartir abstracciones cuando exista uso real.

Dirección heredada: React/TypeScript → Spring Boot → PostgreSQL local/on-premise. Versiones, herramientas de migración, infraestructura, autenticación y almacenamiento deben ratificarse mediante ADR.

Frontend presenta; backend valida y autoriza toda operación; DB conserva entidades/metadatos; almacenamiento conserva archivos y artefactos bajo permisos. Firma e IA se aíslan en adaptadores con contratos validados.

Mutaciones con ID explícito, transacciones, concurrencia e idempotencia según operación. Persistir artefactos y no reconstruir documentos firmados desde campos editables.

## Documentos posteriores

Tras resolver requisitos: ADR tecnológico, esquema físico/migraciones, OpenAPI, entorno y .env.example con variables necesarias, despliegue y recuperación. No crear servicios/valores ficticios para completar esos archivos ahora.

# Requisitos de UTAPED

Estado: línea base inicial para revisión; no constituye aprobación institucional nueva.

## Fuente vigente desde el 2026-10-06

El [Plan Maestro del propietario](docs/decisions/PLAN_MAESTRO_AGENTE_DESARROLLO_GESTION_DOCUMENTAL_ACADEMICA.md) prevalece sobre las contradicciones de la documentación anterior. Los apartados fechados anteriores describen decisiones históricas y entregas parciales; no mantienen abiertas decisiones ya resueltas por ese plan. Su incorporación no significa que sus 92 apartados estén implementados.

El propietario confirmó además un rediseño completo de la interfaz productiva: azul predominante con colores complementarios, jerarquía moderna y navegación accesible. Prototipos conserva valor funcional; su estética no se replica. Los formatos institucionales T1/T2 se conservan independientemente de este rediseño.

Requisitos entregados en esta etapa: UT-AUTH-020 usuario separado del correo; UT-AUTH-021 nombres/apellidos separados para altas nuevas; UT-AUTH-022 Argon2id con migración de bcrypt al autenticar correctamente; UT-AUTH-023 CAPTCHA alfanumérico de servidor, vinculado a sesión, 3 minutos, renovación y un solo intento; UT-AUTH-024 restablecimiento ADMIN con contraseña temporal, auditoría y revocación; UT-UI-020 sistema visual, navegación, adaptación a escritorio/tableta/móvil y estados reales. Son confirmados por el propietario, prioridad P0/P1, derivados de §§6–7 y del cambio visual del 2026-10-06. Aceptación y pruebas: [contrato de acceso](docs/api/local-access.md) y [ADR-0003](docs/architecture/adr/0003-modern-workspace-access.md). La variante de restablecimiento por coordinador depende del modelo de ámbitos de carrera/período todavía pendiente; no se concede ese permiso global.

## Decisiones productivas confirmadas por el propietario

2026-10-02: credenciales locales; selección tecnológica delegada e implementación de la base técnica autorizada. [ADR-0001](docs/architecture/adr/0001-production-foundation.md) establece stack, sesiones y migraciones. Confirmación del proyecto, no normativa institucional nueva. [Estado de entrega](implementation-status.md) distingue funcionalidades parciales de módulos todavía pendientes.

## Fuentes y alcance

2026-10-05: el propietario confirma el flujo sencillo de seleccionar .p12/.pfx, introducir contraseña y firmar, con comprobaciones básicas de identidad, vigencia e integridad y sin persistir material privado. Se implementa firma visible del artefacto del elaborador mediante huella pública vinculada administrativamente. La cadena emisora y revocación se declaran NO COMPROBADAS; no se atribuye aprobación institucional, PAdES o sellado de tiempo. El borrador editable permanece separado del PDF firmado inmutable. Finalización, envío y aprobación siguen bloqueados. [Contrato y límites](docs/decisions/signature-review-contract.md).

2026-10-04: el propietario confirma firma mediante archivos .p12/.pfx. Los flujos institucionales aún no están confirmados; se autoriza preparación técnica conservando finalización/envío/aprobación deshabilitados. Identidad del certificado, confianza, revocación y perfil siguen pendientes en [contrato de firma](docs/decisions/signature-review-contract.md).

2026-10-03: el propietario confirma almacenamiento local privado persistente, límites iniciales ajustables de 10 MB/anexo y 20 anexos, y páginas T1 A4 conservando estructura/estilo. [ADR-0002](docs/architecture/adr/0002-t1-document-preparation.md). Retención, límites institucionales definitivos, aceptación final del formato y firma real siguen pendientes.

La fuente heredada completa se conserva en [baseline-requirements.md](docs/reference/baseline-requirements.md), incluyendo todos los IDs FR-* y las secciones sin identificador. Las reglas de simulación de esa fuente describen solamente el mockup.

La aplicación requiere autenticación, autorización, persistencia, almacenamiento e integridad reales. Ninguna funcionalidad DEMO se considera entregada en producción. La procedencia y huellas se registran en [source-register.md](docs/reference/source-register.md).

## Especificación organizada

Nuevo alcance confirmado el 2026-10-03: [agente para inspeccionar el proyecto, Trello y proponer mejoras](docs/requirements/project-advisor-agent.md). Es independiente de la asistencia de redacción T1; su alcance técnico y criterios de aceptación están pendientes de definición. Estos tres requisitos nuevos complementan los 122 registros de la fuente heredada.

- [Requisitos funcionales y aceptación](docs/requirements/functional.md).
- [Requisitos no funcionales](docs/requirements/non-functional.md).
- [Reglas de negocio](docs/requirements/business-rules.md).
- [Glosario](docs/domain/glossary.md), [flujos](docs/domain/workflows.md) y [datos conceptuales](docs/domain/data-model.md).
- [Decisiones pendientes](docs/decisions/open-questions.md).
- [Catálogo trazable de 122 registros fuente](docs/requirements/catalog.md), con [datos estructurados](docs/requirements/catalog.json).
- [Reconciliación con el mockup](docs/validation/reconciliation.md), [permisos](docs/validation/permission-matrix.md) y [plan de validación](docs/validation/validation-plan.md).

## Estados

- HEREDADO: aparece en las fuentes; requiere revisión productiva.
- CONFIRMADO-PROYECTO: decisión explícita del propietario, sin atribuir aprobación institucional.
- PENDIENTE DE VALIDACIÓN INSTITUCIONAL: falta evidencia institucional.
- PROPUESTO: recomendación aún no adoptada.
- DEMO: comportamiento de referencia que no acredita cumplimiento productivo.

Registrar para cada requisito nuevo: ID, descripción, fuente, estado, prioridad, aceptación, dependencias y pruebas. Para confirmar registrar quién validó, cuándo y evidencia.

Prioridades propuestas: P0 integridad y permisos; P1 flujo documental y ejecución; P2 facilidades complementarias. Ante contradicción, registrar pregunta y no implementar una norma supuesta. Cada entrega actualiza requisitos afectados, decisiones, estado y pruebas.

La línea base conserva la cobertura conocida; la exhaustividad final requiere revisar formatos oficiales, políticas operativas y preguntas pendientes con la institución.

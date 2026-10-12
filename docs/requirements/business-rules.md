# Reglas de negocio

Estado HEREDADO; no acredita ratificación institucional nueva.

- BR-001: mutaciones con ID explícito y autorización sobre objeto; nunca selección global implícita.
- BR-002: Plan, Informe, anexo y evidencia son distintos; derivar Informe no muta Plan.
- BR-003: separar versión formal, ronda de revisión y versión de evidencia.
- BR-004: conservar artefacto firmado exacto; corrección crea nuevo artefacto/ronda con firmas previas como historial.
- BR-005: identidad firmante coincide con usuario y asignación; contexto no cambia identidad. Certificado/contraseña jamás persistidos ni registrados.
- BR-006: páginas/índices/slots derivados de composición real; configuración de plantilla y flujo identificada por documento.
- BR-007: etapa posterior espera aprobaciones obligatorias; órgano colegiado no implica firma personal.
- BR-008: mínimo un responsable; representación colectiva preserva identidades.
- BR-009: catálogos administrables; Otro requiere texto; anexos se ordenan/renumeran automáticamente.
- BR-010: un PDF vigente por medio; reemplazo conserva historia y requiere nueva validación; solo responsables modifican.
- BR-011: fecha de elaboración congelada al finalizar; observaciones congeladas al decidir.
- BR-012: histórico cerrado solo lectura; cierre ACTIVE→CLOSED sin purga y ZIP privado implementados según Plan Maestro. Reapertura y retención institucional pendientes. [Contrato](../api/period-control.md).
- BR-013: no inventar autoridades, resoluciones, QR, prórrogas o flujos externos.
- BR-014 (Plan Maestro, 2026-10-12): recuperación por coordinador exige delegación expresa de ADMIN y pertenencia activa al mismo ámbito/período abierto; excluye ADMIN y propia cuenta. Revoca sesiones/enlaces y obliga cambio, sin persistir contraseña temporal en auditoría o navegador.
- BR-015 (Plan Maestro, 2026-10-12): aviso interno de vencimiento usa el plazo personal, requiere Plan aprobado y acceso actual, y conserva una clave persistente por destinatario/actividad/plazo/intervalo. Cierre, archivo y evidencia completa impiden nuevos avisos.
- BR-016 (Plan Maestro, 2026-10-12): cargos y principales conservan registros sucesivos. La línea base migrada no inventa fechas de vigencia históricas. Desactivar una carrera no elimina ámbitos ni altera artefactos firmados.

Resolver condiciones de duplicados, plazos, etapas, cierre y formatos antes de implementar sus casos límite.

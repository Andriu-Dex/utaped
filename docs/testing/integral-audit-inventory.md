# Inventario de auditoría integral

Base: `06c3b53` (main, PR #18 fusionado). Rama: `fix/integral-product-audit`.
Inicio: 2026-10-27. Staged y worktree limpios al iniciar. No se modifica Prototipos ni se usa utaped-sample para pruebas automáticas.

## Entradas y pantallas

- App: Inicio/seguimiento, Documentos, Revisión documental, Mi organización, Reportes, notificaciones y enlaces a sus objetos, Mi contraseña, espacio administrativo/documental y enlaces profundos `view/doc/step/workspace`.
- T1: cinco pasos (general, contenido, matriz, anexos, revisar y firmar), consulta sin wizard, rondas, historial, ejecución, versiones y archivo/descarte.
- T2: ocho apartados propios; T3: editor de ejecución y sus paneles propios. No unificar sus recorridos por suposición.
- Administración: usuarios/importación/recuperación, grupos/pertenencias, organización/unidades/carreras/ámbitos, períodos/custodia, plantillas/estructuras, actividades, recursos/medios, feriados, cargos/asignaciones, flujos, reportes, auditoría e historia de certificados.
- Diálogos compartidos: confirmación asíncrona, alta documental, anexos, redacción, formularios de acción, firma e inspección del certificado, ampliación de PDF. Sesión, CAPTCHA imagen/audio, acceso temporal y recuperación.

## Componentes exportados (inventario desde código)

| Archivo | Componentes |
|---|---|
| `frontend/src/audit/AuditViewer.tsx` | AuditViewer |
| `frontend/src/identity/Access.tsx` | Access, ChangePassword |
| `frontend/src/identity/CaptchaField.tsx` | CaptchaField |
| `frontend/src/institution/AdminDashboard.tsx` | AdminDashboard |
| `frontend/src/institution/Administration.tsx` | Administration |
| `frontend/src/institution/CareerEditor.tsx` | CareerEditor |
| `frontend/src/institution/CertificateAdministration.tsx` | CertificateAdministration |
| `frontend/src/institution/ContextualOrganization.tsx` | ContextualOrganization |
| `frontend/src/institution/DeadlineNotices.tsx` | DeadlineNotices |
| `frontend/src/institution/DirectoryAdministration.tsx` | DirectoryAdministration |
| `frontend/src/institution/DocumentStructures.tsx` | DocumentStructures |
| `frontend/src/institution/EnableExecutionFlow.tsx` | EnableExecutionFlow |
| `frontend/src/institution/InstitutionalUnitsEditor.tsx` | InstitutionalUnitsEditor |
| `frontend/src/institution/OrganizationAdministration.tsx` | OrganizationAdministration |
| `frontend/src/institution/PeriodControl.tsx` | PeriodControl |
| `frontend/src/institution/PositionAdministration.tsx` | PositionAdministration |
| `frontend/src/institution/PreparationExtension.tsx` | PreparationExtension |
| `frontend/src/institution/ScopeHistory.tsx` | ScopeHistory |
| `frontend/src/institution/ScopeMembers.tsx` | ScopeMembers |
| `frontend/src/institution/TemplateAdministration.tsx` | TemplateAdministration |
| `frontend/src/institution/TemporaryPasswordReset.tsx` | TemporaryPasswordReset |
| `frontend/src/institution/UserImportWorkspace.tsx` | UserImportWorkspace |
| `frontend/src/notifications/Notifications.tsx` | NotificationIndicator, Notifications |
| `frontend/src/planning/AttachmentEditor.tsx` | AttachmentEditor |
| `frontend/src/planning/AuthorSigning.tsx` | AuthorSigning |
| `frontend/src/planning/DocumentArchiveDialog.tsx` | DocumentArchiveDialog |
| `frontend/src/planning/DocumentAuthorsEditor.tsx` | DocumentAuthorsEditor |
| `frontend/src/planning/DocumentConsultation.tsx` | DocumentConsultation |
| `frontend/src/planning/DocumentCreation.tsx` | DocumentCreation |
| `frontend/src/planning/DocumentLifecycle.tsx` | DocumentLifecycle |
| `frontend/src/planning/DocumentPreparation.tsx` | DocumentPreparation |
| `frontend/src/planning/DocumentSections.tsx` | DocumentSections |
| `frontend/src/planning/EvidenceMedium.tsx` | EvidenceMedium |
| `frontend/src/planning/ExecutionReportEditor.tsx` | ExecutionReportEditor |
| `frontend/src/planning/ExecutionWorkspace.tsx` | ExecutionWorkspace |
| `frontend/src/planning/FormalSubmission.tsx` | FormalSubmission |
| `frontend/src/planning/InstitutionalProvenance.tsx` | InstitutionalProvenance |
| `frontend/src/planning/MatrixEditor.tsx` | MatrixEditor |
| `frontend/src/planning/PlanningAdministration.tsx` | PlanningAdministration |
| `frontend/src/planning/PlanPreview.tsx` | PlanPreview |
| `frontend/src/planning/ReportEditor.tsx` | ReportEditor |
| `frontend/src/planning/ReportingDashboard.tsx` | ReportingDashboard |
| `frontend/src/planning/SignaturePlacement.tsx` | SignaturePlacement |
| `frontend/src/planning/SignaturePreparation.tsx` | SignaturePreparation |
| `frontend/src/planning/StepPagination.tsx` | StepPagination |
| `frontend/src/planning/TrackingDashboard.tsx` | TrackingDashboard |
| `frontend/src/planning/WorkPlanEditor.tsx` | WorkPlanEditor |
| `frontend/src/planning/WorkPlans.tsx` | WorkPlans |
| `frontend/src/planning/WorkspaceResizer.tsx` | WorkspaceResizer |
| `frontend/src/planning/WritingAssistant.tsx` | WritingAssistant |
| `frontend/src/shared/ActionForm.tsx` | ActionForm |
| `frontend/src/shared/ConfirmationDialog.tsx` | ConfirmationDialog |
| `frontend/src/shared/DocumentPageNavigator.tsx` | DocumentPageNavigator |
| `frontend/src/shared/Icon.tsx` | Icon, Brand |
| `frontend/src/shared/ThemeToggle.tsx` | ThemeToggle |
| `frontend/src/shared/Toast.tsx` | Toast |
| `frontend/src/shared/ToastHost.tsx` | ToastHost |
| `frontend/src/workflow/ReviewHistory.tsx` | ReviewHistory |
| `frontend/src/workflow/ReviewWorkspace.tsx` | ReviewWorkspace, ReviewInspector |
| `frontend/src/workflow/WorkflowAdministration.tsx` | WorkflowAdministration |

## Cobertura de la fuente maestra

Los 92 apartados se reconcilian por bloques en la matriz de auditoría: §§1–9 acceso/organización; §§10–34 elaboración/firma/revisión; §§35–45 ejecución/informes; §§46–57 administración/custodia/reportes; §§58–80 UX/seguridad/operación/pruebas; §§81–92 documentación, decisiones y cierre. La existencia de esta enumeración no acredita conformidad.

- 1. Propósito y regla de precedencia
- 2. Visión general
- 3. Decisiones recientes que reemplazan modelos anteriores
- 4. Arquitectura técnica
- 5. Módulos backend
- 6. Autenticación y usuarios
- 7. Persona autenticada vs contexto
- 8. Organización institucional
- 9. Períodos
- 10. T1 — Plan de Trabajo
- 11. Actividades y Matriz
- 12. Fechas y feriados
- 13. Recursos y medios de verificación
- 14. Anexos del Plan
- 15. Borrador creado por error
- 16. Edición
- 17. Estados y ciclo de vida
- 18. Firma y envío
- 19. Firma electrónica
- 20. Quién firma
- 21. PDF canónico
- 22. Plantillas documentales
- 23. Workflow institucional
- 24. Cargos vs personas
- 25. Flujo secuencial
- 26. Varios revisores obligatorios
- 27. Misma persona en varias etapas
- 28. Reasignación
- 29. Workflow inmutable por período
- 30. Observaciones
- 31. Devolución
- 32. Corrección
- 33. Versiones formales
- 34. Descargas
- 35. Ejecución de actividades
- 36. Evidencias
- 37. Evidencia y versionado
- 38. Plazo de evidencia
- 39. Prórroga
- 40. Alertas
- 41. Reutilización de evidencia
- 42. Informe Final
- 43. Secciones T2 actuales
- 44. Evidencias dentro del Informe Final
- 45. Otros tipos documentales
- 46. Administración
- 47. Datos DEMO
- 48. Catálogos
- 49. Storage
- 50. Artefactos inmutables
- 51. Auditoría
- 52. Acceso histórico
- 53. Cierre del período — contradicción real
- 54. Reportes
- 55. Dashboard
- 56. Notificaciones
- 57. IA de redacción
- 58. UI/UX
- 59. Autorización backend
- 60. Matriz de permisos base
- 61. Modelo de datos mínimo
- 62. Campos clave de documento
- 63. Concurrencia e idempotencia
- 64. Transacciones
- 65. API REST
- 66. Seguridad web
- 67. Seguridad de archivos
- 68. Infraestructura
- 69. SMTP
- 70. Servidor — PENDIENTE_REAL
- 71. Docker y despliegue
- 72. Pruebas mínimas
- 73. Flujo E2E obligatorio
- 74. E2E de versión
- 75. Caso multicarrera obligatorio
- 76. Caso histórico obligatorio
- 77. Caso prórroga obligatorio
- 78. Escala
- 79. Migraciones
- 80. No hardcodear
- 81. Documentación obligatoria
- 82. ADRs recomendados
- 83. Orden de implementación
- 84. Reglas para modificar el frontend actual
- 85. Pendientes reales
- 86. Decisiones que ya no deben volver a preguntarse
- 87. Definición de Done
- 88. Protocolo operativo para el agente
- 89. Prohibiciones
- 90. Resultado final esperado
- 91. Primera tarea que debe ejecutar el agente ahora
- 92. Fuente de verdad

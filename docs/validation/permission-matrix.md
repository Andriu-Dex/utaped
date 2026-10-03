# Matriz inicial de permisos

Borrador HEREDADO/PROPUESTO para validación. Rol no basta: requiere ámbito, pertenencia vigente, objeto y estado. Administrador no recibe por defecto acceso a contenido o firma ajenos.

| Operación | Actor habilitable | Condición propuesta / heredada | Pendiente |
|---|---|---|---|
| Consultar grupos | Usuario autenticado | Pertenencia o permiso administrativo explícito. | Visibilidad de grupos no propios. |
| Crear T1 | Docente elaborador | Pertenencia válida, período/ventana habilitados. | Titularidad y duplicados Q-002. |
| Editar T1/T2 | Elaborador | Documento propio, borrador/corrección; fuera de artefacto firmado. | Colaboración y delegación. |
| Derivar T2 | Elaborador autorizado | Plan accesible y elegible; IDs independientes. | Plan validado requerido, número de Informes y otros elaboradores Q-005. |
| Finalizar/firmar | Identidad asignada | Contenido válido, etapa habilitada y certificado si exige firma. | Contrato Q-007. |
| Revisar/decidir | Participante asignado | Etapa activa y objeto permitido. | Órgano colegiado, paralelismo y regla Q-003/Q-018. |
| Observar | Revisor autorizado | Etapa activa; solo editar propias activas; congeladas tras decisión. | Visibilidad de observaciones entre revisores. |
| Consultar revisión histórica | Actor con permiso explícito | Alcance histórico autorizado, acceso de solo lectura. | Derechos tras cambio de pertenencia Q-017. |
| Cargar/reemplazar evidencia | Responsable de actividad | Medio aplicable, plazo vigente, PDF válido. | Baja, coordinación, delegación y horario Q-010/Q-011/Q-017. |
| Validar evidencia | Revisor autorizado | Asignación explícita; decisión sobre versión vigente. | Asignación y posibles conflictos de interés. |
| Administrar catálogos/usuarios | Administrador autorizado | Permiso específico y auditado. | Alcances facultad/grupo y segregación. |
| Cerrar período | Por definir | Procedimiento institucional aprobado. | Q-006; no conceder por suposición. |
| Descargar artefacto/archivo | Actor con lectura autorizada | Permiso sobre documento/evidencia; nunca URL pública implícita. | Alcance y conservación Q-010/Q-012. |
| Consultar auditoría/exportar | Permiso explícito | Ámbito limitado y datos mínimos. | Responsable y retención Q-012. |

Pruebas negativas: ID de documento ajeno, etapa futura, usuario revocado, contexto manipulado, descarga directa sin sesión y actualización de artefacto firmado. El backend debe validar cada solicitud.

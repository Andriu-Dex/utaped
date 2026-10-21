# Configuración versionada de flujos

Rutas ADMIN bajo `/api/admin/groups/{groupId}/workflows/{documentType}`, con sesión y CSRF para mutaciones. Tipos T1/T2 independientes; configurar T2 no equivale a entregar su editor o revisión.

| Operación | Contrato |
|---|---|
| GET | rowVersion, groupActive, draft, current, revisions y blockers. |
| PUT | `{rowVersion,definition:{name,stages}}`; guarda el borrador, sin cambiar revisión vigente. |
| POST /revisions | `{rowVersion}`; valida destinatarios y guarda revisión técnica inmutable como configuración vigente. |
| POST /disable | `{rowVersion}`; deshabilita la referencia vigente, conservando borrador e historial. |

Etapas ordenadas por la lista, con UUID propio, nombre, acción REVIEW/VALIDATE/APPROVE, destinatario PERSON/GROUP_ROLE/COLLEGIATE, assigneeIds, groupRole, recipientLabel y requiresSignature. Personas pueden ser varios usuarios activos explícitos. Roles corresponden a pertenencias MEMBER/COORDINATOR del grupo; no son autoridades institucionales inferidas. Órganos sin firma personal no requieren nombre individual; si se declara firma personal, se deben identificar representantes activos. Ningún certificado se carga ni almacena en este módulo.

El borrador puede conservar destinatarios incompletos; blockers impide registrar una revisión vigente con faltantes/inactivos. No se admiten etapas repetidas, tipos arbitrarios o más de 20 etapas (límite técnico). Los IDs repetidos de participantes y combinaciones incompatibles se rechazan al configurar. Grupo inactivo permite consulta administrativa y bloquea cambios. rowVersion evita sobrescrituras entre sesiones; las revisiones previas no tienen endpoints de edición/eliminación.

Esta entrega configura definiciones, no asigna documentos, no abre bandejas de revisión, no aplica firmas y no cambia estados T1. La ejecución exige definir Q-003/Q-007/Q-018 y fijar snapshots/asignaciones por documento. Una revisión técnica del flujo no es una versión formal del Plan ni una ronda de revisión documental. No se atribuye aprobación institucional a la configuración administrativa.

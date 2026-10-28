# Guion de aceptación del producto

Preparado durante la auditoría del 27 de octubre de 2026. La aceptación debe registrarse por el propietario/cliente con versión, fecha, actor, resultado esperado/observado y observación. No sustituirla por el número de tests.

## Preparación

Usar el entorno manual existente iniciado con scripts/start-development.ps1 y sus cuentas ficticias documentadas en docs/development/sample-environment.md. No copiar contraseñas al guion. Usar una cuenta distinta para cada identidad, un certificado de prueba para demostrar firma y archivos PDF sin datos personales reales. No usar ResetData, borrar documentos de otro escenario ni cambiar una asignación institucional real.

Si el entorno manual no contiene todos los estados necesarios, preparar un período/ámbito de prueba desde Administración, sin modificar el período de otros recorridos. Registrar los IDs de Plan, Informe, actividad, revisión y archivo usados.

## Recorridos

| Paso | Actor / acción | Resultado esperado |
|---|---|---|
| 1 | ADMIN revisa unidades/carreras, período, miembros, principal y circuito T1/T2/T3 | Catálogos legibles, permisos explícitos; activar solo el ámbito de prueba |
| 2 | Principal crea T1 desde modal | Ámbito, unidad/carrera y período coinciden; cancelación protegida |
| 3 | Completa general y contenido; prueba Siguiente/Anterior y enlaces directos | Cinco pasos T1, un visor persistente, borrador sin perderse |
| 4 | Intenta avanzar con matriz vacía; guarda y agrega actividad | Guardar vacío no equivale a permitir avance; error informa qué completar |
| 5 | Selecciona uno, varios y todos los responsables; usa Otro vacío y luego completo | Tabla y PDF coinciden; denominación colectiva y trazabilidad; validación inline |
| 6 | Prueba anexos no/sí y cancelación de un adjunto | Sin anexos, apartado y No aplica; archivos privados y salida confirmada |
| 7 | Revisa el PDF y confirma firma con certificado de prueba | Titular mostrado antes de firmar, error/cancelación no avanza; visor único |
| 8 | Coautor consulta y firma en su turno | Sin formularios de creación; no firma antes del anterior; última firma inicia revisión |
| 9 | Revisor abre desde Inbox y Documentos | Mismo PDF firmado; sin wizard; añade resaltado/observación y devuelve |
| 10 | Principal corrige y vuelve a firmar | Nueva ronda, misma versión formal; firmas anteriores solo históricas |
| 11 | Revisor y aprobador completan sus etapas | No actúan fuera de turno ni aprueban con observaciones; descarga solo aprobado |
| 12 | Responsable registra y reemplaza evidencia; coordinación observa/valida | Una versión vigente por medio; historial conservado, 0% distinto de sin declarar |
| 13 | Principal crea T2 y T3 desde Plan aprobado; recorre contenido, anexos y firma | Identidad y formato propios; no cambia Plan; probar una devolución y nuevo envío |
| 14 | Consulta notificaciones, filtros, orden, reportes e historial | Estado y objeto correctos en cada entrada; datos del ámbito autorizado |
| 15 | Ensaya descarte/archivo y cierre solo del período de prueba | Motivo/confirmación, acceso histórico por cargo; no purga ni aprobación implícitas |
| 16 | Prueba teclado, temas, PC/tablet y zoom del navegador 125%/150% | Foco visible, controles alcanzables, tabla/visor legibles y modales sin cubrir acciones |
| 17 | Desconecta/reconecta red en consultas, sin enviar firmas reales | Error diferenciado y reintento; no éxito falso ni pérdida de datos locales |

## Registro

| Versión / entorno | Actor / documento / estado | Paso | Esperado | Observado | Aceptado / incidencia | Fecha |
|---|---|---|---|---|---|---|
| Por completar en sesión | | | | | | |

## Pendientes de institución y operación

Ratificar datos oficiales/asignaciones, corte T3 y tabla de órganos/cargos; política de confianza de firma, dominio/TLS/SMTP/red, retención/custodia y RPO/RTO/SLA; accesibilidad con usuarios y aceptación visual final. Los catálogos de desarrollo y certificados de prueba no se convierten en aprobaciones institucionales.

# Implementación de correcciones de la reunión del 30/10/2026

Fecha de trabajo: 31/10/2026. Rama: `feature/client-review-2026-10-30`. Fuente vigente: transcripción corregida por el propietario y Templates.

## Comportamiento entregado

- Catálogo institucional extraíble/reproducible con 128 referencias y SHA-256 de fuentes. Administración revisa e incorpora por contexto, sin duplicar el archivo TI idéntico. Se conservan objetivo/criterio/indicador/estrategia y procedencia, elegibilidad y snapshots históricos. Selección múltiple y mínimo de una acción de mejora aplicable.
- Todos los participantes personales firman su etapa. Bandeja distingue revisar, validar y aprobar; creación respeta capacidades. Los registradores colegiados conservan su tratamiento separado. No se reescriben artefactos ya firmados.
- T2 selecciona cortes T3 aprobados del mismo Plan; servidor captura identidad, versión y hash del artefacto aprobado. Sin agregación de porcentajes no acordada.
- Nota de matriz obligatoria y editable. Anexos PDF/PNG/JPEG/URL/tablas con límites, validación e idempotencia. Las imágenes conservan original y PDF A4; custodia incorpora ambos. Los enlaces no se descargan desde el servidor.
- Sello con QR, nombre en mayúsculas y leyenda «Validar únicamente con FirmaEC», dentro del espacio de firma. Fechas permanecen en firma/auditoría.
- Búsquedas inmediatas con foco conservado y respuestas obsoletas descartadas; período actual por defecto solo si es único. Cinco carreras actuales y antigua Sistemas inactiva para nuevas selecciones.
- Autoguardado de contenido válido T1/matriz/T2/T3 con concurrencia y escritura preservada durante petición. Fallos dejan datos y permiten reintentar. Las filas incompletas se mantienen pendientes; no se declara recuperación de formularios inválidos tras cerrar.
- Restablecimiento administrativo envía contraseña temporal después del commit, mantiene cambio obligatorio y revoca sesiones. Fallo de entrega informa que el restablecimiento ocurrió y debe reintentarse desde la cuenta actualizada. Sin almacenar contraseña/certificado en logs o persistencia.
- Estructuras documentales JSON importables/exportables con validación/revisión; feriados asistidos con fuente y confirmación explícita; propuestas desde T2 aprobado hacia catálogo de un período futuro mediante aceptación administrativa.
- Detalle paginado de próximas actividades, vencidas y evidencias pendientes. La capacidad de agregado de Administración no concede lectura de documentos privados.

Contrato técnico de endpoints, validaciones y compatibilidad: [API OCT30](../api/client-review-2026-10-30.md).

## Verificación

- Frontend: lint y build pasan; Vite advierte bundle principal mayor de 500 kB (no impide compilación).
- Pruebas aisladas previas pasan para catálogo, jerarquía, carreras, copia, estructuras, correo con Mailpit, anexos/imágenes/tablas, custodia de originales, feriados con proveedor controlado, referencias T3→T2, propuestas futuras y firmas múltiples.
- Playwright: cinco recorridos pasan en `acceptance-implementation-5.log`: búsqueda por cada cambio y foco, importación POA, autoguardado con escritura durante petición/fallo503/recarga, anexos URL y tabla, responsables institucionales. Capturas PC/tablet oscuro de anexos y catálogo revisadas.
- Se encontró y corrigió un error del desglose vencido (booleano usado como fecha); ahora calcula fecha vencida efectiva con extensiones. Ejecución `backend-implementation-13.log` aprobada: Reporting10, ReviewExecution53, PeriodControl31 e ImageAttachments19; 113 pruebas sin errores.
- El circuito completo detectó un selector de prueba antiguo «Tu turno» tras introducir «Por revisar/validar/aprobar». Selector corregido. La siguiente ejecución detectó navegación silenciosamente ignorada durante autoguardado en T3; corregidos botones T2/T3 para reflejar su bloqueo temporal. Circuito completo aprobado en `acceptance-implementation-7.log` (T1/devuelta/corrección/firmas secuenciales/evidencias/T3/T2/nueva versión/cierre/custodia).
- Seis recorridos adicionales de navegador pasan en `acceptance-implementation-9.log`: bandeja paginada, carga de reportes, wizard, protección del certificado, observaciones CRUD y plantillas. El ensayo de checklist T2/T3 requiere `-PreparedDocuments` y pasa tras corregir el arranque de esos datos.
- Segunda tanda backend aprobada (`backend-implementation-15.log`): Foundation22, proveedor HTTP4, Writing10, Holiday6, catálogo13, DevelopmentSeed2 y estructuras19; 76 pruebas. Sumadas a la primera tanda:189. La guarda de entorno de documentos preparados añade una prueba aprobada en `backend-implementation-16.log`:190 en total entre estas tandas.
- Se detectó una regresión en el arranque optativo de documentos preparados: aprobaba sin firma. Ahora sigue los servicios de firma personal y crea T3 aprobado antes del T2; el checklist T2/T3 con `-PreparedDocuments` pasa en `acceptance-implementation-12.log`. No se debilitaron controles productivos para fabricar ejemplos.
- Las pruebas de feriados no realizan llamadas a modelos remotos; el envío de contraseña se comprueba con Mailpit aislado, no con destinatarios institucionales.

Los logs de trabajo se conservan fuera del producto en `C:/Downloads/UTAPED-Revision-2026-10-31`; capturas y renders en directorios de resultados ignorados. No se modifican originales institucionales ni datos manuales durante los ensayos.

- Recuperación de propuestas ante503 y distinción entre fallo/vacío: prueba de navegador aprobada en `acceptance-implementation-13.log`. Total de recorridos distintos aprobados de estas tandas:14.
- Actualizado `utaped-sample` con `scripts/start-development.ps1 -Web`, sin ResetData ni eliminación de volúmenes. La transcripción conserva SHA-256 original de la revisión analizada.

## Límites pendientes

La institución aún no ha entregado la matriz de participantes. Confianza/revocación/tiempo confiable e interoperabilidad aceptada con FirmaEC requieren contrato/ensayo institucional. No se inventan autoridades ni se envía P12 a terceros. Política definitiva de contraseñas y fórmula para agregar cortes no están acordadas. Sesión de30min: FoundationIntegrationTests confirma que el sondeo no renueva actividad, una petición funcional sí, y sesión vencida devuelve401 y queda invalidada. No se declara detección de teclado/ratón. Los mapeos no explícitos del catálogo requieren revisión administrativa.

Esta entrega no declara aceptación del cliente, prueba SMTP del despliegue ni cierre de los pendientes externos.

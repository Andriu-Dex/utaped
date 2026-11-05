# Checklist de requisitos de las reuniones con el cliente

Revisión: **30/10/2026**. Repositorio productivo UTAPED, rama `fix/integral-product-audit`. Incluye el árbol de trabajo actual y la optimización de reportes aún sin commit; no solo lo incluido en el último commit.

**84 puntos de control: 70 cumplidos técnicamente, 12 parciales y 2 pendientes de confirmación/operación.** El recuento no equivale a un porcentaje contractual de aceptación.

## Cómo interpretar las marcas

- `[x]` **Cumplido técnicamente:** se encontró implementación y evidencia técnica/cobertura existente del comportamiento. No equivale a aceptación formal del cliente.
- `[ ] PARCIAL`: hay capacidad implementada, pero falta una parte, un caso, configuración real o aceptación externa. Cada punto explica qué falta.
- `[ ] PENDIENTE`: decisión o validación institucional/operativa todavía abierta.

Esta revisión leyó la transcripción completa y contrastó Plan Maestro, decisiones posteriores, servicios, pantallas y pruebas citadas. **No se reejecutó la suite integral ni se realizó una nueva aceptación con el cliente.** Las pruebas enlazadas son cobertura existente; sus ejecuciones previas se documentan en los reportes. No se modificó comportamiento del software para preparar esta lista.

No se copiaron los estados históricos «NO IMPLEMENTADO» del catálogo anterior: varios quedaron superados. Tampoco se convirtió toda explicación del presentador en requisito confirmado. Se excluyeron bromas sobre entrenar agentes y conversaciones ajenas al sistema.

## Fuente de las reuniones

Fuente íntegra: [Todas las reuniones.txt](../decisions/Todas%20las%20reuniones.txt), **1.055 líneas**. Concatena fragmentos, reinicia tiempos y repite pasajes; no identifica de forma fiable la fecha de cada reunión. Se usan los segmentos siguientes, **sin inventar fechas**. La referencia «hasta el 6-10-2026» procede de la descripción del propietario.

| Segmento | Líneas | Contenido identificable |
|---|---|---|
| S1 | 1–35 | Primera presentación: acceso, matriz, firma, revisión, administración y dashboard |
| S2 | 36–303 | Carátula, encabezado, responsables, recursos, medios y flujos |
| S3 | 304–376 | Orden de ingreso, firma/finalización y observaciones visuales |
| S4 | 377–513 | Resaltado sencillo, discusión Quipux y actividades opcionales |
| S5 | 514–580 | Pasajes repetidos sobre orden y resaltado |
| S6 | 581–868 | Preguntas de negocio: titularidad, carreras, flujos, plazos, firma e informes |
| S7 | 869–1054 | Histórico, cuentas, recuperación, reportes, IA, puertos y servidor |
| S8 | 1055 | Distinción final entre informe de ejecución e informe final |

Las fuentes técnicas E01–E16 se detallan al final. La transcripción acredita lo dicho; el [Plan Maestro](../decisions/PLAN_MAESTRO_AGENTE_DESARROLLO_GESTION_DOCUMENTAL_ACADEMICA.md), las [decisiones posteriores](../decisions/open-questions.md) y [requirements.md](../../requirements.md) permiten resolver cambios autorizados. Una decisión del propietario no se presenta como normativa universitaria ratificada.

## Checklist

### Acceso y cuentas

- [x] **RC-001. Crear cuentas internamente, sin registro público.** Fuente: S6 29:29–29:57; S7 0:05–0:28. [E01](#e01). Altas administrativas y delegación acotada.
- [x] **RC-002. Ingresar con el identificador del correo sin arroba.** Fuente: S7 2:36–3:07. [E01](#e01). Username separado del correo utilizado para recuperación.
- [x] **RC-003. Registrar nombres, apellidos, carrera y cargo sin exigir teléfono/cédula.** Fuente: S7 2:24–2:58. [E01](#e01), [E02](#e02).
- [x] **RC-004. Cambiar la contraseña temporal en el primer ingreso.** Fuente: S1 0:29–0:44. [E01](#e01). Se restringe navegación hasta completar el cambio.
- [x] **RC-005. Restablecer contraseñas desde administración.** Fuente: S7 10:38–11:09. [E01](#e01). Alternativa cuando el correo no está disponible.
- [x] **RC-006. Ofrecer recuperación mediante enlace por correo.** Fuente: S7 1:35–2:16 y 10:43–11:09. [E01](#e01). Flujo/token/envío implementados; entrega en el servidor final se evalúa aparte en RC-083.

### Organización, carreras y responsabilidad

- [x] **RC-007. Administrar grupos, comisiones, unidades e integrantes.** Fuente: S1 0:59–1:07; S6 9:25–10:25. [E02](#e02). Ámbitos explícitos de grupo/carrera/período.
- [x] **RC-008. Elegir carrera/unidad desde datos precargados y contemplar varias carreras por docente.** Fuente: S2 7:34–8:10; S6 9:25–10:22. [E02](#e02). Opciones del ámbito autorizado, sin imponer una carrera universal.
- [x] **RC-009. Un Plan vigente por comisión, carrera y período.** Fuente: S6 1:09–1:57 y 9:39–10:05. [E02](#e02). Unicidad por ámbito; las versiones formales no se confunden con duplicados.
- [x] **RC-010. Conservar el Plan de la unidad al cambiar el responsable.** Fuente: S6 0:43–1:06. [E02](#e02). Titularidad institucional, transferencia explícita e historial.
- [x] **RC-011. Solo el responsable principal edita.** Fuente: S6 3:07–4:05 y 11:08–11:24. [E02](#e02). La membresía o condición de revisor no otorgan edición.
- [x] **RC-012. Varios elaboradores pueden figurar y firmar, conservando un editor principal.** Fuente: S6 3:26–4:17 y 13:48–14:11. [E05](#e05). Coautores ordenados y firmas sucesivas.
- [x] **RC-013. Cargar listas desde una plantilla CSV.** Fuente: S2 18:57–19:28. [E01](#e01). Importación de usuarios validada; no acredita importación universal de flujos.
- [ ] **RC-014. PARCIAL — Generar automáticamente dos planificaciones para una unidad unipersonal de dos carreras.** Fuente: S6 9:39–11:08. [E02](#e02). Backend de copia explícita unipersonal añadido posteriormente: crea borradores independientes por carreras seleccionadas, con matriz revalidada y sin firmas/anexos/evidencias. Falta integrar frontend y ratificar si satisface la automatización requerida. Véase el contrato de cierre.

### Creación del Plan y matriz

- [x] **RC-015. Orden: información general → contenido → matriz/actividades → anexos → revisión/firma.** Fuente: S3 1:18–2:38; S5 0:38–2:00. [E03](#e03). Cinco pasos vigentes por decisión posterior; no se exige conservar seis pantallas del prototipo.
- [x] **RC-016. Justificación precargada editable.** Fuente: S2 14:58–15:36. [E03](#e03). Se puede conservar, cambiar o reemplazar el texto inicial.
- [x] **RC-017. Seleccionar actividades sin imponer POA/plan de mejoras a toda comisión.** Fuente: S1 1:20–1:42; S4 7:09–7:36. [E03](#e03). Obligatoriedad configurable y categorías opcionales.
- [x] **RC-018. Añadir actividad propia si no está en catálogo.** Fuente: S2 29:03–29:26; S3 0:22–0:47; S4 7:16–7:36. [E03](#e03). No se limita la matriz a los ejemplos precargados.
- [x] **RC-019. Fechas desde/hasta completas y responsables por actividad.** Fuente: S1 1:42–2:58; S2 15:53–16:35. [E03](#e03). Fechas civiles y rango dentro del período.
- [x] **RC-020. Exigir al menos un responsable por actividad.** Fuente: S1 2:00–2:29; S2 18:06–18:09. [E03](#e03). Validación del servidor.
- [x] **RC-021. Seleccionar todos y mostrar Responsable de la comisión cuando corresponde.** Fuente: S2 18:45–19:41. [E03](#e03). Se conservan identidades individuales para trazabilidad.
- [x] **RC-022. Recursos y medios desde catálogos administrables.** Fuente: S2 21:30–22:36. [E03](#e03). Selección múltiple, sin obligar a escribir cada recurso a mano.
- [x] **RC-023. Otro, especifique con texto obligatorio.** Fuente: S2 22:45–23:02. [E03](#e03). No se cuentan valores vacíos como recursos/medios válidos.
- [x] **RC-024. Fuente editable y Elaborado por automático desde la comisión.** Fuente: S2 20:00–20:43. [E03](#e03). Fuente puede ser comisión o documento; no se impone un marco normativo inventado.
- [x] **RC-025. Permitir atravesar feriados, pero impedir inicio/fin en feriado.** Fuente: S6 17:59–18:33. [E03](#e03). Calendario administrable; no se bloquea todo intervalo que contenga un feriado.

### Documento institucional y previsualización

- [x] **RC-026. Generar documentos con los formatos institucionales, en A4.** Fuente: S1 4:26–5:45; S2 26:53–28:17; S6 23:52. [E04](#e04). A4 prevalece por decisión posterior del propietario frente a la referencia originalmente en Carta.
- [ ] **RC-027. PARCIAL — Ratificar fidelidad exacta de todas las plantillas.** Fuente: S1 4:32–5:45; S2 26:53–28:17. [E04](#e04). Se corrigieron encabezado, tablas, bordes, índices e historial, con pruebas. Falta aceptación visual del cliente de T1/T2/T3 y casos largos; no se acredita igualdad absoluta solo porque exista generación PDF.
- [x] **RC-028. Tipo de unidad correcto, nombre completo de facultad y carrera debajo.** Fuente: S2 6:59–8:38 y 12:55–14:32. [E04](#e04). No usar FISEI como sustituto del nombre completo de la celda formal.
- [x] **RC-029. Congelar la fecha de elaboración al finalizar.** Fuente: S2 9:01–9:21 y 13:54–14:01. [E04](#e04). No cambiar por fecha de revisión ni confundir con cada firma.
- [x] **RC-030. Mantener Anexos con (No aplica) cuando no hay archivos.** Fuente: S1 4:07–4:20; S2 4:22–4:31. [E04](#e04).
- [x] **RC-031. Ver páginas mediante anterior/siguiente e índices/referencias.** Fuente: S1 8:38–10:18; S2 4:37–4:44. [E04](#e04). Cantidad real de páginas y visor persistente actual.
- [x] **RC-032. Historial de cambios con versión, elaboración y fecha.** Fuente: S2 23:27–24:22. [E04](#e04). Tabla institucional; ronda y versión formal separadas.
- [ ] **RC-033. PARCIAL — Mantener obligatoriamente la nota fija debajo de la matriz.** Fuente: S2 20:45–20:54. [E04](#e04). La transcripción no reproduce el contenido de «esta nota». El aviso actual de protección se incluye si `privacyNoticeEnabled=true` y puede omitirse. Cotejar la referencia oficial y confirmar si es la misma nota y si debe ser no desactivable; no imponerlo por inferencia.

### Firma y descarga final

- [x] **RC-034. Firmar dentro del sistema con archivo personal y contraseña.** Fuente: S1 6:18–6:45; S6 20:33–23:46. [E05](#e05). .p12/.pfx y firma criptográfica PDF, no una imagen como sustituto.
- [x] **RC-035. No persistir credencial privada ni contraseña.** Fuente: S1 6:58–7:02; S6 21:41–22:03. [E05](#e05). Uso temporal en memoria; no se afirma borrado absoluto de todas las copias internas del runtime.
- [x] **RC-036. Firmar en el espacio de la tabla de responsabilidades.** Fuente: S1 6:47–6:57; S6 21:48–21:59. [E05](#e05). Slots derivados del artefacto, sin página fija.
- [x] **RC-037. Apariencia de firma con QR y texto.** Fuente: S6 23:00–23:46. [E05](#e05). Titular/fecha y QR de metadatos; el QR no acredita confianza institucional por sí mismo.
- [x] **RC-038. Enviar automáticamente a revisión al completar elaboración firmada.** Fuente: S6 12:02–12:39. [E05](#e05). La última firma de coautor activa revisión; no se omiten elaboradores obligatorios.
- [x] **RC-039. Bloquear edición tras firma/envío y revisar el mismo PDF.** Fuente: S3 2:19–2:34; S6 12:47–12:58. [E05](#e05). Artefactos inmutables, hashes y firmas conservadas.
- [x] **RC-040. Descargar el documento final solo tras aprobación y con autorización.** Fuente: S6 24:23–25:01. [E05](#e05). Sustituye el criterio previo «solo el último validador»; no habilita descarga pública ni acceso privado implícito de ADMIN.

### Revisiones, observaciones y versiones

- [x] **RC-041. Configurar flujos distintos por comisión/carrera/tipo.** Fuente: S2 24:49–26:48; S6 4:32–6:48. [E06](#e06). Cargos/personas por período; no un flujo universal de Decano/Subdecano.
- [x] **RC-042. Mantener fijo el flujo durante el semestre.** Fuente: S6 11:32–11:55. [E06](#e06). Asignación congelada al activar y snapshot por documento.
- [x] **RC-043. Revisión secuencial con todos los participantes obligatorios.** Fuente: S6 6:55–7:54. [E06](#e06). Sin adelantar etapas futuras ni aprobar en paralelo.
- [x] **RC-044. Impedir una misma persona en etapas incompatibles del documento.** Fuente: S6 7:58–8:20. [E06](#e06). Incluye separar elaboradores y revisores.
- [x] **RC-045. Observaciones generales o por página.** Fuente: S1 8:24–8:36; S3 3:14–3:28. [E06](#e06). Autor/tarea/ronda explícitos; edición/retiro propio antes de decidir incorporados por decisiones posteriores.
- [x] **RC-046. Resaltado sencillo asociado a observación.** Fuente: S3 3:39–5:21; S4 0:02–0:23. [E06](#e06). Varias zonas por página, sin herramientas complejas de dibujo.
- [x] **RC-047. Exigir observación al devolver; impedir aprobación con observaciones vigentes.** Fuente: S1 10:39–11:09; S4 2:07–2:24; S6 14:19–14:29. [E06](#e06). Validado en UI/backend; las retiradas antes de decidir no siguen bloqueando.
- [x] **RC-048. Devolver con los datos completos y habilitar correcciones del documento.** Fuente: S6 14:34–16:15. [E06](#e06). No se recrea desde cero ni se restringe edición a una frase.
- [x] **RC-049. Reiniciar revisión desde el primer participante al reenviar.** Fuente: S6 7:03–7:19. [E06](#e06). Nueva ronda, sin perder historia.
- [x] **RC-050. Diferenciar devolución de una nueva versión formal de un aprobado.** Fuente: S6 13:02–13:10 y 16:16–16:51. [E06](#e06). No se incrementa versión formal por cada corrección ni se revoca silenciosamente un aprobado.
- [ ] **RC-051. PARCIAL — Reasignar por coordinador una tarea cuyo revisor dejó el cargo.** Fuente: S6 8:26–9:32. [E06](#e06). Backend de sustitución excepcional añadido posteriormente para coordinador: actor inactivo o pertenencia explícitamente inactiva, tarea sin decisión, historial y slot original conservados. Falta frontend y aceptación de la representación del sustituto en el PDF originalmente firmado. Ausencia temporal de una cuenta todavía activa sin inactividad registrada sigue por definir.

### Ejecución y evidencias

- [x] **RC-052. Completar la actividad al cubrir todos sus medios requeridos.** Fuente: S6 25:29–26:08. [E07](#e07). Cobertura documental separada de ejecución física/declarada.
- [x] **RC-053. Evidencia documental PDF y referencias digitales cuando el medio lo permite.** Fuente: S6 25:49–26:00 y 29:13–29:23. [E07](#e07). URL/repositorio no se convierte automáticamente a PDF.
- [x] **RC-054. Visualizar/reemplazar evidencias conservando versiones.** Fuente: S6 26:54–27:09. [E07](#e07). Responsables autorizados e historial; no sobrescribir físicamente el anterior.
- [x] **RC-055. Bloquear cargas/reemplazos tras las 23:59 del vencimiento.** Fuente: S6 17:16–17:31. [E07](#e07). Zona America/Guayaquil y prórroga explícita.
- [x] **RC-056. Prórroga individual limitada, autorizada por coordinador.** Fuente: S6 19:29–20:26 y 26:00–26:10. [E07](#e07). Motivo/historial y límite acumulado; el cómputo hábil actual es provisional, no normativa ratificada.
- [x] **RC-057. Notificar próximos vencimientos y tiempo restante.** Fuente: S6 17:40–17:50; S1 7:53–7:59. [E08](#e08). Campana/pendientes/avisos por destinatario, con control de duplicación.
- [x] **RC-058. No imponer un workflow de aprobación de cada evidencia.** Fuente: S6 26:54–27:09. [E07](#e07). Los responsables cargan y verifican su respaldo.
- [ ] **RC-059. PARCIAL — Precisar reutilización de un mismo PDF entre medios/actividades/comisiones.** Fuente: S6 26:14–26:51. [E07](#e07). Cargas vinculadas a documento/actividad/medio. La conversación permite otra comisión y matiza la misma; falta política expresa si debe bloquearse por hash. No se encontró exclusividad por contenido.

### Informes

- [x] **RC-060. Informe final derivado de Plan previo y uno por Plan.** Fuente: S6 27:26–28:17. [E09](#e09). T2 desde T1 aprobado; descarte de borrador permite reemplazo controlado.
- [x] **RC-061. Precargar actividades, cumplimiento, evidencia y datos del Plan.** Fuente: S2 11:01–12:23; S6 18:47–19:07. [E09](#e09). Precarga al crear; no se afirma generación/firma programada sin intervención del responsable.
- [x] **RC-062. Mismo responsable institucional para el Informe y aislamiento del Plan fuente.** Fuente: S6 28:22–28:43. [E09](#e09). Cambios/firmas del T2 no modifican el T1.
- [x] **RC-063. Añadir evidencias PDF al final como páginas consecutivas.** Fuente: S6 28:48–29:23. [E09](#e09). Paginación real; enlaces digitales quedan como referencias textuales.
- [x] **RC-064. Informe de ejecución distinto del Informe final.** Fuente: S6 27:26–27:49; S8 24:53–25:11. [E09](#e09). T3 derivado con selección de actividades; granularidad/cortes finales pendientes de ratificación, no un segundo T2 único.

### Administración, historia y custodia

- [x] **RC-065. Gestionar períodos y ventana de elaboración de cinco días.** Fuente: S1 15:37–15:43; S6 17:03–17:15. [E10](#e10). Cinco días hábiles por defecto si no se fija fin explícito; reapertura del ámbito implementada. Cómputo definitivo requiere confirmación.
- [x] **RC-066. Administrar secciones, orden y obligatoriedad del documento.** Fuente: S1 16:24–16:44; S4 6:31–6:47; S6 24:05. [E10](#e10). Versiones de estructura/snapshot; no cambia el documento histórico.
- [x] **RC-067. Archivar lógicamente un borrador erróneo conservando el registro.** Fuente: S6 2:17–2:58. [E10](#e10). Descartar borrador no hace borrado físico; consulta restringida a administración autorizada.
- [x] **RC-068. Auditoría y exportación del recorrido documental.** Fuente: S1 18:54–19:36. [E10](#e10). Eventos por actor/documento, sin contraseñas ni credenciales privadas.
- [x] **RC-069. Acceso histórico restringido por período/ámbito y autoridad histórica explícita.** Fuente: S7 0:45–1:23. [E10](#e10). Nuevo miembro o ADMIN técnico no obtiene todos los PDF pasados; cargos históricos asignables.
- [x] **RC-070. Cerrar período, bloquear escritura y descargar custodia histórica.** Fuente: S1 19:59–20:06; S7 3:10–3:40. [E10](#e10). ZIP íntegro con estados, artefactos y auditoría, incluido historial de observaciones.
- [ ] **RC-071. PENDIENTE — Ratificar eliminación semestral frente a conservación histórica.** Fuente: S7 1:11–1:23 y 3:10–3:40. [E10](#e10). Solicitudes contradictorias. Existe purga manual con custodia/verificación y gracia; no equivale a autorización institucional de borrado automático.

### Reportes y experiencia de usuario

- [x] **RC-072. Reportes de planificaciones/actividades por comisión y globales.** Fuente: S1 7:42–7:51 y 19:36–19:50; S7 3:54–4:32. [E11](#e11). Estados, pendientes, filtros y CSV.
- [x] **RC-073. Comparar cantidad/distribución de actividades entre comisiones.** Fuente: S7 4:33–4:42. [E11](#e11). Gráfico/detalle, sin inventar ranking personal de docentes.
- [x] **RC-074. Porcentaje de ejecución con significado verificable.** Fuente: S7 4:17–4:32. [E11](#e11). Avance declarado y cobertura separados de evidencia completa; aceptación de fórmula final pendiente, sin presentar respaldo como ejecución física.
- [x] **RC-075. Dashboard administrativo de resumen institucional.** Fuente: S1 20:33–20:54. [E11](#e11). Indicadores, alertas y ventanas independientes. Inicio personal separado de Reportes por decisión posterior.
- [x] **RC-076. Interfaz moderna, iconos intuitivos y uso en PC/tablet.** Fuente: S1 22:11–22:26; S7 4:50–5:25 y 11:50–12:20. [E12](#e12). Temas claro/oscuro y componentes compartidos; no se exige aplicación móvil.

### Datos, integraciones y aceptación final

- [ ] **RC-077. PARCIAL — Lista oficial completa de docentes, carreras y responsables reales.** Fuente: S6 2:00–2:11, 4:42–6:48 y 9:25–10:25. [E13](#e13). Templates aporta referencias oficiales; las cuentas del escenario son ficticias. Falta ratificar todas las asignaciones reales por período.
- [ ] **RC-078. PARCIAL — Todos los flujos oficiales y excepciones Consejo Directivo/Decano/Subdecano.** Fuente: S2 24:49–26:48; S6 4:42–6:48. [E06](#e06). Motor configurable y registrador de órgano sin firma personal implementados; falta tabla institucional completa de casos/firmantes.
- [ ] **RC-079. PARCIAL — Trámite Quipux con constancia del coordinador.** Fuente: S4 2:43–5:57. [E14](#e14). Gate/constancia configurables posteriores a aprobación implementados; no integración externa. Confirmar orden y obligatoriedad por flujo frente a las propuestas sucesivas de la reunión.
- [ ] **RC-080. PARCIAL — Todos los tipos documentales adicionales que entregue el cliente.** Fuente: S2 2:36–2:51; S6 6:18–6:48 y 27:26–27:49. [E09](#e09). T1/T2/T3 soportados; no hay diseñador universal probado para convertir cualquier formato/evento nuevo sin desarrollo. Lista definitiva pendiente.
- [ ] **RC-081. PARCIAL — Redacción con proveedor real de una cuenta del sistema.** Fuente: S1 3:43–4:03; S2 3:57–4:22; S7 5:45–7:05. [E15](#e15). UI/servicio/proveedor configurable, aplicar/descartar y recuperación de fallos existen. Pruebas con dobles no acreditan cuenta/modelo externo institucional operativo.
- [ ] **RC-082. PENDIENTE — Aceptación operativa/institucional de la firma real.** Fuente: S6 20:33–23:46. [E05](#e05). Firma técnica disponible; confianza, revocación/tiempo y significado de «local» requieren aceptación. No se promete firma exclusivamente en navegador sin acordarlo.
- [ ] **RC-083. PARCIAL — Correo y recuperación en la infraestructura final.** Fuente: S7 7:40–11:09. [E01](#e01). SMTP configurable y aceptación por servidor externo en desarrollo registradas; no acredita llegada a bandeja ni salida SMTP institucional.
- [ ] **RC-084. PARCIAL — Despliegue en servidor/puerto autorizado y operación institucional.** Fuente: S7 7:11–7:40 y 11:12–11:44. [E16](#e16). Puerto 64430 productivo previsto, TLS/respaldos/herramientas preparados. Entorno manual 64431 no es entrega final; faltan servidor/dominio/red/custodios y aceptación.

## Diferencias que no deben ocultarse

| Tema | Regla/evidencia más reciente y límite |
|---|---|
| Descarga | S6 24:23–25:01 sustituye «solo último validador»: documento aprobado y usuario autorizado. |
| Estados | S6 13:19–13:38 reduce a tres estados formales. Ronda, firma y plazo son dimensiones independientes. |
| Flujos y reasignación | Congelar el flujo del semestre no resuelve sustituir excepcionalmente a un actor de una tarea activa. |
| Multicarrera | Crear documentos por dos ámbitos no acredita generar/copiar automáticamente el mismo contenido a ambos. |
| Quipux | S4 contiene varias secuencias propuestas. El gate actual es configurable; confirmar el orden institucional real. |
| Nota inferior | S2 habla de una nota fija sin reproducir su texto; cotejarla con el aviso opcional actual antes de concluir que se trata de la misma. |
| Cinco/tres días | Elaboración y prórroga de evidencia son plazos distintos. Cómputo hábil actual es provisional. |
| Histórico/purga | Custodia y purga manual protegida existen. No autorizan borrado automático de datos institucionales. |
| Firma | QR y firma CMS existen; no acreditan por sí solos identidad institucional, confianza del visor o revocación. |
| IA y correo | Integración y pruebas con dobles/aceptación SMTP no equivalen a proveedor final ni recepción en bandeja. |

No se considera obligación contractual compartir contraseñas, firmar por terceros, recoger cédula/teléfono, entrenar un modelo propio, integrar Quipux sin contrato o destruir datos sin custodia. Los números de puerto SMTP mal transcritos se corrigen en S7 a **587**.

## Evidencia técnica consultada

Los enlaces a pruebas representan cobertura existente, no una ejecución nueva durante este análisis. Los reportes recientes documentan las verificaciones efectivamente ejecutadas. La [matriz de auditoría](../testing/integral-audit-matrix.md) complementa esta checklist, pero no sustituye la fuente de las reuniones.

<a id="e01"></a>

### E01 — Cuentas y recuperación

[Accounts](../../backend/src/main/java/ec/edu/uta/utaped/identity/Accounts.java), [PasswordService](../../backend/src/main/java/ec/edu/uta/utaped/identity/PasswordService.java), [UserImportService](../../backend/src/main/java/ec/edu/uta/utaped/identity/UserImportService.java), [FoundationIntegrationTests](../../backend/src/test/java/ec/edu/uta/utaped/FoundationIntegrationTests.java), [UserImportIntegrationTests](../../backend/src/test/java/ec/edu/uta/utaped/UserImportIntegrationTests.java), [alcance real de SMTP](https://github.com/Andriu-Dex/utaped/blob/f2243b23019f62243365d89a67bc12f031bf60ba/docs/reports/2026-10-28-41-external-smtp-local-configuration.md).

<a id="e02"></a>

### E02 — Organización y titularidad

[OrganizationService](../../backend/src/main/java/ec/edu/uta/utaped/institution/OrganizationService.java), [WorkPlanService](../../backend/src/main/java/ec/edu/uta/utaped/planning/WorkPlanService.java), [OrganizationIntegrationTests](../../backend/src/test/java/ec/edu/uta/utaped/OrganizationIntegrationTests.java). Caso específico: `principalOnlyUniqueScopeAndExplicitHistoricalAccess`.

<a id="e03"></a>

### E03 — Plan y matriz

[WorkPlanEditor](../../frontend/src/planning/WorkPlanEditor.tsx), [MatrixEditor](../../frontend/src/planning/MatrixEditor.tsx), [ActivityService](../../backend/src/main/java/ec/edu/uta/utaped/planning/ActivityService.java), [BusinessCalendar](../../backend/src/main/java/ec/edu/uta/utaped/planning/BusinessCalendar.java), [DocumentIntegrationTests](../../backend/src/test/java/ec/edu/uta/utaped/DocumentIntegrationTests.java).

<a id="e04"></a>

### E04 — Documento institucional

[T1Template](../../backend/src/main/java/ec/edu/uta/utaped/documents/T1Template.java), [InstitutionalHeader](../../backend/src/main/java/ec/edu/uta/utaped/documents/InstitutionalHeader.java), [InstitutionalHistory](../../backend/src/main/java/ec/edu/uta/utaped/documents/InstitutionalHistory.java), [AttachmentService](../../backend/src/main/java/ec/edu/uta/utaped/documents/AttachmentService.java), [DocumentIntegrationTests](../../backend/src/test/java/ec/edu/uta/utaped/DocumentIntegrationTests.java), [pruebas de documentos largos/anexos](../../backend/src/test/java/ec/edu/uta/utaped/ReviewExecutionIntegrationTests.java). `T1Template` condiciona expresamente la nota a `privacyNoticeEnabled`.

<a id="e05"></a>

### E05 — Firma

[AuthorSignatureService](../../backend/src/main/java/ec/edu/uta/utaped/signing/AuthorSignatureService.java), [Pkcs12PdfSigner](../../backend/src/main/java/ec/edu/uta/utaped/signing/Pkcs12PdfSigner.java), [Pkcs12PdfSignerTests](../../backend/src/test/java/ec/edu/uta/utaped/Pkcs12PdfSignerTests.java), [circuito de firmas](../../backend/src/test/java/ec/edu/uta/utaped/ReviewExecutionIntegrationTests.java), [decisión vigente de firma](../decisions/open-questions.md). Casos: `coauthorsSignSequentiallyAndOnlyTheLastSignatureStartsReview`, `requiredReviewerSignatureCannotBeBypassedAndPreservesAuthorBytes`.

<a id="e06"></a>

### E06 — Flujos y observaciones

[ReviewService](../../backend/src/main/java/ec/edu/uta/utaped/workflow/ReviewService.java), [PeriodWorkflowService](../../backend/src/main/java/ec/edu/uta/utaped/workflow/PeriodWorkflowService.java), [ReviewWorkspace](../../frontend/src/workflow/ReviewWorkspace.tsx), [PeriodControlIntegrationTests](../../backend/src/test/java/ec/edu/uta/utaped/PeriodControlIntegrationTests.java), [ReviewExecutionIntegrationTests](../../backend/src/test/java/ec/edu/uta/utaped/ReviewExecutionIntegrationTests.java). Casos: `sequentialStagesRejectFutureActorAndRequireEveryDecision`, `highlightAndReturnPreserveArtifactAndAdvanceOnlyReviewRound`, `editableObservationsLifecycleAndAudit`. No se encontró un camino que actualice el actor de una tarea activa por reasignación.

<a id="e07"></a>

### E07 — Evidencias

[EvidenceService](../../backend/src/main/java/ec/edu/uta/utaped/planning/EvidenceService.java), [ReviewExecutionIntegrationTests](../../backend/src/test/java/ec/edu/uta/utaped/ReviewExecutionIntegrationTests.java), [reglas provisionales de plazos](../planning/institutional-rules-and-execution-report.md). Casos: `individualExtensionRetainsOriginalDeadlineAndHistory`, `externalReferencesRespectMediumPermissionsVersionsAndFrozenT2`.

<a id="e08"></a>

### E08 — Avisos y pendientes

[DeadlineNoticeService](../../backend/src/main/java/ec/edu/uta/utaped/notifications/DeadlineNoticeService.java), [PendingWorkService](../../backend/src/main/java/ec/edu/uta/utaped/planning/PendingWorkService.java), [NotificationIntegrationTests](../../backend/src/test/java/ec/edu/uta/utaped/NotificationIntegrationTests.java).

<a id="e09"></a>

### E09 — Informes

[ReportService](../../backend/src/main/java/ec/edu/uta/utaped/planning/ReportService.java), [ExecutionReportService](../../backend/src/main/java/ec/edu/uta/utaped/planning/ExecutionReportService.java), [T2Template](../../backend/src/main/java/ec/edu/uta/utaped/documents/T2Template.java), [T3Template](../../backend/src/main/java/ec/edu/uta/utaped/documents/T3Template.java), [ReviewExecutionIntegrationTests](../../backend/src/test/java/ec/edu/uta/utaped/ReviewExecutionIntegrationTests.java). Casos: `evidenceVersionsAndIndependentT2CompleteTheDocumentCircuit`, `longT2PreservesEveryNarrativeEvidenceAndSignaturePageWithoutChangingItsPlan`, `executionReportsCutThePlanAreNumberedAndProduceTheirOwnOfficialPdf`.

<a id="e10"></a>

### E10 — Administración y custodia

[PeriodArchiveService](../../backend/src/main/java/ec/edu/uta/utaped/institution/PeriodArchiveService.java), [DocumentLifecycleService](../../backend/src/main/java/ec/edu/uta/utaped/planning/DocumentLifecycleService.java), [DocumentTemplateService](../../backend/src/main/java/ec/edu/uta/utaped/planning/DocumentTemplateService.java), [PreparationExtensionService](../../backend/src/main/java/ec/edu/uta/utaped/planning/PreparationExtensionService.java), [AuditQueryService](../../backend/src/main/java/ec/edu/uta/utaped/audit/AuditQueryService.java), [PeriodControlIntegrationTests](../../backend/src/test/java/ec/edu/uta/utaped/PeriodControlIntegrationTests.java), [AuditExportIntegrationTests](../../backend/src/test/java/ec/edu/uta/utaped/AuditExportIntegrationTests.java), [DocumentStructureIntegrationTests](../../backend/src/test/java/ec/edu/uta/utaped/DocumentStructureIntegrationTests.java).

<a id="e11"></a>

### E11 — Reportería

[ReportingService](../../backend/src/main/java/ec/edu/uta/utaped/planning/ReportingService.java), [ReportingIntegrationTests](../../backend/src/test/java/ec/edu/uta/utaped/ReportingIntegrationTests.java), [aceptación Inicio/Reportes](../../frontend/tests/sample/management-reporting.spec.ts), [AdminOverview](../../frontend/src/institution/AdminOverview.tsx).

<a id="e12"></a>

### E12 — Interfaz

[App](../../frontend/src/App.tsx), [reglas visuales vigentes](../../AGENTS.md), [auditoría previa](../testing/integral-audit-matrix.md), [aceptación administrativa](../../frontend/tests/sample/admin-overview.spec.ts), [revisión reciente de Inicio](https://github.com/Andriu-Dex/utaped/blob/f2243b23019f62243365d89a67bc12f031bf60ba/docs/reports/2026-10-30-48-home-reporting-separation-frontend.md).

<a id="e13"></a>

### E13 — Datos institucionales

[Procedencia oficial y límites](../development/institutional-reference.md), [datos de referencia](../../backend/src/main/resources/development/institutional-reference.json), [Plan Maestro y entregables prometidos](../decisions/PLAN_MAESTRO_AGENTE_DESARROLLO_GESTION_DOCUMENTAL_ACADEMICA.md). Los nombres institucionales de Templates no convierten a las cuentas de demostración en designaciones reales.

<a id="e14"></a>

### E14 — Quipux

[FormalSubmissionService](../../backend/src/main/java/ec/edu/uta/utaped/planning/FormalSubmissionService.java), [ReviewExecutionIntegrationTests](../../backend/src/test/java/ec/edu/uta/utaped/ReviewExecutionIntegrationTests.java), [decisiones vigentes](../decisions/open-questions.md). Caso: `approvedQuipuxGateRequiresCoordinatorProofAndNeverChangesSignedBytes`; acredita el gate implementado, no la validación de su orden por la institución.

<a id="e15"></a>

### E15 — Redacción asistida

[WritingService](../../backend/src/main/java/ec/edu/uta/utaped/writing/WritingService.java), [WritingIntegrationTests](../../backend/src/test/java/ec/edu/uta/utaped/WritingIntegrationTests.java), [configuración y límites](../development/writing-configuration.md). Proveedor desactivado de fábrica; pruebas con servidores/dobles no acreditan servicio externo habilitado.

<a id="e16"></a>

### E16 — Operación

[Preparación operativa](../operations/production-readiness.md), [preflight](../../scripts/production-preflight.py), [variables productivas de ejemplo](../../.env.production.example). No se inspeccionaron ni copiaron secretos de archivos privados para elaborar esta checklist.

## Actualización backend posterior a la checklist

El propietario autorizó implementar el cierre técnico. RC-014 y RC-051 siguen parciales de extremo a extremo hasta completar la aceptación del alcance; la UI ya fue integrada y revisada técnicamente; [contrato de las nuevas APIs](../api/review-reassignment-and-career-copies.md), [guía frontend](../api/review-reassignment-and-career-copies.md), [decisiones para cliente](../validation/client-closeout-decisions.md) y [protocolo de aceptación final](../validation/final-acceptance-checklist.md). Las observaciones iniciales de ausencia de estas operaciones quedaron superadas por esta tanda, no por la mera gestión de cargos.

## Cierres recomendados

1. Validar tabla real de asignaciones, orden de Quipux, nota fija, cómputo de plazos y retención; conservar respuesta escrita.
2. Definir/implementar/probar reasignación excepcional de tareas activas preservando actor histórico, PDF firmado y flujo semestral.
3. Validar caso unipersonal multicarrera: dos creaciones independientes actuales frente a generación/copia automática descrita en S6.
4. Realizar aceptación visual de T1/T2/T3 contra ejemplos oficiales, incluyendo documentos largos y evidencias múltiples.
5. Ensayar firma, correo/recuperación, proveedor de redacción y respaldos en infraestructura final con datos autorizados.
6. Marcar cada punto abierto solo con evidencia adjunta; separar desarrollo técnico, configuración real y aceptación del cliente.

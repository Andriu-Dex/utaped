# Auditoría integral de flujos documentos e interfaces

Fecha de cierre: 2026-10-25. Alcance: aplicación UTAPED en el repositorio actual; Prototipos permanece como referencia externa de solo lectura.

El estado anterior a la auditoría se guardó en el commit local 422b5c3. Tras la revisión, el propietario autorizó su cierre en commits locales: 6061b71 (documentos), 99e3fcf (flujos y firma) y 9bf18ee (interfaz). No se publicó la rama ni se realizó push.

## Fuentes y criterio

Se contrastaron requirements.md, el Plan Maestro vigente, docs/decisions/open-questions.md y las precisiones posteriores del propietario, los requisitos heredados y las plantillas oficiales. Los contratos y datos aún pendientes no se convierten en normativa nueva. Se conserva A4 y se distingue la presentación moderna de la aplicación de la composición institucional de T1/T2/T3.

## Hallazgos y correcciones

| Área | Inconsistencia | Corrección |
|---|---|---|
| PDF y adjuntos | El ensamblaje interpretaba texto de anexos como marcadores de composición | No limpiar ni estampar instrucciones internas en páginas importadas; prueba de contenido, render y descarga original |
| Texto nativo PDF | Literales como @@PN@@ o @@SIGTOP@@ podían confundirse con instrucciones internas y desaparecer | Contexto corto único por composición, ajeno al contenido del Snapshot y sin persistencia; extracción/limpieza solo de esos tokens, con prueba de frases y líneas literales |
| Estructuras publicadas | Firmas/historial numerados y separados innecesariamente; texto adicional heredaba negrita/cursiva de una nota | Títulos finales sin número, sección compatible continua y tipografía normal de cuerpo |
| T3 | Unidad académica/administrativa fija de la fuente oficial | Tipo y nombre propios del documento; se conserva el formato particular T3 y su prefijo de versión oficial |
| Firma | Selector/actualización de artefactos y salidas podían descartar una preparación local de certificado | Indicadores booleanos de cambios y operación, selector bloqueado mientras hay datos pendientes, confirmación de salida y formularios coordinados; secretos no persistidos |
| Revisión | Falta filtro T3 y protección de la preparación de firma del revisor | T3 disponible, cancelación/salida/recarga protegidas |
| T2/T3 | Anexos rotulados como Plan T1; regresos a paso 1 | Tipo correcto, navegación inferior contextual y regreso al último contenido pertinente |
| Anexos | Cancelar edición/Escape podía perder datos | Confirmación y restauración de foco, reintento ante fallo de consulta |
| Evidencias | Formularios habilitados en período cerrado aunque la mutación era rechazada | Indicadores de carga/coordinación coherentes con el estado del período |
| Resultados T2 | Ausencia de porcentaje representada como 0% | «Sin declarar» cuando no existe valor, conservando un 0% declarado real |
| Listas y administración | Fallo de red confundido con vacío o carga permanente | Estados distinguibles y reintentos en documentos, reportes, ámbitos/cuentas/historial, directorio, períodos y flujos |
| UI PC/tablet | Etiquetas oscuras de bajo contraste, tabla excesivamente comprimida, foco suprimido e iconos de texto | Contraste semántico corregido, tabla con ancho legible y desplazamiento accesible, foco visible, validación de página inline e iconos SVG |
| Historial general | Informes mostraban «Historial del Plan»; fallo de rondas parecía lista vacía | Denominación documental común y reintento diferenciado |

## Cobertura de revisión

- Permisos y aislamiento: ID documental explícito, identidad distinta del contexto, acceso por ámbito y cargos, lectura histórica contextual, concurrencia y cierre.
- Ciclo: borrador, autores, preparación, firmas secuenciales, envío, revisión, observaciones, devolución/corrección, aprobación y versiones formales independientes de rondas.
- Ejecución: responsables, recursos/medios con Otro, archivos/referencias, reemplazos/versiones, período cerrado, informes T2/T3 derivados sin modificar el Plan.
- Documentos: contenido corto/largo, índices y enlaces, páginas/slots reales, estructuras publicadas, tablas, encabezados y conservación de artefactos firmados.
- Interfaz: navegación, acciones optativas, cancelación, errores/reintentos, teclado/foco, light/dark, escritorio/tablet y vocabulario consistente.

Esta cobertura combina inspección de código y pruebas de escenarios; no equivale a aceptación institucional ni a garantía de ausencia absoluta de defectos.

## Pendientes reales que no se inventaron

Asignaciones y catálogos institucionales definitivos; tablas de cargos y firmantes; ratificación de cortes del T3; contratos DTIC/CA y operación de firma; SMTP/TLS/servidor y custodia/retención operativa; formatos adicionales y aceptación con usuarios.

La descarga ZIP histórica está limitada a los archivos autorizados descritos en su contrato. Las fuentes no establecen explícitamente la misma prohibición para vistas previas históricas de borradores por cargos con permiso: se registra la ambigüedad y se mantienen los permisos existentes.

## Validación

- Lint, TypeScript y build frontend aprobados. Permanece el aviso conocido de tamaño del bundle; no se amplió el alcance con una partición arquitectónica ajena a estos defectos.
- Suite backend completa: 209 pruebas, 0 fallos/errores. Log: backend/target/2026-10-25-project-audit-backend-final.log.
- Aceptación aislada: 24/24, incluido circuito T1, devolución, firmas secuenciales, evidencias, T2/T3, siete roles, navegación, recuperación y protección de datos locales. Log: backend/target/2026-10-25-project-audit-acceptance-final.log.
- La revisión visual posterior detectó que NumberMarker confundía el contexto incluido en SIGTOP/SIGBOTTOM con un contador. Se excluyeron esas coincidencias y se añadió la comprobación geométrica de que cada número visible se encuentra en el pie. Tras ese último ajuste se repitieron DocumentIntegrationTests (16), DocumentStructureIntegrationTests (17) y ReviewExecutionIntegrationTests (38): 71/71 aprobadas. Log: backend/target/2026-10-25-project-audit-document-regression.log.
- Inspección de PDF renderizado: estructura personalizada de 6 páginas con firmas/historial juntos, sin numerarlos; celda de firma libre y contador en pie; literales del usuario conservados; T3 con unidad real y pie propio. Evidencia ignorada en backend/target/document-qa/audit-structured-signatures-final.png, audit-native-literals.png, audit-t3-front.png y audit-t3-footer.png.
- Revisión de capturas de bandeja, matriz e Informe en tablet oscuro: texto legible, tabla con desplazamiento accesible y navegación inferior coherente. Capturas en frontend/test-results.
- Los entornos de prueba se limpiaron al terminar. El entorno manual utaped-sample se actualizó sin ResetData, con volúmenes y credenciales conservados. Vite local y /api/auth/csrf responden HTTP 200 en http://127.0.0.1:5173.

El backend general se comprobó después de recuperar Docker; la interrupción del diagnóstico anterior no se considera un resultado aprobado. El checkpoint inicial es 422b5c3. Las correcciones de auditoría se cerraron después en los tres commits locales indicados al inicio, con autorización expresa del propietario. La documentación registra este cierre; no hubo push ni publicación.


## Recuperación del entorno de comprobación

El 2026-10-25 Docker Desktop no iniciaba por sockets de inferencia y Secrets Engine inaccesibles en Windows. Se detuvieron sus procesos de inicio y se renombraron de forma reversible únicamente directorios runtime que contenían esos sockets; se comprobó su contenido antes. Las copias preservadas son run.utaped-audit-20261025-114425, run.utaped-audit-20261025-114535 dentro de LocalAppData/Docker y docker-secrets-engine.utaped-audit-20261025-114535 dentro de LocalAppData. No se restableció Docker ni se borraron imágenes, volúmenes o bases. El motor volvió a responder y permitió relanzar las comprobaciones.

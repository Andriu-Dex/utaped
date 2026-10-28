# Auditoría integral de producto y recuperación

Fecha: 27 de octubre de 2026. Base: 06c3b53, PR #18 fusionado en main. Rama: fix/integral-product-audit. El staged y el worktree estaban limpios al comenzar. Auditoría técnica bajo el plan local instruction.md; no constituye aceptación institucional ni autorización de despliegue.

## Resultado y correcciones

Se separaron las reglas actuales de entregas históricas, se inventariaron pantallas/entradas y se trazaron 122 IDs heredados y los 92 apartados del Plan Maestro a 32 escenarios de auditoría. Se contrastaron los contratos actuales, decisiones del propietario y reuniones (L2, L7–8 y L451–472), sin tratar las demostraciones de prototipos como aceptación del software productivo.

Las nuevas pruebas reprodujeron fallos de recuperación que la suite anterior no detectaba:

- Detalle de revisión: error 503 con carga indefinida. Ahora termina la carga, deshabilita decisiones basadas en estado no disponible y permite reintentar desde los tres consumidores del inspector.
- Quipux: fallo de consulta presentado como trámite pendiente/aprobado. El error se muestra separado del estado real y permite reconsulta sin inventar un requisito.
- Plazo de elaboración e integrantes: consultas fallidas sin reintento y capacidad de gestión confundida con un fallo técnico. Se distinguen el error y el 403 legítimo y se preserva el formulario de plazo existente.
- Consultas administrativas: cargos mostraba un falso vacío; catálogos, estructuras y avisos no permitían recuperación explícita. Se añadieron carga/error/reintento y protección de ediciones pendientes.
- Cobertura E2E obsoleta: buscaba el formulario de vinculación de certificados eliminado por decisión del propietario. Se verifica el historial y la ausencia de vinculación, conservando las pruebas de firma real aportada.
- Documentación vigente contradictoria: actualizada para autoría múltiple, cinco pasos y certificado aportado, con entregas anteriores identificadas como históricas.

Las correcciones de producto se limitan al estado de consulta y recuperación; no amplían permisos, no cambian el circuito confirmado, no modifican el motor documental y no reescriben artefactos firmados ni archivos oficiales.

## Cobertura y evidencias

- [Inventario de módulos y fuentes](../testing/integral-audit-inventory.md).
- [Matriz de requisitos, escenarios, resultado y evidencia](../testing/integral-audit-matrix.md).
- [Hallazgos, causas, reproducción y regresión](../testing/integral-audit-findings.md).
- [Checklist de documentos oficiales](../testing/integral-audit-document-checklist.md).
- [Guion de aceptación del propietario/cliente](../testing/integral-audit-acceptance-script.md).

Roles: siete identidades de aceptación y anónimo, con verificaciones específicas de coautor, registrador colegiado y lectura histórica por cargo en backend. Se probaron el circuito T1, observaciones/devolución/corrección, firmas secuenciales, descarga aprobada, ejecución y evidencias, T2/T3, versiones formales, cierre/historia y revocación en sesión. Se ampliaron expresamente los escenarios T2/T3 con devolución y nueva preparación/firma, conservando los bytes de la ronda anterior y el Plan base.

UI: navegación real por los trece módulos administrativos a 1440×900 claro, 1920×1080 oscuro, 1024×768 claro y 820×1180 oscuro, sin errores JavaScript ni desbordamiento del documento de la aplicación. Se revisaron capturas de escritorio/tablet, confirmaciones/foco y una medición puntual de contraste de controles secundarios >=4.5. El zoom nativo 125%/150% y la aceptación con tecnologías de apoyo quedan incluidos en el guion humano; no se presentan como una certificación automática de accesibilidad.

Documentos: PDFs recién generados del motor vigente, comparados con las fuentes oficiales y con inspección OOXML de los DOCX, sin editar originales. Se revisaron portada, encabezado, índices, matriz, anexos vacíos y firmas/control en T1/T2/T3; pruebas de contenido extenso y de geometría complementan las muestras visuales. A4 y orientaciones según autorización. MuPDF y geometría PDF confirman la rejilla; el suavizado de Poppler a baja resolución no se corrigió alterando un documento correcto. No se usaron PNG antiguos de la carpeta de pruebas como evidencia actual.

## Verificación ejecutada

- Frontend: lint, TypeScript y build correctos. Persiste el aviso conocido de bundle mayor de 500 kB; no se usó esta auditoría para una partición arquitectónica ajena a los defectos.
- Backend completo: 215 pruebas, cero fallos/errores/omisiones. Resumen conservado antes de la regresión selectiva en backend/target/integral-audit/backend-baseline-summary.json.
- Regresión ampliada T2/T3: dos casos de ReviewExecutionIntegrationTests, ambos correctos, después de añadir devolución/nueva firma a esos recorridos.
- E2E final: ocho pruebas correctas, después de reconciliar el flujo de certificados y del último lote administrativo.
- Aceptación global final: 29 pruebas correctas en integral-acceptance-final-verified.log, después del último ajuste de sincronización de la fixture administrativa. La batería anterior de 28 casos y la regresión dirigida de cuatro casos también pasaron; no se suman como escenarios únicos adicionales.
- Guardas de aislamiento: cinco casos correctos. Operación/limpieza: once pruebas Python correctas.

Los primeros fallos de las pruebas nuevas se conservaron antes de corregir. El caso administrativo adicional se estabilizó manteniendo la red fallida hasta recibir el clic real de reintento: las consultas tardías de arranque no pueden recuperarlo anticipadamente. Las repeticiones fallidas por esa sincronización no se presentan como un fallo funcional nuevo ni se ocultan tras la ejecución dirigida. El primer intento del caso Quipux usaba una consulta de lista inválida y se sustituyó por una fixture propia; ese fallo no se atribuye al producto. Los tests con respuestas HTTP controladas prueban recuperación de UI; los permisos, transacciones y firmas se comprobaron además sobre servicios reales aislados.

## Respaldo y transporte reales en entorno aislado

El respaldo histórico V33 se restauró correctamente, pero carecía de PDF aprobado: su ensayo de transporte quedó incompleto y no se contabilizó como aprobado. Una primera captura de la nueva semilla ocurrió antes de finalizar su transacción y produjo un respaldo vacío; se detectó por el inventario y se descartó como evidencia de recuperación documental.

Se añadió al lanzador de aceptación una opción explícita de documentos preparados, espera del evento DEVELOPMENT_DOCUMENTS_CREATED, filtro de tests y respaldo opcional del proyecto fijo utaped-acceptance. El runner manual ya tenía la espera de semilla y se mantuvo intacto. El comprobador HTTPS acepta el archivo de credenciales del origen recuperado y explica si falta la fixture de aprobado; no imprime secretos.

Ensayo final: backup V36 con cuatro documentos y diez archivos verificados; restauración en utaped-restore-integral-current-20261027, hash del PDF aprobado recuperado comprobado sobre HTTPS, sesión Secure e identidad correctas, servicios internos sin puertos públicos y cuarenta lecturas de salud con ocho trabajadores. p95 observado: 2.06 s en este ensayo acotado; no es SLA ni dimensionamiento. Certificado TLS de prueba confiado solo para la prueba y sin correo enviado. El destino y el sandbox TLS se limpiaron; los respaldos locales quedan ignorados por Git.

No se restableció ni se purgó utaped-sample, no se cambió su configuración ni se publicaron datos/ramas. El backend, la DB y Mailpit manuales mantienen sus volúmenes.

## Reproducción y registros

Lanzadores desde la raíz: scripts/test-backend.ps1, scripts/test-e2e.ps1 y scripts/test-acceptance.ps1. Para regresión dirigida: `./scripts/test-acceptance.ps1 -TestPattern 'auxiliary administrative queries'`.

Para preparar un respaldo de fixture actual: `./scripts/test-acceptance.ps1 -PreparedDocuments -TestPattern 'administrative modules remain' -BackupDestination backend/target/backups/<directorio-nuevo>`. Es únicamente el proyecto desechable; el directorio debe ser nuevo. Restaurarlo mediante operations.py en un proyecto utaped-restore-* nuevo, y ejecutar test-operations-transport.py con --credential-env-file .env.acceptance antes de que otra ejecución renueve esas credenciales. Limpiar únicamente recursos con la etiqueta exacta de ese proyecto de ensayo.

Logs locales ignorados en backend/target/2026-10-27-integral-*: baseline-backend, report-rounds, e2e-close, acceptance-final-verified, operations-tests, isolation-tests, prepared-backup-final, restore-final y transport-final. PDFs/contactos/capturas en backend/target/integral-audit y frontend/test-results. Los respaldos con datos ficticios y las credenciales no se versionan.

## Límites y siguientes decisiones

No se encontraron P0/P1 abiertos en los escenarios ejecutados. Los P2 reproducidos quedan corregidos y vinculados a regresiones; esta conclusión no equivale a ausencia absoluta de defectos en todas las combinaciones.

La auditoría técnica deja el alcance comprobado preparado para la revisión del propietario con el guion. Siguen pendientes la aceptación institucional/visual y de accesibilidad con usuarios, datos/asignaciones oficiales definitivos, política de confianza de firma, corte definitivo T3 y condiciones de operación institucional (dominio/TLS/SMTP/red, custodio, retención, RPO/RTO y SLA). El ensayo aislado no confirma esos contratos. La operación de purga solo se ejercita en fixtures y no se ejecutó sobre la base manual.

Cierre mediante commits locales por lotes tras comprobar los resultados finales. Sin push, PR, merge ni despliegue en esta tarea.

Commits de implementación y pruebas: `6b776ac` (recuperación de consultas) y `6d6896f` (rondas/ensayos aislados). La documentación se cierra en un commit local independiente.

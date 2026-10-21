# Importación administrativa y evidencias externas

Fecha: 2026-10-12. Rama: `feature/modern-workspace-access`. Implementación productiva en `C:\Documentos\D-Proyectos\Git\utaped`. Prototipos continúa congelado. Alcance autorizado: siguiente tanda amplia con interfaces modernas. Solo commits locales; no se hizo push ni se actualizó el PR.

## Resultado

Se entregan importación CSV con prevalidación, creación atómica de cuentas y credenciales temporales individuales de descarga única, más evidencias mediante enlaces HTTPS versionados. El T2 incorpora esas referencias como texto y conserva los PDFs como páginas anexas. Las interfaces amplían el diseño del propietario con tarjetas, métricas, pasos, historial y contraste en ambos temas para PC/tablet.

Este bloque quedó implementado y comprobado. El producto completo aún tiene pendientes de asistencia, operación y validación institucional descritos en [implementation-status.md](../../implementation-status.md).

## Implementación

1. Flyway V21 añade lotes temporales de importación y el tipo PDF/URL de catálogo/evidencia. La restricción SQL exige archivo para PDF y referencia sin archivo para URL. Las filas previas siguen siendo PDF.
2. ADMIN puede cargar CSV UTF-8 con BOM opcional, coma o punto y coma, comillas, hasta 1 MiB y 500 filas. Las columnas están delimitadas; correos/usuarios se normalizan y errores aparecen por fila. No se aceptan privilegios ADMIN, contraseñas o sobrescritura de cuentas existentes. Parser: Apache Commons CSV 1.14.1; se verificó su [API oficial de configuración](https://commons.apache.org/proper/commons-csv/apidocs/org/apache/commons/csv/CSVFormat.Builder.html).
3. Confirmar vuelve a comprobar datos y guarda todo el lote en una transacción. Bloqueo del lote y restricciones únicas impiden repetir o sobrescribir cuentas bajo concurrencia. Las pertenencias opcionales son de ámbitos/períodos activos, sin designación de principal, acceso documental automático ni delegación de recuperación.
4. Cada cuenta USER recibe contraseña aleatoria independiente, hash Argon2id y cambio obligatorio. La descarga privada se genera en memoria una sola vez, con cabeceras que impiden caché. No se guardan contraseñas en el lote, auditoría ni almacenamiento del navegador. El nombre de CSV exportado queda ignorado por Git. Si se pierde la respuesta después de confirmar, se consulta el estado y se usa restablecimiento administrativo.
5. La preparación caduca a los 30 minutos. Se limpian filas personales al confirmar y cada diez minutos si caducaron; se mantienen metadata de lote y auditoría. El archivo CSV fuente no se conserva. Estos tiempos/límites son técnicos, no una política institucional ratificada.
6. El catálogo de medios permite PDF privado o enlace HTTPS. El servidor conserva tipo y denominación en cada selección de matriz; editar el catálogo no cambia selecciones existentes. Los medios históricos y `Otro` siguen usando PDF.
7. Registrar/reemplazar referencia exige responsable asignado, acceso al documento explícito, Plan aprobado, plazo individual y período abierto. Se rechazan URLs sin HTTPS, con credenciales, controles o espacios. No hay consulta/descarga remota desde el servidor. Versiones anteriores conservan título, URL, autor y fecha.
8. Cobertura, avisos de evidencia pendiente e informes reconocen los medios URL. No se atribuye hash, páginas, integridad o validación del recurso remoto. La descarga PDF de una referencia se rechaza.
9. El T2 congela ID/versión/título/URL en el snapshot y los representa en medios/anexos. Reemplazar referencias del Plan no altera un T2 firmado. La metadata vacía nueva se omite para conservar hashes de artefactos anteriores sin referencias. El ZIP de cierre conserva todas las versiones de referencia y evita tratar un `file_id` nulo como archivo.
10. UI: importador por pasos con plantilla descargable, resultados filtrados/paginados, confirmación explícita y protección al salir; ejecución con tarjetas por medio, versión actual, historial, enlaces seguros y formularios separados. Se corrige presentación de fechas de prórroga, se incorporan etiquetas españolas de auditoría y contraste oscuro. No se añadieron estilos para celular.

## Validación realizada

| Comprobación | Resultado |
|---|---|
| Backend completo, `docker compose -f compose.test.yml run --rm tests mvn -B -q test` | 116 ejecuciones en 17 suites; 0 fallos, errores u omisiones. Incluye casos heredados ejecutados en clases derivadas. |
| Importación | UTF-8/BOM/separadores, encabezados/formato, duplicados, cambio posterior de datos, hash de contraseña, cambio obligatorio, propietario/ADMIN, caducidad/limpieza, repetición y pertenencia sin privilegios. Dos lotes simultáneos sobre la misma cuenta: solo uno confirma y el otro no deja cuentas parciales. |
| Evidencia externa integrada | Tipo de medio congelado incluso al editar catálogo, PDF/URL no intercambiables, permiso y URL inválida, conflicto de versión, cobertura, anexos PDF reales, referencia textual y snapshot/bytes firmados inmutables al reemplazar. Cierre y manifiesto incluyen ambas versiones. |
| Frontend `npm run lint`, `npm run build` | Correctos después de los últimos cambios; TypeScript y Vite completados. |
| Playwright estándar | Cinco escenarios correctos: acceso/contexto/recuperación, temas, tablet sin almacenamiento, flujos y administración. |
| Playwright sample | Tres escenarios correctos: administración contextual, circuito T1/T2 con PDF + URL y CSV/primer acceso. Tras ajustes de contraste, fechas, avisos al salir y singular, se repitieron los dos afectados: correctos. |
| Revisión visual | Importación y evidencias en 1440×1000 y 820×1180, claro/oscuro, sin desbordamiento horizontal de página. T2 A4 de cinco páginas del caso de prueba: portada, tabla y referencias revisadas mediante PNG de PDFBox, PyMuPDF y Poppler. |
| Git | `git diff --check` correcto. CSV de credenciales, variables privadas, certificados, runtime, compilados y evidencias de prueba permanecen fuera del commit. |

Las pruebas PostgreSQL se ejecutaron en `utaped_test`. E2E estándar usó `utaped-e2e`; sample usó `utaped_dev` dentro de `utaped-sample`. No se reinició ni borró la base principal. Los certificados de firma son fixtures temporales; no se utilizaron certificados personales reales. La descarga de credenciales del E2E se consumió en memoria y eliminó, sin incluir contraseñas en screenshots/reportes.

Se corrigió un error inicial de sintaxis en una prueba añadida y una expectativa de 409 frente al 403 real del período cerrado; la ejecución final completa pasó. Docker se reconstruyó con éxito después de esa corrección. Los renderizadores emiten avisos heredados de fuente/conversión de la plantilla; los documentos y las referencias del escenario se comprobaron visualmente. Esto no sustituye pruebas exhaustivas de documentos largos o enlaces de longitud máxima.

Fuentes de pruebas: [UserImportIntegrationTests.java](../../backend/src/test/java/ec/edu/uta/utaped/UserImportIntegrationTests.java), [ReviewExecutionIntegrationTests.java](../../backend/src/test/java/ec/edu/uta/utaped/ReviewExecutionIntegrationTests.java), [user-import.spec.ts](../../frontend/tests/sample/user-import.spec.ts) y [product-circuit.spec.ts](../../frontend/tests/sample/product-circuit.spec.ts).

Evidencias locales ignoradas: logs `backend/target/import-reference-*.log`; PDFs/PNG `backend/target/document-qa/t2-external*`; capturas `frontend/test-results/user-import-*` y `frontend/test-results/evidence-external-*`. No son archivos publicados ni una dependencia de ejecución.

## Commits

- `65d6269`: backend, migración y pruebas de importación/evidencias externas.
- `0e449bc`: interfaces modernas, E2E y exclusión del CSV privado en Git.
- Documentación, contratos y estado se registran en un commit local adicional al cerrar esta entrega.

## Cómo probar y actualización manual

No se requieren nuevas variables de entorno ni cambiar puertos. Flyway V21 se aplica al arrancar el backend actualizado, sin reset de datos. Para actualizar el entorno habitual de Docker desde la raíz:

```powershell
docker compose --profile web up -d --build
```

Si trabaja localmente, reinicie el backend con su configuración habitual y mantenga `npm run dev` en frontend; el proxy persistente existente se conserva. El entorno de ejemplo se actualiza con el [procedimiento documentado](../development/sample-environment.md).

1. ADMIN → Usuarios → Abrir importador → Descargar plantilla CSV. Complete usuarios nuevos, analice y revise filas. Confirme y guarde privadamente la descarga; entregue cada credencial al titular. El usuario debe cambiarla al ingresar.
2. ADMIN → Recursos y medios → crear medio de verificación con Enlace HTTPS externo. Seleccionarlo en la matriz del T1 antes de firmar. Tras aprobación, el responsable registra título y URL en Actividades y evidencias; reemplazar conserva el historial.
3. Crear T2 desde el Plan aprobado; generar previsualización y comprobar referencia textual y anexos PDF. El cierre conserva estas referencias y bloquea modificaciones.

Detalles y límites: [contrato de importación y referencias](../api/user-imports-and-external-evidence.md).

## Pendientes y siguiente tanda

Siguen pendientes alta/importación delegada, catálogo oficial de cargos con vigencia, adjuntos no PDF y otros formatos oficiales, políticas institucionales de repositorios/retención y pruebas de carga/lotes máximos/documentos extensos. No se consulta disponibilidad o autenticidad de URLs, cuyo contenido puede cambiar en el sitio de origen.

La siguiente tanda propuesta es asistencia de redacción con comparación/aplicación explícita y herramienta interna separada para inspeccionar código y planificación de Trello. Requiere configurar proveedor/modelo/credenciales y delimitar repositorio, tableros y datos autorizados antes de conectar servicios reales. Después se ampliarán pruebas de aceptación y preparación operativa: HTTPS, SMTP institucional y respaldos con restauración comprobada.

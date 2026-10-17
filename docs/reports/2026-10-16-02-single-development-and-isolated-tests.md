# Entorno diario único y pruebas aisladas

Fecha: 2026-10-16. Rama: fix/document-creation-options. Autorización: aplicar la organización acordada, conservar datos y hacer únicamente commits locales.

## Implementación

- Se reutiliza utaped-sample con su DB utaped_dev y sus volúmenes existentes. No se renombraron proyectos ni se fusionaron bases.
- Nuevo scripts/start-development.ps1: PostgreSQL, backend y Mailpit para Vite local. La opción -Web añade el frontend compilado sobre la misma base. El comando anterior start-development-sample sigue disponible, ahora también con -Web; ResetData continúa siendo una acción destructiva explícita y no se ejecutó.
- Vite toma BACKEND_PORT de .env.sample, fallback 18082, y conserva prioridad para API_TARGET explícito. El .env histórico ya no redirige silenciosamente al otro backend. Se leen estos valores únicamente en configuración del servidor de desarrollo, sin publicar secretos en el frontend.
- El arranque ajusta APP_PUBLIC_URL para los enlaces de recuperación según Vite 5173 o Docker 64431, restaura la variable del proceso al salir y espera a que responda CSRF. El modo Docker reinicia Nginx para resolver el backend recreado.
- Nuevo scripts/test-acceptance.ps1: proyecto fijo utaped-acceptance, credenciales aleatorias privadas en .env.acceptance, semillas en PostgreSQL y volúmenes independientes. Puertos 24431/28082/25435/28027/21027. La suite sample y todos sus contextos usan ese destino, sin URL o credenciales manuales por defecto.
- Se unificaron los logins/API duplicados de aceptación. Los guardas rechazan proyecto manual, URL manual, archivo manual y configuración ausente antes de solicitudes, lectura de contraseñas o SQL.
- CI prepara la caché externa Maven, ejecuta los cinco guardas de aislamiento y limpia los volúmenes temporales de backend/E2E; no se ejecutó GitHub Actions desde esta sesión.
- Backend/E2E/aceptación limpian recursos desechables al terminar. KeepOnFailure conserva el entorno fallido para diagnóstico; una ejecución posterior de E2E/aceptación lo recrea. Maven usa una caché externa conservada, no un volumen de datos de aplicación.
- Se retiró el DELETE de throttling E2E: con una DB nueva era innecesario y podía ejecutarse antes de Flyway. La prueba de recarga ahora realiza el cambio obligatorio del primer acceso, evitando depender de una contraseña modificada en otra prueba anterior.
- README, guías, requisitos, instrucciones y propuesta aprobada actualizados. No se cambió código funcional backend, permisos, firmas ni documentos institucionales.

## Acción sobre Docker y conservación

Se detuvieron utaped, utaped-e2e, utaped-tests y las tres restauraciones antiguas utaped-restore-acceptance-20261012, utaped-restore-complete-20261012 y utaped-restore-complete-v2-20261012. Las pruebas ejecutadas después se limpiaron mediante sus lanzadores. Las copias utaped/restauraciones mantienen sus datos y volúmenes; pueden seguir apareciendo apagadas en Docker Desktop. No se ejecutó una purga global ni se tocaron caseflow-ai/dev-project-agent.

Estado diario final comprobado: utaped-sample con db/backend/mail activos; frontend Docker detenido para trabajar con Vite. El puerto institucional sigue siendo 64430; el entorno local conserva sus puertos anteriores para evitar reasignaciones de datos.

Se comprobaron 46 usuarios y 122 documentos en la base manual antes y después de la aceptación automatizada. Las pruebas nuevas no añadieron registros a esa base. Esto verifica esos conteos, no equivale a una auditoría byte por byte de toda la DB. Los registros automáticos antiguos (por ejemplo «Ámbito recuperación…») siguen conservados: limpiarlos requerirá identificar cuáles corresponden a revisión manual. No se regeneraron contraseñas, no se reiniciaron datos ni se eliminaron documentos manuales.

No se requirió reasignar ni purgar almacenamiento manual; por ello no se hizo un nuevo respaldo/migración. Antes de una futura depuración o cambio de nombre de volúmenes sigue siendo obligatorio respaldar DB y documentos. Los ensayos de restauración se arrancan solo para su verificación y se detiene su proyecto exacto al finalizar; no se alteró su lógica de recuperación ni se eliminaron sus evidencias.

## Validación ejecutada

- Lint y build frontend: aprobados, sin advertencias finales de lint.
- Aceptación sobre DB nueva y frontend Docker: 11 escenarios aprobados, incluyendo T1/T2, firmas, devolución, evidencias, cierre, roles, modal, teclado/tema, importación y referencias institucionales.
- E2E generales sobre DB nueva: 8 aprobados en la repetición final. La primera ejecución detectó la dependencia del primer acceso en recarga; se corrigió la prueba y la suite final pasó completa.
- Cinco pruebas Node de aislamiento: rechazados cuatro destinos/configuraciones inseguros, aceptado únicamente el destino aislado.
- Parsers PowerShell: cinco scripts sin errores; codificación UTF-8 con BOM para mensajes correctos en Windows PowerShell 5.1.
- Arranque Docker -Web: página y CSRF HTTP 200; regreso al modo Vite correcto. Proxy Vite real en puerto temporal 15174, sin API_TARGET: CSRF y CAPTCHA HTTP 200; servidor temporal cerrado al terminar.
- Backend PostgreSQL: 139 pruebas en 21 suites, sin fallos, errores ni omisiones; lanzador terminó con código 0 y eliminó sus contenedores/red/volúmenes desechables. Caché utaped-maven-cache conservada. Los avisos de fuentes/layout de los casos documentales extensos no causaron fallos; no se rediseñaron los documentos en esta tarea.
- Compose de desarrollo, aceptación y backend validado con config --quiet; diff --check sin errores.
- Limpieza de utaped-acceptance y utaped-e2e comprobada tras éxito: sin contenedores ni volúmenes de esos proyectos. Volúmenes manuales utaped-sample_postgres-data y utaped-sample_document-data conservados.

## Uso desde la raíz

```powershell
./scripts/start-development.ps1
cd frontend
npm run dev
```

Abrir http://127.0.0.1:5173. Los cambios del frontend se reflejan al guardar. Primera instalación: npm ci. Para distribución Docker: desde raíz `./scripts/start-development.ps1 -Web`, URL http://127.0.0.1:64431.

Pruebas desde raíz: scripts/test-backend.ps1, scripts/test-e2e.ps1 o scripts/test-acceptance.ps1. El alias npm run test:e2e:sample desde frontend ejecuta el mismo lanzador aislado en Windows. No hace falta añadir variables manualmente: los archivos privados de prueba se generan automáticamente y permanecen ignorados. Credenciales manuales: las existentes en .env.sample.

No se hizo push ni se actualizó el pull request.

# Aceptación por roles, documentos largos y preparación operativa

Fecha: 2026-10-12. Rama: `feature/modern-workspace-access`. Alcance autorizado: aceptación por roles, documentos largos y preparación operativa, preservando UI moderna y el prototipo congelado. Agente de código/Trello fuera del alcance.

## Resultado

Se amplió la aceptación mediante solicitudes HTTP y un escenario UI de siete identidades: ADMIN, principal, coordinador, integrante, revisor, aprobador y ajeno. Se verifican lectura/edición separadas, ausencia de privilegios documentales implícitos para ADMIN, ejecutores futuros sin acceso anticipado y baja de pertenencia con una sesión ya abierta. No se crean permisos institucionales nuevos.

Se añadió navegador documental compartido entre preparación y revisión: salto a página real, anterior/siguiente, progreso, referencias a firmas/anexos/observaciones y mensaje de carga. Con un resaltado sin guardar se conserva la confirmación existente al cambiar de página. No se permite dibujar antes de cargar la imagen. La navegación no regenera ni modifica el PDF firmado; la composición mantiene los índices/slots reales.

Se conserva el caso de T1 extenso con 35 actividades y se agrega T2 de 18 páginas, con narrativas extensas y siete páginas de evidencia. Se comprueban los marcadores finales, índice, cantidad real, límites de firmas, aprobación/descarga y bytes del Plan base. Se renderizó con Poppler y se revisaron páginas seleccionadas y hoja de contacto de las 18 páginas: portada, continuaciones densas, fin de narrativa, anexos y firmas, sin recortes observados. La posición de firmas sigue la plantilla y puede estar después de anexos; no se asume una página fija.

## Preparación operativa

- `compose.production.yml`: HTTPS 64430→443, certificado/clave privados montados en solo lectura, cookies Secure, firma sin HTTP, cabeceras del proxy y SMTP real configurable. Se retiran los puertos publicados de DB/backend/Mailpit y Mailpit no forma parte del arranque productivo normal.
- `.env.production.example`: configuración separada de los entornos habituales. No contiene credenciales reales. La propiedad SMTP_SSL complementa STARTTLS para SMTP con SSL implícito.
- `scripts/production-preflight.py`: validación de solo lectura de URL/puertos, exclusión de semillas, configuración/credenciales necesarias, pareja certificado/clave, identidad y expiración mínima técnica de siete días. El bootstrap inicial exige opción explícita y retirada posterior de credenciales. No acredita CA/DNS/firewall ni envío SMTP.
- `scripts/operations.py`: respaldo conjunto DB custom, volumen privado e imágenes originales, manifiesto con hashes y metadata; pausa/resume de escritores, validación y restauración en un proyecto nuevo `utaped-restore-*`. No sobrescribe el origen ni destinos existentes. Conserva permisos/bytes y verifica metadata/hashes antes de iniciar el aplicativo recuperado.
- `scripts/test-operations-transport.py`: ensayo HTTPS con certificado temporal confiado solo por el cliente de prueba, preflight ficticio, login/cookie, PDF aprobado y lecturas de salud. Usa una recuperación de usuarios ficticios, sin correo/IA ni CA instalada en Windows. Retira sus servicios temporales y conserva el conjunto restaurado.

Fuentes: [HTTPS Nginx](https://nginx.org/en/docs/http/configuring_https_servers.html), [merge de Compose](https://docs.docker.com/reference/compose-file/merge/) y [pg_restore 18](https://www.postgresql.org/docs/18/app-pgrestore.html). Configuración y límites propios: [guía operativa](../operations/production-readiness.md).

## Comprobaciones ejecutadas

| Comprobación | Resultado final |
|---|---|
| Backend PostgreSQL aislado `utaped_test` | 131 ejecuciones / 19 suites, cero fallos/errores/omitidos. Incluye pruebas heredadas ejecutadas por subclases; no son 131 escenarios únicos. |
| Herramientas Python | Siete pruebas: integridad, divergencia DB/archivo, archivos inseguros, destinos/volúmenes existentes protegidos —incluidos los que no tienen etiquetas— y reinicio de escritores tras fallo. |
| Frontend | Lint sin advertencias; TypeScript/build, 69 módulos, aprox. 452 kB JS / 44 kB CSS, gzip 123/10 kB. |
| E2E general | Cinco escenarios aprobados de acceso, sesión, tema, administración y flujos. |
| E2E sample | Cinco escenarios aprobados: administración contextual, bandeja paginada, circuito T1/T2 con navegador documental, siete roles y revocación, importación/cambio de contraseña. |
| Respaldo completo de ejemplo | 57 documentos, 156 archivos privados, Flyway 21 e imágenes exactas de backend/frontend. Metadata y SHA-256 coinciden. |
| Restauración | Proyecto nuevo `utaped-restore-complete-v2-20261012`, DB `utaped_restore`; mismos conteos/metadata y 156 archivos verificados antes de arrancar. |
| HTTPS sobre recuperación | Certificado de prueba validado, cookie Secure, login/identidad correctos y PDF firmado descargado con el hash intacto. Servicios internos sin puertos públicos. |
| Lecturas concurrentes acotadas | 40 consultas /healthz con ocho trabajadores, todas correctas; p95 aproximado 2,067 s en este equipo durante el ensayo. No es carga documental ni una garantía de capacidad. |
| Visual | Navegador PC/tableta claro/oscuro y documentos institucionales; sin desbordamiento horizontal en escenarios comprobados. |

Los primeros ensayos encontraron dos supuestos incorrectos en pruebas: una ruta de recuperación de cuentas que no correspondía al contrato y una suposición de firmas antes de anexos. Se corrigieron para usar el endpoint y metadata reales. En operación se corrigió la herencia de build al reutilizar imágenes por ID y se añadió archivo de imágenes para no depender del cache local. Un intento de arranque de recuperación chocó con un puerto ocupado; se añadió comprobación previa y se terminó en otro destino nuevo, sin borrar los datos parciales ni afectar al origen. La lectura JSON del preflight se ajustó para la codificación de consola Windows.

La DB principal, archivos .env privados y certificados personales no se modificaron. La aplicación de ejemplo se pausó temporalmente para el respaldo consistente y se retomó. No se desplegó HTTPS sobre la instalación principal. Los datos/archivos de ensayo y claves temporales no se rastrean en Git; no se restauran reportes eliminados. Las pruebas automatizadas no consumen el proveedor real de redacción aunque esté activado en el entorno de ejemplo.

## Archivos locales de evidencia

Respaldo completo: `backups/2026-10-12-acceptance-complete/` — dump, volumen, imágenes y manifiesto privados. Capturas/documentos: `frontend/test-results/` y `backend/target/document-qa/`. Resultados: logs de validación y XML de Surefire en `backend/target/`. Son salidas locales ignoradas; no se publican junto al código. El entorno recuperado final permanece en loopback 64434; los dos destinos de ensayo anteriores se detuvieron conservando sus volúmenes para diagnóstico. El ensayo temporal HTTPS usó 64433 y retiró sus servicios. El puerto institucional continúa siendo 64430.

## Configuración manual y siguiente tanda

Para activar operación real hacen falta hostname/IP, certificado/CA y clave HTTPS protegida, SMTP/remitente autorizados, condiciones de red y ubicación/custodia/frecuencia/cifrado/retención de respaldos. La [guía](../operations/production-readiness.md) indica variables y comandos de preflight/arranque; no es necesario cambiar .env/.env.sample para seguir desarrollando. No se enviaron correos externos ni se afirmaron RPO/RTO o políticas institucionales definitivas.

Siguiente implementación propuesta: alternativa accesible al CAPTCHA visual y carga documental más amplia; además, recorrido de aprobación con el cliente y validación de insumos reales cuando DTIC los entregue. Permanecen políticas de retención/reapertura, sustitución de ejecutores y administración avanzada registradas en el estado vigente.

Entrega en commits locales por bloque, sin push ni actualización del PR conforme a la instrucción vigente: `7772294` operación/aceptación backend, `0be745c` navegación/aceptación UI y `af8bdf8` protección de volúmenes; la documentación se registra aparte. No se declara finalizado el producto institucional por completar esta tanda técnica.

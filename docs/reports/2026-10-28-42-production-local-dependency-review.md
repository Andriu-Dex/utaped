# Dependencias locales y preparación productiva

Fecha: 2026-10-28. Alcance: revisión estática del runtime, integración SMTP, composición de despliegue y guardas operativas. No es aceptación integral institucional ni despliegue.

## Hallazgos y correcciones

- El override productivo omitía los alias SMTP_USER/SMTP_PASS usados por el propietario: corregido, manteniendo prioridad canónica.
- El preflight admitía URL loopback: ahora rechaza localhost, subdominios .localhost, IP loopback/indefinida y dominios .invalid. SMTP local/de ejemplo y remitente .invalid también quedan rechazados.
- Añadida detección explícita de documentos de desarrollo habilitados, además del perfil/credenciales existentes.
- La prueba operativa HTTPS usa localhost intencionadamente: ahora lo habilita mediante una excepción explícita, y el resultado indica que no acredita configuración de despliegue. No se ejecutó ese ensayo de red en esta entrega.

## Runtime inspeccionado

| Área | Evidencia / conclusión |
|---|---|
| Acceso a API | Frontend usa /api del mismo origen; proxy Vite loopback solo en servidor de desarrollo. |
| Persistencia | Servicios PostgreSQL y almacenamiento en volumen; localStorage limitado a preferencias visuales. |
| Documentos | Plantillas desde classpath, fuentes en imagen y composición real; no depende de la carpeta externa Prototipos. |
| Firma | Servicios criptográficos reales; fixtures firmadas solo bajo perfil development-seed. Confianza institucional pendiente según contrato vigente. |
| Ejemplos | Semillas requieren perfil explícito y BD utaped_dev; no se ejecutan por defecto. CSV de importación contiene una fila de ejemplo intencionada, no una cuenta runtime. |
| Despliegue | Nginx compilado, HTTPS; puertos DB/backend privados; Mailpit excluido del arranque productivo. |
| Redacción asistida | Proveedores HTTP reales; deshabilitada si no se configura. No se comprobó una petición externa durante esta revisión. |
| Correo | SMTP configurable por entorno, sin secretos en frontend ni Git; prueba externa anterior no equivale a aceptación del servidor institucional. |

## Verificación realizada

18 pruebas Python de operación/preflight y cinco guardas de aislamiento correctas. Renderizado real de Compose con configuración ficticia: alias SMTP, prioridad canónica, exclusión Mailpit, servicios privados y HTTPS correctos. Lint, TypeScript y build correctos; persiste aviso conocido de tamaño de bundle. No se recompiló backend ni se repitió suite documental porque no cambió lógica Java/React. No se publicó ni desplegó producción, ni se borró el entorno manual.

Pendientes de operación: dominio/IP final, certificado HTTPS válido, secretos propios de .env.production, cuentas/ámbitos/autoridades institucionales, respaldos y aceptación real de los flujos. Las políticas y confianza de firma siguen en docs/decisions/open-questions.md; no se sustituyen por simulaciones.

Cambios staged del propietario conservados fuera de este commit.

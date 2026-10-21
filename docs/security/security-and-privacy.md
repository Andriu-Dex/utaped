# Seguridad y privacidad

Propuesta de producto para revisión institucional; no certifica cumplimiento legal.

Entrega 2026-10-06: [acceso productivo](../api/local-access.md) con Argon2id, migración de bcrypt, CAPTCHA servidor con sesión/TTL/consumo único, CSRF, throttling y reset ADMIN auditado con revocación. Se mantienen los límites de autorización actuales; coordinadores esperan sus ámbitos. No hay bypass de CAPTCHA por entorno ni respuestas en texto en la API.

- Autorizar backend por identidad, pertenencia, objeto y etapa; negar por defecto. No confiar en roles DEMO o contexto cliente.
- Si hay credenciales locales: hashes mediante mecanismo mantenido, recuperación de un solo uso, expiración y respuestas que no revelen usuarios. Definir sesión, revocación y protección de intentos.
- Proteger tráfico y secretos del entorno; jamás claves en frontend/Git.
- Certificados privados y contraseñas de firma solo durante operación; no persistir, loguear ni respaldar. Contrato definitivo con DTIC.
- Validar contenido/tipo/tamaño PDF, descarga por objeto y rutas seguras; definir análisis y tratamiento de PDF activo.
- Preservar artefactos e historia con integridad y restauración verificable; auditoría atribuible protegida de modificación ordinaria.
- Minimizar datos personales; cédula no obligatoria sin validación. Definir finalidades, responsable, retención y accesos con institución.
- IA requiere política de transmisión de datos; no enviar información personal o secretos sin autorización.
- CAPTCHA productivo generado/validado en servidor; conserva controles contra abuso. Alternativa accesible al reto visual aún pendiente de acordar.

Pendientes: matriz de permisos, política de datos, firma, identidad, red/alojamiento y correo. Registrar decisiones antes de integración real.

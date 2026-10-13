# Aceptación por identidad, documentos largos y recuperación

Entrega 2026-10-12. Pruebas técnicas con datos ficticios; la aceptación formal del cliente requiere su propio recorrido y registro. Siete cuentas de ejemplo representan identidades distintas, no cambios de identidad por selector de contexto.

| Actor | Recorrido verificado | Restricción verificada |
|---|---|---|
| ADMIN | Administra organización/cuentas. | No obtiene contenido, PDFs ni historial privados por ser ADMIN. |
| Principal | Crea y edita T1; deriva T2 aprobado, firma y conserva historial. | No administra usuarios globales ni altera documentos firmados. |
| Coordinador | Consulta ámbito; prórrogas y reset solo bajo asignación/delegación específica. | No edita el documento por coordinarlo ni recibe permiso universal de recuperación. |
| Integrante | Consulta un documento con pertenencia y concesión explícitas. | No edita/genera artefactos; retirar pertenencia revoca acceso en la sesión ya abierta. |
| Revisor | Interviene sobre el PDF firmado cuando llega su turno, registra resaltado/observación y devuelve/firma. | No accede por anticipado a un borrador ajeno ni modifica bytes del elaborador. |
| Aprobador | Completa la etapa final y accede al aprobado. | No actúa ni accede antes de activarse su etapa. |
| Ajeno | Acceso local a su cuenta. | No obtiene documento/artefacto ni API administrativa de otro ámbito. |

Sesión anónima: 401. Acceso documental ajeno/revocado: 404 para no revelar existencia. Lectura autorizada sin edición: 403 ante mutación. La separación y los códigos se verifican tanto con solicitudes HTTP como desde UI. No se inventan permisos de Decano/Subdecano ni aceptación de múltiples elaboradores.

## Documentos largos

- T1: prueba existente con 35 actividades y justificación extensa; índice y ubicaciones reales, sin número fijo de páginas.
- T2: nuevo caso de 18 páginas con antecedentes, conclusiones y oportunidades de mejora extensos, marcadores finales comprobados y siete páginas de evidencia. Se verifican índice/firmas/rangos a partir de la composición, firma incremental, aprobación y descarga, sin modificar el Plan base. Las firmas pueden ir después de anexos según el documento institucional; no se asume una posición absoluta ni que estén antes de anexos.
- Navegador reutilizable para preparación y revisión: salto numérico, anterior/siguiente, accesos derivados a firmas/anexos y páginas con observaciones. Rechaza números fuera del rango. Cambiar página con un resaltado sin guardar conserva la confirmación existente.
- Render Poppler y revisión visual de portada, páginas densas, fin de narrativa y sección de firmas. Los anexos preservan sus páginas originales; el motor no regenera el aprobado al consultarlo.

## Comandos

```powershell
docker compose -f compose.test.yml run --rm tests mvn -B -q test
python -m unittest discover -s scripts/tests -v
```

Desde frontend: `npm run lint`, `npm run build`, `npx playwright test --config=playwright.config.ts` y `npx playwright test --config=playwright.sample.config.ts`, con sus entornos aislados levantados y variables de Mailpit según las guías. Las pruebas no reinician la DB principal.

La suite sample controla la disponibilidad/respuesta de redacción para evitar llamadas facturables incluso si `.env.sample` tiene un proveedor real activado. No convierte el runtime productivo en una simulación. La protección de CAPTCHA solo se sustituye en fixtures identificadas por el nombre exacto de DB/proyecto.

## Aceptación institucional y operación

Registrar para cada escenario: fecha, versión/imagen, identidad/rol, período/ámbito, datos ficticios utilizados, resultado esperado/real y observación del cliente. Corregir antes de aprobar un flujo fallido. Build/lint o pruebas técnicas no sustituyen la aprobación del cliente.

[Preparación operativa](../operations/production-readiness.md): HTTPS/SMTP y backup/restore, con comprobación conjunta DB/PDF. El ensayo de 40 lecturas concurrentes de salud es una comprobación acotada; no representa carga documental ni dimensionamiento institucional.

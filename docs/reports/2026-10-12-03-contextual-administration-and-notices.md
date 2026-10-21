# Administración contextual, historial y avisos internos

Fecha: 2026-10-12. Rama: `feature/modern-workspace-access`. Trabajo productivo en UTAPED; Prototipos permaneció como referencia congelada. Alcance autorizado: siguiente implementación. Solo commits locales, sin push ni actualización del PR.

## Resultado

Se entregan edición/desactivación de carreras, historial visible de cargos y responsables principales, recuperación de cuentas por coordinador expresamente autorizado, historial de prórrogas y avisos internos programados de vencimiento/evidencia pendiente. Se conservan el diseño del propietario y los controles documentales existentes.

La entrega completa este bloque de administración/alertas; no declara terminado todo el producto ni sustituye decisiones institucionales pendientes.

## Cambios

1. Flyway V20 añade delegación de recuperación, historiales sucesivos de pertenencias/principales y registro persistente de avisos. Los registros existentes reciben una línea base explícita, sin inventar fechas anteriores.
2. ADMIN edita carreras mediante versión optimista y puede desactivarlas sin borrar sus ámbitos/documentos. Los nombres guardados en documentos y artefactos firmados no se reemplazan por un cambio de catálogo.
3. ADMIN concede recuperación al COORDINATOR seleccionado en Carreras y ámbitos. El servidor exige pertenencia activa al mismo ámbito, período ACTIVE y destinatario USER activo. Excluye ADMIN y cuenta propia. Revocar la autorización o retirar la pertenencia bloquea nuevas acciones inmediatamente.
4. Reset temporal comparte los controles existentes: Argon2id, versión actual, contraseña diferente, revocación de sesiones/enlaces y cambio obligatorio. Se añade aviso privado al titular; las contraseñas no aparecen en historial/auditoría. Los cambios voluntarios/por enlace incrementan también la versión de cuenta.
5. Mi organización presenta ámbitos propios, historial y recuperación según permisos reales. Una cuenta ADMIN no obtiene contenido documental por administrar personas. El destino ACCOUNT de los avisos lleva a la seguridad de la cuenta.
6. Ejecución muestra prórrogas por actividad: destinatario, fechas anterior/nueva, motivo, autorizador y fecha. Los nuevos ZIP de cierre incorporan historial de cargos/principales; no se reconstruyen archivos históricos ya generados.
7. Programador interno de avisos usa America/Guayaquil, aprobación, período vigente, responsables activos con acceso, plazo personal y cobertura de medios. Cierre/archivo/evidencia completa impiden nuevos avisos. La restricción única evita duplicados concurrentes y tras reinicio.
8. Períodos académicos muestra configuración segura y permite a ADMIN comprobar vencimientos manualmente. La bandeja funciona sin SMTP. Frecuencia inicial ajustable: una hora entre comprobaciones, tres días de anticipación y reiteración diaria tras vencer.
9. Durante pruebas apareció una carrera entre el CAPTCHA inicial y una renovación prematura. El botón de renovación espera a que la imagen termine de cargar o falle; no se cambia la validación de servidor ni se añade bypass.
10. Las vistas mantienen modo claro/oscuro, feedback toast, confirmación y advertencia de cambios sin guardar. Se verificaron PC/tableta; no se añadieron estilos para celular. Se corrigió el contraste del título del historial en modo oscuro.

Contrato detallado: [administración contextual](../api/contextual-administration.md). Requisitos: UT-ORG-050, UT-AUTH-050, UT-NOT-050, UT-EXE-050. Fuente: Plan Maestro §§6.6, 8, 39–40 y 56.

## Verificación realizada

| Verificación | Resultado |
|---|---|
| Suite completa de backend sobre PostgreSQL aislado | 107 casos ejecutados, 0 fallos, 0 errores, 0 omitidos. |
| Regresión posterior de administración/avisos y cierre | 26 casos ejecutados, 0 fallos/errores/omitidos, tras incorporar historial al ZIP y texto de actividad al aviso. |
| Comprobación final de reset, permisos y concurrencia con creación documental | 3 casos, 0 fallos/errores/omitidos, con el orden final de bloqueo: período, cuentas, ámbito; se reproduce una creación concurrente que toma primero la cuenta y luego el ámbito. |
| `npm run lint` | Correcto. |
| `npm run build` | TypeScript y Vite correctos; 61 módulos, sin error de compilación. |
| E2E de acceso/administración/temas/flujos | 5 escenarios correctos, 27.2 s. |
| E2E sample final | 2 escenarios correctos, 31.4 s: recuperación contextual e historia; circuito T1/T2, alerta manual, prórroga visible, firmas, devolución, aprobación y cierre. |
| UI | Capturas PC y tableta oscura inspeccionadas; sin desbordamiento horizontal en tableta. |
| Git | Revisión de diff/archivos; entornos privados, certificados, builds y capturas permanecen ignorados. |

Los números contabilizan ejecuciones, incluyendo casos heredados repetidos entre clases. No representan 107 reglas institucionales distintas. La regresión posterior se limita a los módulos afectados; no se atribuye una segunda suite completa a esos comandos.

Evidencia local ignorada:

- `backend/target/contextual-full-tests.log`
- `backend/target/contextual-final-regression.log`
- `backend/target/contextual-reset-final.log`
- `backend/target/contextual-access-browser.log`
- `backend/target/contextual-final-sample-browser.log`
- `frontend/test-results/contextual-desktop.png`
- `frontend/test-results/contextual-tablet-dark.png`

Fallos intermedios resueltos: inserción duplicada de acceso en el fixture de alertas, renovación de CAPTCHA antes de completar la carga, y sintaxis de PowerShell al pasar una lista de clases de prueba. Los resultados de la tabla corresponden a las ejecuciones finales indicadas.

Se utilizaron `utaped_test`, `utaped-e2e` y `utaped_dev`. Las cuentas nuevas del escenario de recuperación son ficticias; las credenciales semilla se conservan. La base principal no se vació ni se reinició. Ninguna prueba manipuló certificados institucionales reales.

## Para probar

Reconstruir y arrancar la aplicación con el procedimiento habitual. Para el entorno de ejemplo:

```powershell
./scripts/start-development-sample.ps1
```

No usar `-ResetData` para actualizar. Flyway ejecuta V20 conservando los registros. No se requieren variables nuevas; las cuatro variables de avisos son opcionales y están documentadas en `.env.example` y el contrato.

ADMIN debe conceder la recuperación a cada coordinador pertinente; las semillas no delegan ese permiso automáticamente. La autorización permite restablecer el acceso global de la cuenta destinataria: entregar la contraseña temporal por canal privado y nunca incluirla en documentos o comentarios.

## Pendientes y siguiente implementación

- Alta/importación delegada, catálogo completo de cargos y fechas de vigencia, permisos institucionales de Decano/Subdecano y sustitución de ejecutores congelados.
- Frecuencia institucional de avisos y canal de correo para recordatorios. El programador actual es para el monolito con comprobaciones síncronas; quedan carga, grandes volúmenes y operación institucional.
- Referencias externas de evidencia, anexos no PDF, importación masiva de usuarios y asistencia/agente de código/Trello.
- Accesibilidad alternativa del CAPTCHA, políticas de retención/reapertura, HTTPS, SMTP institucional y respaldos restaurables.

Siguiente tanda propuesta: importación de usuarios con prevalidación y referencias externas de evidencia; después asistencia de redacción y agente de inspección, según configuración y permisos de proveedores.

## Commits locales

- `459a9b7`: backend, migración V20, configuración y pruebas de permisos/avisos.
- `60bc8f0`: interfaz, historial, recuperación, alertas, CAPTCHA y E2E.
- `c17fe1e`: orden de bloqueos de recuperación y prueba concurrente.
- Commit de documentación: requisitos, estado, contrato de API, operación y este reporte.

No se realizó push, PR ni merge.

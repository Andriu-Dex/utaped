# Informe de ejecución, estructuras y controles institucionales

Fecha: 2026-10-21. Rama: `feature/institutional-rules-and-execution-report`.

Se completa y estabiliza la tanda iniciada en F1–F4 y continuada en F5–F8. Se implementa la aplicación productiva en UTAPED; Prototipos permanece congelado. Las reglas provisionales se registran como decisiones del proyecto, sin convertirlas en normativa institucional ratificada. [Contrato y guía de uso](../api/institutional-rules-and-execution.md).

## Resultado funcional

### Informe de ejecución

- Modal «Crear documento»: Plan de Trabajo, Informe de ejecución e Informe final con nombres/códigos diferenciados. T3 es tipo interno; el formato oficial es `UTA-SGC-A-1-1-P4-T2`.
- Cortes I/II… independientes de un Plan aprobado, con selección de actividades, porcentajes/observaciones, análisis FODA, conclusiones/recomendaciones, mejoras, enlace HTTPS, anexos y coautores. Un borrador activo por Plan, apertura idempotente y numeración sin consumir descartes de borrador.
- Editor por siete pasos con protección de cambios, recuperación de URL/sección, conservación ante errores y estados de lectura. Se reutilizan adjuntos, PDF, espacios de firma, certificados personales temporales, revisión/observaciones/rondas, auditoría y descarga aprobada.
- El T2 final permanece independiente; ningún corte cambia el contenido, firmas o PDF del Plan aprobado.
- Habilitación explícita de T3 en un ámbito cuyo período ya tenga flujos activados: Administración elige una revisión publicada y sus participantes quedan congelados una sola vez. T1/T2 existentes no cambian.

### Documentos institucionales

- Plantilla T3 basada en el DOCX oficial; PDF proporcionado usado como referencia visual. El PDF fuente muestra un índice de cuatro entradas; se conserva el contenido más completo del DOCX, incluyendo FODA, acciones de mejora y firmas/control.
- A4, índice/páginas/espacios de firma derivados de composición. Se corrigió el logo de portada tapado por el sombreado de párrafos Word, el índice que comenzaba en portada y la cuadrícula de tablas. Se conservan encabezado/pie y gráficos institucionales.
- Estructuras publicadas por formato: orden de apartados, secciones adicionales, obligatoriedad y texto inicial. Se protegen secciones oficiales, su título, firmas/control al final y formato de tablas/encabezados.
- Revisión/estructura/contenido copiados por documento; una publicación posterior no altera documentos existentes ni PDFs. Las versiones formales conservan su estructura anterior. Las secciones adicionales obligatorias se verifican al preparar; el borrador puede estar incompleto.
- La matriz T1 reordenada sigue horizontal en A4 y las narrativas verticales. No se recalculan PDFs firmados.

### Seguridad, permisos y trámites

- Coordinación busca candidatos por nombre en su ámbito activo, sin exponer el directorio completo. Solo puede agregar/retirar integrantes ordinarios; no administradores, coordinadores, principal o permisos de recuperación.
- Se registra el origen delegado. Agregar una pertenencia no habilita el restablecimiento de la contraseña global de esa cuenta. Se preservan las autorizaciones administrativas existentes.
- Borradores descartados manualmente: solo ADMIN. El archivo automático por cierre es histórico y no se confunde con un descarte. Un Informe descartado puede consultarse sin conceder lectura de evidencias de su Plan privado.
- Historia cerrada mediante cargo vigente con permiso explícito, por carrera o facultad. La administración técnica por sí sola no concede lectura de aprobados históricos. Permiso revocable y auditado; ejemplos Decanato/Subdecanato solo en la semilla de desarrollo.
- Constancia Quipux opcional del flujo congelado. Tras aprobar, coordinación registra un PDF privado de hasta 10 MiB. Sin constancia requerida se bloquean evidencia, informes derivados y cierre; se permite descargar el aprobado para tramitarlo. Sin integración remota ni cambios al PDF firmado.
- JSON históricos compatibles al añadir la exigencia opcional de Quipux. Firma sin persistencia de certificados/contraseñas; QR de metadatos sin la referencia no contratada a un validador público.

### Plazos, indicadores y custodia

- Cinco días hábiles por defecto si al crear un período no se indica fin de elaboración. Una fecha explícita permite configurar otra ventana; las fechas existentes no cambian. Fines de semana/feriados registrados excluidos.
- Prórrogas y reapertura por ámbito con el máximo acumulado configurable de tres días hábiles, motivo, autorización y concurrencia. No superar fin de período ni reabrir uno cerrado. T2/T3 usan el período vigente.
- Cobertura de evidencias separada del avance declarado. Media simple de últimos porcentajes aprobados por actividad: T2 final prevalece, luego último T3. Se muestra el número de actividades incluidas y ausencia de declaración; no se presenta una fórmula institucional ponderada.
- Cierre → descarga de ZIP completo por ADMIN → verificación SHA-256 de la copia local → gracia configurable de 30 días → purga manual con motivo/confirmación. Una descarga HTTP registrada no demuestra custodia y no basta para borrar.
- Verificación en streaming sin duplicar el ZIP almacenado. Proxy habilitado para ese cuerpo binario hasta el tope técnico del backend; archivos ordinarios conservan sus límites. Se comprobó que 13 MiB llegan a verificación y se rechazan por hash incorrecto, en lugar de por el límite ordinario del proxy.
- Purga conserva inventario/auditoría/huella/responsables, evita dependencias entre períodos y elimina bytes después del commit. No se habilita borrado automático.

### Experiencia de usuario

- Formularios de acción cerrados inicialmente, cancelación protegida, foco y avisos Toast/modales compartidos, sin alert/confirm/prompt nativos.
- Se conserva la interfaz existente, tarjetas, jerarquía, modo oscuro y presentación en PC/tablet; no se agregan estilos para teléfonos.
- Se corrigió la navegación rápida del visor que agotaba los turnos de renderizado: espera acotada conservando concurrencia máxima de dos, controles durante carga y botón explícito de reintento.
- Selectores con nombres accesibles; campos de resultados identificados por actividad. Se validan recarga del Informe de ejecución, cancelación que retiene valores y sincronización después de registrar Quipux.

## Verificación ejecutada

- `scripts/test-backend.ps1`: 177 pruebas en 23 suites sin fallos/errores. PostgreSQL/Flyway aislados, V1–V33.
- Regresión adicional de los bloques afectados: 74 pruebas (períodos/custodia/cargos, estructura, ejecución/revisión y semilla), todas aprobadas. Después del ajuste del selector de Planes se repitió el escenario T3 completo, incluyendo opciones, permisos de descartes y separación 75% declarado / 100% de cobertura, aprobado.
- `npm run lint` y `npm run build`: aprobados. Se mantiene un aviso no bloqueante de Vite por el tamaño del bundle; no se amplió esta tanda a dividir todas las rutas.
- Aceptación de navegador: 16 escenarios de `scripts/test-acceptance.ps1` aprobados, incluyendo siete roles, circuito completo T1/T2/T3, recarga, estructura versionada, descarte privado, ZIP inválido/válido y transporte de 13 MiB. Entorno desechable limpiado.
- `nginx -t` de configuración local y productiva: aprobado. Para la productiva se usó certificado efímero de prueba, sin despliegue institucional.
- Revisión visual de PDF T3 (portada/resultados), A4 T1 con estructura reordenada, y pantallas de configuración, custodia y T3 en escritorio/tablet claro/oscuro. Capturas y PDFs permanecen en carpetas ignoradas `backend/target/document-qa` y `frontend/test-results`.
- Durante la validación se corrigieron incompatibilidad de JSON previo, concatenación SQL del selector T3, permisos de historia/descarte, logo/índice/cuadrícula y límites de renderizado. Las pruebas antiguas que consideraban T3 inválido o concedían acceso histórico al antiguo autor se actualizaron a las reglas explícitas de esta entrega.

No se consumieron proveedores externos de redacción ni certificados institucionales reales. Las pruebas usan cuentas/certificados ficticios en entornos desechables; no ejecutan limpieza sobre la DB manual.

## Entorno de desarrollo

Respaldo conjunto previo a migrar: `backend/target/backups/pre-institutional-rules-20261021`, con DB, volumen privado e imágenes. Verificación del respaldo: migración V24, un documento, cero archivos privados registrados. Se mantuvo el entorno persistente `utaped-sample`.

Backend local actualizado a V33: conserva siete usuarios y el documento existente. Las credenciales y variables privadas se conservaron. La semilla agrega ejemplos faltantes sin sustituir personalizaciones ni alterar flujos activados. No se restableció la base.

Para probar con recarga de frontend:

```powershell
cd C:\Documentos\D-Proyectos\Git\utaped\frontend
npm run dev
```

Frontend en `http://127.0.0.1:5173`, backend en `http://127.0.0.1:18082`. El backend ya responde. No hacen falta variables nuevas. `scripts/start-development.ps1` permite volver a iniciar los servicios si se detienen; no requiere reconstruir Docker por cambios de interfaz.

Un período activado sin T3 necesita habilitación explícita desde Períodos académicos; una plantilla nueva se configura desde Plantillas. La consulta histórica requiere asignación del cargo/permisos correspondientes, aunque la cuenta sea antiguo autor. Estas acciones se describen en el contrato enlazado.

## Estado y siguiente paso

Implementación backend: commit local `e4cfad6`. Interfaz, proxy y aceptación: `2fc2530`. La documentación se registra en un tercer commit de esta misma rama. No se hizo push, PR, merge ni despliegue institucional.

Sigue el recorrido de aceptación del propietario/cliente, empezando por crear Plan, completar matriz, firmar/revisar, registrar ejecución/Quipux cuando aplique y generar ambos informes. Antes de uso institucional deben ratificarse periodicidad del corte, cargos/firmas/órganos, sustitución de tareas activas, estructuras, retención/custodio y condiciones operativas reales de HTTPS/SMTP/carga. El agente de código/Trello lo desarrolla el propietario por separado y no forma parte de esta tanda.

# Preparación de firma y revisión final

## Decisión y alcance

Cambios visuales previos guardados en commit local d93be48 tras lint y build. Sin push. El propietario autorizó separar preparación y aplicación de firmas; esta implementación queda sin commit.

## Flujo T1

1. Información general.
2. Contenido.
3. Actividades y matriz.
4. Anexos y opciones.
5. Preparación de firma: documento preparado automáticamente, nombres/cargos de elaboradores, espacios y páginas reales de firma, circuito de revisión y pendientes. Permite volver a información general para configurar autores. No contiene el formulario de certificado ni aplica firmas.
6. Revisión final y firma: documento completo a ancho completo, comenzando por la portada. Confirmación local de revisión asociada al ID del artefacto seleccionado; después se habilita Preparar firma. Los certificados y las contraseñas mantienen sus protecciones y no se persisten.

Las páginas se obtienen de signatureSlots. No se permite reposicionar firmas arbitrariamente ni se inventan firmantes o reglas institucionales. Para artefactos anteriores, los nombres/cargos se consultan en el propio documento conservado y no se sustituyen por la configuración actual.

El circuito no cambia: la primera firma congela el contenido y la última activa la revisión institucional. T2/T3 mantienen sus flujos existentes.

## Composición y accesibilidad

Se elimina el reordenamiento de revisión final mediante display:contents y order/important. El orden del documento, confirmación y acciones de firma se expresa mediante un destino de portal estable después del visor, conservando el estado y las protecciones de navegación. La revisión final muestra siempre el documento aunque el paso anterior estuviese en modo formulario.

Se conserva el diseño visual del propietario y el ajuste de paneles. El separador también termina el arrastre al perder la captura del puntero.

## Verificación

Lint, TypeScript y build correctos. Se mantiene el aviso conocido del bundle de más de 500 kB. Las pruebas de aceptación comprueban ausencia de firma en el paso 5, bloqueo hasta confirmar revisión, orden visual de la confirmación después del visor, uso del mismo artefacto, cancelación segura del certificado y el circuito T1/T2/T3. Capturas desktop/tablet de revisión final inspeccionadas.

Resultado final: 24 pruebas de aceptación aprobadas en utaped-acceptance; entorno descartable limpiado. Evidencia ignorada: backend/target/2026-10-27-signature-preparation-acceptance-verified.log. Entorno manual utaped-sample actualizado, sin restablecer datos; frontend y backend responden HTTP 200. Sin cambios backend ni de formatos documentales.

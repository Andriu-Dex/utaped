# Datos de referencia Julio–Diciembre 2026

Confirmación del propietario, 2026-10-17: **los documentos de templates son fuentes oficiales**. Sus datos institucionales son fiables dentro de lo que cada fuente define; no son nombres inventados. La excepción confirmada es el tamaño Carta de una referencia: el producto usa **A4** para T1 y T2. Cuentas, contraseñas, asignaciones del escenario y ventanas técnicas de prueba no adquieren carácter institucional por esta confirmación.


Decisión del propietario: 2026-10-14. Semillas exclusivamente para el perfil `development-seed`, base `utaped_dev`. Los datos curados están en `backend/src/main/resources/development/institutional-reference.json`; no se leen los archivos originales durante el arranque ni se añaden dependencias de Excel/PDF al backend.

## Procedencia

| Fuente en docs/templates | Uso |
|---|---|
| comisiones.xlsx | 23 nombres de unidades, comisiones y clubes; normalización de espacios y clasificación por denominación. |
| UNIDADES Y COMISIONES JULIO_DICIEMBRE_2026_(SISTEMAS-SOFTWARE-TI).pdf | Período, carreras y relaciones de unidades/comisiones con TI, Software y Sistemas; denominaciones específicas ante diferencias con el Excel. |
| TI-JD2026-UTIT-PLAN-V01-signed.pdf | Unidad de Titulación, período, nueve actividades de su matriz, quince recursos y once medios, conservando categorías POA/plan de mejoras/acción de mejora. |
| PROPUESTAS DE MEJORA CARRERA DE TI.xlsx | 46 propuestas de sus 14 hojas como actividades opcionales de mejora; corresponden al grupo temático de cada hoja. No representan integraciones entregadas ni políticas aprobadas. |
| Plantillas DOCX T1 y T2 | Formatos del producto ya existentes, conservados sin cambios. |

Los códigos TI/SW/SIS identifican el catálogo técnico de ejemplo; no son códigos documentales oficiales. El error «FISER» del Excel se reconcilia con «FISEI» del PDF. Las propuestas de Congreso CSEI se asocian al grupo de eventos, y las del Club Académico de TI a ese club. Los grupos sin carrera determinable se registran sin inventar ámbitos. Los clubes de TI/Software tienen su carrera por denominación. La Comisión de Plataformas Virtuales figura como FISEI en el PDF y no recibe una carrera supuesta.

## Configuración del escenario

- Período fijo: **Julio–Diciembre 2026**; límites de mes 01/07/2026–31/12/2026.
- Elaboración y revisión: mismo intervalo completo, configuración técnica para pruebas, no calendario institucional confirmado.
- 3 carreras y 23 grupos; 30 ámbitos para combinaciones respaldadas por las fuentes y denominaciones.
- 55 actividades opcionales, no seleccionadas automáticamente en un Plan. Las fechas de la matriz del PDF (11/08/2026–31/12/2026) son referencia del documento original; las actividades de catálogo no imponen un cronograma a todo nuevo Plan.
- 15 recursos y 11 medios. Los medios del ejemplo usan carga PDF: la información del PDF se representa documentalmente, sin generar URLs de evidencia falsas.
- Las siete cuentas `dev.*` conservan sus credenciales y correos `example.invalid`. Principal/coordinador/integrante y flujos T1/T2 son asignaciones ficticias de prueba, no reproducciones de cargos oficiales.
- No se importan docentes reales como cuentas, correos institucionales, contraseñas, certificados, firmas, porcentajes de ejecución ni aprobaciones del PDF. No se crea un documento firmado.

## Conservación y actualización

Identificadores de semilla estables; inserción sin duplicados. Reiniciar no cambia contraseñas ni restaura ediciones administrativas. La transición desde el escenario anterior cambia únicamente los nombres genéricos exactos originales y, para el período, exige ACTIVE, flujos sin activar y ausencia de documentos. Grupo/carrera tampoco se renombran si ya están usados por un documento. Se retiran las opciones genéricas originales de catálogo sin purgarlas; las matrices y artefactos existentes conservan sus snapshots.

Los datos adicionales creados por pruebas/revisiones permanecen. No ejecutar un reinicio destructivo para actualizar estas semillas. Un reinicio completo con `-ResetData` es una operación voluntaria distinta documentada en [entorno de ejemplo](sample-environment.md), que elimina los registros de sample.

No modifica la base principal ni configura el calendario productivo. Una vez terminado 2026, las ventanas respetarán las fechas almacenadas; el sistema no desplaza silenciosamente este período.

## Limpieza autorizada del escenario — 2026-10-17

El propietario permite reemplazar los datos de desarrollo actuales. Para retirar las pruebas acumuladas se reconstruyen los catálogos y ámbitos a partir del JSON trazable a templates, conservando los siete usuarios dev y sus hashes de contraseña. Se eliminan de la DB los documentos, cuentas extra, grupos/períodos/carreras y demás registros operativos de ejemplo; las sesiones se invalidan. Las referencias se regeneran con sus mismos identificadores estables y los nombres oficiales. No es un cambio de reglas de producción ni una migración Flyway.

La herramienta scripts/clean-development-data.py requiere el proyecto exacto utaped-sample, la DB utaped_dev, el perfil development-seed y un respaldo emparejado verificado de esa misma base. Sin --apply solo muestra el alcance. La operación no elimina archivos del volumen: los antiguos archivos sin referencias quedan fuera de la UI y permanecen también en el respaldo; no se presenta como purga física de almacenamiento.

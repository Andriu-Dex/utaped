# Revisión del encabezado frente al ejemplo institucional

## Alcance y fuentes

Revisión sin modificar código, templates ni datos. Se contrastó docs/templates/TI-JD2026-UTIT-PLAN-V01-signed.pdf (páginas 1 y 3) con backend/target/document-qa/t1-short.pdf, generado por las pruebas de la composición actual engine10, y con InstitutionalHeader.java. El ejemplo y la salida actual son A4; las diferencias no se justifican por la conversión de Carta a A4.

Las imágenes completas revisadas se conservaron, ignoradas por Git, en backend/target/header-source-review. Se usó renderizado y extracción de posiciones/tamaños del PDF, no solamente comparación de textos. El documento actual es una fixture de pruebas: los nombres y contenidos propios de cada Plan no deben confundirse con diferencias de formato.

## Diferencias confirmadas

| Elemento | Ejemplo institucional | Composición actual | Ajuste recomendado |
|---|---|---|---|
| Procedencia | Celda izquierda solo con la etiqueta; celda derecha reúne nombre completo de facultad/unidad y carrera(s) | La izquierda contiene etiqueta más unidad; la derecha solo carrera | Mantener la izquierda como etiqueta y agrupar la información real a la derecha |
| Ancho y posición | Aproximadamente 168 mm, desde 21 mm del borde izquierdo | Aproximadamente 150 mm, desde 30 mm del borde izquierdo | Reproducir ancho y desplazamiento del encabezado del ejemplo, sin cambiar arbitrariamente el margen del cuerpo |
| Alturas | Encabezado de aproximadamente 31 mm; fecha compacta | Aproximadamente 35 mm; la fila de fecha es más alta | Ajustar alturas de cada fila al ejemplo, con expansión cuando el contenido lo necesite |
| Título del Plan | Mayúsculas, Times New Roman Bold de 10 pt, azul #2F5496 y espaciado entre caracteres | Valor conserva su capitalización; 9 pt, azul #1F4E79 y espaciado ordinario | Aplicar el estilo institucional al título sin cambiar la identidad/título almacenado |
| Etiquetas de procedencia y fecha | 7 pt, negrita y espaciado entre caracteres; la fecha en azul | 9 pt, sin negrita, sin ese espaciado | Recuperar la jerarquía y estilos del ejemplo |
| Fecha | Mes escrito: 07 de agosto de 2026 | Numérica: 23/10/2026 | Usar formato español con mes en letras, manteniendo la fecha real del documento y no la del ejemplo |
| Leyenda del logo | Dos líneas: SISTEMA DE GESTIÓN / DE LA CALIDAD | Tres líneas: SISTEMA DE / GESTIÓN DE LA / CALIDAD | Recuperar distribución, ancho disponible y alineación del bloque de logo |
| Bordes y gris | Bordes más finos y gris de referencia | Borde de 1 pt en gris azulado y gris #E7E6E6 | Contrastar y reproducir grosor/tonos del ejemplo |

La conversión actual a Liberation Serif conserva una alternativa métrica a Times New Roman, pero no reproduce por sí sola el espaciado y la distribución del original. La fuente del título universitario sí mantiene 11 pt en ambos; no todo el encabezado necesita rediseñarse.

## Decisiones que deben conservarse

- Etiqueta específica Unidad académica o Unidad administrativa conforme a la decisión explícita más reciente del propietario. El ejemplo conserva ambas alternativas genéricas, pero eso no revoca lo confirmado.
- Fecha en celda derecha y etiqueta en izquierda, ya corregidas.
- Procedencia de ámbitos y Planes base; no sustituir automáticamente la comisión actual por Unidad de Titulación ni copiar carreras del ejemplo a todos los Planes.
- No inferir varias carreras para un ámbito que actualmente tiene una sola. El ejemplo contiene dos; mostrar únicamente las realmente asociadas al documento y tratar una eventual procedencia multicarrera como alcance distinto.
- A4, documentos independientes y conservación de los bytes de artefactos firmados.

## Conclusión

El encabezado todavía es una aproximación y requiere una corrección integral basada en el ejemplo institucional, principalmente en la fila de procedencia, geometría y estilos. No basta con corregir únicamente la fecha o los bordes.

No se ejecutaron pruebas de aplicación porque esta entrega solo revisa y documenta diferencias. Sin commit.

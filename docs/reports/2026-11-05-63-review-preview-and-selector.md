# Acceso a revisión, previsualización y selector de comisión

La bandeja de revisión permanece disponible para los usuarios institucionales y el servidor sigue limitando sus tareas y documentos. Se elimina la deducción incompleta de permisos basada en el texto de los flujos, que podía ocultar el acceso ante asignaciones por rol, cargo o cambios durante una sesión.

La caché en memoria de páginas de borrador se vacía al cerrar o expirar la sesión. Al editar la búsqueda de comisión se invalida la selección anterior y la opción resaltada se comunica mediante `aria-activedescendant`. Los enlaces a reportes históricos retirados apuntan a la revisión conservada en Git, sin restaurar los archivos eliminados.

Validación: lint y compilación frontend; pruebas de aceptación aisladas para acceso a revisión, cambio de selección con teclado, limpieza de caché al cerrar sesión, creación con documentos preparados, referencias institucionales y confirmaciones anidadas. Se actualizaron los selectores utilizados por los recorridos existentes. Se completó también el recorrido automatizado multirrol de captura en un entorno aislado. Estas comprobaciones no sustituyen una auditoría integral ni la aceptación institucional.

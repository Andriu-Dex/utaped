# Navegación secuencial del Plan T1

Se incorporaron controles Anterior y Siguiente en Anexos y Preparación; Firma permite regresar a Preparación. Volver al Plan conserva su función de regresar a Información general.

La navegación recupera el documento actual antes de cambiar de sección. Anexos pide confirmación cuando hay datos locales sin guardar; cancelar conserva el formulario. Preparación y Firma bloquean la navegación durante operaciones o cambios pendientes. Los errores de navegación se muestran mediante toast sin abandonar la sección.

Se conservaron los cambios previamente staged de MatrixEditor y document-workspace.css. No hay cambios de backend, permisos ni firma.

Validación: compilación TypeScript/Vite y lint aprobados. Comprobación posterior con Playwright: recorrido Anexos → Preparación → Firma y regreso a Anexos aprobado, integrado en la suite de aceptación de la revisión siguiente. Persiste la advertencia de tamaño del bundle.

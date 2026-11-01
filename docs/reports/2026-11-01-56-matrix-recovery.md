# Recuperación de actividades incompletas

El editor conserva en el servidor, por documento y usuario, los cambios incompletos de la matriz como borrador de recuperación independiente de la matriz oficial. Al volver a abrir el documento se recuperan; si cambió la versión de base se solicita una decisión antes de restaurarlos. El guardado oficial elimina el borrador confirmado y mantiene las validaciones y el bloqueo de preparación mientras existan cambios pendientes.

La recuperación controla concurrencia mediante su propia revisión. Ante errores de conexión, la interfaz conserva lo escrito y permite reintentar; deshacer los cambios elimina el borrador de recuperación. El cambio de responsable no expone ni atribuye al nuevo responsable los borradores privados anteriores.

Se verificaron los casos dirigidos de aislamiento, concurrencia, validaciones, cambio de responsable y recuperación después de recargar, además de fallo de conexión y reintento. También se ejecutaron lint, comprobación de tipos y compilación del frontend. Estas comprobaciones dirigidas no sustituyen una suite integral ni la aceptación institucional.

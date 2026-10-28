# Revisión de navegación de visores

Se revisaron los cambios del propietario en el staged antes de ampliar la reportería: medición compartida de la barra superior, navegación de páginas fija, superficies opacas y estilos oscuros para lectura, previsualización y ampliación. Se incorpora la nueva norma visual de AGENTS.md.

Se retiró la medición duplicada del inspector de revisión: intentaba acceder al contenedor antes de terminar su carga y no volvía a ejecutarse. La medición compartida en App proporciona la altura de la barra a estos visores. Las reglas de firma, consulta y protección de borradores se conservan.

Verificación: lint y build/TypeScript correctos; tres pruebas de aceptación aisladas correctas sobre preparación automática, pasos persistentes y firma de revisión. La prueba de revisión comprueba la posición real del navegador al desplazarse, en escritorio y tablet, además de las confirmaciones de salida y conservación de contraseña temporal en memoria. Sin cambios backend ni en datos manuales.

Este commit cierra los cambios anteriores a la implementación del panel de gestión y sus reportes.

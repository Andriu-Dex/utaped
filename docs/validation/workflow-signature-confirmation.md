# Ficha de confirmación de flujo y firma

Fecha: 2026-10-04. Plantilla para recoger decisiones; no es un flujo aprobado. Completar una ficha por grupo y tipo T1/T2. Hasta confirmar Q-003/Q-018 y la política Q-007, no habilitar finalización, envío ni aprobación.

## Identificación

- Grupo/unidad: pendiente.
- Tipo documental: pendiente.
- Nombre y revisión técnica de configuración: pendiente.
- Persona/área que valida, fecha y evidencia: pendiente.
- Alcance de la validación y vigencia: pendiente.

## Etapas

| Orden | Nombre/acción | Personas, rol u órgano | Representantes si corresponde | Firma obligatoria | Condición de avance | Efecto de devolución |
|---|---|---|---|---|---|---|
| Por completar | Por completar | Por completar | Por completar | Por confirmar | Por confirmar | Por confirmar |

Precisar si las personas actúan en paralelo o secuencialmente; quiénes son obligatorias; si basta una decisión y para qué etapa; quién representa al órgano; si una decisión colegiada exige firma personal. No equiparar falta de intervención a aprobación. Registrar tratamiento de bajas/cambios de miembros y fechas de corrección, sin inventar reaperturas.

## Certificados .p12/.pfx

Mecanismo confirmado por el propietario. Solicitar únicamente especificaciones y certificados públicos necesarios; no incorporar claves privadas ni contraseñas a esta ficha o al repositorio.

| Decisión | Respuesta/evidencia pendiente |
|---|---|
| Vinculación de titular del certificado con cuenta autenticada | Identificador comprobable, responsable y procedimiento de alta/renovación/revocación de la vinculación. Comparar nombres visibles no acredita identidad. |
| Confianza | Autoridades/cadenas públicas admitidas y cómo se mantienen. No aceptar automáticamente raíces incluidas en el .p12. |
| Revocación | OCSP/CRL, disponibilidad y comportamiento ante resultado desconocido/fallo. |
| Perfil de firma | Perfil concreto, interoperabilidad requerida, múltiples firmas y ubicación visible. |
| Tiempo | Necesidad de TSA, proveedor y tratamiento de errores. La fecha del servidor no equivale a sello de tiempo. |
| Transporte y secretos | HTTPS obligatorio, procesamiento en memoria, límites, reintentos y ausencia de logs/almacenamiento del material privado. |
| Aceptación | Archivos de prueba permitidos, verificador institucional y evidencia de validación. |

## Cierre

Registrar decisiones en open-questions.md y el contrato, agregar escenarios de aceptación y recién entonces implementar las transiciones correspondientes. Una configuración vigente administrativa sigue siendo una revisión técnica hasta obtener esta evidencia.

# Habilitar la asistencia de redacción

La aplicación funciona sin proveedor; de fábrica `AI_WRITING_ENABLED=false`. No es necesario configurar IA para iniciar sesión, editar, guardar o completar el circuito documental.

## Configuración manual

1. Elegir una cuenta del sistema, revisar tratamiento de datos/cuotas y comprobar un modelo habilitado en esa cuenta. Los nombres históricos de modelos no acreditan disponibilidad actual.
2. En el archivo `.env` privado del entorno elegido, configurar:

```dotenv
AI_WRITING_ENABLED=true
AI_WRITING_PROVIDER=GROQ
AI_WRITING_MODEL=<modelo habilitado en la cuenta>
GROQ_API_KEY=<clave privada>
```

Para OpenAI, usar `AI_WRITING_PROVIDER=OPENAI` y `OPENAI_API_KEY`, con su modelo habilitado. No se requiere configurar ambos proveedores. Nunca usar prefijo `VITE_`, incluir claves en archivos rastreados ni copiar salidas de `docker compose config` que las revelen.

3. Recrear backend para incorporar las variables:

```powershell
docker compose --profile web up -d --build backend
```

En el entorno aislado de ejemplo usar `.env.sample` y mantener el nombre del proyecto/overrides:

```powershell
docker compose --env-file .env.sample -p utaped-sample -f compose.yml -f compose.sample.yml --profile web up -d --build backend
```

Con Maven local, exportar las mismas variables en la sesión PowerShell antes de iniciar el backend. La guía [de arranque local](local-setup.md) sigue aplicando; claves/cookies/backend no se trasladan a Vite.

4. Entrar como responsable de un borrador, escribir texto existente, abrir «Mejorar redacción», elegir estilo y autorizar el envío. Comparar, aplicar o descartar. Aplicar exige posteriormente guardar el borrador.

Se debe comprobar con texto ficticio antes de autorizar contenido institucional. En esta entrega se probaron contratos HTTP con servidores locales y la interfaz con respuestas controladas; no se consumieron cuentas externas ni se acreditó calidad/disponibilidad de un modelo real.

## Límites técnicos

| Variable | Inicial | Rango admitido |
|---|---:|---|
| AI_WRITING_TIMEOUT_SECONDS | 30 | 1–60 |
| AI_WRITING_MAX_INPUT_CHARACTERS | 8000 | 1–50000 |
| AI_WRITING_MAX_OUTPUT_TOKENS | 2000 | 1–8000 |
| AI_WRITING_USER_LIMIT | 15 | 1–100 por ventana de 15 minutos |
| AI_WRITING_CONCURRENCY | 2 | 1–20 por instancia |

El modelo también debe admitir el límite de tokens configurado. Una respuesta truncada se rechaza y conserva la edición manual. Un error de clave, cuota, timeout o disponibilidad muestra un mensaje genérico; no se expone el cuerpo privado del proveedor. No hay fallback ni reintentos automáticos.

## Verificación operativa pendiente

Probar disponibilidad del modelo, calidad de redacción en T1/T2 y costos con la cuenta elegida; verificar egreso HTTPS hacia el proveedor y políticas de datos del cliente. `store:false` no promete retención cero. Consultar [datos OpenAI](https://developers.openai.com/api/docs/guides/your-data) o [datos Groq](https://console.groq.com/docs/your-data).

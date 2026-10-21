# Proxy persistente para desarrollo local

Fecha: 2026-10-07. Rama: `feature/modern-workspace-access`. Sin commit, push ni actualización del PR.

## Diagnóstico

`npm run dev` iniciaba Vite correctamente, pero `/api` se reenviaba por defecto a `http://localhost:8080`. El backend Docker del propietario está publicado en `127.0.0.1:18080`, y el `.env` principal contiene `BACKEND_PORT=18080`. La configuración Vite no había cambiado desde su incorporación inicial; exportar `$env:API_TARGET` solucionaba solo la sesión actual de PowerShell. Al abrir otra terminal, la variable no se conserva y reaparece la discrepancia.

El rechazo de conexión impedía cargar CSRF y CAPTCHA; no indicaba credenciales incorrectas ni un problema con la separación del administrador.

## Cambios

- `frontend/vite.config.ts` lee automáticamente `BACKEND_PORT` desde el entorno y los archivos `.env` de la raíz, con `loadEnv` y rutas relativas al archivo de configuración.
- `API_TARGET` explícito, desde la terminal o los archivos `.env` de frontend, conserva prioridad para otras instalaciones.
- Sin configuración utiliza `127.0.0.1:8080`, coherente con Compose predeterminado. No se fija 18080 para todos los desarrolladores.
- Solo se consultan prefijos `BACKEND_PORT` y `API_TARGET` para configurar el proceso de desarrollo; no se exponen secretos de la raíz mediante variables `VITE_`.
- Se agrega `frontend/.env.example` con override opcional comentado y se actualizan la guía local y el estado de implementación.
- No se cambian el `.env` privado, los puertos de Docker, el puerto institucional 64430 ni los cambios previamente staged del propietario.

## Verificación ejecutada

- `npm --prefix frontend run lint`: correcto.
- `npm --prefix frontend run build`: TypeScript y Vite correctos.
- Vite iniciado temporalmente en 15174, sin `API_TARGET` ni `BACKEND_PORT` exportados: resolvió automáticamente `http://127.0.0.1:18080` desde el `.env` principal.
- Solicitudes a través de ese proxy: CSRF HTTP 200; CAPTCHA HTTP 200, `image/png`, cuerpo no vacío. La comprobación inicial esperaba SVG; se corrigió la expectativa al formato PNG realmente servido y la repetición pasó.
- Override explícito `API_TARGET=http://127.0.0.1:28080`: prioridad comprobada mediante resolución de configuración, sin contactar ese servidor.
- El servidor temporal de verificación se cerró. No se iniciaron ni detuvieron los contenedores del propietario. No se repitió toda la suite E2E: el cambio afecta la configuración de desarrollo y se verificaron las dos solicitudes que fallaban.

## Uso

Detener Vite con Ctrl+C y volver a ejecutar, desde `frontend`:

```powershell
npm run dev
```

No es necesario volver a exportar `API_TARGET` en el entorno actual del propietario. Los cambios de variables requieren reiniciar Vite. Si el backend se detiene, aún habrá rechazo de conexión: Vite no arranca el backend. Para iniciarlo con sus dependencias, desde la raíz:

```powershell
docker compose up -d db mail backend
```

La configuración del proxy de Vite no modifica Nginx ni despliega cambios en la interfaz Docker.

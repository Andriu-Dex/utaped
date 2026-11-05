# Reporte de Modernización del CAPTCHA de Audio Institucional

**Fecha:** 2026-11-05  
**Identificador:** `2026-11-05-58-captcha-audio-neural-modernization`  
**Estado:** Implementado, validado y desplegado localmente.

---

## 1. Objetivo

Resolver los problemas de accesibilidad, inteligibilidad y experiencia de usuario en el CAPTCHA de audio de inicio de sesión y recuperación de contraseña:
1. Eliminar la fricción de doble interacción haciendo que la reproducción comience inmediatamente al pulsar el botón de audio.
2. Corregir la distorsión, aspereza digital y discontinuidades acústicas causadas por truncamiento de muestras en el algoritmo de remuestreo del backend.
3. Sustituir la voz sintética por formantes (eSpeak NG) y el prefijo redundante («letra...» / «número...») por clips de voz neuronal humana en español ecuatoriano (`es-EC-AndreaNeural`), eliminando los silencios muertos acumulados que producían una sensación de audio trabado y cortes abruptos.

---

## 2. Cambios Realizados

### 2.1. Frontend (`frontend/src/identity/CaptchaField.tsx`)
* **Reproducción automática inmediata:** Se sustituyó la apertura pasiva del reproductor por `playAudio()`, la cual despliega el elemento `<audio>` e invoca inmediatamente `play()`.
* **Rebobinado interactivo:** Si el reproductor ya se encuentra visible y el usuario presiona nuevamente el botón de audio (*«Escuchar código»*), la reproducción se reinicia desde el segundo 0 (`currentTime = 0; play()`).
* **Limpieza de estado al renovar:** Al pulsar el botón de cambio de código de verificación, se desmonta el reproductor y se restablece el estado para no reproducir audios obsoletos o desfasados.
* **Preload:** Configurado en `preload="auto"` para disponibilidad instantánea del buffer de audio.

### 2.2. Backend (`backend/src/main/java/ec/edu/uta/utaped/identity/CaptchaAudio.java`)
* **Interpolación lineal de muestras:** Se sustituyó el truncamiento entero directo (`int i = ((int)offset) * 2`) en la variación de velocidad aleatoria por una interpolación lineal continua entre muestras adyacentes (`(1.0 - frac) * s0 + frac * s1`). Esto eliminó 154 discontinuidades extremas de amplitud (de hasta 14 933 unidades en PCM de 16 bits), suprimiendo chasquidos digitales (*pops*) y zumbido áspero.
* **Cadencia rítmica fluida:** Se reajustó la pausa entre caracteres a un intervalo de dictado natural (~350 ms; `7718 + random.nextInt(1102)` muestras) con micro-variaciones de velocidad y ganancia para preservar la protección ante bots sin generar silencios excesivos ni sensación de traba.

### 2.3. Herramienta y Activos de Audio (`backend/src/main/resources/captcha-audio/`, `scripts/generate-captcha-clips.py`)
* **Generación neuronal offline:** Se actualizó `scripts/generate-captcha-clips.py` para sintetizar los 32 caracteres del alfabeto institucional (`23456789ABCDEFGHJKLMNPQRSTUVWXYZ`) con la voz ecuatoriana `es-EC-AndreaNeural` vía `edge-tts` y procesamiento `ffmpeg` / `numpy`.
* **Pronunciación directa:** Se eliminaron las palabras intermedias redundantes (*«letra»* y *«número»*), adoptando nombres y fonética directa en español (*«cu ... erre ... nueve ... tres ... tres»*).
* **Recorte de silencios y fundido suave:** Se recortaron los silencios muertos de inicio y fin dejando 40 ms de margen natural, y se aplicó una atenuación de coseno suave de 15 ms (*fade-in* y *fade-out*) en los extremos de cada clip para asegurar ausencia de chasquidos al abrir o cerrar muestras.
* **Empaquetado estático:** Los 32 archivos `.wav` (PCM 16-bit mono a 22 050 Hz) quedan empaquetados en el JAR de la aplicación, manteniendo cero dependencias de servicios externos o internet en tiempo de ejecución.
* **Documentación:** Actualizado `backend/src/main/resources/captcha-audio/README.md`.

---

## 3. Validaciones Ejecutadas

| Tipo de Verificación | Herramienta / Suite | Resultado |
|---|---|---|
| **Pruebas unitarias backend** | `scripts/test-backend.ps1` (`CaptchaAudioTests`, `CaptchaGenerationTests`) | **Aprobado** (2/2 pruebas). Verifica límites de duración, canales mono 16-bit a 22.05 kHz y muestras audibles sin metadatos. |
| **Empaquetado de backend** | `mvn -B -DskipTests package` en contenedor Maven | **BUILD SUCCESS** (`utaped-0.0.1-SNAPSHOT.jar`). Desplegado al contenedor `utaped-backend-1`. |
| **Linting frontend** | `npm --prefix frontend run lint` (`oxlint`) | **0 errores, 0 advertencias** (133 archivos analizados). |
| **Compilación frontend** | `npm --prefix frontend run build` (`tsc -b && vite build`) | **Exitoso** (dist generado correctamente sin errores de tipos). |
| **Pruebas E2E** | Playwright `tests/e2e/captcha-audio.spec.ts` sobre entorno en vivo | **Aprobado** (2/2 pruebas en 2.8s). Teclado, repetición, renovación, conservación de credenciales y descarte de retos obsoletos. |
| **Análisis acústico en vivo** | Script Python contra `/api/auth/captcha/audio` | **200 OK**. Duración 4.70s para 5 caracteres, sin clipping, amplitud normalizada (máx: 20 554, mín: -15 808). |

---

## 4. Alcance y Pendientes

* **Alcance cubierto:** Accesibilidad del CAPTCHA en inicio de sesión y recuperación de contraseña, naturalidad del audio, eliminación de distorsiones y pausas excesivas.
* **Pendientes institucionales no afectados:** Matriz de participantes, firmas electrónicas FirmaEC y formalización institucional de perfiles permanecen conforme al estado reportado.


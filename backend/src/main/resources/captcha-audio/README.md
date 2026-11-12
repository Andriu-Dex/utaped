# Clips de voz del CAPTCHA

32 archivos WAV mono PCM de 16 bits / 22 050 Hz, con nombres y pronunciación directa de letras y números en español. El servidor compone los clips con pausas fluidas y micro-variaciones de velocidad/volumen para robustez ante bots. No se envían textos a servicios de terceros en tiempo de ejecución ni se requiere sintetizador en producción.

Regeneración: `python scripts/generate-captcha-clips.py`. Los archivos actuales se obtuvieron con voz neuronal en español ecuatoriano (`es-EC-AndreaNeural`), normalizados con recorte de silencios muertos y atenuación suave de bordes (*fade-in / fade-out* de 15 ms).

Estas frases son recursos compartidos estáticos; nunca incluir aquí códigos emitidos, respuestas de usuarios o certificados.

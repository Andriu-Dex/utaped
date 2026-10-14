# Clips de voz del CAPTCHA

32 archivos WAV mono PCM de 16 bits / 22 050 Hz, con nombres de letras y números en español. El servidor compone seis clips con pausas y pequeñas variaciones de velocidad/volumen. No se envían textos a un servicio de voz ni se requiere un sintetizador en producción.

Regeneración: `python scripts/generate-captcha-clips.py`, con `espeak-ng` instalado en el entorno de preparación. Los archivos actuales se obtuvieron con eSpeak NG 1.51, voz `es`, velocidad 145, a partir de las frases del script. Se distribuyen formas de onda de esas frases; no se redistribuye el ejecutable, código o datos de voz del sintetizador.

Herramienta de preparación: [eSpeak NG](https://github.com/espeak-ng/espeak-ng), licencia [GPL-3.0-or-later](https://github.com/espeak-ng/espeak-ng/blob/master/COPYING). Conservar los avisos/licencias originales si en el futuro se distribuye el sintetizador o sus datos. La [FAQ de GNU sobre salidas de programas](https://www.gnu.org/licenses/gpl-faq.html#GPLOutput) distingue la licencia del programa de la de su salida.

Estas frases son recursos compartidos; nunca incluir aquí códigos emitidos, respuestas de usuarios o certificados. La claridad de pronunciación requiere aceptación con usuarios, además de las pruebas automáticas del formato/reproducción.

# Estrategia de proveedores de IA

La asistencia de redacción continuará simulada durante la fase de mockup. La integración futura se aislará detrás de `AiWritingService`, que dependerá de una interfaz `AiProvider`.

```text
React → Spring Boot → AiWritingService → AiProvider
                                      ├─ GroqAiProvider
                                      └─ OpenAiProvider
```

- Desarrollo: GroqCloud, con GPT-OSS 120B como modelo objetivo configurable.
- Producción: OpenAI API, con GPT-5.6 Luna como modelo objetivo configurable.
- Mockup actual: `MockAiProvider` local, determinista y sin tráfico de red.

Los nombres de modelos son configuración futura. El backend seleccionará el proveedor mediante `AI_PROVIDER=groq` o `AI_PROVIDER=openai`, acompañado de variables de modelo y credenciales administradas fuera del frontend.

Las claves nunca se incluirán en React, variables `VITE_*`, `localStorage`, logs ni artefactos documentales. Spring Boot realizará las llamadas y aplicará autenticación, límites y manejo de errores. Ante indisponibilidad, la interfaz conservará el texto original y permitirá continuar sin asistencia.


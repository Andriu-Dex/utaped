# Entorno de desarrollo con datos de ejemplo

El entorno de ejemplo tiene su propia base `utaped_dev`, volúmenes, credenciales y proyecto Compose `utaped-sample`. No modifica los usuarios ni datos del entorno principal. Requiere Docker Desktop activo con contenedores Linux.

Desde la raíz del repositorio, en PowerShell:

```powershell
./scripts/start-development-sample.ps1
```

Abrir http://127.0.0.1:64431. La primera ejecución genera `.env.sample`, ignorado por Git, con contraseñas aleatorias. Consultar localmente `UTAPED_SEED_PASSWORD` para iniciar sesión; no compartir este archivo. El puerto institucional principal sigue siendo 64430. En este entorno aislado se usan API 18082, PostgreSQL 15435 y Mailpit 18027.

| Usuario | Uso del escenario |
|---|---|
| dev.admin | Usuarios, carreras, ámbitos, catálogos, flujos y certificados públicos. |
| dev.principal | Elaboración del T1 y del Informe derivado. |
| dev.coordinator | Coordinación y prórrogas individuales. |
| dev.reviewer | Primera etapa del flujo. |
| dev.approver | Aprobación final. |
| dev.member | Consulta y responsabilidad de actividades si se asigna. |
| dev.outsider | Comprobación de aislamiento; sin acceso al ámbito de ejemplo. |

Los correos usan `example.invalid`. Los períodos se generan respecto al día de instalación; no son períodos oficiales. Se incluyen carrera, comisión, ámbito, integrantes y catálogos de ejemplo. No se crea un Plan firmado ficticio. Los flujos iniciales T1/T2 tienen dos personas sin firma de revisión obligatoria; ADMIN puede configurar una política distinta.

Para firmar, vincular la huella pública de un certificado de pruebas al usuario desde Certificados. Después el usuario aporta su .p12/.pfx y contraseña en cada firma. No hay certificados ni claves privadas preinstalados. El perfil permite HTTP exclusivamente para este escenario local; la operación institucional requiere HTTPS. Los datos de ejemplo no acreditan asignaciones institucionales.

Las semillas son idempotentes: repetir el arranque no restablece contraseñas ni sobrescribe cambios existentes. El perfil `development-seed` rechaza bases distintas de `utaped_dev`.

Para reiniciar voluntariamente solo los datos del entorno de ejemplo:

```powershell
./scripts/start-development-sample.ps1 -ResetData
```

Este comando detiene la aplicación de ejemplo antes de truncar sus tablas, verifica el nombre de base, conserva Flyway y regenera usuarios/catálogos. Destruye registros de ese escenario; no usarlo para una base institucional. Los archivos privados antiguos del volumen pueden permanecer sin referencia hasta su limpieza administrativa; el comando no declara una purga de almacenamiento.

Para detener sin borrar datos:

```powershell
docker compose --env-file .env.sample -p utaped-sample -f compose.yml -f compose.sample.yml --profile web down
```

## Prueba del circuito

Con el entorno iniciado y Chromium instalado:

```powershell
cd frontend
npm run test:e2e:sample
```

La prueba crea registros adicionales solo en el entorno de ejemplo y utiliza certificados temporales de pruebas en memoria. Recorre asignación/activación de flujos, elaboración, devolución, corrección, firmas sucesivas, aprobación, evidencia, T2, actualización formal y cierre histórico con ZIP autorizado, además de aislamiento y vista oscura en tableta. No utilizar certificados personales reales para las pruebas automatizadas.

Desde V19, las semillas asignan las revisiones de ejemplo sin activar automáticamente el período. Para probar firmas manualmente: Administración → Períodos académicos → elegir período → revisar T1/T2 de cada ámbito → Activar flujos del período. No cierre el período de ejemplo hasta terminar el escenario: quedará de solo lectura y no existe reapertura automática. [Contrato y pasos](../api/period-control.md).

## Administración contextual y alertas

Tras reconstruir el entorno, Flyway incorpora V20 sin reiniciar los datos. En Carreras y ámbitos, ADMIN puede editar carreras y conceder recuperación de cuentas al COORDINATOR seleccionado. En Mi organización, ese coordinador ve el historial y las cuentas recuperables del ámbito. Las semillas no conceden automáticamente esta autorización. En Períodos académicos aparece la configuración técnica de avisos y la comprobación manual. Variables opcionales y defaults: [contrato](../api/contextual-administration.md).

La suite sample añade un escenario de edición de carrera, historial, autorización/revocación, reset y cambio obligatorio de una cuenta ficticia nueva; conserva las credenciales semilla. El circuito T1/T2 comprueba avisos y el historial visible de una prórroga individual.

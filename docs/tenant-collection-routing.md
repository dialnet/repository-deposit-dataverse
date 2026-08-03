# Enrutado de depósito por tenant (colección por tenant)

Esta funcionalidad permite que **una sola instancia** de este plugin deposite en
**colecciones distintas de un mismo Dataverse** según el tenant de OpenCDMP que publica,
en lugar de levantar un plugin de depósito por tenant.

- **Misma instalación de Dataverse y mismo token** para todos los tenants (variables de
  entorno del plugin: `DEPOSIT_DATAVERSE_URL`, `DEPOSIT_DATAVERSE_ACCESS_TOKEN`).
- **Lo único que cambia por tenant es la colección** (el *alias* del dataverse hoja donde
  se crean los datasets).

## Cómo funciona

OpenCDMP construye la llamada de depósito como `{deposit_source.url} + "/api/deposit"`. Si en
la configuración de *deposit* del tenant se pone la `url` del source con un segmento de ruta:

```
url = https://<host-del-plugin>/t/<aliasColeccion>
```

la petición llega a `https://<host-del-plugin>/t/<aliasColeccion>/api/deposit`. El plugin
expone un controlador que captura `<aliasColeccion>` y lo usa como colección destino,
sobreescribiendo el identificador derivado del plan.

- Controlador: [`TenantScopedDepositController`](../web/src/main/java/org/opencdmp/deposit/controller/TenantScopedDepositController.java)
  mapeado en `/t/{targetAlias}/api/deposit` (+ `/configuration`, `/logo`, `/authenticate`).
- El servicio deposita en `POST {repositoryUrl}/dataverses/{targetAlias}/datasets` cuando el
  alias viene por la ruta; si no, mantiene el comportamiento original
  (`DataverseBuilder.buildDataverseIdentifier(plan)` → semántico `dataverse.identifier` del
  plan, y si no, `DEPOSIT_DATAVERSE_HOST`).
- El endpoint clásico [`/api/deposit`](../web/src/main/java/org/opencdmp/deposit/controller/DepositController.java)
  se mantiene intacto como comportamiento por defecto (colección por defecto).

El `targetAlias` se valida contra `^[A-Za-z0-9_-]+$` para que no pueda alterar la ruta de la
llamada saliente a Dataverse.

### El alias es el de la colección hoja

En Dataverse el *alias* es único en toda la instalación, no una ruta. Si tienes anidamiento
(`root` → colección del tenant → colección de PGDs), el valor a usar es el **alias de la
colección hoja** (p. ej. `unirioja-pgd`), no una ruta compuesta. Debe ser URL-safe
(`A-Z a-z 0-9 - _`), y el token compartido debe tener permiso para crear/publicar datasets
en esa colección.

## Configuración en OpenCDMP (por tenant)

En *Tenant Configuration → Deposit Plugins* del tenant (o vía
`POST /api/tenant-configuration/persist`, tipo `DepositPlugins`):

| Campo | Valor | Motivo |
|---|---|---|
| `repositoryId` | `dataverse` | Debe coincidir con el `repository-id` que reporta el plugin (`dataverse.yml`). El front usa ese id para `logo`, `get-available-auth-methods` y el depósito; si no coincide, esos endpoints dan **404**. |
| `url` | `https://<host-del-plugin>/t/<alias>` | Sin `/api` (OpenCDMP añade `/api/deposit`). Aquí viaja el alias de la colección. |
| `disableSystemSources` | `true` | Elimina el source de sistema, de modo que el `repositoryId = dataverse` no colisione con el source de sistema (que también es `dataverse`). |
| `issuerUrl` / `clientId` / `clientSecret` / `scope` | los mismos que el source de sistema | Token con el que OpenCDMP llama al plugin. |
| `pdfTransformerId` / `rdaTransformerId` | los de tus file-transformers | Requeridos por el validador. |

> Nota: el `repositoryId` debe ser `dataverse` **con** `disableSystemSources = true`. Un id
> distinto (p. ej. `dataverse-unirioja`) haría que el front pidiese `logo`/`auth-methods`
> por el id que reporta el plugin (`dataverse`) y no encontrara el source → 404.

## Requisitos en el plugin

- `security.yml` debe autorizar el prefijo nuevo: `authorized-endpoints: [ api, t ]`.
- Imagen de runtime basada en **glibc** (`eclipse-temurin:21-jre`, no `-alpine`): las libs
  nativas de netty (QUIC/HTTP3, epoll) que usa el cliente HTTPS necesitan `libgcc_s.so.1`.

## Requisito en OpenCDMP

Los *deposit sources* por tenant llegan con `maxInMemorySizeInBytes = 0` (el modelo de
persistencia no guarda ese campo). El backend debe aplicar un valor por defecto al construir
el `WebClient`; en su defecto, `getConfiguration` y el depósito fallan con
`DataBufferLimitException: Exceeded limit on max bytes to buffer : 0` y el repositorio no
aparece como disponible. (Corregido en el fork PLATICA de OpenCDMP,
`DepositServiceImpl.getDepositClient`.)

## Alcance y limitaciones

- El enrutado por colección **solo aplica al primer depósito** de un plan (`depositFirst`).
- Las **versiones nuevas** de un plan ya depositado se envían con `previousDOI` y
  **actualizan el dataset existente en su colección original** — Dataverse no mueve datasets
  entre colecciones al versionar.
- URL de instalación y token son **compartidos**; si algún tenant necesitara su propia
  instalación o token, habría que extender el plugin a un registro de destinos por clave.

## Ejemplo (tenant UNIRIOJA → colección `unirioja-pgd`)

Deposit source del tenant:

```json
{
  "repositoryId": "dataverse",
  "url": "https://deposit-dataverse/t/unirioja-pgd",
  "issuerUrl": "https://<idp>/realms/<realm>/protocol/openid-connect/token",
  "clientId": "api",
  "clientSecret": "<secret>",
  "scope": "plugins",
  "pdfTransformerId": "docx-file-transformer",
  "rdaTransformerId": "rda-file-transformer"
}
```
con `disableSystemSources: true`.

Al publicar un plan **nuevo** de UNIRIOJA, la llamada llega a
`POST /t/unirioja-pgd/api/deposit` y el dataset se crea en
`{DEPOSIT_DATAVERSE_URL}/dataverses/unirioja-pgd/datasets`.

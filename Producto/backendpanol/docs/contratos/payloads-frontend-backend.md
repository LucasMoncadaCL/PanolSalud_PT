# Payloads Frontend / Backend

- Última actualización: 2026-05-10
- Alcance: contratos JSON usados por frontend y backend

## 1) Payload de error público (backend -> frontend)

Formato uniforme para respuestas de error consumidas por UI:

```json
{
  "code": "400",
  "message": "Solicitud invalida",
  "timestamp": "2026-05-07T10:00:00Z"
}
```

Campos:

1. `code` (string)
- Código HTTP serializado como string (`"400"`, `"401"`, `"500"`, etc.).

2. `message` (string)
- Mensaje general, no detallado internamente.

3. `timestamp` (ISO-8601 string)
- Momento de generación del error.

## 2) Payload de logs internos (warning/error)

Formato estructurado para observabilidad (GCP/log sinks):

```json
{
  "event": "client_error_handled",
  "timestamp": "2026-05-07T10:00:00Z",
  "endpoint_tag": "auth",
  "code": 400,
  "error_type": "HttpMessageNotReadableException",
  "user_uuid": "Ninguno",
  "path": "/api/v2/auth/login",
  "cause": "Detalle técnico del error"
}
```

Campos:

1. `event` (string)
2. `timestamp` (ISO-8601 string)
3. `endpoint_tag` (string)
4. `code` (number)
5. `error_type` (string)
6. `user_uuid` (string UUID | `"Ninguno"`)
7. `path` (string)
8. `cause` (string)

## 3) Payload de login

### Request (`POST /api/v2/auth/login`)

```json
{
  "rut": "22307980",
  "password": "******"
}
```

Nota frontend:
- El RUT puede visualizarse formateado (`22.307.980`), pero se envía limpio (`22307980`).

### Response 200

```json
{
  "accessToken": "<jwt>",
  "role": "COORDINADOR",
  "expiresInSeconds": 3600
}
```

## 4) Payload de logout

### Request (`POST /api/v2/auth/logout`)

- Sin body.
- Requiere header `Authorization: Bearer <jwt>`.

### Response 204

- Sin body.

## 5) Consumo en frontend

- `src/services/apiClient.ts` espera en errores: `code`, `message`, `timestamp`.
- Cualquier otro detalle técnico debe permanecer en logs internos del backend.

# PSD-92 - Endpoint categorias activas (selector de producto)

## Objetivo

Exponer un endpoint para poblar el selector de **categoria** en el formulario de producto (crear/editar), retornando **solo categorias activas** y en un formato liviano (`id`, `name`).

## Endpoint

- `GET /api/categorias/active`
- `GET /api/categories/active` (alias para consumo frontend)

### Respuesta (200)

Array (puede ser vacio):

```json
[
  { "id": 1, "name": "Reactivos" },
  { "id": 2, "name": "Insumos" }
]
```

DTO utilizado:

- `src/main/java/com/panol_project/backendpanol/modules/catalog/category/domain/CategoriaActiveSelectorResponse.java`

## Seguridad

Requisitos:

1. **JWT valido** (se autentica antes de entrar al endpoint).
2. Rol **COORDINADOR** (`hasRole('COORDINADOR')`).

Implementacion:

- Endpoint y `@PreAuthorize`: `src/main/java/com/panol_project/backendpanol/modules/catalog/category/api/CategoriaController.java`
- Regla de filtro (excepcion a `permitAll` en categorias): `src/main/java/com/panol_project/backendpanol/bootstrap/config/SecurityConfig.java`

## Implementacion (resumen)

El endpoint reutiliza el caso de uso existente `listarSelector()` (que ya filtra por `active = true`) y mapea a la respuesta `{id, name}`.

## Pruebas

Test MVC agregado:

- `src/test/java/com/panol_project/backendpanol/api/CategoriaActiveEndpointSecurityTest.java`

Comando (evita codegen jOOQ en test):

```bash
./mvnw -Djooq.codegen.skip=true test
```

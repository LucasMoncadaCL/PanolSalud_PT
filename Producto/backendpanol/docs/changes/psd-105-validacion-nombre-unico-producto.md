# PSD-105 - Validacion de nombre unico de producto (Subtarea 2)

Fecha: 2026-04-27

## Resumen

Se implemento la validacion de nombre unico de producto en backend para `implement`, considerando comparacion case-insensitive y solo sobre implementos activos.

La solucion aplica en dos niveles:

- Validacion previa en `ImplementService` antes de persistir (linea principal).
- Captura de `DataIntegrityViolationException` como fallback ante carrera concurrente (segunda linea de defensa).

En ambos casos, la API responde de forma controlada con `400 Bad Request`, codigo de negocio `IMPLEMENT_NAME_DUPLICATE` y mensaje:

`Ya existe un producto con el nombre '{nombre}'`

## Cambios funcionales

### 1) Validacion previa en Service

Se agrego validacion previa para detectar duplicados activos por nombre (case-insensitive):

- En `crear(...)`: valida si ya existe un implemento activo con el mismo nombre.
- En `editar(...)`: valida si existe otro implemento activo con el mismo nombre, excluyendo el `id` en edicion.

Si detecta duplicado, lanza `BadRequestException` con codigo `IMPLEMENT_NAME_DUPLICATE`.

### 2) Fallback por constraint unico

Se mantiene la captura de `DataIntegrityViolationException` (SQLState `23505`) para cubrir escenarios concurrentes.

Ante esta condicion tambien se retorna la misma excepcion de negocio (`BadRequestException`) con el mismo codigo y mensaje controlado.

## Cambios tecnicos

### Contrato de repositorio

Se agregaron nuevos metodos en `ImplementRepository`:

- `existsActiveByNameIgnoreCase(String nombre)`
- `existsActiveByNameIgnoreCaseAndIdNot(String nombre, Integer excludedId)`

### Implementacion jOOQ

En `ImplementJooqRepository` se implementaron ambos metodos con `SELECT EXISTS`, usando:

- Filtro por `IMPLEMENT.ACTIVE = true`
- Comparacion `lower(IMPLEMENT.NAME) = lower(nombre)`
- `Locale.ROOT` para normalizacion estable de `toLowerCase`

### Servicio de aplicacion

En `ImplementService`:

- Se extrajo la regla de negocio de duplicado en metodos privados (`validateUniqueActiveNameForCreate`, `validateUniqueActiveNameForUpdate`).
- Se unifico la construccion de error en `duplicateNameException(...)` para mantener mensaje/codigo consistentes.
- Se migro de `ConflictException` a `BadRequestException` en duplicados de nombre.

## Pruebas agregadas/actualizadas

Archivo: `src/test/java/com/panol_project/backendpanol/modules/catalog/implement/application/ImplementServiceTest.java`

Coberturas incluidas:

- Falla en creacion cuando ya existe nombre activo (case-insensitive a nivel de consulta).
- Falla en edicion cuando existe otro implemento activo con el mismo nombre.
- Fallback por constraint unico (`23505`) retornando `BadRequestException` con mensaje controlado.

## Archivos modificados

- `src/main/java/com/panol_project/backendpanol/modules/catalog/implement/domain/ImplementRepository.java`
- `src/main/java/com/panol_project/backendpanol/modules/catalog/implement/infrastructure/ImplementJooqRepository.java`
- `src/main/java/com/panol_project/backendpanol/modules/catalog/implement/application/ImplementService.java`
- `src/test/java/com/panol_project/backendpanol/modules/catalog/implement/application/ImplementServiceTest.java`

## Como validar manualmente

1. Crear un implemento con nombre nuevo -> `201 Created`.
2. Intentar crear otro con mismo nombre en distinto casing -> `400` con `IMPLEMENT_NAME_DUPLICATE`.
3. Editar un implemento para usar nombre de otro implemento activo -> `400` con `IMPLEMENT_NAME_DUPLICATE`.
4. Editar un implemento manteniendo su propio nombre -> no debe fallar por duplicado.

## Notas

- La validacion se centra en implementos activos, segun la subtarea.
- La segunda linea de defensa evita exponer errores SQL crudos incluso bajo concurrencia.

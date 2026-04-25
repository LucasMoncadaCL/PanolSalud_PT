# Arquitectura Backend (Monolito Modular)

Este backend sigue un enfoque de monolito modular para facilitar una futura migracion a microservicios.

## Capas principales

- `bootstrap`: configuracion de arranque (security, wiring global).
- `modules`: dominio funcional dividido por modulos de negocio.
- `shared`: utilidades transversales y tipos comunes de error/API.
- `jooq` (generado en `target`): metamodelo SQL de la base relacional.

## Reglas de dependencia

1. `modules/*` no debe depender de `bootstrap/*`.
2. Los modulos se comunican por contratos (services/interfaces/eventos), evitando acceso directo arbitrario entre modulos.
3. `shared/*` no debe contener logica de dominio; solo cross-cutting estable.
4. El acceso a BD de cada modulo debe quedar dentro de su carpeta `infrastructure`.

## Modulos actuales

- `catalog/category`

## Proxima expansion sugerida

- `catalog/implement`
- `inventory`
- `loans`
- `identity-access`
- `locations`
- `reporting`

## Migracion futura a microservicios

Cada modulo puede separarse progresivamente extrayendo:

- API del modulo
- logica de aplicacion
- repositorios y contratos de persistencia

manteniendo contratos HTTP/eventos estables entre componentes.

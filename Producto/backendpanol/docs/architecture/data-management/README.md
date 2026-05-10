# Gestión de Datos: Guía Operativa

Esta carpeta define cómo gestionar el modelo híbrido (PostgreSQL + MongoDB), qué hace cada capa y qué decisiones técnicas aplicar para escalar con seguridad.

## Contenido

1. [01-flujo-end-to-end.md](./01-flujo-end-to-end.md)
   Flujo completo desde request HTTP hasta persistencia, eventos y lectura.

2. [02-responsabilidades-por-capa.md](./02-responsabilidades-por-capa.md)
   Límites claros entre backend, PostgreSQL y MongoDB.

3. [03-postgresql-guia-tecnica.md](./03-postgresql-guia-tecnica.md)
   Procedimientos/funciones, índices, triggers, constraints, transacciones y mantenimiento.

4. [04-mongodb-guia-tecnica.md](./04-mongodb-guia-tecnica.md)
   Colecciones, índices, políticas de retención, modelo append-only y consistencia con SQL.

5. [05-backend-integracion-datos.md](./05-backend-integracion-datos.md)
   Cómo orquestar ambos motores desde backend (casos de uso, idempotencia, errores, observabilidad).

6. [06-runbook-operacional-datos.md](./06-runbook-operacional-datos.md)
   Checklist operativo: despliegues, validaciones, backups, incidentes y recuperación.

7. [07-aligerar-sql-prestamos.md](./07-aligerar-sql-prestamos.md)
   Estrategia de particionado, archivado y retencion para tablas de prestamos.

8. [08-flujo-migraciones-flyway.md](./08-flujo-migraciones-flyway.md)
   Flujo estándar para diseñar, revisar, desplegar y validar migraciones SQL con Flyway.

9. [09-uuid-cutover-backup-rollback.md](./09-uuid-cutover-backup-rollback.md)
   Runbook operativo para backup, despliegue y rollback del corte UUID-only.

10. [10-inventario-deuda-id-numerico.md](./10-inventario-deuda-id-numerico.md)
   Inventario de endpoints/componentes que aún usan IDs numéricos.

---

## Convenciones

- **Fuente de verdad relacional**: PostgreSQL (`user`, `role`, `implement`, `stock`, `loan`, etc.).
- **Eventos, trazabilidad y notificaciones**: MongoDB (`inventory_movements`, `loan_events`, `audit_logs`, `notifications`, `stock_alerts`).
- **Migraciones SQL**: solo forward-only (`Vn__*.sql`) con Flyway.
- **Esquema Mongo**: controlado por aplicación + validaciones y versiones de documento.


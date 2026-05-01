package com.panol_project.backendpanol.modules.catalog.implement.infrastructure;

import static com.panol_project.backendpanol.jooq.tables.Category.CATEGORY;
import static com.panol_project.backendpanol.jooq.tables.Implement.IMPLEMENT;
import static com.panol_project.backendpanol.jooq.tables.Location.LOCATION;
import static com.panol_project.backendpanol.jooq.tables.Stock.STOCK;

import com.panol_project.backendpanol.jooq.enums.ItemTypeEnum;
import com.panol_project.backendpanol.jooq.tables.records.ImplementRecord;
import com.panol_project.backendpanol.modules.catalog.implement.domain.ImplementItemType;
import com.panol_project.backendpanol.modules.catalog.implement.domain.ImplementRepository;
import com.panol_project.backendpanol.modules.catalog.implement.domain.ImplementCategorySummary;
import com.panol_project.backendpanol.modules.catalog.implement.domain.ImplementLocationSummary;
import com.panol_project.backendpanol.modules.catalog.implement.domain.ImplementSummary;
import com.panol_project.backendpanol.modules.catalog.implement.domain.ImplementStockSummary;
import com.panol_project.backendpanol.modules.catalog.implement.domain.Implemento;
import com.panol_project.backendpanol.modules.catalog.implement.domain.StockStatusFilter;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

@Repository
public class ImplementJooqRepository implements ImplementRepository {

    private final DSLContext dsl;
    private static final Field<String> IMPLEMENT_OBSERVATIONS = DSL.field(DSL.name("observations"), String.class);
    private static final Field<String> IMPLEMENT_BARCODE = DSL.field(DSL.name("barcode"), String.class);
    private static final Field<String> IMPLEMENT_IMG_URL = DSL.field(DSL.name("img_url"), String.class);

    public ImplementJooqRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Optional<Implemento> findById(Integer id) {
        return dsl.selectFrom(IMPLEMENT)
                .where(IMPLEMENT.ID.eq(id))
                .fetchOptional()
                .map(this::toDomain);
    }

    @Override
    public Optional<ImplementSummary> findSummaryById(Integer id) {
        return dsl.select(
                        IMPLEMENT.ID,
                        IMPLEMENT.NAME,
                        IMPLEMENT.DESCRIPTION,
                        IMPLEMENT_BARCODE,
                        IMPLEMENT_IMG_URL,
                        IMPLEMENT.ACTIVE,
                        CATEGORY.ID,
                        CATEGORY.NAME,
                        CATEGORY.ACTIVE,
                        LOCATION.ID,
                        LOCATION.NAME,
                        LOCATION.DESCRIPTION,
                        STOCK.TOTAL_STOCK,
                        STOCK.MIN_STOCK,
                        STOCK.AVAILABLE,
                        STOCK.RESERVED,
                        STOCK.LOANED,
                        STOCK.DAMAGED
                )
                .from(IMPLEMENT)
                .leftJoin(CATEGORY).on(CATEGORY.ID.eq(IMPLEMENT.CATEGORY_ID))
                .join(LOCATION).on(LOCATION.ID.eq(IMPLEMENT.LOCATION_ID))
                .leftJoin(STOCK).on(STOCK.IMPLEMENT_ID.eq(IMPLEMENT.ID))
                .where(IMPLEMENT.ID.eq(id))
                .fetchOptional(record -> {
                    Integer categoryId = record.get(CATEGORY.ID);
                    ImplementCategorySummary category = categoryId == null
                            ? null
                            : new ImplementCategorySummary(
                                    categoryId,
                                    record.get(CATEGORY.NAME),
                                    record.get(CATEGORY.ACTIVE)
                            );

                    return new ImplementSummary(
                            record.get(IMPLEMENT.ID),
                            record.get(IMPLEMENT.NAME),
                            record.get(IMPLEMENT.DESCRIPTION),
                            record.get(IMPLEMENT_BARCODE),
                            record.get(IMPLEMENT_IMG_URL),
                            record.get(IMPLEMENT.ACTIVE),
                            category,
                            new ImplementLocationSummary(
                                    record.get(LOCATION.ID),
                                    record.get(LOCATION.NAME),
                                    record.get(LOCATION.DESCRIPTION)
                            ),
                            new ImplementStockSummary(
                                    record.get(STOCK.TOTAL_STOCK),
                                    record.get(STOCK.MIN_STOCK),
                                    record.get(STOCK.AVAILABLE),
                                    record.get(STOCK.RESERVED),
                                    record.get(STOCK.LOANED),
                                    record.get(STOCK.DAMAGED)
                            )
                    );
                });
    }

    @Override
    public List<ImplementSummary> findAllSummaries(String name, Integer categoryId, StockStatusFilter stockStatusFilter) {
        Condition condition = IMPLEMENT.ACTIVE.isTrue();

        if (name != null) {
            condition = condition.and(DSL.lower(IMPLEMENT.NAME).like("%" + name.toLowerCase(Locale.ROOT) + "%"));
        }

        if (categoryId != null) {
            condition = condition.and(IMPLEMENT.CATEGORY_ID.eq(categoryId));
        }

        if (stockStatusFilter != null) {
            Field<Integer> stockField = resolveStockField(stockStatusFilter);
            condition = condition.and(stockField.gt(0));
        }

        return dsl.select(
                        IMPLEMENT.ID,
                        IMPLEMENT.NAME,
                        IMPLEMENT.DESCRIPTION,
                        IMPLEMENT_BARCODE,
                        IMPLEMENT_IMG_URL,
                        IMPLEMENT.ACTIVE,
                        CATEGORY.ID,
                        CATEGORY.NAME,
                        CATEGORY.ACTIVE,
                        LOCATION.ID,
                        LOCATION.NAME,
                        LOCATION.DESCRIPTION,
                        STOCK.TOTAL_STOCK,
                        STOCK.MIN_STOCK,
                        STOCK.AVAILABLE,
                        STOCK.RESERVED,
                        STOCK.LOANED,
                        STOCK.DAMAGED
                )
                .from(IMPLEMENT)
                .leftJoin(CATEGORY).on(CATEGORY.ID.eq(IMPLEMENT.CATEGORY_ID))
                .join(LOCATION).on(LOCATION.ID.eq(IMPLEMENT.LOCATION_ID))
                .leftJoin(STOCK).on(STOCK.IMPLEMENT_ID.eq(IMPLEMENT.ID))
                .where(condition)
                .orderBy(IMPLEMENT.NAME.asc())
                .fetch(record -> {
                    Integer summaryCategoryId = record.get(CATEGORY.ID);
                    ImplementCategorySummary category = summaryCategoryId == null
                            ? null
                            : new ImplementCategorySummary(
                                    summaryCategoryId,
                                    record.get(CATEGORY.NAME),
                                    record.get(CATEGORY.ACTIVE)
                            );

                    return new ImplementSummary(
                            record.get(IMPLEMENT.ID),
                            record.get(IMPLEMENT.NAME),
                            record.get(IMPLEMENT.DESCRIPTION),
                            record.get(IMPLEMENT_BARCODE),
                            record.get(IMPLEMENT_IMG_URL),
                            record.get(IMPLEMENT.ACTIVE),
                            category,
                            new ImplementLocationSummary(
                                    record.get(LOCATION.ID),
                                    record.get(LOCATION.NAME),
                                    record.get(LOCATION.DESCRIPTION)
                            ),
                            new ImplementStockSummary(
                                    record.get(STOCK.TOTAL_STOCK),
                                    record.get(STOCK.MIN_STOCK),
                                    record.get(STOCK.AVAILABLE),
                                    record.get(STOCK.RESERVED),
                                    record.get(STOCK.LOANED),
                                    record.get(STOCK.DAMAGED)
                            )
                    );
                });
    }

    @Override
    public boolean existsActiveByNameIgnoreCase(String nombre) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(IMPLEMENT)
                        .where(IMPLEMENT.ACTIVE.isTrue()
                                .and(IMPLEMENT.NAME.likeIgnoreCase(nombre)))
        );
    }

    @Override
    public boolean existsActiveByNameIgnoreCaseAndIdNot(String nombre, Integer excludedId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(IMPLEMENT)
                        .where(IMPLEMENT.ACTIVE.isTrue()
                                .and(IMPLEMENT.ID.ne(excludedId))
                                .and(IMPLEMENT.NAME.likeIgnoreCase(nombre)))
        );
    }

    @Override
    public Implemento create(
            String nombre,
            String descripcion,
            Integer categoriaId,
            Integer locationId,
            ImplementItemType itemType,
            String barcode,
            String imgUrl,
            String observations
    ) {
        return dsl.insertInto(IMPLEMENT)
                .set(IMPLEMENT.NAME, nombre)
                .set(IMPLEMENT.DESCRIPTION, descripcion)
                .set(IMPLEMENT.CATEGORY_ID, categoriaId)
                .set(IMPLEMENT.LOCATION_ID, locationId)
                .set(IMPLEMENT.ITEM_TYPE, toJooqItemType(itemType))
                .set(IMPLEMENT_BARCODE, barcode)
                .set(IMPLEMENT_IMG_URL, imgUrl)
                .set(IMPLEMENT_OBSERVATIONS, observations)
                .returning()
                .fetchOptional()
                .map(this::toDomain)
                .orElseThrow();
    }

    @Override
    public Implemento update(
            Integer id,
            String nombre,
            String descripcion,
            Integer categoriaId,
            Integer locationId,
            ImplementItemType itemType,
            String barcode,
            String imgUrl,
            String observations
    ) {
        return dsl.update(IMPLEMENT)
                .set(IMPLEMENT.NAME, nombre)
                .set(IMPLEMENT.DESCRIPTION, descripcion)
                .set(IMPLEMENT.CATEGORY_ID, categoriaId)
                .set(IMPLEMENT.LOCATION_ID, locationId)
                .set(IMPLEMENT.ITEM_TYPE, toJooqItemType(itemType))
                .set(IMPLEMENT_BARCODE, barcode)
                .set(IMPLEMENT_IMG_URL, imgUrl)
                .set(IMPLEMENT_OBSERVATIONS, observations)
                .set(IMPLEMENT.UPDATED_AT, OffsetDateTime.now())
                .where(IMPLEMENT.ID.eq(id))
                .returning()
                .fetchOptional()
                .map(this::toDomain)
                .orElseThrow();
    }

    @Override
    public int updateMinStockByImplementId(Integer implementId, Integer minStock) {
        return dsl.insertInto(STOCK)
                .set(STOCK.IMPLEMENT_ID, implementId)
                .set(STOCK.MIN_STOCK, minStock)
                .onConflict(STOCK.IMPLEMENT_ID)
                .doUpdate()
                .set(STOCK.MIN_STOCK, minStock)
                .execute();
    }

    @Override
    public Optional<Integer> findMinStockByImplementId(Integer implementId) {
        return dsl.select(STOCK.MIN_STOCK)
                .from(STOCK)
                .where(STOCK.IMPLEMENT_ID.eq(implementId))
                .fetchOptional(STOCK.MIN_STOCK);
    }

    @Override
    public int updateActive(Integer id, boolean active) {
        return dsl.update(IMPLEMENT)
                .set(IMPLEMENT.ACTIVE, active)
                .set(IMPLEMENT.UPDATED_AT, OffsetDateTime.now())
                .where(IMPLEMENT.ID.eq(id))
                .execute();
    }

    /**
     * Resuelve el campo jOOQ de la tabla STOCK que corresponde al filtro de estado solicitado.
     * Permite aplicar la condición {@code WHERE stock.{campo} > 0} de forma type-safe.
     */
    private Field<Integer> resolveStockField(StockStatusFilter filter) {
        return switch (filter) {
            case AVAILABLE -> STOCK.AVAILABLE;
            case RESERVED -> STOCK.RESERVED;
            case LOANED -> STOCK.LOANED;
            case DAMAGED -> STOCK.DAMAGED;
            // TODO: Deuda técnica — la columna 'blocked' no está incluida en el codegen de jOOQ actual.
            //  Se usa DSL.field() dinámico como workaround. Cuando se regenere el codegen (ej: tras
            //  agregar la columna al schema), reemplazar por STOCK.BLOCKED (field tipado) para
            //  garantizar type-safety en tiempo de compilación.
            //  Ref: PSD-25 observación de deuda técnica aprobada 2026-05-01.
            case BLOCKED -> DSL.field(DSL.name("stock", "blocked"), Integer.class);
        };
    }

    private Implemento toDomain(ImplementRecord record) {
        String observations = null;
        String barcode = null;
        String imgUrl = null;
        try {
            observations = record.get(IMPLEMENT_OBSERVATIONS);
            barcode = record.get(IMPLEMENT_BARCODE);
            imgUrl = record.get(IMPLEMENT_IMG_URL);
        } catch (IllegalArgumentException ignored) {
            // Cuando el codegen de jOOQ no incluye la columna "observations" en ImplementRecord,
            // el record no contiene ese Field y record.get(Field) lanza IllegalArgumentException.
            // En ese caso, exponemos observations como null para evitar 500.
        }

        return new Implemento(
                record.getId(),
                record.getName(),
                record.getDescription(),
                record.getCategoryId(),
                record.getLocationId(),
                toDomainItemType(record.getItemType()),
                barcode,
                imgUrl,
                observations,
                record.getActive(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }

    private ImplementItemType toDomainItemType(ItemTypeEnum itemType) {
        return itemType == null
                ? null
                : ImplementItemType.fromLiteral(itemType.getLiteral()).orElse(null);
    }

    private ItemTypeEnum toJooqItemType(ImplementItemType itemType) {
        return ItemTypeEnum.lookupLiteral(Objects.requireNonNull(itemType, "itemType").literal());
    }
}

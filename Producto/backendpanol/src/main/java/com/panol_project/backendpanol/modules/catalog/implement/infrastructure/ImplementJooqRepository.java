package com.panol_project.backendpanol.modules.catalog.implement.infrastructure;

import static com.panol_project.backendpanol.jooq.tables.Category.CATEGORY;
import static com.panol_project.backendpanol.jooq.tables.Implement.IMPLEMENT;
import static com.panol_project.backendpanol.jooq.tables.Location.LOCATION;

import com.panol_project.backendpanol.jooq.tables.records.ImplementRecord;
import com.panol_project.backendpanol.modules.catalog.implement.domain.ImplementRepository;
import com.panol_project.backendpanol.modules.catalog.implement.domain.ImplementCategorySummary;
import com.panol_project.backendpanol.modules.catalog.implement.domain.ImplementLocationSummary;
import com.panol_project.backendpanol.modules.catalog.implement.domain.ImplementSummary;
import com.panol_project.backendpanol.modules.catalog.implement.domain.Implemento;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

@Repository
public class ImplementJooqRepository implements ImplementRepository {

    private final DSLContext dsl;

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
    public List<ImplementSummary> findAllSummaries() {
        return dsl.select(
                        IMPLEMENT.ID,
                        IMPLEMENT.NAME,
                        CATEGORY.ID,
                        CATEGORY.NAME,
                        CATEGORY.ACTIVE,
                        LOCATION.ID,
                        LOCATION.NAME,
                        LOCATION.DESCRIPTION
                )
                .from(IMPLEMENT)
                .leftJoin(CATEGORY).on(CATEGORY.ID.eq(IMPLEMENT.CATEGORY_ID))
                .join(LOCATION).on(LOCATION.ID.eq(IMPLEMENT.LOCATION_ID))
                .orderBy(IMPLEMENT.ID.asc())
                .fetch(record -> {
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
                            category,
                            new ImplementLocationSummary(
                                    record.get(LOCATION.ID),
                                    record.get(LOCATION.NAME),
                                    record.get(LOCATION.DESCRIPTION)
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
                                .and(DSL.lower(IMPLEMENT.NAME).eq(nombre.toLowerCase(Locale.ROOT))))
        );
    }

    @Override
    public boolean existsActiveByNameIgnoreCaseAndIdNot(String nombre, Integer excludedId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(IMPLEMENT)
                        .where(IMPLEMENT.ACTIVE.isTrue()
                                .and(IMPLEMENT.ID.ne(excludedId))
                                .and(DSL.lower(IMPLEMENT.NAME).eq(nombre.toLowerCase(Locale.ROOT))))
        );
    }

    @Override
    public Implemento create(String nombre, String descripcion, Integer categoriaId, Integer locationId) {
        return dsl.insertInto(IMPLEMENT)
                .set(IMPLEMENT.NAME, nombre)
                .set(IMPLEMENT.DESCRIPTION, descripcion)
                .set(IMPLEMENT.CATEGORY_ID, categoriaId)
                .set(IMPLEMENT.LOCATION_ID, locationId)
                .returning()
                .fetchOptional()
                .map(this::toDomain)
                .orElseThrow();
    }

    @Override
    public Implemento update(Integer id, String nombre, String descripcion, Integer categoriaId, Integer locationId) {
        return dsl.update(IMPLEMENT)
                .set(IMPLEMENT.NAME, nombre)
                .set(IMPLEMENT.DESCRIPTION, descripcion)
                .set(IMPLEMENT.CATEGORY_ID, categoriaId)
                .set(IMPLEMENT.LOCATION_ID, locationId)
                .set(IMPLEMENT.UPDATED_AT, OffsetDateTime.now())
                .where(IMPLEMENT.ID.eq(id))
                .returning()
                .fetchOptional()
                .map(this::toDomain)
                .orElseThrow();
    }

    private Implemento toDomain(ImplementRecord record) {
        return new Implemento(
                record.getId(),
                record.getName(),
                record.getDescription(),
                record.getCategoryId(),
                record.getLocationId(),
                record.getActive(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}

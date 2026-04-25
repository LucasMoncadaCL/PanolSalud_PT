package com.panol_project.backendpanol.modules.catalog.category.infrastructure;

import static com.panol_project.backendpanol.jooq.tables.Category.CATEGORY;
import static com.panol_project.backendpanol.jooq.tables.Implement.IMPLEMENT;
import static org.jooq.impl.DSL.lower;
import static org.jooq.impl.DSL.noCondition;

import com.panol_project.backendpanol.jooq.tables.records.CategoryRecord;
import com.panol_project.backendpanol.modules.catalog.category.domain.CategoriaResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
public class CategoriaRepository {

    private final DSLContext dsl;

    public CategoriaRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<CategoriaResponse> findAll(boolean includeInactive) {
        Condition condition = includeInactive ? noCondition() : CATEGORY.ACTIVE.isTrue();

        return dsl.selectFrom(CATEGORY)
                .where(condition)
                .orderBy(CATEGORY.NAME.asc())
                .fetch(this::toResponse);
    }

    public List<CategoriaResponse> findOnlyActive() {
        return findAll(false);
    }

    public Optional<CategoryRecord> findById(Integer id) {
        return dsl.selectFrom(CATEGORY)
                .where(CATEGORY.ID.eq(id))
                .fetchOptional();
    }

    public boolean existsByNombre(String nombre, Integer excludingId) {
        Condition condition = lower(CATEGORY.NAME).eq(nombre.toLowerCase(Locale.ROOT));
        if (excludingId != null) {
            condition = condition.and(CATEGORY.ID.ne(excludingId));
        }

        return dsl.fetchExists(dsl.selectOne().from(CATEGORY).where(condition));
    }

    public Optional<CategoryRecord> findActiveById(Integer id) {
        return dsl.selectFrom(CATEGORY)
                .where(CATEGORY.ID.eq(id).and(CATEGORY.ACTIVE.isTrue()))
                .fetchOptional();
    }

    public CategoriaResponse create(String nombre) {
        return dsl.insertInto(CATEGORY)
                .set(CATEGORY.NAME, nombre)
                .set(CATEGORY.ACTIVE, true)
                .set(CATEGORY.CREATED_AT, OffsetDateTime.now())
                .returning()
                .fetchOptional()
                .map(this::toResponse)
                .orElseThrow();
    }

    public CategoriaResponse updateNombre(Integer id, String nombre) {
        return dsl.update(CATEGORY)
                .set(CATEGORY.NAME, nombre)
                .where(CATEGORY.ID.eq(id))
                .returning()
                .fetchOptional()
                .map(this::toResponse)
                .orElseThrow();
    }

    public void deactivate(Integer id) {
        dsl.update(CATEGORY)
                .set(CATEGORY.ACTIVE, false)
                .where(CATEGORY.ID.eq(id))
                .execute();
    }

    public void deleteById(Integer id) {
        dsl.deleteFrom(CATEGORY)
                .where(CATEGORY.ID.eq(id))
                .execute();
    }

    public int countImplementsByCategoryId(Integer categoryId) {
        return dsl.fetchCount(
                dsl.selectOne()
                        .from(IMPLEMENT)
                        .where(IMPLEMENT.CATEGORY_ID.eq(categoryId))
        );
    }

    public int countActiveImplementsByCategoryId(Integer categoryId) {
        return dsl.fetchCount(
                dsl.selectOne()
                        .from(IMPLEMENT)
                        .where(IMPLEMENT.CATEGORY_ID.eq(categoryId)
                                .and(IMPLEMENT.ACTIVE.isTrue()))
        );
    }

    public CategoriaResponse toResponse(CategoryRecord record) {
        return new CategoriaResponse(
                record.getId(),
                record.getName(),
                record.getActive(),
                record.getCreatedAt(),
                null
        );
    }
}

package com.panol_project.backendpanol.modules.users.infrastructure;

import com.panol_project.backendpanol.modules.users.domain.UserRepository;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.panol_project.backendpanol.jooq.tables.User.USER;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;

@Repository
public class UserJooqRepository implements UserRepository {

    private final DSLContext dsl;

    public UserJooqRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Map<Integer, String> findNamesByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }

        return dsl.select(USER.ID, USER.NAME)
                .from(USER)
                .where(USER.ID.in(ids))
                .fetchMap(USER.ID, USER.NAME);
    }

    @Override
    public Map<UUID, String> findNamesByUuids(List<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return Map.of();
        }

        var userUuidField = field(name("uuid"), UUID.class);

        return dsl.select(userUuidField, USER.NAME)
                .from(USER)
                .where(userUuidField.in(uuids))
                .fetchMap(userUuidField, USER.NAME);
    }
}

package com.panol_project.backendpanol.modules.users.domain;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface UserRepository {
    Map<Integer, String> findNamesByIds(List<Integer> ids);
    Map<UUID, String> findNamesByUuids(List<UUID> uuids);
}

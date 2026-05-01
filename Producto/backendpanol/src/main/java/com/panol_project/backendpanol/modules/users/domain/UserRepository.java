package com.panol_project.backendpanol.modules.users.domain;

import java.util.List;
import java.util.Map;

public interface UserRepository {
    Map<Integer, String> findNamesByIds(List<Integer> ids);
}

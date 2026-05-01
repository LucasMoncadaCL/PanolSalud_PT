package com.panol_project.backendpanol.modules.users.application;

import com.panol_project.backendpanol.modules.users.domain.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public Map<Integer, String> getNombresUsuarios(List<Integer> userIds) {
        return repository.findNamesByIds(userIds);
    }
}

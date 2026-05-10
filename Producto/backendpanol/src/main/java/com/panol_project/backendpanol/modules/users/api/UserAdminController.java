package com.panol_project.backendpanol.modules.users.api;

import com.panol_project.backendpanol.modules.users.api.dto.ChangeRoleRequest;
import com.panol_project.backendpanol.modules.users.api.dto.CreateUserRequest;
import com.panol_project.backendpanol.modules.users.api.dto.UpdateUserRequest;
import com.panol_project.backendpanol.modules.users.api.dto.UserAdminSummaryResponse;
import com.panol_project.backendpanol.modules.users.application.UserAdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/legacy/api/v1/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    @PreAuthorize("hasRole('DIRECTOR')")
    List<UserAdminSummaryResponse> listUsers() {
        return userAdminService.listUsers();
    }

    @PostMapping
    @PreAuthorize("hasRole('DIRECTOR')")
    ResponseEntity<Void> createUser(@Valid @RequestBody CreateUserRequest request, @AuthenticationPrincipal Jwt jwt) {
        userAdminService.createUser(request, jwt);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('DIRECTOR')")
    ResponseEntity<Void> changeRole(
            @PathVariable String userId,
            @Valid @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        userAdminService.changeRole(userId, request.role(), jwt);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('DIRECTOR')")
    ResponseEntity<Void> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        userAdminService.updateUser(userId, request, jwt);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/active")
    @PreAuthorize("hasRole('DIRECTOR')")
    ResponseEntity<Void> setActive(
            @PathVariable String userId,
            @RequestParam boolean active,
            @AuthenticationPrincipal Jwt jwt
    ) {
        userAdminService.setActive(userId, active, jwt);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('DIRECTOR')")
    ResponseEntity<Void> deleteUser(
            @PathVariable String userId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        userAdminService.deleteUser(userId, jwt);
        return ResponseEntity.noContent().build();
    }
}


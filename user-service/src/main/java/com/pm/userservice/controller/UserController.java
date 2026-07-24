package com.pm.userservice.controller;

import com.pm.userservice.DTO.UserResponseDTO;
import com.pm.userservice.DTO.UserUpdateRequestDTO;
import com.pm.userservice.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMe(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(userService.getById(userId));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponseDTO> updateMe(@AuthenticationPrincipal UUID userId,
                                                 @Valid @RequestBody UserUpdateRequestDTO request) {
        return ResponseEntity.ok(userService.update(userId, request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal UUID userId) {
        userService.delete(userId);
        return ResponseEntity.noContent().build();
    }
}